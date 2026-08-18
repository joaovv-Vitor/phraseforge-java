# PhraseForge

Biblioteca digital de frases de pensadores, filósofos, escritores e outras
personalidades. Projeto de estudo em **Java 25 + Spring Boot** (backend) e
**React + TypeScript** (frontend).

## Objetivo

Descoberta, organização e gerenciamento de frases. A frase é o elemento
visual principal da interface, com estética editorial, minimalista e
sofisticada, definida pelo protótipo em `docs/prototype/`.

> O protótipo é a referência visual principal do MVP. Diferenças de escopo
> funcional entre o protótipo e o README seguem o que está documentado aqui.

## Stack

| Camada | Tecnologias |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Web, Spring Data JPA, Hibernate, Flyway, Bean Validation, Lombok, Actuator, springdoc-openapi |
| Banco | MySQL 8 |
| Frontend | React 19, TypeScript, Vite 8, React Router 7, Tailwind CSS 4, TanStack Query 5 |
| Infra | Docker, Docker Compose |

## Arquitetura

Backend monolítico e modular, organizado por domínio:

```
author/    category/    tag/    phrase/    → controller → service → repository → entity
common/    (AuditableEntity, PagedResponse, SlugUtil)
config/    (CorsConfig, OpenApiConfig)
exception/ (@RestControllerAdvice, ApiError)
```

- DTOs na comunicação com a API (entidades nunca são serializadas).
- Auditoria reutilizável via `AuditableEntity` (created_at/updated_at).
- Relacionamentos N:N (`Phrase` ↔ `Category`, `Phrase` ↔ `Tag`) através das
  tabelas de junção `phrase_categories`/`phrase_tags`.
- Banco criado exclusivamente pelo Flyway (`ddl-auto=validate`).

## Como executar

Requisitos: Docker + Docker Compose, Java 25, Node 22+.

### 1. Banco (MySQL)

```bash
cp .env.example .env
docker compose up -d db
```

Aguarde o serviço `db` ficar `healthy` (`docker compose ps`).

### 2. Backend

```bash
cd backend/phraseforge-api
./mvnw spring-boot:run
```

As migrations Flyway (V1–V10) criam o schema; V7 também insere dados de demonstração.
API disponível em `http://localhost:8080/api/v1`.
Swagger UI: `http://localhost:8080/swagger-ui.html`.
Actuator health: `http://localhost:8080/actuator/health`.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Aplicação em `http://localhost:5173`. O Vite faz proxy de `/api` para o
backend (sem CORS no desenvolvimento). Para apontar para outro backend,
defina `VITE_API_URL` em `frontend/.env.local` ou no ambiente do processo.

### Tudo de uma vez (Docker)

```bash
docker compose up -d --build
```

Frontend disponível em `http://localhost:5173`, backend em
`http://localhost:8080` e banco em `localhost:3306`. No frontend servido pelo
Compose, o Nginx encaminha `/api` internamente para o backend.

## Variáveis de ambiente

Copie `.env.example` para `.env` e ajuste:

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_HOST` | Host do MySQL | `localhost` |
| `DB_PORT` | Porta do MySQL | `3306` |
| `DB_NAME` | Nome do banco | `phraseforge` |
| `DB_USERNAME` | Usuário | `phraseforge` |
| `DB_PASSWORD` | Senha | `phraseforge` |
| `MYSQL_ROOT_PASSWORD` | Senha root do container MySQL | `phraseforge-root` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas no CORS | `http://localhost:5173` |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | E-mail do primeiro administrador; exige a senha correspondente | vazio |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Senha usada uma única vez para criar o primeiro administrador | vazio |
| `VITE_API_URL` | Base URL da API para o frontend; em Docker é um build arg | `/api/v1` |

Nunca commite credenciais reais. Após criar o primeiro administrador, remova a
senha de bootstrap do ambiente de execução.

## API

Documentação interativa (Swagger): `http://localhost:8080/swagger-ui.html`.

### Endpoints principais

```
GET    /api/v1/phrases           listagem paginada + filtros (query, authorId, categoryId, tagId, language)
GET    /api/v1/phrases/random    frase aleatória
GET    /api/v1/phrases/{id}      detalhe
POST   /api/v1/phrases           criar
PUT    /api/v1/phrases/{id}      atualizar
DELETE /api/v1/phrases/{id}      excluir

GET    /api/v1/authors           listagem paginada
GET    /api/v1/authors/{id}      detalhe
GET    /api/v1/authors/{id}/phrases
POST/PUT/DELETE /api/v1/authors/...

GET    /api/v1/categories        listagem paginada
GET    /api/v1/categories/{id}
GET    /api/v1/categories/{id}/phrases
POST/PUT/DELETE /api/v1/categories/...

GET    /api/v1/tags              listagem paginada
POST/PUT/DELETE /api/v1/tags/...
```

Respostas de erro consistentes: `{ "status", "message", "timestamp" }`.

## Estrutura do projeto

```
├── backend/phraseforge-api/   Spring Boot (Maven)
├── frontend/                  Vite + React + TS
│   ├── Dockerfile             Build multi-stage Node + Nginx
│   └── nginx.conf             SPA fallback e proxy de `/api`
├── docs/prototype/            Protótipo Figma (referência visual)
├── docker-compose.yml
├── .env.example
└── README.md
```

## Limitações do MVP (deliberadas)

- **Ano das frases:** o campo `phrases.year` (`SMALLINT`) representa o ano
  associado à frase/fonte — **não necessariamente a data exata em que a frase
  foi dita ou escrita**. O MVP não modela eras (a.C./d.C.) nem datas
  aproximadas ("c. 170 d.C."). Anos incertos não são preenchidos, e números
  não devem ser lidos como precisão histórica quando a fonte é incerta.
- **Sem autenticação:** a área administrativa é aberta (controle de acesso é
  escopo da V1).
- **Busca:** filtros via query string (contém), sem busca full-text do MySQL.

## Testes

```bash
cd backend/phraseforge-api
./mvnw test
```

Testes de service (Mockito), repository (`@DataJpaTest` com H2 em modo MySQL)
e controller (`@WebMvcTest`). O H2 é dependência apenas de teste; produção e
desenvolvimento usam MySQL. Java 25 executa o Mockito como agente configurado
no Maven Surefire.

## Estado da estabilização

A branch `fix/mvp-stabilization` contém as correções de contratos TypeScript,
validação de recursos relacionados, paginação de listas, carregamento paginado
de opções administrativas, configuração do agente Mockito e execução do
frontend via Docker Compose.

## Roadmap

**MVP** — Catálogo de frases · Autores · Categorias · Tags · Pesquisa ·
Filtros · Frase aleatória · CRUD administrativo · API REST

**V1** — Autenticação · JWT · Usuários · Roles · Favoritos

[Especificação de autenticação e favoritos](docs/superpowers/specs/2026-08-17-phraseforge-auth-favorites-design.md)

**V2** — Ranking · Frase do dia · Recomendações · Histórico · Sugestões ·
Recursos avançados da API
