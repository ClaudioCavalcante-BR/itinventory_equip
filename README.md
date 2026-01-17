# itinventory_equip

Back-end do ecossistema **ITInventory (Controller-Inventory)**, responsável pela API REST de inventário de equipamentos de TI. A aplicação implementa autenticação via **JWT**, regras de autorização por **perfis (roles)** e persistência em **MySQL**, além de disponibilizar busca textual via **Elasticsearch** (com **Kibana**) e um **ETL** (Python) para indexação.

## Sumário

1. O que é o projeto
2. Objetivos
3. Principais funcionalidades
4. Stack de tecnologias
5. Arquitetura e diretrizes
6. Estrutura de pastas
6.1 Padrões de nomes
6.2 Busca textual (Elasticsearch)
7. Como executar
8. Build e execução (Maven/JAR e Docker)
9. Configurações e variáveis
10. Endpoints e exemplos de uso
11. Matriz de permissões
12. Testes manuais sugeridos
13. Segurança e privacidade
14. Licença
15. Autor e repositórios relacionados

## 1. O que é o projeto

O **itinventory_equip** é uma API REST desenvolvida em **Java 21 + Spring Boot 4.0**, destinada a suportar o controle de ativos de TI (equipamentos) no ecossistema ITInventory. O serviço fornece autenticação centralizada (login), emissão de token JWT, recuperação de perfil do usuário autenticado e endpoints para manutenção de equipamentos e usuários (conforme permissões do perfil).

Além do CRUD transacional em MySQL, o projeto contempla um módulo de **Search** (integração com Elasticsearch) para consultas textuais de equipamentos, bem como um componente de **ETL** (Python) para indexação dos registros do banco no índice do Elasticsearch, possibilitando consultas e análises via **Kibana**.

## 2. Objetivos

1. Centralizar regras de negócio do inventário de equipamentos em uma API segura, auditável e reutilizável.
2. Fornecer autenticação via JWT e autorização baseada em roles (ADMIN, GESTOR_TI, ANALISTA_TI, USUARIO).
3. Disponibilizar endpoints REST para CRUD e exportação CSV, com contratos previsíveis para consumo pelo front-end.
4. Permitir cenários de busca textual e filtros por Elasticsearch, desacoplando consultas analíticas do banco transacional.
5. Assegurar reprodutibilidade do ambiente via Docker (MySQL, Elasticsearch, Kibana e ETL) e configuração por profiles.

## 3. Principais funcionalidades

1. Autenticação e sessão (JWT)
   1. Login via `POST /api/usuarios/login`.
   2. Retorno de payload contendo token, dados do usuário e perfil.
   3. Endpoint `GET /api/usuarios/my-profile` para validar sessão e obter perfil do usuário autenticado.
2. Equipamentos (inventário)
   1. Listagem paginada `GET /api/equipments?page=0&size=10`.
   2. Detalhe por id `GET /api/equipments/{id}`.
   3. Criação `POST /api/equipments`.
   4. Atualização `PUT /api/equipments/{id}`.
   5. Exclusão `DELETE /api/equipments/{id}` (restrita a ADMIN).
   6. Exportação CSV `GET /api/equipments/export`.
3. Usuários (administrativo)
   1. Listagem paginada `GET /api/usuarios` (somente ADMIN).
   2. Criação `POST /api/usuarios` (somente ADMIN).
   3. Atualização `PUT /api/usuarios/{id}` e exclusão `DELETE /api/usuarios/{id}` (somente ADMIN).
   4. Ativar/Inativar (soft control) `PATCH /api/usuarios/{id}/ativar` e `PATCH /api/usuarios/{id}/inativar` (somente ADMIN).
   5. Exportação CSV `GET /api/usuarios/export` (somente ADMIN).
4. Perfis (administrativo)
   1. Listagem `GET /api/profiles` e opções `GET /api/profiles/options` (somente ADMIN).
