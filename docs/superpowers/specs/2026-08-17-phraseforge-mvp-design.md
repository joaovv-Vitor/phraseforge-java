# PhraseForge MVP — Design Document

**Date:** 2026-08-17
**Status:** Approved design

## 1. Objective

PhraseForge is a digital library of quotes from thinkers, philosophers, writers, and
personalities. This document specifies the MVP: a catalog with authors, categories,
tags, search, filters, random quote, and administrative CRUD. Later versions (V1/V2)
are explicitly out of scope.

The project doubles as a study project for Java 25 + Spring Boot. Implementations
must be simple, explicit, and educational — no premature abstractions.

## 2. Stack

- **Backend:** Java 25, Spring Boot 4.1, Maven, Spring Web (MVC), Spring Data JPA,
  Hibernate, MySQL, Flyway, Bean Validation, Lombok, Actuator, springdoc-openapi
  (Swagger UI).
- **Frontend:** React 19, TypeScript, Vite, React Router, Tailwind CSS v4,
  TanStack Query.
- **Infra:** Docker + Docker Compose.

## 3. Repo layout

```
phraseforge-java/
├── backend/phraseforge-api/     Spring Boot app (package com.phraseforge.phraseforge_api)
├── frontend/                    Vite React app
├── docs/prototype/              Figma prototype export (visual source of truth)
├── docs/superpowers/specs/      This spec
├── docker-compose.yml
├── .env.example
└── README.md
```

## 4. Database (MySQL, managed exclusively by Flyway)

Exactly six business tables — no `users`, no `favorites` in MVP:

| Table | Columns |
|---|---|
| `authors` | `id` BIGINT PK AI, `name` VARCHAR(150) NOT NULL, `slug` VARCHAR(180) NOT NULL UNIQUE, `birth_year` SMALLINT NULL, `death_year` SMALLINT NULL, `biography` TEXT NULL, `created_at` TIMESTAMP NOT NULL, `updated_at` TIMESTAMP NOT NULL |
| `phrases` | `id` BIGINT PK AI, `content` TEXT NOT NULL, `author_id` BIGINT NOT NULL FK→authors, `year` SMALLINT NULL, `language` VARCHAR(10) NOT NULL, `source` VARCHAR(300) NULL, `created_at` TIMESTAMP NOT NULL, `updated_at` TIMESTAMP NOT NULL |
| `categories` | `id` BIGINT PK AI, `name` VARCHAR(100) NOT NULL, `slug` VARCHAR(120) NOT NULL UNIQUE, `description` VARCHAR(500) NULL, `created_at`/`updated_at` |
| `tags` | `id` BIGINT PK AI, `name` VARCHAR(50) NOT NULL UNIQUE, `created_at`/`updated_at` |
| `phrase_categories` | `id` BIGINT PK AI, `phrase_id` FK, `category_id` FK, UNIQUE `(phrase_id, category_id)` |
| `phrase_tags` | `id` BIGINT PK AI, `phrase_id` FK, `tag_id` FK, UNIQUE `(phrase_id, tag_id)` |

- All PKs `BIGINT`, auto-generated (`GenerationType.IDENTITY` for MySQL). No UUIDs.
- `year` and `source` are optional on phrases. No `Work`/`Book` entity in MVP.
- **Flyway migration sequence** (must match exactly, no gaps):
  1. `V1__create_authors.sql`
  2. `V2__create_categories.sql`
  3. `V3__create_tags.sql`
  4. `V4__create_phrases.sql`
  5. `V5__create_phrase_categories.sql`
  6. `V6__create_phrase_tags.sql`
  7. `V7__seed_data.sql`  ← seed follows the six schema migrations, NOT an arbitrary V8.
- `spring.jpa.hibernate.ddl-auto=validate` (schema owned by Flyway).

## 5. Backend architecture

Package root `com.phraseforge.phraseforge_api`. Domain-based packages, each owning
its `controller`, `service`, `repository`, `dto`, `entity`:

