import os
import time
import json
import requests
import mysql.connector

MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_DB = os.getenv("MYSQL_DB", "itinventory_equip")
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASS = os.getenv("MYSQL_PASSWORD", "root")

ES_HOST = os.getenv("ELASTIC_HOST", "http://localhost:9200")
INDEX = os.getenv("ELASTIC_INDEX", "itinventory-equipments")


def esperar_dependencias(segundos: int = 5):
    print(f" Aguardando {segundos}s para MySQL e Elasticsearch subirem...")
    time.sleep(segundos)


def criar_indice():
    mapping = {
        "mappings": {
            "properties": {
                "idEquipment": {"type": "long"},

                "type": {"type": "keyword"},
                "brand": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
                "model": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
                "assetNumber": {"type": "keyword"},
                "status": {"type": "keyword"},
                "location": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},
                "responsible": {"type": "text", "fields": {"keyword": {"type": "keyword"}}},

                "acquisitionDate": {"type": "date"},
                "acquisitionValue": {"type": "double"},

                "ativo": {"type": "boolean"},
                "criadoEm": {"type": "date"},
                "atualizadoEm": {"type": "date"},

                "categoriaId": {"type": "long"},
                "filialAtualId": {"type": "long"},
                "fornecedorId": {"type": "long"}
            }
        }
    }

    url = f"{ES_HOST}/{INDEX}"
    print(f"📌 Criando índice '{INDEX}' em {url} ...")
    resp = requests.put(url, json=mapping)

    if resp.status_code in (200, 201):
        print(" Índice criado.")
    elif resp.status_code == 400 and "resource_already_exists_exception" in resp.text:
        print("Índice já existe.")
    else:
        print(f"Falha ao criar índice ({resp.status_code}): {resp.text[:1000]}")


def conectar_mysql():
    print(f" Conectando ao MySQL em {MYSQL_HOST} / DB {MYSQL_DB} ...")
    conn = mysql.connector.connect(
        host=MYSQL_HOST,
        database=MYSQL_DB,
        user=MYSQL_USER,
        password=MYSQL_PASS
    )
    print("✅ Conexão MySQL OK.")
    return conn


def carregar_equipments(conn):
    sql = """
          SELECT
              id_equipment,
              `type`,
              brand,
              model,
              asset_number,
              status,
              location,
              responsible,
              acquisition_date,
              acquisition_value,
              ativo,
              criado_em,
              atualizado_em,
              id_categoria,
              id_filial_atual,
              id_fornecedor
          FROM equipment
          WHERE ativo = 1; \
          """
    cur = conn.cursor()
    cur.execute(sql)
    rows = cur.fetchall()
    cur.close()
    print(f" Carregados {len(rows)} equipments ATIVOS do MySQL.")
    return rows


def montar_bulk(rows):
    """
    Formato NDJSON:
    { "index": { "_index": "...", "_id": "..." } }
    { ...doc... }
    """
    lines = []

    for (
            id_equipment, type_, brand, model, asset_number, status, location, responsible,
            acq_date, acq_value, ativo, criado_em, atualizado_em,
            id_categoria, id_filial_atual, id_fornecedor
    ) in rows:

        doc = {
            "idEquipment": int(id_equipment),
            "type": type_,
            "brand": brand,
            "model": model,
            "assetNumber": asset_number,
            "status": status,
            "location": location,
            "responsible": responsible,
            "acquisitionDate": acq_date.isoformat() if acq_date else None,
            "acquisitionValue": float(acq_value) if acq_value is not None else None,

            "ativo": bool(ativo),
            "criadoEm": criado_em.isoformat() if criado_em else None,
            "atualizadoEm": atualizado_em.isoformat() if atualizado_em else None,

            "categoriaId": int(id_categoria) if id_categoria is not None else None,
            "filialAtualId": int(id_filial_atual) if id_filial_atual is not None else None,
            "fornecedorId": int(id_fornecedor) if id_fornecedor is not None else None
        }

        meta = {"index": {"_index": INDEX, "_id": str(id_equipment)}}
        lines.append(json.dumps(meta, ensure_ascii=False))
        lines.append(json.dumps(doc, ensure_ascii=False))

    return "\n".join(lines) + "\n"


def enviar_bulk(body: str):
    url = f"{ES_HOST}/_bulk"
    print(f" Enviando bulk para {url} ... (bytes: {len(body)})")
    resp = requests.post(url, data=body, headers={"Content-Type": "application/x-ndjson"})

    print(f" Bulk status: {resp.status_code}")
    if resp.status_code >= 300:
        print(resp.text[:2000])
        return

    payload = resp.json()
    if payload.get("errors"):
        print("️ Bulk concluiu com erros. Trecho:")
        print(resp.text[:2000])
    else:
        print(" Bulk indexado sem erros.")


def main():
    esperar_dependencias(5)
    criar_indice()

    conn = conectar_mysql()
    try:
        rows = carregar_equipments(conn)
        if not rows:
            print(" Nenhum equipment ATIVO encontrado. Encerrando.")
            return

        bulk_body = montar_bulk(rows)
        enviar_bulk(bulk_body)
        print(" ETL concluído.")
    finally:
        conn.close()
        print("🔌 Conexão MySQL encerrada.")


if __name__ == "__main__":
    main()