5. Busca textual (Elasticsearch)
   1. Busca simples `GET /api/equipments/search/search?q=DELL&page=0&size=10`.
   2. Busca avançada `POST /api/equipments/search/search/advanced?page=0&size=10`.
   3. Observação: endpoints de Search são habilitados por propriedade `search.es.enabled=true`.
6. Observabilidade (Actuator)
   1. Endpoints expostos para apoio a diagnóstico: `/actuator/health`, `/actuator/info`, `/actuator/mappings`, `/actuator/beans`, `/actuator/env`.

## 4. Stack de tecnologias

1. Plataforma
   1. Java 21
   2. Spring Boot 4.0.0
2. Web e validação
   1. spring-boot-starter-webmvc
   2. spring-boot-starter-validation
3. Persistência
   1. spring-boot-starter-data-jpa
   2. MySQL Connector/J 9.5.0
4. Segurança
   1. spring-boot-starter-security
   2. JJWT 0.11.5 (jwt-api, jwt-impl, jwt-jackson)
5. Search
   1. Elasticsearch Java Client 8.15.0
   2. Elasticsearch REST Client 8.15.0
6. Observabilidade
   1. spring-boot-starter-actuator
7. Infra (Docker)
   1. MySQL 8.4
   2. Elasticsearch 8.15.0
   3. Kibana 8.15.0
   4. ETL (Python) para indexação do MySQL no Elasticsearch

## 5. Arquitetura e diretrizes

1. Separação por camadas
   1. `controller`: endpoints REST (Users, Profiles, Equipments).
   2. `service`: regras de negócio e orquestração (CRUD, autenticação e autorização).
   3. `repository`: acesso a dados (JPA).
   4. `dto`: contratos de entrada/saída.
   5. `config`: segurança (JWT, CORS, SecurityFilterChain) e ajustes de Web.
   6. `search/*`: integração com Elasticsearch, documentos, serviços e listeners/eventos para indexação.
2. Segurança (stateless)
   1. JWT enviado via header `Authorization: Bearer <token>`.
   2. Sessão **stateless** (sem cookies e sem estado no servidor).
   3. Tratamento padronizado de 401/403 (entry point e access denied handler).
3. CORS (consumo pelo front-end)
   1. Origens permitidas incluem `http://localhost:5173` e domínios `*.vercel.app`.
   2. Preflight `OPTIONS` liberado globalmente.
4. Profiles de execução
   1. `application.yaml`: defaults (porta, JWT e flags).
   2. `application-local.yaml`: aponta para MySQL local (porta 3306) e Search desabilitado por padrão.
   3. `application-docker.yaml`: aponta para MySQL em Docker (porta 3307) e Search habilitado.

## 6. Estrutura de pastas