```
author/    category/    tag/    phrase/
```

Shared:

```
common/      AuditableEntity, PagedResponse<T>, SlugUtil
config/      CorsConfig, OpenApiConfig
exception/   ApiExceptionHandler (@RestControllerAdvice), ResourceNotFoundException, DuplicateResourceException, ApiError
```

- **Auditing:** `AuditableEntity` with `@CreatedDate`/`@LastModifiedDate`
  (`Instant`), `@EnableJpaAuditing`, `@MappedSuperclass`. Audited entities extend it.
  No per-entity repetition.
- **Entities** (JPA maps DB exactly):
  - `Author` 1:N `Phrase` (`OneToMany` on author, `ManyToOne` on phrase).
  - `Phrase` N:N `Category` and N:N `Tag` **via explicit join entities**
    `PhraseCategory` and `PhraseTag` (`@ManyToOne` both sides), mirroring the
    `phrase_categories`/`phrase_tags` tables. No unchecked `@ManyToMany`.
- **Repositories:** Spring Data JPA. Custom query methods for the duplicate check
  and filters. `@EntityGraph` where needed to avoid N+1.
- **Services:** business rules live here. Simple explicit methods, no generic CRUD
  framework, no excessive interfaces.
- **Mappers:** plain static/`@Component` mapper classes per domain
  (`AuthorMapper`, `PhraseMapper`, …). No MapStruct (keep dependency surface small
  and code explicit).
- **DTOs:** request DTOs with Bean Validation, response DTOs. Detailed view vs.
  summary view where they genuinely differ (`PhraseResponse` detailed,
  `PhraseSummaryResponse` in listings).

## 6. API (`/api/v1`)

- **Phrases:** `GET /phrases` (paged, filters `query`, `authorId`, `categoryId`,
  `tagId`, `language`), `GET /phrases/{id}`, `GET /phrases/random`,
  `POST /phrases`, `PUT /phrases/{id}`, `DELETE /phrases/{id}`.
  - Route ordering: `/phrases/random` registered before `/phrases/{id}`.
- **Authors:** `GET /authors` (paged), `GET /authors/{id}`,
  `GET /authors/{id}/phrases`, `POST`, `PUT /authors/{id}`, `DELETE /authors/{id}`.
- **Categories:** `GET /categories`, `GET /categories/{id}`,
  `GET /categories/{id}/phrases`, `POST`, `PUT`, `DELETE`.
- **Tags:** `GET /tags`, `POST`, `PUT /tags/{id}`, `DELETE /tags/{id}`.
  Tag↔phrase association is managed through phrase create/update.

**Paged response shape:** `{ content, page, size, totalElements, totalPages }`.

**Business rules:**
- Phrase: content required, author required, language required; year/source optional.
- Author: name required, unique slug (server-generated from name), optional years.
  Duplicate name → 409.
- Category: name required, unique slug. Tag: name required, unique.
- **Duplicate phrase rule:** same `content` + same `author` → 409, enforced in the
  service layer (not a DB unique constraint). Same content under different authors is
  allowed.

## 7. Validation & errors

- Bean Validation on all request DTOs (`@NotBlank`, `@Size`, `@Min`, `@Max`,
  pattern for language code, etc.).
- Global `@RestControllerAdvice`:
  - `400` validation/illegal arguments,
  - `404` not found,
  - `409` duplicate/slug conflict (including `DataIntegrityViolationException` for
    unique-constraint races),
  - `500` generic (logged, no stack trace to client).
- Error body: `{ "status", "message", "timestamp" }`.

## 8. Backend tests

- **Unit (service):** Mockito. Duplicate-phrase rule, author/category/tag creation
  and slug generation, not-found paths.
