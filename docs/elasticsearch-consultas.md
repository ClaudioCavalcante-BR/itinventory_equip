# Sugestões de buscas no Elasticsearch (índice: `itinventory-equipments`)

Este arquivo traz exemplos prontos (via **cURL**) para você consultar o índice `itinventory-equipments` no **Elasticsearch** (porta `9200`).

## 0) Variáveis (opcional)
Se quiser, defina estas variáveis no terminal para não repetir URL/índice:

```bash
export ES="http://localhost:9200"
export IDX="itinventory-equipments"
```

---

## 1) Checagens rápidas

### Cluster/health
```bash
curl -s "$ES/_cluster/health?pretty"
```

### Índices existentes
```bash
curl -s "$ES/_cat/indices?v"
```

### Contagem de documentos no índice
```bash
curl -s "$ES/$IDX/_count?pretty"
```

### Mapping (campos e tipos)
```bash
curl -s "$ES/$IDX/_mapping?pretty"
```

Observação: campos do tipo `text` possuem subcampo `.keyword` (ex.: `brand.keyword`, `model.keyword`, `location.keyword`, `responsible.keyword`) para filtros/ordenação/agregações exatas.

---

## 2) Listagem e paginação

### 2.1) Listar os 10 primeiros
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 10,
  "query": { "match_all": {} }
}'
```

### 2.2) Paginar (page=0,size=10) → `from = page * size`
Exemplo: page=1,size=10 (ou seja, pula 10 e traz os próximos 10):
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "from": 10,
  "size": 10,
  "query": { "match_all": {} }
}'
```

### 2.3) Ordenar por data (mais recente primeiro)
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 10,
  "sort": [
    { "criadoEm": "desc" }
  ],
  "query": { "match_all": {} }
}'
```

---

## 3) Filtros exatos (recomendado com `.keyword` e `term`)

### 3.1) Filtrar por `type`
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 20,
  "query": {
    "term": { "type": "NOTEBOOK" }
  }
}'
```

### 3.2) Filtrar por `status`
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 20,
  "query": {
    "term": { "status": "EM_USO" }
  }
}'
```

### 3.3) Filtrar por `ativo=true`
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 20,
  "query": {
    "term": { "ativo": true }
  }
}'
```

---

## 4) Filtros por texto (quando o campo é `text`)

### 4.1) Buscar marca (ex.: “Dell”) usando `match`
> `brand` é `text`. Para busca “contém / análise”, use `match`.

```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 20,
  "query": {
    "match": { "brand": "Dell" }
  }
}'
```

### 4.2) Buscar por trecho com `wildcard` (mais pesado)
> Use com cuidado em bases grandes.

```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 20,
  "query": {
    "wildcard": { "model.keyword": "*ThinkPad*" }
  }
}'
```

---

## 5) Consulta combinada (bool + filtros)

### 5.1) Exemplo: NOTEBOOK + EM_USO + location contém “SEDE” + ativo=true
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 50,
  "query": {
    "bool": {
      "filter": [
        { "term": { "type": "NOTEBOOK" } },
        { "term": { "status": "EM_USO" } },
        { "term": { "ativo": true } }
      ],
      "must": [
        { "match": { "location": "SEDE" } }
      ]
    }
  }
}'
```

### 5.2) Exemplo: brand exata (keyword) + responsible exato (keyword)
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 50,
  "query": {
    "bool": {
      "filter": [
        { "term": { "brand.keyword": "Dell" } },
        { "term": { "responsible.keyword": "TI CENTRAL" } }
      ]
    }
  }
}'
```

---

## 6) Intervalos (range)

### 6.1) Filtrar por valor de aquisição (acquisitionValue)
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 50,
  "query": {
    "range": {
      "acquisitionValue": {
        "gte": 3000,
        "lte": 8000
      }
    }
  }
}'
```

### 6.2) Filtrar por data de aquisição (acquisitionDate)
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 50,
  "query": {
    "range": {
      "acquisitionDate": {
        "gte": "2023-01-01",
        "lte": "2025-12-31"
      }
    }
  }
}'
```

---

## 7) Agregações (relatórios rápidos)

### 7.1) Contagem por status
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 0,
  "aggs": {
    "por_status": {
      "terms": { "field": "status" }
    }
  }
}'
```

### 7.2) Contagem por tipo
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 0,
  "aggs": {
    "por_tipo": {
      "terms": { "field": "type" }
    }
  }
}'
```

### 7.3) Top 10 marcas (exato via keyword)
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 0,
  "aggs": {
    "top_marcas": {
      "terms": { "field": "brand.keyword", "size": 10 }
    }
  }
}'
```

### 7.4) Estatísticas de valores (min, max, avg, sum)
```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 0,
  "aggs": {
    "stats_valores": {
      "stats": { "field": "acquisitionValue" }
    }
  }
}'
```

---

## 8) Buscar por “ID” do equipment
No ETL, o campo é `idEquipment` (long). Exemplo:

```bash
curl -s "$ES/$IDX/_search?pretty" -H 'Content-Type: application/json' -d '
{
  "size": 1,
  "query": {
    "term": { "idEquipment": 1 }
  }
}'
```

---

## 9) Dicas de uso (importantes)

1. **Filtro exato** → prefira `term` com campos `keyword` (ex.: `brand.keyword`, `location.keyword`).
2. **Busca textual** → use `match` em campos `text` (ex.: `brand`, `model`, `location`, `responsible`).
3. **Paginação** → use `from` e `size`.
4. **Relatórios** → `aggs` + `size: 0`.
5. Se quiser “resetar” o índice para reindexar do zero:
   ```bash
   curl -X DELETE "$ES/$IDX"
   ```