```text
ITINVENTORY_EQUIP (Spring Boot + MySQL + Elasticsearch) — Estrutura de pastas e responsabilidades

itinventory_equip/
├─ docs/                                 		            -> Documentação operacional do projeto (guias, exemplos, runbooks)
│  ├─ elasticsearch-consultas.md         		            -> Exemplos de consultas e uso do Elasticsearch
│  └─ subir-Docker_Elasticsearch_desligar.md	          -> Passo a passo para subir/desligar a infra via Docker
│  └─ consultas_elasticsearch_+_kibana.md		            -> Exemplos de consultas e uso do Kibana                              		
│  └─ mockups/
│	      └─wireframe	
│	       └─ITInventory_Wireframe_v1.1.odg		            -> arquivo original para visualizaçãoes e alterações   
│        └─ITInventory_Wireframe_v1.1.pdf 		          -> fluxo de navegação, arquivo para visualização   
│	  
├─ docker/                               		            -> Infra/artefatos para execução em containers (ambiente local)
│  └─ infra/
│     ├─ docker-compose.yaml             		            -> Stack da infra (ex.: Elasticsearch/Kibana/serviços auxiliares)
│     ├─ equipamentos.csv                		            -> Dataset de apoio (carga/ETL e validações)
│     ├─ init/
│     │  └─ 01_schema_and_seed.sql       		            -> Script de schema + seed inicial do banco (DB)
│     ├─ backup/
│     │  └─ itinventory_local_dump.sql   		            -> Backup/dump usado pela infra (restauração local)
│     └─ etl/
│        ├─ Dockerfile                   		            -> Imagem do job de ETL (container dedicado)
│        ├─ requirements.txt             		            -> Dependências Python do ETL
│        └─ etl.py                       		            -> Rotina de ETL (carga/transformação para ES/DB)
│
├─ src/                                  		           -> Código-fonte do backend
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ br/com/infnet/itinventory/
│  │  │     ├─ ItinventoryEquipApplication.java		    -> Bootstrap (ponto de entrada do Spring Boot)
│  │  │     │                                  
│  │  │     │
│  │  │     ├─ config/                          	    -> Configurações transversais (Security,CORS,filtros)
│  │  │     │  ├─ SecurityConfig.java           	    -> Regras de autorização/autenticação (Spring Security)
│  │  │     │  ├─ JwtAuthenticationFilter.java  	    -> Filtro JWT (extração/validação do token)
│  │  │     │  └─ WebConfig.java                	    -> Config web (CORS, MVC/infra HTTP)
│  │  │     │
│  │  │     ├─ controller/                      	    -> Endpoints REST (camada de API)
│  │  │     │  ├─ EquipmentController.java      	    -> CRUD, paginação e exportação de equipamentos
│  │  │     │  ├─ UserController.java           	    -> CRUD, listagem e ativação/inativação de usuários
│  │  │     │  └─ ProfileController.java        	    -> Consulta de perfis (apoio ao frontend)
│  │  │     │
│  │  │     ├─ dto/                             	    -> DTOs (contrato de entrada/saída da API)
│  │  │     │  ├─ EquipmentRequestDTO.java      	    -> Payload de criação/atualização de Equipment
│  │  │     │  ├─ EquipmentResponseDTO.java     	    -> Payload de retorno de Equipment
│  │  │     │  ├─ EquipmentFilterRequest.java   	    -> Request de filtros/pesquisa estruturada (DB)
│  │  │     │  ├─ UserCreateRequestDTO.java     	    -> Payload de criação de User
│  │  │     │  ├─ UserUpdateRequestDTO.java     	    -> Payload de atualização de User
│  │  │     │  ├─ UserResponseDTO.java          	    -> Payload de retorno de User
│  │  │     │  ├─ ProfileOptionDTO.java         	    -> DTO “opção” (ex.: id + nome) para combos/selects
│  │  │     │  ├─ ProfileResponseDTO.java       	    -> DTO de retorno de Profile
│  │  │     │  ├─ AuthPayload.java              	    -> Payload do fluxo de autenticação/login
│  │  │     │  └─ AuthUserDTO.java              	    -> Dados do usuário autenticado (retorno/token)
│  │  │     │
│  │  │     ├─ model/                           	    -> Domínio (entidades JPA + enums)
│  │  │     │  ├─ Equipment.java                	    -> Entidade JPA de equipamento
│  │  │     │  ├─ EquipmentType.java            	    -> Enum de tipo de equipamento
│  │  │     │  ├─ EquipmentStatus.java          	    -> Enum de status de equipamento
│  │  │     │  ├─ User.java                     	    -> Entidade JPA de usuário
│  │  │     │  └─ Profile.java                  	    -> Entidade JPA de perfil/acesso
│  │  │     │
│  │  │     ├─ repository/                      	   -> Acesso a dados (Spring Data/JPA)
│  │  │     │  ├─ EquipmentRepository.java      	   -> Operações de persistência/consulta de Equipment
│  │  │     │  ├─ UserRepository.java           	   -> Operações de persistência/consulta de User
│  │  │     │  └─ ProfileRepository.java        	   -> Operações de persistência/consulta de Profile
│  │  │     │
│  │  │     ├─ service/                         	   -> Regras de negócio e orquestração
│  │  │     │  ├─ EquipmentService.java         	   -> Regras + CRUD + integração com indexação/busca
│  │  │     │  ├─ UserService.java              	   -> Regras de usuários (criar/editar/ativar/inativar)
│  │  │     │  ├─ ProfileService.java           	   -> Regras/consulta de perfis (para cadastro/validação)
│  │  │     │  ├─ TokenService.java                  -> Emissão/validação de token (JWT)
│  │  │     │  └─ SecurityService.java          	   -> Apoio ao contexto de segurança (usuário logado)
│  │  │     │
│  │  │     ├─ exception/                       	   -> Erros padronizados + handlers (API)
│  │  │     │  ├─ ApiError.java                 	   -> Modelo de erro retornado ao cliente
│  │  │     │  ├─ GlobalExceptionHandler.java   	   -> @ControllerAdvice (tratamento centralizado)
│  │  │     │  ├─ RestAuthenticationEntryPoint.java	 -> 401 (não autenticado) — resposta padronizada
│  │  │     │  │  
│  │  │     │  ├─ RestAccessDeniedHandler.java  	   -> 403 (sem permissão) — mensagem por regra/rota
│  │  │     │  ├─ EquipmentNotFoundException.java	   -> 404/negócio: equipamento não encontrado
│  │  │     │  │                                
│  │  │     │  ├─ EquipmentBusinessException.java	   -> Regras de negócio/validação do domínio
│  │  │     │  │                                
│  │  │     │  └─ ForbiddenOperationException.java	 -> Operação proibida por regra de acesso/estado
│  │  │     │                                   
│  │  │     └─ search/                          	   -> Busca/indexação via Elasticsearch (camada dedicada)
│  │  │        ├─ config/
│  │  │        │  ├─ ElasticsearchClientConfig.java	 -> Cliente ES (conexão e beans de integração)
│  │  │        │  │                                
│  │  │        │  └─ SearchAsyncConfig.java        	 -> Execução assíncrona (threads/executors)
│  │  │        ├─ doc/
│  │  │        │  └─ EquipmentDoc.java             	 -> Documento ES (projeção otimizada do Equipment)
│  │  │        ├─ dto/
│  │  │        │  └─ EquipmentSearchRequest.java   	 -> Request de busca textual + filtros (ES)
│  │  │        ├─ controller/
│  │  │        │  └─ EquipmentSearchController.java  -> Endpoint(s) para busca no Elasticsearch
│  │  │        │                                   
│  │  │        ├─ service/
│  │  │        │  └─ EquipmentSearchService.java    	-> Orquestra consultas e retorno (ES)
│  │  │        ├─ index/
│  │  │        │  ├─ EquipmentIndexer.java         	    -> Contrato para indexação (API interna)
│  │  │        │  ├─ ElasticsearchEquipmentIndexer.java -> Implementação ES (index/update/delete)
│  │  │        │  │                                
│  │  │        │  └─ NoOpEquipmentIndexer.java     	  -> Fallback (quando ES não está disponível)
│  │  │        ├─ event/
│  │  │        │  ├─ EquipmentIndexEvent.java      	  -> Evento de indexação (publicação)
│  │  │        │  ├─ EquipmentIndexOperation.java    	-> Tipo de operação (CREATE/UPDATE/DELETE)
│  │  │        │  └─ EquipmentIndexEventListener.java	-> Consumidor do evento (aciona a indexação)
│  │  │        │                                   
│  │  │        └─ listener/
│  │  │           └─ EquipmentIndexListener.java   	  -> Listener dedicado (ponte para o indexer)
│  │  └─ resources/                               	  -> Configurações e recursos do runtime
│  │     ├─ application.yaml                      	  -> Config padrão (profiles/porta/logs)
│  │     ├─ application-local.yaml                	  -> Overrides do ambiente local
│  │     ├─ application-docker.yaml               	  -> Overrides para execução via Docker
│  │     ├─ static/                               	  -> Arquivos estáticos (se aplicável)
│  │     └─ templates/                            	  -> Templates server-side (se aplicável)
│  │
│  └─ test/                                       	  -> Testes automatizados
│     ├─ java/
│     │  └─ br/com/infnet/itinventory/
│     │     ├─ ItinventoryEquipApplicationTests.java	-> Smoke test (contexto Spring sobe corretamente)
│     │     │                                  
│     │     └─ service/
│     │        └─ EquipmentServiceTest.java     	  -> Testes do service de Equipment (regras/fluxos)
│     └─ resources/                                 -> Recursos de teste (mocks, configs, fixtures)
└─ target/                               		        -> Artefatos gerados build(Maven)[ger. automati.]
```