- **Repository:** `@DataJpaTest` against **H2 in MySQL mode** (test-scope-only
  dependency; H2 never touches production config). Flyway migrations run against H2
  with a dialect-tolerant migration set where needed. Document any MySQL-specific
  behavior H2 cannot reproduce rather than over-engineering.
- **Controller:** `@WebMvcTest` + MockMvc for main endpoints and HTTP codes.
- No Testcontainers in MVP. H2 is test-scope-only; MySQL remains the only
  production/dev database.

## 9. OpenAPI

- springdoc-openapi dependency; Swagger UI at `/swagger-ui.html`.
- `OpenApiConfig` with API title, version, description.
- DTO annotations where helpful for schema clarity.

## 10. Frontend

Vite + React 19 + TS + React Router + Tailwind v4 + TanStack Query.

**Visual tokens — reproduced from the prototype (source of truth):**

| Token | Value |
|---|---|
| `--bg` | `#f9f8f6` (paper) |
| `--bg-card` | `#ffffff` |
| `--text` | `#111110` |
| `--text-secondary` | `#6b6b68` |
| `--text-tertiary` | `#a8a8a4` |
| `--border` | `#e4e3e0` |
| `--border-subtle` | `#eeede9` |
| `--radius` | `3px` |
| Display font | Lora (serif), quotes italic |
| UI font | Inter (sans) |
| Body | 15px, line-height 1.6 |

**Shared layout:**
- Sticky header, 52px, bottom `--border-subtle`; Lora wordmark "PhraseForge";
  desktop nav buttons (Explore, Autores, Categorias) with active state; mobile
  hamburger + slide-down menu.
- Max-width 1040px container; generous padding (`4rem 2rem`).
- Uppercase micro-labels: `0.6875rem`, weight 600, letter-spacing `0.1em`,
  color `--text-tertiary`.
- Page titles: Lora, `clamp(2rem, 4vw, 2.75rem)`, weight 400, letter-spacing `-0.02em`.
- Pill buttons: border `--border`, radius 99px, hover fills with ink.
- Quote cards / list rows: hairline dividers `--border-subtle`.

**UI language: Portuguese** (user decision). Prototype English labels are translated
(e.g. "Explore" → "Explorar", "New Quote" → "Nova Frase", "Share" → "Compartilhar",
"Copy" → "Copiar", "Authors" → "Autores", "Categories" → "Categorias"). Quote content
remains as authored.
- **Year display:** `year` stays `SMALLINT` (schema unchanged). The MVP intentionally
  does **not** model BCE/CE eras or approximate dates (e.g. "c. 170 AD"). Where the
  source data is uncertain, numeric years must not be presented as historically
  precise — the frontend renders the year as a plain number, and seed data omits
  `year` for quotes with uncertain dating. This deviation from the prototype's
  "c. 170 AD" formatting is documented in the README.

**Structure:**

```
src/
├── components/   Header, Footer, Layout, QuoteCard, QuoteActions, Pagination, SearchInput, Chip, PillButton, EmptyState, Loader, ErrorState, admin/*
├── layouts/      PublicLayout, AdminLayout
├── pages/        Home, Explore, QuoteDetail, Authors, AuthorDetail, Categories, CategoryDetail, admin/{Dashboard, Phrases, Authors, Categories, Tags, PhraseForm, AuthorForm, CategoryForm, TagForm}
├── services/     api.ts (central client), phrases.ts, authors.ts, categories.ts, tags.ts
├── hooks/        usePhrases, usePhrase, useRandomPhrase, useAuthors, useAuthor, useAuthorPhrases, useCategories, useCategory, useCategoryPhrases, useTags, useAdminCounts
├── types/        models.ts (Author, Phrase, Category, Tag, Paged<T>)
├── lib/          utils.ts (clipboard, formatYear, slug helpers)
└── main.tsx / App.tsx (router)
```

- **Central HTTP client** in `services/api.ts` (`fetch`-based, base URL from
  `VITE_API_URL` env with `/api/v1` default), API functions in per-domain files.
  No HTTP calls inside components.
