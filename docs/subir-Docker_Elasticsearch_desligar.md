# Passos para subir Docker + Elasticsearch (e desligar)

Este guia é focado no seu cenário atual:

- Elasticsearch exposto em `http://localhost:9200`
- (Opcional) Kibana em `http://localhost:5601`
- Infra localizada em `itinventory_equip/docker/infra` (ajuste o caminho se necessário)

---

Rode a aplicacäo Run -itinventoryEquipApplication - no Intellig
---

## 1) Pré-requisitos (uma vez)

### 1.1) Subir o Docker Desktop
No macOS, o Docker precisa estar **rodando** (Docker Desktop aberto).

Teste:
```bash
docker info
```

Se aparecer erro “Cannot connect to the Docker daemon…”, abra o **Docker Desktop** e rode novamente.

---

## 2) Subir Elasticsearch (via Docker Compose)

### 2.1) Ir até a pasta do compose
Exemplo (ajuste para o seu caminho real):
```bash
cd /Users/claudioalmeida/dev/Desenvolvimento_Full_Stack_com_React_e_Spring_Boot_25E4_3/itinventory_equip/itinventory_equip/docker/infra
```

### 2.2) Subir em background
```bash
docker compose up -d
```
 
### 2.3) Ver status e portas
```bash
docker compose ps
```
Você deve ver algo como:
- Esperado no projeto desenvolvimento:
- itinv_mysql healthy
- itinv_elasticsearch healthy
- itinv_kibana up ou Running
- itinv_etl Started

### 2.4) Acompanhar logs do Elasticsearch
```bash
docker compose logs -f --tail=200 elasticsearch
```

---

## 3) Validar que o Elasticsearch está “no ar”

### 3.1) Health do cluster
```bash
curl -s "http://localhost:9200/_cluster/health?pretty"
```

### 3.2) Listar índices
```bash
curl -s "http://localhost:9200/_cat/indices?v"
```

Se o índice do ETL existir:
- `itinventory-equipments`

---
### 3.3) Validar ETL (se executou e concluiu)
```bash
docker compose ps -a
```

---
### 3.4) Ver log do ETL (últimas linhas)
```bash
docker logs itinv_etl --tail 200
```

## 4) Rodar o ETL (para popular o índice)

### 4.1) Ativar a venv do ETL
```bash
cd /Users/claudioalmeida/dev/Desenvolvimento_Full_Stack_com_React_e_Spring_Boot_25E4_3/itinventory_equip/itinventory_equip/docker/infra/etl
source .venv/bin/activate
```

### 4.2) Rodar o ETL
```bash
python etl.py
```

### 4.3) Conferir contagem de documentos
```bash
curl -s "http://localhost:9200/itinventory-equipments/_count?pretty"
```

### 4.3.1) Algumas buscas e testes no Elasticsearch
```bash
-- a) Ver o mapeamento do índice (entender campos)

 curl -s "http://localhost:9200/itinventory-equipments/_mapping?pretty"

-- b) Ver 5 documentos (para “olhar dados”)

curl -s "http://localhost:9200/itinventory-equipments/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 5,
    "query": { "match_all": {} }
  }'

-- c) Buscar por texto (brand/model/location/responsible etc.)

curl -s "http://localhost:9200/itinventory-equipments/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "multi_match": {
        "query": "Dell",
        "fields": ["brand^2", "model^2", "location", "responsible", "assetNumber"]
      }
    }
  }'

-- d) Buscar por assetNumber exato (quando for keyword)
  -- Primeiro, descubra no mapping se existe assetNumber.keyword. Se existir, use:

curl -s "http://localhost:9200/itinventory-equipments/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "term": { "assetNumber.keyword": "INV-00100" }
    }
  }'
------------
  -- Se não existir .keyword, use match simples:
  
curl -s "http://localhost:9200/itinventory-equipments/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": { "match": { "assetNumber": "INV-00100" } }
  }'

-- e) Filtrar por status (ex.: ATIVO / EM_USO etc.)

curl -s "http://localhost:9200/itinventory-equipments/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "query": { "term": { "status": "EM_USO" } }
  }'


-- f) Descobrir quais status realmente existem no índice

curl -s "http://localhost:9200/itinventory-equipments/_search?pretty" \
  -H "Content-Type: application/json" \
  -d '{
    "size": 0,
    "aggs": {
      "status_distintos": {
        "terms": { "field": "status", "size": 50 }
      }
    }
  }'

```

---

## 5) Desligar / parar os serviços

### 5.1) Parar containers (mantém volumes)
```bash
cd /Users/claudioalmeida/dev/Desenvolvimento_Full_Stack_com_React_e_Spring_Boot_25E4_3/itinventory_equip/itinventory_equip/docker/infra
docker compose stop
```

### 5.2) Derrubar containers (mantém volumes)
```bash
docker compose down
```
 O que ele faz:           
  Para e remove containers (MySQL, Elasticsearch, Kibana, ETL).
  Remove a network criada pelo compose (normalmente).
  Mantém os volumes nomeados (ex.: itinv_mysql_data).
 
 Impacto:
  Ao subir de novo com docker compose up -d, o MySQL volta com o mesmo banco já preenchido (porque o volume ainda existe).
  O script ./init/*.sql normalmente não roda de novo no MySQL se o volume já tem dados (porque o entrypoint do MySQL só executa /docker-entrypoint-initdb.d quando o data dir está “vazio”).
 
 Quando usar:
  Quando você só quer “desligar tudo” e depois ligar novamente sem perder nada.


### 5.3) Derrubar containers e apagar volumes (CUIDADO: perde dados persistidos por volume)
Use somente se você **quiser resetar tudo**:
```bash
docker compose down -v
```
 O que ele faz:  
  Tudo que o down faz +
  Remove os volumes declarados no compose (ex.: itinv_mysql_data).
 
 Impacto:
  Você perde o banco dentro do volume (MySQL volta zerado).
  Ao subir novamente (up -d), o MySQL vai entender que é primeira inicialização e vai:
  criar o banco/usuário
  executar novamente os scripts em ./init (ex.: 01_schema_and_seed.sql)
  Útil para “reset total” quando algo ficou inconsistente ou você quer reimportar dados.
 
 Quando usar:
  Quando você quer resetar tudo do zero (estrutura + seeds), o MySQL não subia.

---

## 6) Desativar a `.venv` (sair do Python virtualenv)
No terminal onde você ativou a `.venv`:

```bash
deactivate
```

---

## 7) Troubleshooting rápido

### 7.1) Porta 9200 não responde
1) Veja se o container está de pé:
```bash
docker compose ps
```

2) Veja logs:
```bash
docker compose logs -f --tail=200 elasticsearch
```

3) Cheque health:
```bash
curl -s "http://localhost:9200/_cluster/health?pretty"
```

### 7.2) “Cannot connect to the Docker daemon…”
- Abra o **Docker Desktop** e aguarde ficar “Ready”.
- Rode:
```bash
docker info
```

### 7.3) Reindexar do zero
Se você quiser apagar e recriar o índice:
```bash
curl -X DELETE "http://localhost:9200/itinventory-equipments"
```
Depois rode o ETL novamente.