Visão objetiva das pastas mais relevantes

1. `src/main/java/.../controller`: contrato público da API.
2. `src/main/java/.../config`: regras de segurança (JWT e CORS) e filtros.
3. `src/main/java/.../service`: serviços de domínio e autenticação.
4. `src/main/java/.../search`: busca textual no Elasticsearch.
5. `docker/infra`: ambiente reprodutível (MySQL + ES + Kibana + ETL).

### 6.1 Padrões de nomes

1. Java
   1. Classes em PascalCase (ex.: `EquipmentController`).
   2. Pacotes por responsabilidade (`controller`, `service`, `repository`, `dto`).
2. Endpoints
   1. Base `/api`.
   2. Recursos com nomes no plural (ex.: `/api/equipments`, `/api/usuarios`).
3. Profiles e configs
   1. Arquivos `application-<profile>.yaml`.
   2. Seleção por `SPRING_PROFILES_ACTIVE`.

### 6.2 Busca textual (Elasticsearch)

Objetivo

Disponibilizar endpoints para consultas textuais e filtros (ex.: marca/modelo/localização/status) sem sobrecarregar o banco transacional. A busca é habilitada via `search.es.enabled=true` e usa o índice `itinventory-equipments`.

Componentes

1. `search/doc/EquipmentDoc`: documento indexado (representação otimizada do Equipment no Elasticsearch).
2. `search/service/EquipmentSearchService`: encapsula consultas e construção de query.
3. `search/controller/EquipmentSearchController`: endpoints para front.
4. `docker/infra/etl/etl.py`: indexação do MySQL para o Elasticsearch.

