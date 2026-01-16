# Consultas Elasticsearch + Kibana (Dev Tools)

Este documento reúne consultas de referência para execução no **Kibana → Dev Tools → Console**, úteis para validação do cluster, inspeção de índices e realização de pesquisas (listagem, filtros, paginação), além de agregações para relatórios rápidos.

> **Observação importante**: estas consultas estão configuradas para buscar **em todos os índices** (sem especificar nome de índice). Se você quiser consultar um índice específico, basta trocar `GET _search` por `GET <NOME_DO_INDICE>/_search` (e o mesmo padrão para `_count` e `_mapping`).

---

## 1) Cluster / Índices / Estrutura

### 1.1 Cluster health
```http
GET _cluster/health?pretty
```

### 1.2 Índices existentes (cat indices)
```http
GET _cat/indices?v
```

### 1.3 Contagem de documentos (no cluster / em todos os índices)
```http
GET _count?pretty
```

### 1.4 Mapping (campos e tipos) — todos os índices
```http
GET _mapping?pretty
```

> Campos `text` normalmente possuem subcampo `.keyword` para **filtro/ordenação/agregações exatas**, por exemplo: `brand.keyword`, `model.keyword`, `location.keyword`, `responsible.keyword`.

---

## 2) Listagem e paginação

### 2.1 Listar os 10 primeiros (todos os índices)
```http
GET _search?pretty
{
  "size": 10,
  "query": { "match_all": {} }
}
```

### 2.2 Paginar (page=1,size=10 → from=10)
```http
GET _search?pretty
{
  "from": 10,
  "size": 10,
  "query": { "match_all": {} }
}
```

### 2.3 Ordenar por data (mais recente primeiro)
```http
GET _search?pretty
{
  "size": 10,
  "sort": [
    { "criadoEm": "desc" }
  ],
  "query": { "match_all": {} }
}
```

---

## 3) Filtros exatos (term + keyword quando aplicável)

### 3.1 Filtrar por `type`
```http
GET _search?pretty
{
  "size": 20,
  "query": {
    "term": { "type": "NOTEBOOK" }
  }
}
```

### 3.2 Filtrar por `status`
```http
GET _search?pretty
{
  "size": 20,
  "query": {
    "term": { "status": "EM_USO" }
  }
}
```

### 3.3 Filtrar por `ativo=true`
```http
GET _search?pretty
{
  "size": 20,
  "query": {
    "term": { "ativo": true }
  }
}
```

---

## 4) Filtros por texto

### 4.1 Buscar marca (ex.: “Dell”) usando `match`
```http
GET _search?pretty
{
  "size": 20,
  "query": {
    "match": { "brand": "Dell" }
  }
}
```

### 4.2 Buscar por trecho com `wildcard` (mais pesado)
```http
GET _search?pretty
{
  "size": 20,
  "query": {
    "wildcard": { "model.keyword": "*ThinkPad*" }
  }
}
```

---

## 5) Consulta combinada (bool + filtros)

### 5.1 NOTEBOOK + EM_USO + location contém “SEDE” + ativo=true
```http
GET _search?pretty
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
        { "match": { "location": "Matriz" } }
      ]
    }
  }
}
```

### 5.2 brand exata (keyword) + responsible exato (keyword)
```http
GET _search?pretty
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
}
```

---

## 6) Intervalos (range)

### 6.1 Filtrar por valor de aquisição (acquisitionValue)
```http
GET _search?pretty
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
}
```

### 6.2 Filtrar por data de aquisição (acquisitionDate)
```http
GET _search?pretty
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
}
```

---

## 7) Agregações (relatórios rápidos)

### 7.1 Contagem por status
```http
GET _search?pretty
{
  "size": 0,
  "aggs": {
    "por_status": {
      "terms": { "field": "status" }
    }
  }
}
```

### 7.2 Contagem por tipo
```http
GET _search?pretty
{
  "size": 0,
  "aggs": {
    "por_tipo": {
      "terms": { "field": "type" }
    }
  }
}
```

### 7.3 Top 10 marcas (exato via keyword)
```http
GET _search?pretty
{
  "size": 0,
  "aggs": {
    "top_marcas": {
      "terms": { "field": "brand.keyword", "size": 10 }
    }
  }
}
```

### 7.4 Estatísticas de valores (min, max, avg, sum)
```http
GET _search?pretty
{
  "size": 0,
  "aggs": {
    "stats_valores": {
      "stats": { "field": "acquisitionValue" }
    }
  }
}
```

---

## 8) Buscar por ID do equipment (idEquipment)
```http
GET _search?pretty
{
  "size": 1,
  "query": {
    "term": { "idEquipment": 1 }
  }
}
```