- TanStack Query for server state; React Router for routing.
- **Pages (per prototype):**
  - `/` **Home:** one random phrase centered; category micro-label, Lora italic
    quote, author + year, actions (Nova Frase, Copiar, Compartilhar), transition
    on new quote.
  - `/explore` **Explorar:** Lora heading, search input with icon, category filter
    pills, result count, grid of quote cards (hairline-bordered, hover bg-card),
    pagination.
  - `/phrases/:id` **Quote Detail:** back link, category label, quote, author/year,
    chip row (categories + tags + language + source), actions (Copiar,
    Compartilhar), "Mais de {autor}" related list.
  - `/authors` **Autores:** Lora heading, search, list rows (name, description,
    phrase count) with hairlines.
  - `/authors/:id` **Author Detail:** back link, name, birth/death years,
    biography, phrase count, phrase list.
  - `/categories` **Categorias:** Lora heading, list rows (name, description,
    phrase count, arrow).
  - `/categories/:id` **Category Detail:** category phrases (filtered view).
  - `/admin` **Admin:** slim sidebar (Dashboard, Frases, Autores, Categorias,
    Tags), stat cards, tables, add/edit forms. Functional but consistent identity.
    No auth in MVP.
- No favorites/hearts anywhere (V1).

## 11. Docker

`docker-compose.yml`:
- `db`: MySQL 8, env-driven credentials, volume, named volume.
  **Healthcheck** (`mysqladmin ping`) with the backend `depends_on` MySQL
  `condition: service_healthy` — no `sleep` hacks.
- `backend`: built from `backend/phraseforge-api/Dockerfile`, depends on healthy db,
  exposes 8080, env for DB vars.
- `frontend`: built from `frontend/Dockerfile`, served by Nginx on host port 5173,
  depends on the backend, proxies `/api` to the backend, and serves the React SPA
  fallback for client-side routes.

## 12. Configuration & secrets

- Env vars: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
  (+ `VITE_API_URL` for frontend; local development uses `frontend/.env.local`,
  Docker uses a build argument with `/api/v1` as default).
- `.env.example` committed; real `.env` gitignored. No real credentials committed.
- CORS: dev origins allowlist (e.g. `http://localhost:5173`) via config, not
  blanket `*`.

## 13. Seed data

`V7__seed_data.sql` inserts ~7-8 authors (Marcus Aurelius, Sócrates, Aristóteles,
Friedrich Nietzsche, Simone de Beauvoir, Albert Einstein, Virginia Woolf, …),
~12-20 phrases with categories and tags, linked via `phrase_categories`/
`phrase_tags`. No external API dependency.

## 14. Out of scope (do NOT implement)

Users, authentication, JWT, Spring Security, roles, favorites, weekly ranking,
recommendations, history, comments, ratings, notifications, AI generation, Redis,
Kafka, microservices, work/book entities.

## 15. Implementation order

1. Backend deps/config → 2. MySQL config → 3. Flyway migrations → 4. Entities →
5. Repositories → 6. Services → 7. DTOs → 8. Validation → 9. Exception handling →
10. Controllers → 11. Backend tests → 12. OpenAPI → 13. React structure →
14. Shared layout/components → 15. Home → 16. Explore → 17. Quote Detail →
18. Authors → 19. Categories → 20. Admin → 21. Frontend/backend integration →
22. Docker Compose → 23. README/documentation.

## 16. Definition of done

- MySQL starts via Docker; Flyway migrations run cleanly; schema matches model.
- CRUD works for phrases, authors, categories, tags; relationships, search,
  filters, pagination, and random phrase work.
- Validations and consistent error responses work.
- Backend tests exist and pass.
- OpenAPI/Swagger reachable.
- React consumes the real API; main prototype screens implemented.
- README documents run instructions; prototype present in repo.
- No V1/V2 functionality; no secrets committed.