### Estrutura pronta no Beckend e para consumo do Frontend(claudio-itinventory-front) que ainda não foi implementada.

```
src/
├─ pages/
│  └─ Search/
│     ├─ EquipmentTextSearchPage.jsx         -> Tela principal de busca textual (input, filtros, resultados)
│     └─ EquipmentTextSearchPage.module.css  -> Estilos isolados da tela
│
├─ routes/
│  └─ Router.jsx                             -> Inclusão da rota privada:
│                                              /search/equipments (ou /equipments/text-search)
│                                              usando PrivateRoute (e validação de usuário ativo)
│
├─ services/
│  └─ SearchService.js                       -> Integração com backend/search (Elasticsearch via API)
│                                              Métodos fundamentais (exemplos):
│                                              - searchEquipmentsText({ q, page, size, filters, sort })
│                                              - exportEquipmentsTextCsv({ q, filters, sort })
│                                              - getSearchFacets({ q }) (opcional: agregações)
│
├─ components/
│  └─ Search/
│     ├─ SearchBar.jsx                       -> Campo de busca + submit + debounce (UX)
│     ├─ SearchFilters.jsx                   -> Filtros (status/tipo/categoria/unidade etc.)
│     ├─ SearchResultGrid.jsx                -> Grid/tabela de resultados (DataGrid)
│     ├─ SearchResultActions.jsx             -> Ações: exportar, limpar filtros, recarregar
│     └─ SearchEmptyState.jsx                -> Estado vazio (sem resultados / sem termo)
│
├─ store/                                    -> (opcional, se desejar estado global para busca)
│  └─ searchSlice.js                         -> Estado da busca (q, filtros, paginação, ordenação, resultados)
│                                              Métodos fundamentais (reducers/actions):
│                                              - setQuery(), setFilters(), setPagination(), setSort()
│                                              - setResults(), clearResults(), setLoading(), setError()
│
├─ constants/
│  └─ searchConstants.js                      -> Parâmetros padrão (pageSize, sorts permitidos, labels)
│
└─ context/                                  -> (alternativa ao Redux para busca)
   └─ SearchContext.jsx                       -> Controla estado da busca e expõe ações principais
                                                 Métodos fundamentais:
                                                 - runSearch(), clearSearch(), exportResults()
```

## 7. Como executar

### 7.1 Pré-requisitos

1. Java 21 (JDK)
2. Maven 3.9+ (ou `./mvnw`)
3. Docker + Docker Compose
4. Git

### 7.2 Subir a infraestrutura (recomendado)

A infraestrutura local do projeto fica em `docker/infra` e sobe:

- MySQL (porta **3307** no host)
- Elasticsearch (porta **9200** no host)
- Kibana (porta **5601** no host)
- ETL (executa e finaliza, para indexar no Elasticsearch)

Comandos:

```bash
cd docker/infra
docker compose up -d
```

Verificação rápida:

```bash
docker ps
curl -s http://localhost:9200/_cluster/health
```

### 7.3 Executar a API localmente (IDE/Terminal)

Com a infra Docker em execução, rode a aplicação com o profile `docker`:

```bash
# na raiz do projeto
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker

# alternativa (variável de ambiente)
export SPRING_PROFILES_ACTIVE=docker
./mvnw spring-boot:run
```

A API sobe, por padrão, em:

- `http://localhost:8081`

Observação: o `application-docker.yaml` já aponta o datasource para `127.0.0.1:3307` e habilita `search.es.enabled=true`.

### 7.4 Credenciais de exemplo (seed)

Ao subir o MySQL via `docker/infra`, o script `01_schema_and_seed.sql` cria perfis e usuários para testes (senha padrão `123456`). Exemplos:

- ADMIN (perfil ADMIN): `paulo.martins051@empresa.com` / `123456`
- ADMIN (perfil ADMIN): `bruno.almeida055@empresa.com` / `123456`
- GESTOR_TI: `aline.moreira052@empresa.com` / `123456`
- ANALISTA_TI: `rafael.ferreira053@empresa.com` / `123456`
- USUARIO: `camila.lima054@empresa.com` / `123456`

## 8. Build e execução (Maven/JAR e Docker)

### 8.1 Build do JAR

```bash
./mvnw -DskipTests clean package
```

Execução do JAR:

```bash
java -jar target/itinventory_equip-0.0.1-SNAPSHOT.jar
```

### 8.2 Executar via Docker (imagem da API)

O projeto possui `Dockerfile` na raiz, com build multi-stage (Maven + JRE). Exemplo:

```bash
docker build -t itinventory_equip:local .

docker run --rm -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=docker \
  itinventory_equip:local
```

Observação: em provedores (ex.: Render), a aplicação lê `PORT` e usa fallback `8081`.

## 9. Configurações e variáveis

Arquivos de referência

1. `src/main/resources/application.yaml`
2. `src/main/resources/application-local.yaml`
3. `src/main/resources/application-docker.yaml`

Principais propriedades

1. Porta
   1. `server.port` (default 8081)
2. Datasource
   1. `spring.datasource.url`
   2. `spring.datasource.username`
   3. `spring.datasource.password`
3. JWT
   1. `security.jwt.secret`
   2. `security.jwt.expiration-minutes`
4. Search
   1. `search.es.enabled`
   2. `search.es.host`
   3. `search.es.port`
   4. `search.es.index`

Exemplo (ambiente Docker local)

- MySQL: `jdbc:mysql://127.0.0.1:3307/itinventory_equip`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

## 10. Endpoints e exemplos de uso

### 10.1 Login (obter token)

```bash
curl -s -X POST http://localhost:8081/api/usuarios/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"paulo.martins051@empresa.com","password":"123456"}'
```

Resposta esperada (exemplo):

- `token`: JWT
- `userId`, `name`, `email`, `profileCode`, `nivelAcesso`, flags de ativo

### 10.2 My Profile (validar sessão)

```bash
TOKEN="<cole-o-token-aqui>"

curl -s http://localhost:8081/api/usuarios/my-profile \
  -H "Authorization: Bearer ${TOKEN}"
```

### 10.3 Listar equipamentos

```bash
curl -s "http://localhost:8081/api/equipments?page=0&size=10" \
  -H "Authorization: Bearer ${TOKEN}"
```

### 10.4 Exportar CSV de equipamentos

```bash
curl -L -o equipamentos.csv \
  -H "Authorization: Bearer ${TOKEN}" \
  http://localhost:8081/api/equipments/export
```

### 10.5 Search (Elasticsearch)

> Disponível somente quando `search.es.enabled=true`.

Busca simples:

```bash
curl -s "http://localhost:8081/api/equipments/search/search?q=dell&page=0&size=10" \
  -H "Authorization: Bearer ${TOKEN}"
```

Busca avançada:

```bash
curl -s -X POST "http://localhost:8081/api/equipments/search/search/advanced?page=0&size=10" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"texto":"dell","status":"EM_USO","type":"NOTEBOOK"}'
```

## 11. Matriz de permissões

As permissões abaixo refletem a regra de autorização implementada na configuração de segurança.

1. Público (sem token)
   1. `POST /api/usuarios/login`
   2. `GET /actuator/**`
2. Autenticado (qualquer perfil válido)
   1. `GET /api/usuarios/my-profile`
3. Equipamentos
   1. `GET /api/equipments/**`: ADMIN, GESTOR_TI, ANALISTA_TI, USUARIO
   2. `POST /api/equipments/**`: ADMIN, GESTOR_TI, ANALISTA_TI
   3. `PUT /api/equipments/**`: ADMIN, GESTOR_TI
   4. `DELETE /api/equipments/**`: ADMIN
4. Usuários
   1. `GET /api/usuarios/**`: ADMIN
   2. `POST /api/usuarios`: ADMIN
   3. `PUT /api/usuarios/**`: ADMIN
   4. `DELETE /api/usuarios/**`: ADMIN
   5. `PATCH /api/usuarios/{id}/ativar|inativar`: ADMIN
   6. `GET /api/usuarios/export`: ADMIN
5. Perfis
   1. `GET /api/profiles/**`: ADMIN

## 12. Testes manuais sugeridos

1. Autenticação
   1. Login válido retorna token e payload.
   2. Login inválido retorna erro com status apropriado.
   3. Token ausente em rota protegida retorna 401.
2. Permissões
   1. Usuário USUARIO consegue apenas GET de equipamentos.
   2. Usuário ANALISTA_TI consegue criar equipamentos, mas não excluir.
   3. Usuário GESTOR_TI consegue criar e atualizar.
   4. ADMIN consegue todas as operações.
3. Exportações
   1. CSV de equipamentos baixa arquivo.
   2. CSV de usuários (ADMIN) baixa arquivo e mantém separador compatível.
4. Search (quando habilitado)
   1. Rodar ETL para indexar.
   2. Consultar endpoints de search e validar retorno.
   3. Abrir Kibana e reproduzir consultas do diretório `docs/`.

## 13. Segurança e privacidade

1. JWT
   1. Token emitido no login, esperado em `Authorization: Bearer`.
   2. Sessão stateless (sem cookies).
2. CORS
   1. Origens controladas para permitir consumo pelo front em `localhost` e `*.vercel.app`.
3. Dados de seed
   1. Credenciais padrão (senha `123456`) são destinadas a ambiente de desenvolvimento e avaliação.
   2. Em produção, recomenda-se hashing/BCrypt persistido no banco e rotação de segredo JWT.

## 14. Licença

MIT License

Copyright (c) 2025

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## 15. Autor e repositórios relacionados

1. Autor: ClaudioCavalcante-BR
2. Repositórios relacionados
   1. Back-end: itinventory_equip(https://github.com/ClaudioCavalcante-BR/itinventory_equip)
   2. Front-end: claudio-itinventory-front(https://github.com/ClaudioCavalcante-BR/claudio-itinventory-front)

