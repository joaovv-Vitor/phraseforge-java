# PhraseForge MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete PhraseForge MVP — a Spring Boot 4.1 (Java 25) REST API backed by MySQL/Flyway, plus a Vite/React/TS frontend reproducing the Figma prototype's editorial design — running via Docker Compose.

**Architecture:** Monolithic Spring Boot app organized by domain (`author/`, `category/`, `tag/`, `phrase/`) under package `com.phraseforge.phraseforge_api`, layered controller→service→repository→entity with DTOs (no entities exposed). Explicit join entities (`PhraseCategory`, `PhraseTag`) for N:N relationships. React frontend with TanStack Query + React Router, centralized HTTP client, Tailwind v4 with the prototype's tokens.

**Tech Stack:** Java 25, Spring Boot 4.1.0, Spring MVC, Spring Data JPA, Hibernate, MySQL 8, Flyway 12.4.0, Bean Validation, Lombok, Actuator, springdoc-openapi 3.1.0, H2 (test-only); React 19, TypeScript, Vite 8, React Router 7, Tailwind v4, TanStack Query 5; Docker Compose.

**Spec:** `docs/superpowers/specs/2026-08-17-phraseforge-mvp-design.md`

## Global Constraints

- Java 25, Spring Boot parent 4.1.0 (already in `backend/phraseforge-api/pom.xml`). No version overrides in pom except explicit third-party deps.
- Package root: `com.phraseforge.phraseforge_api`. Domain-based packages, no top-level layer packages.
- Schema created ONLY by Flyway. `spring.jpa.hibernate.ddl-auto=validate`.
- Flyway migration files (exact sequence, no gaps): `V1__create_authors.sql`, `V2__create_categories.sql`, `V3__create_tags.sql`, `V4__create_phrases.sql`, `V5__create_phrase_categories.sql`, `V6__create_phrase_tags.sql`, `V7__seed_data.sql`.
- All PKs BIGINT `GenerationType.IDENTITY`. No UUIDs. Tables: authors, phrases, categories, tags, phrase_categories, phrase_tags. NO users/favorites tables.
- API prefix `/api/v1`. Phrase listing supports pagination + filters `query`, `authorId`, `categoryId`, `tagId`, `language`. Route `/phrases/random` must be declared before `/phrases/{id}`.
- DTOs for all requests/responses. Entities never serialized directly.
- Duplicate phrase rule (same content + same author) enforced in service layer → 409.
- Slug uniqueness and tag/category/author name uniqueness enforced in service → 409.
- Error body: `{ "status", "message", "timestamp" }` via `@RestControllerAdvice`; 400/404/409/500.
- H2 test-scope-only. MySQL is the only runtime DB. No Testcontainers.
- **H2 test isolation:** tests run against V1–V6 only — `V7__seed_data.sql` is EXCLUDED from the H2 test context (test `application.properties` sets `spring.flyway.target=6`). Tests start from an empty database and create their own fixture data. MySQL dev/runtime runs the full V1–V7 sequence (schema + seed).
- **Test fixtures are self-contained:** no test may rely on seeded records or seeded names. Repository tests build their own authors/categories/tags/phrases with neutral names (e.g. "Test Author") and assert only against their own fixtures.
- Migrations SQL must run on BOTH MySQL and H2 in MySQL mode (avoid `ENGINE=InnoDB` clauses and native/MySQL-only SQL).
- No `sleep` in docker-compose; backend `depends_on` MySQL with `condition: service_healthy` + MySQL healthcheck.
- UI language: Portuguese. No favorites/hearts anywhere (V1 feature). No auth/security.
- UI text/labels in Portuguese; quote content left as authored.
- `.env.example` committed; `.env` gitignored; no real credentials.
- Frontend: `npm run build` (tsc -b && vite build) and `npm run lint` (oxlint) must pass. Backend: `./mvnw test` must pass.
- **Phrase year semantics:** `phrases.year` (SMALLINT) represents the year associated with the phrase/source — not necessarily the exact date the phrase was spoken or written. The MVP does NOT model BCE/CE eras or approximate dates. Years are never presented as historically precise; seed data omits `year` where dating is uncertain. Documented in README.
- **Educational workflow:** after EVERY completed task, the implementer provides a short summary covering: what was implemented; important Java/Spring concepts used; architectural decisions; tests added; and what should be studied before proceeding. Keep the implementation simple; no unnecessary abstractions.

---

## Task 1: Backend dependencies, env config, and CORS

**Files:**
- Modify: `backend/phraseforge-api/pom.xml` (add 4 dependencies)
- Modify: `backend/phraseforge-api/src/main/resources/application.properties`
- Create: `backend/phraseforge-api/src/test/resources/application.properties`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/config/CorsConfig.java`

**Interfaces:**
- Produces: runtime DB config driven by env vars `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`; test DB config using H2 MySQL mode; CORS allowlist property `app.cors.allowed-origins`.

- [ ] **Step 1: Add dependencies to pom.xml**

Insert before the `</dependencies>` closing tag (after the existing test starters):

```xml
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-core</artifactId>
		</dependency>
		<dependency>
			<groupId>org.flywaydb</groupId>
			<artifactId>flyway-mysql</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>3.1.0</version>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>test</scope>
		</dependency>
```

`flyway-core` and `flyway-mysql` need no version (managed by Spring Boot 4.1). `h2` is test-scope-only, version managed.

- [ ] **Step 2: Rewrite `application.properties`**

```properties
spring.application.name=phraseforge-api

# MySQL datasource driven by environment variables (see .env.example)
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:phraseforge}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME:phraseforge}
spring.datasource.password=${DB_PASSWORD:phraseforge}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Schema is owned by Flyway; Hibernate only validates it
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false

# Flyway
spring.flyway.enabled=true

# Application-specific
app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:5173}

# Actuator
management.endpoints.web.exposure.include=health,info

# OpenAPI / Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html
```

- [ ] **Step 3: Create test application.properties**

`backend/phraseforge-api/src/test/resources/application.properties`:

```properties
# Tests run against H2 in MySQL compatibility mode (test-scope-only dependency).
# Production and development always use MySQL (see main application.properties).
spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
# Tests run schema migrations V1-V6 only. V7__seed_data.sql is excluded so
# tests start from an EMPTY database and build their own fixtures. MySQL
# development/runtime applies V1-V7 (schema + seed).
spring.flyway.target=6

app.cors.allowed-origins=http://localhost:5173
```

- [ ] **Step 4: Create CORS config**

`backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/config/CorsConfig.java`:

```java
package com.phraseforge.phraseforge_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Dev allowlist only; tightened for production via CORS_ALLOWED_ORIGINS env var.
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

- [ ] **Step 5: Verify build and context loads**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: `PhraseforgeApiApplicationTests.contextLoads` PASSES (uses H2 test config — no MySQL needed). All tests green.

- [ ] **Step 6: Commit**

```bash
git add backend/phraseforge-api/pom.xml backend/phraseforge-api/src/main/resources/application.properties backend/phraseforge-api/src/test/resources/application.properties backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/config/CorsConfig.java
git commit -m "feat(backend): add flyway, openapi, h2 test db, and CORS config"
```

### Educational summary

- **Java/Spring concepts:** dependency management via Spring Boot starter parent; env-var placeholders in `application.properties` (`${DB_HOST:localhost}`); CORS via `WebMvcConfigurer`; test-slice awareness (test `application.properties` overrides the datasource).
- **Architectural decision:** a separate test properties file points tests at H2 (MySQL mode) while production config stays MySQL-only.
- **Study before proceeding:** Spring property binding, profiles vs. separate test properties, how `@WebMvcTest`/`@DataJpaTest` slices pick up config.

---

## Task 2: Flyway migrations (V1–V6 schema)

**Files:**
- Create: `backend/phraseforge-api/src/main/resources/db/migration/V1__create_authors.sql`
- Create: `backend/phraseforge-api/src/main/resources/db/migration/V2__create_categories.sql`
- Create: `backend/phraseforge-api/src/main/resources/db/migration/V3__create_tags.sql`
- Create: `backend/phraseforge-api/src/main/resources/db/migration/V4__create_phrases.sql`
- Create: `backend/phraseforge-api/src/main/resources/db/migration/V5__create_phrase_categories.sql`
- Create: `backend/phraseforge-api/src/main/resources/db/migration/V6__create_phrase_tags.sql`

**Interfaces:**
- Produces: MySQL-compatible schema that H2 (MODE=MySQL) can also execute. Column names/types are the contract for the JPA entities in Task 4.

- [ ] **Step 1: Write V1__create_authors.sql**

```sql
CREATE TABLE authors (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(180) NOT NULL,
    birth_year  SMALLINT     NULL,
    death_year  SMALLINT     NULL,
    biography   TEXT         NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_authors PRIMARY KEY (id),
    CONSTRAINT uk_authors_slug UNIQUE (slug)
);
```

- [ ] **Step 2: Write V2__create_categories.sql**

```sql
CREATE TABLE categories (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)  NOT NULL,
    slug        VARCHAR(120)  NOT NULL,
    description VARCHAR(500)  NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);
```

- [ ] **Step 3: Write V3__create_tags.sql**

```sql
CREATE TABLE tags (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT pk_tags PRIMARY KEY (id),
    CONSTRAINT uk_tags_name UNIQUE (name)
);
```

- [ ] **Step 4: Write V4__create_phrases.sql**

```sql
CREATE TABLE phrases (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    content     TEXT         NOT NULL,
    author_id   BIGINT       NOT NULL,
    year        SMALLINT     NULL,
    language    VARCHAR(10)  NOT NULL,
    source      VARCHAR(300) NULL,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    CONSTRAINT pk_phrases PRIMARY KEY (id),
    CONSTRAINT fk_phrases_author FOREIGN KEY (author_id) REFERENCES authors (id)
);
CREATE INDEX idx_phrases_author_id ON phrases (author_id);
```

- [ ] **Step 5: Write V5__create_phrase_categories.sql**

```sql
CREATE TABLE phrase_categories (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    phrase_id   BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    CONSTRAINT pk_phrase_categories PRIMARY KEY (id),
    CONSTRAINT fk_phrase_categories_phrase FOREIGN KEY (phrase_id) REFERENCES phrases (id),
    CONSTRAINT fk_phrase_categories_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT uk_phrase_categories UNIQUE (phrase_id, category_id)
);
```

- [ ] **Step 6: Write V6__create_phrase_tags.sql**

```sql
CREATE TABLE phrase_tags (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    phrase_id BIGINT NOT NULL,
    tag_id    BIGINT NOT NULL,
    CONSTRAINT pk_phrase_tags PRIMARY KEY (id),
    CONSTRAINT fk_phrase_tags_phrase FOREIGN KEY (phrase_id) REFERENCES phrases (id),
    CONSTRAINT fk_phrase_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (id),
    CONSTRAINT uk_phrase_tags UNIQUE (phrase_id, tag_id)
);
```

- [ ] **Step 7: Verify migrations run on H2**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: `contextLoads` still passes. Flyway applies V1–V6 to H2 during context startup (validate-only schema). If a migration fails on H2, fix the SQL so it is portable (no `ENGINE=InnoDB`, no MySQL-only keywords) and rerun.

- [ ] **Step 8: Commit**

```bash
git add backend/phraseforge-api/src/main/resources/db/migration/
git commit -m "feat(backend): add Flyway schema migrations V1-V6"
```

### Educational summary

- **Java/Spring concepts:** Flyway versioned migrations; why `ddl-auto=validate` keeps Hibernate from owning the schema; portable SQL (no `ENGINE=` clauses) so H2 can execute the same files.
- **Architectural decision:** schema defined once in SQL, applied to both MySQL (runtime) and H2 (tests).
- **Study before proceeding:** Flyway checksums/locations, why migrations must be immutable once applied.

---

## Task 3: Auditing base entity and common support types

**Files:**
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/common/AuditableEntity.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/common/PagedResponse.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/common/SlugUtil.java`
- Modify: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/PhraseforgeApiApplication.java`

**Interfaces:**
- Produces: `AuditableEntity` (extends in every main entity; `getId()` returns `Long`), `PagedResponse<T>` with static `from(org.springframework.data.domain.Page<T>)`, `SlugUtil.toSlug(String)`.

- [ ] **Step 1: Enable JPA auditing on the application class**

`PhraseforgeApiApplication.java` — add `@EnableJpaAuditing`:

```java
package com.phraseforge.phraseforge_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PhraseforgeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhraseforgeApiApplication.class, args);
	}

}
```

- [ ] **Step 2: Create AuditableEntity**

`common/AuditableEntity.java` — reuses id/audit fields across all entities (no repetition):

```java
package com.phraseforge.phraseforge_api.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Base class carrying the BIGINT auto-increment id and created_at/updated_at
 * auditing columns shared by every domain entity. Auditing values are filled
 * automatically by Spring Data JPA's AuditingEntityListener.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
```

- [ ] **Step 3: Create PagedResponse**

`common/PagedResponse.java`:

```java
package com.phraseforge.phraseforge_api.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
```

- [ ] **Step 4: Create SlugUtil**

`common/SlugUtil.java` — strips accents (NFD normalization) so "Sócrates" → "socrates":

```java
package com.phraseforge.phraseforge_api.common;

import java.text.Normalizer;
import java.util.Locale;

public final class SlugUtil {

    private SlugUtil() {
    }

    public static String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? "untitled" : slug;
    }
}
```

- [ ] **Step 5: Verify**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: green.

- [ ] **Step 6: Commit**

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/common/ backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/PhraseforgeApiApplication.java
git commit -m "feat(backend): add auditable base entity, paged response, slug util"
```

### Educational summary

- **Java/Spring concepts:** `@MappedSuperclass` + `@EntityListeners(AuditingEntityListener.class)`; `@CreatedDate`/`@LastModifiedDate` with `@EnableJpaAuditing`; records for DTO-like carriers; `GenerationType.IDENTITY` for MySQL auto-increment.
- **Architectural decision:** shared `AuditableEntity` removes per-entity audit repetition; `PagedResponse<T>` standardizes paging.
- **Study before proceeding:** how JPA auditing hooks into the persistence lifecycle; `@PrePersist` vs Spring Data auditing.

---

## Task 4: Author domain — entity, repository, DTOs, mapper

**Files:**
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/Author.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/AuthorRepository.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/dto/AuthorSummaryResponse.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/dto/AuthorResponse.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/dto/CreateAuthorRequest.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/dto/UpdateAuthorRequest.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/AuthorMapper.java`

**Interfaces:**
- Produces (used by Task 5 service/controller):
  - `Author` entity: `Long getId()`, `String getName()`, `String getSlug()`, `Integer getBirthYear()`, `Integer getDeathYear()`, `String getBiography()`; setters for all mutable fields.
  - `AuthorRepository extends JpaRepository<Author, Long>`: `Optional<Author> findBySlug(String)`, `Optional<Author> findByName(String)`, `boolean existsByName(String)`, `boolean existsBySlug(String)`, `List<Object[]> findPhraseCounts()` returning rows `[authorId(Long), count(Long)]`.
  - `AuthorMapper` (component): `AuthorSummaryResponse toSummary(Author author, long phraseCount)`, `AuthorResponse toResponse(Author author, long phraseCount)`.

- [ ] **Step 1: Write the failing entity test helper — first the entity**

`author/Author.java`:

```java
package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.common.AuditableEntity;
import com.phraseforge.phraseforge_api.phrase.Phrase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "authors")
public class Author extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 180)
    private String slug;

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "death_year")
    private Integer deathYear;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    @OneToMany(mappedBy = "author")
    private List<Phrase> phrases = new ArrayList<>();

    protected Author() {
    }

    public Author(String name, String slug, Integer birthYear, Integer deathYear, String biography) {
        this.name = name;
        this.slug = slug;
        this.birthYear = birthYear;
        this.deathYear = deathYear;
        this.biography = biography;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Integer getDeathYear() {
        return deathYear;
    }

    public void setDeathYear(Integer deathYear) {
        this.deathYear = deathYear;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public List<Phrase> getPhrases() {
        return phrases;
    }
}
```

- [ ] **Step 2: Write the failing repository test**

Create `backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/author/AuthorRepositoryTest.java`:

```java
package com.phraseforge.phraseforge_api.author;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthorRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void findBySlug_returnsAuthor() {
        authorRepository.save(new Author("Test Author", "test-author", 1900, 1980, null));

        Optional<Author> found = authorRepository.findBySlug("test-author");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Author");
    }

    @Test
    void findPhraseCounts_returnsZeroWhenNoPhrases() {
        authorRepository.save(new Author("Test Author", "test-author", null, null, null));

        List<Object[]> counts = authorRepository.findPhraseCounts();

        assertThat(counts).hasSize(1);
        assertThat((Long) counts.get(0)[0]).isNotNull();
        assertThat((Long) counts.get(0)[1]).isZero();
    }
}
```

Note: tests build their own fixtures (empty DB — V7 seed is excluded) and use
neutral names; no test depends on seeded data. `List` needs
`import java.util.List;`. H2 (MODE=MySQL) supports the schema from V1.

- [ ] **Step 3: Run to verify it fails**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=AuthorRepositoryTest`
Expected: COMPILATION FAILURE — `AuthorRepository` does not exist.

- [ ] **Step 4: Create the repository**

`author/AuthorRepository.java`:

```java
package com.phraseforge.phraseforge_api.author;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findBySlug(String slug);

    Optional<Author> findByName(String name);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    /**
     * Returns [authorId, phraseCount] pairs for all authors (0 for authors
     * without phrases). A single GROUP BY query avoids per-author N+1 counts.
     */
    @Query("select a.id, count(p) from Author a left join a.phrases p group by a.id")
    List<Object[]> findPhraseCounts();
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=AuthorRepositoryTest`
Expected: PASS.

- [ ] **Step 6: Create the DTOs**

`author/dto/AuthorSummaryResponse.java`:

```java
package com.phraseforge.phraseforge_api.author.dto;

public record AuthorSummaryResponse(
        Long id,
        String name,
        String slug,
        Integer birthYear,
        Integer deathYear,
        long phraseCount) {
}
```

`author/dto/AuthorResponse.java`:

```java
package com.phraseforge.phraseforge_api.author.dto;

import java.time.Instant;

public record AuthorResponse(
        Long id,
        String name,
        String slug,
        Integer birthYear,
        Integer deathYear,
        String biography,
        long phraseCount,
        Instant createdAt,
        Instant updatedAt) {
}
```

`author/dto/CreateAuthorRequest.java`:

```java
package com.phraseforge.phraseforge_api.author.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAuthorRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Min(value = -10000, message = "Birth year is out of range")
        @Max(value = 10000, message = "Birth year is out of range")
        Integer birthYear,

        @Min(value = -10000, message = "Death year is out of range")
        @Max(value = 10000, message = "Death year is out of range")
        Integer deathYear,

        @Size(max = 10000, message = "Biography must be at most 10000 characters")
        String biography) {
}
```

`author/dto/UpdateAuthorRequest.java` (same shape as Create — PUT is a full update):

```java
package com.phraseforge.phraseforge_api.author.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAuthorRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Min(value = -10000, message = "Birth year is out of range")
        @Max(value = 10000, message = "Birth year is out of range")
        Integer birthYear,

        @Min(value = -10000, message = "Death year is out of range")
        @Max(value = 10000, message = "Death year is out of range")
        Integer deathYear,

        @Size(max = 10000, message = "Biography must be at most 10000 characters")
        String biography) {
}
```

- [ ] **Step 7: Create the mapper**

`author/AuthorMapper.java`:

```java
package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.AuthorSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthorMapper {

    public AuthorSummaryResponse toSummary(Author author, long phraseCount) {
        return new AuthorSummaryResponse(
                author.getId(),
                author.getName(),
                author.getSlug(),
                author.getBirthYear(),
                author.getDeathYear(),
                phraseCount);
    }

    public AuthorResponse toResponse(Author author, long phraseCount) {
        return new AuthorResponse(
                author.getId(),
                author.getName(),
                author.getSlug(),
                author.getBirthYear(),
                author.getDeathYear(),
                author.getBiography(),
                phraseCount,
                author.getCreatedAt(),
                author.getUpdatedAt());
    }
}
```

- [ ] **Step 8: Run full test suite**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: all green.

- [ ] **Step 9: Commit**

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/ backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/author/
git commit -m "feat(backend): add author entity, repository, DTOs, and mapper"
```

### Educational summary

- **Java/Spring concepts:** JPA `@Entity`/`@Table` mapping to an existing schema; Spring Data derived queries (`findBySlug`, `existsBySlug`); a `@Query` with `GROUP BY` projection to compute counts without N+1; records for request/response DTOs; `@DataJpaTest` for repository tests against H2.
- **Architectural decision:** entity kept thin; mapping to DTOs done in a dedicated mapper.
- **Study before proceeding:** lazy vs eager fetching; `@DataJpaTest` transaction rollback semantics.

---

## Task 5: Author service and controller

**Files:**
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/exception/ResourceNotFoundException.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/exception/DuplicateResourceException.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/AuthorService.java`
- Create: `backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/AuthorController.java`

**Interfaces:**
- Consumes: `AuthorRepository`, `AuthorMapper`, `SlugUtil`, DTOs from Task 4.
- Produces: `AuthorService` with `PagedResponse<AuthorSummaryResponse> list(Pageable)`, `AuthorResponse getById(Long)`, `AuthorResponse create(CreateAuthorRequest)`, `AuthorResponse update(Long, UpdateAuthorRequest)`, `void delete(Long)`. `AuthorController` under `/api/v1/authors` with endpoints `GET ""`, `GET "/{id}"`, `POST ""`, `PUT "/{id}"`, `DELETE "/{id}"`.
- Produces (shared, used by later tasks): `ResourceNotFoundException`, `DuplicateResourceException`.

- [ ] **Step 1: Create the exception classes**

`exception/ResourceNotFoundException.java`:

```java
package com.phraseforge.phraseforge_api.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

`exception/DuplicateResourceException.java`:

```java
package com.phraseforge.phraseforge_api.exception;

public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Write the failing service test**

Create `backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/author/AuthorServiceTest.java`:

```java
package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import com.phraseforge.phraseforge_api.author.dto.UpdateAuthorRequest;
import com.phraseforge.phraseforge_api.common.SlugUtil;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;

    @Spy
    private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorService authorService;

    @Test
    void create_generatesSlugAndSaves() {
        when(authorRepository.existsByName("Test Author")).thenReturn(false);
        when(authorRepository.existsBySlug("test-author")).thenReturn(false);
        when(authorRepository.save(any(Author.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateAuthorRequest("Test Author", 1900, 1980, "Test biography");
        var response = authorService.create(request);

        assertThat(response.slug()).isEqualTo("test-author");
        assertThat(response.name()).isEqualTo("Test Author");
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(authorRepository.existsByName("Test Author")).thenReturn(true);

        var request = new CreateAuthorRequest("Test Author", null, null, null);

        assertThatThrownBy(() -> authorService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    void update_missingAuthor_throwsNotFound() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        var request = new UpdateAuthorRequest("New", null, null, null);

        assertThatThrownBy(() -> authorService.update(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

Note: `SlugUtil` is a static utility, so no mock needed. The test asserts the service generates the slug via `SlugUtil.toSlug`.

- [ ] **Step 3: Run to verify it fails**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=AuthorServiceTest`
Expected: COMPILATION FAILURE — `AuthorService` missing.

- [ ] **Step 4: Create the service**

`author/AuthorService.java`:

```java
package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.AuthorSummaryResponse;
import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import com.phraseforge.phraseforge_api.author.dto.UpdateAuthorRequest;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.common.SlugUtil;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    public AuthorService(AuthorRepository authorRepository, AuthorMapper authorMapper) {
        this.authorRepository = authorRepository;
        this.authorMapper = authorMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuthorSummaryResponse> list(Pageable pageable) {
        Page<Author> page = authorRepository.findAll(pageable);
        Map<Long, Long> counts = phraseCounts();
        return PagedResponse.from(page.map(author ->
                authorMapper.toSummary(author, counts.getOrDefault(author.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public AuthorResponse getById(Long id) {
        Author author = findByIdOrThrow(id);
        return authorMapper.toResponse(author, phraseCountFor(id));
    }

    @Transactional
    public AuthorResponse create(CreateAuthorRequest request) {
        ensureNameAvailable(request.name());
        Author author = new Author(
                request.name(),
                SlugUtil.toSlug(request.name()),
                request.birthYear(),
                request.deathYear(),
                request.biography());
        ensureSlugAvailable(author.getSlug());
        Author saved = authorRepository.save(author);
        return authorMapper.toResponse(saved, 0L);
    }

    @Transactional
    public AuthorResponse update(Long id, UpdateAuthorRequest request) {
        Author author = findByIdOrThrow(id);
        ensureNameAvailableForUpdate(request.name(), author);
        author.setName(request.name());
        author.setSlug(SlugUtil.toSlug(request.name()));
        author.setBirthYear(request.birthYear());
        author.setDeathYear(request.deathYear());
        author.setBiography(request.biography());
        return authorMapper.toResponse(author, phraseCountFor(id));
    }

    @Transactional
    public void delete(Long id) {
        Author author = findByIdOrThrow(id);
        if (!author.getPhrases().isEmpty()) {
            throw new DuplicateResourceException(
                    "Author with id " + id + " has phrases and cannot be deleted");
        }
        authorRepository.delete(author);
    }

    private Author findByIdOrThrow(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + id));
    }

    private void ensureNameAvailable(String name) {
        if (authorRepository.existsByName(name)) {
            throw new DuplicateResourceException("Author already exists: " + name);
        }
    }

    private void ensureNameAvailableForUpdate(String name, Author current) {
        if (!current.getName().equals(name) && authorRepository.existsByName(name)) {
            throw new DuplicateResourceException("Author already exists: " + name);
        }
    }

    private void ensureSlugAvailable(String slug) {
        if (authorRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Author slug already exists: " + slug);
        }
    }

    private Map<Long, Long> phraseCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : authorRepository.findPhraseCounts()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private long phraseCountFor(Long authorId) {
        return authorRepository.findPhraseCounts().stream()
                .filter(row -> ((Long) row[0]).equals(authorId))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);
    }
}
```

- [ ] **Step 5: Run the service test to verify it passes**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=AuthorServiceTest`
Expected: PASS.

- [ ] **Step 6: Write the failing controller test**

Create `backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/author/AuthorControllerTest.java`:

```java
package com.phraseforge.phraseforge_api.author;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthorService authorService;

    @Test
    void getById_returnsAuthor() throws Exception {
        when(authorService.getById(1L)).thenReturn(
                new AuthorResponse(1L, "Test Author", "test-author", null, null, null, 0L, null, null));

        mockMvc.perform(get("/api/v1/authors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Author")))
                .andExpect(jsonPath("$.slug", is("test-author")));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(authorService.create(any(CreateAuthorRequest.class))).thenReturn(
                new AuthorResponse(1L, "Test Author", "test-author", null, null, null, 0L, null, null));

        String body = objectMapper.writeValueAsString(
                new CreateAuthorRequest("Test Author", null, null, null));

        mockMvc.perform(post("/api/v1/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Author")));
    }
}
```

- [ ] **Step 7: Run to verify it fails**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=AuthorControllerTest`
Expected: FAIL — controller missing.

- [ ] **Step 8: Create the controller**

`author/AuthorController.java`:

```java
package com.phraseforge.phraseforge_api.author;

import com.phraseforge.phraseforge_api.author.dto.AuthorResponse;
import com.phraseforge.phraseforge_api.author.dto.AuthorSummaryResponse;
import com.phraseforge.phraseforge_api.author.dto.CreateAuthorRequest;
import com.phraseforge.phraseforge_api.author.dto.UpdateAuthorRequest;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @GetMapping
    public PagedResponse<AuthorSummaryResponse> list(Pageable pageable) {
        return authorService.list(pageable);
    }

    @GetMapping("/{id}")
    public AuthorResponse getById(@PathVariable Long id) {
        return authorService.getById(id);
    }

    @PostMapping
    public ResponseEntity<AuthorResponse> create(@Valid @RequestBody CreateAuthorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authorService.create(request));
    }

    @PutMapping("/{id}")
    public AuthorResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdateAuthorRequest request) {
        return authorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 9: Run the controller test to verify it passes**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=AuthorControllerTest`
Expected: PASS.

- [ ] **Step 10: Run full suite and commit**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: green.

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/ backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/exception/ backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/author/
git commit -m "feat(backend): add author service and controller with CRUD"
```

### Educational summary

- **Java/Spring concepts:** `@Service` + `@Transactional`; constructor injection; custom runtime exceptions vs checked exceptions; `@RestControllerAdvice` not yet needed because controller tests mock the service; `@WebMvcTest` + `@MockitoBean` + MockMvc.
- **Architectural decision:** duplicate checks live in the service (not DB constraints) so error messages are meaningful; business rules are testable in isolation with Mockito.
- **Study before proceeding:** `@Transactional` propagation and when lazy collections load; Spring MVC content negotiation.

---

## Task 6: Category domain — full stack

**Files:**
- Create: `.../category/Category.java`
- Create: `.../category/CategoryRepository.java`
- Create: `.../category/CategoryMapper.java`
- Create: `.../category/CategoryService.java`
- Create: `.../category/CategoryController.java`
- Create: `.../category/dto/CategoryResponse.java`
- Create: `.../category/dto/CategorySummaryResponse.java`
- Create: `.../category/dto/CreateCategoryRequest.java`
- Create: `.../category/dto/UpdateCategoryRequest.java`
- Create: `.../phrase/PhraseCategory.java`
- Create: `.../phrase/PhraseCategoryRepository.java`
- Test: `.../category/CategoryServiceTest.java`, `.../category/CategoryControllerTest.java`, `.../category/CategoryRepositoryTest.java`

**Interfaces:**
- Consumes: `ResourceNotFoundException`, `DuplicateResourceException`, `SlugUtil`, `PagedResponse`.
- Produces:
  - `Category` entity, `CategoryRepository` (`Optional<Category> findBySlug`, `boolean existsByName`, `boolean existsBySlug`).
  - `PhraseCategory` entity (join entity; `Category getCategory()`), `PhraseCategoryRepository` (`List<Object[]> findPhraseCounts()` → `[categoryId, count]`, `void deleteByCategoryId(Long)`).
  - `CategoryService`: `list(Pageable)`, `getById(Long)`, `create(CreateCategoryRequest)`, `update(Long, UpdateCategoryRequest)`, `delete(Long)`.
  - `CategoryController` at `/api/v1/categories`: `GET ""`, `GET "/{id}"`, `POST ""`, `PUT "/{id}"`, `DELETE "/{id}"`. (The `/api/v1/categories/{id}/phrases` endpoint is added in Task 9 once `PhraseService` exists.)

- [ ] **Step 1: Create PhraseCategory join entity**

`phrase/PhraseCategory.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "phrase_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_phrase_categories", columnNames = {"phrase_id", "category_id"}))
public class PhraseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "phrase_id", nullable = false)
    private Phrase phrase;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    protected PhraseCategory() {
    }

    public PhraseCategory(Phrase phrase, Category category) {
        this.phrase = phrase;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public Phrase getPhrase() {
        return phrase;
    }

    public Category getCategory() {
        return category;
    }
}
```

- [ ] **Step 2: Create PhraseCategoryRepository**

`phrase/PhraseCategoryRepository.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PhraseCategoryRepository extends JpaRepository<PhraseCategory, Long> {

    @Query("select pc.category.id, count(pc) from PhraseCategory pc group by pc.category.id")
    List<Object[]> findPhraseCounts();

    @Modifying
    @Query("delete from PhraseCategory pc where pc.category.id = :categoryId")
    void deleteByCategoryId(@Param("categoryId") Long categoryId);
}
```

- [ ] **Step 3: Create Category entity**

`category/Category.java`:

```java
package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    protected Category() {
    }

    public Category(String name, String slug, String description) {
        this.name = name;
        this.slug = slug;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
```

- [ ] **Step 4: Create CategoryRepository**

`category/CategoryRepository.java`:

```java
package com.phraseforge.phraseforge_api.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);
}
```

- [ ] **Step 5: Write repository test**

`category/CategoryRepositoryTest.java`:

```java
package com.phraseforge.phraseforge_api.category;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findBySlug_returnsCategory() {
        categoryRepository.save(new Category("Stoicism", "stoicism", "Ancient school"));

        Optional<Category> found = categoryRepository.findBySlug("stoicism");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Stoicism");
    }
}
```

- [ ] **Step 6: Create DTOs**

`category/dto/CategorySummaryResponse.java`:

```java
package com.phraseforge.phraseforge_api.category.dto;

public record CategorySummaryResponse(
        Long id,
        String name,
        String slug,
        long phraseCount) {
}
```

`category/dto/CategoryResponse.java`:

```java
package com.phraseforge.phraseforge_api.category.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        long phraseCount) {
}
```

`category/dto/CreateCategoryRequest.java`:

```java
package com.phraseforge.phraseforge_api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description) {
}
```

`category/dto/UpdateCategoryRequest.java`:

```java
package com.phraseforge.phraseforge_api.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must be at most 100 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description) {
}
```

- [ ] **Step 7: Create CategoryMapper**

`category/CategoryMapper.java`:

```java
package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CategorySummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategorySummaryResponse toSummary(Category category, long phraseCount) {
        return new CategorySummaryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                phraseCount);
    }

    public CategoryResponse toResponse(Category category, long phraseCount) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                phraseCount);
    }
}
```

- [ ] **Step 8: Write service test**

`category/CategoryServiceTest.java`:

```java
package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private com.phraseforge.phraseforge_api.phrase.PhraseCategoryRepository phraseCategoryRepository;

    @Spy
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void create_generatesSlugAndSaves() {
        when(categoryRepository.existsByName("Stoicism")).thenReturn(false);
        when(categoryRepository.existsBySlug("stoicism")).thenReturn(false);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = categoryService.create(new CreateCategoryRequest("Stoicism", "Ancient school"));

        assertThat(response.slug()).isEqualTo("stoicism");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(categoryRepository.existsByName("Stoicism")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CreateCategoryRequest("Stoicism", null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void delete_missingCategory_throwsNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 9: Create the service**

`category/CategoryService.java`:

```java
package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CategorySummaryResponse;
import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
import com.phraseforge.phraseforge_api.category.dto.UpdateCategoryRequest;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.common.SlugUtil;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.PhraseCategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final PhraseCategoryRepository phraseCategoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository,
                           PhraseCategoryRepository phraseCategoryRepository,
                           CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.phraseCategoryRepository = phraseCategoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<CategorySummaryResponse> list(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(pageable);
        Map<Long, Long> counts = phraseCounts();
        return PagedResponse.from(page.map(category ->
                categoryMapper.toSummary(category, counts.getOrDefault(category.getId(), 0L))));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = findByIdOrThrow(id);
        return categoryMapper.toResponse(category, phraseCountFor(id));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        ensureNameAvailable(request.name());
        Category category = new Category(
                request.name(),
                SlugUtil.toSlug(request.name()),
                request.description());
        ensureSlugAvailable(category.getSlug());
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved, 0L);
    }

    @Transactional
    public CategoryResponse update(Long id, UpdateCategoryRequest request) {
        Category category = findByIdOrThrow(id);
        if (!category.getName().equals(request.name()) && categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Category already exists: " + request.name());
        }
        category.setName(request.name());
        category.setSlug(SlugUtil.toSlug(request.name()));
        category.setDescription(request.description());
        return categoryMapper.toResponse(category, phraseCountFor(id));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findByIdOrThrow(id);
        phraseCategoryRepository.deleteByCategoryId(id);
        categoryRepository.delete(category);
    }

    private Category findByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private void ensureNameAvailable(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateResourceException("Category already exists: " + name);
        }
    }

    private void ensureSlugAvailable(String slug) {
        if (categoryRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Category slug already exists: " + slug);
        }
    }

    private Map<Long, Long> phraseCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : phraseCategoryRepository.findPhraseCounts()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }

    private long phraseCountFor(Long categoryId) {
        return phraseCategoryRepository.findPhraseCounts().stream()
                .filter(row -> ((Long) row[0]).equals(categoryId))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);
    }
}
```

- [ ] **Step 10: Create the controller**

`category/CategoryController.java`:

```java
package com.phraseforge.phraseforge_api.category;

import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CategorySummaryResponse;
import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
import com.phraseforge.phraseforge_api.category.dto.UpdateCategoryRequest;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public PagedResponse<CategorySummaryResponse> list(Pageable pageable) {
        return categoryService.list(pageable);
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id) {
        return categoryService.getById(id);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 11: Create controller test**

`category/CategoryControllerTest.java`:

```java
package com.phraseforge.phraseforge_api.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phraseforge.phraseforge_api.category.dto.CategoryResponse;
import com.phraseforge.phraseforge_api.category.dto.CreateCategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getById_returnsCategory() throws Exception {
        when(categoryService.getById(1L)).thenReturn(new CategoryResponse(1L, "Stoicism", "stoicism", null, 0L));

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Stoicism")));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(categoryService.create(any(CreateCategoryRequest.class)))
                .thenReturn(new CategoryResponse(1L, "Stoicism", "stoicism", null, 0L));

        String body = objectMapper.writeValueAsString(new CreateCategoryRequest("Stoicism", null));

        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug", is("stoicism")));
    }
}
```

- [ ] **Step 12: Run all tests, then commit**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: all green.

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/category/ backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/phrase/PhraseCategory.java backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/phrase/PhraseCategoryRepository.java backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/category/
git commit -m "feat(backend): add category domain with join entity and CRUD"
```

### Educational summary

- **Java/Spring concepts:** explicit join entity (`PhraseCategory`) with `@ManyToOne` on both sides and a `UNIQUE(phrase_id, category_id)` constraint — the JPA model mirrors the `phrase_categories` table exactly; `@Modifying` `@Query` bulk deletes.
- **Architectural decision:** the N:N relationship goes through a join entity rather than a hidden `@ManyToMany`, keeping the DB model transparent.
- **Study before proceeding:** join tables vs `@ManyToMany`; orphan/dangling rows when deleting a category.

---

## Task 7: Tag domain — full stack

**Files:**
- Create: `.../tag/Tag.java`
- Create: `.../tag/TagRepository.java`
- Create: `.../tag/TagMapper.java`
- Create: `.../tag/TagService.java`
- Create: `.../tag/TagController.java`
- Create: `.../tag/dto/TagResponse.java`
- Create: `.../tag/dto/CreateTagRequest.java`
- Create: `.../tag/dto/UpdateTagRequest.java`
- Create: `.../phrase/PhraseTag.java`
- Create: `.../phrase/PhraseTagRepository.java`
- Test: `.../tag/TagServiceTest.java`, `.../tag/TagControllerTest.java`

**Interfaces:**
- Consumes: `ResourceNotFoundException`, `DuplicateResourceException`, `PagedResponse`.
- Produces: `Tag` entity, `TagRepository` (`Optional<Tag> findByName`, `boolean existsByName`), `PhraseTag` join entity, `PhraseTagRepository` (`void deleteByTagId(Long)`), `TagService` (`list(Pageable)`, `create(CreateTagRequest)`, `update(Long, UpdateTagRequest)`, `delete(Long)`), `TagController` at `/api/v1/tags` (`GET ""`, `POST ""`, `PUT "/{id}"`, `DELETE "/{id}"`).

- [ ] **Step 1: Create PhraseTag join entity**

`phrase/PhraseTag.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.tag.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "phrase_tags",
        uniqueConstraints = @UniqueConstraint(name = "uk_phrase_tags", columnNames = {"phrase_id", "tag_id"}))
public class PhraseTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "phrase_id", nullable = false)
    private Phrase phrase;

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    protected PhraseTag() {
    }

    public PhraseTag(Phrase phrase, Tag tag) {
        this.phrase = phrase;
        this.tag = tag;
    }

    public Long getId() {
        return id;
    }

    public Phrase getPhrase() {
        return phrase;
    }

    public Tag getTag() {
        return tag;
    }
}
```

- [ ] **Step 2: Create PhraseTagRepository**

`phrase/PhraseTagRepository.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhraseTagRepository extends JpaRepository<PhraseTag, Long> {

    @Modifying
    @Query("delete from PhraseTag pt where pt.tag.id = :tagId")
    void deleteByTagId(@Param("tagId") Long tagId);
}
```

- [ ] **Step 3: Create Tag entity**

`tag/Tag.java`:

```java
package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
public class Tag extends AuditableEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    protected Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

- [ ] **Step 4: Create TagRepository**

`tag/TagRepository.java`:

```java
package com.phraseforge.phraseforge_api.tag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {

    Optional<Tag> findByName(String name);

    boolean existsByName(String name);
}
```

- [ ] **Step 5: Create DTOs**

`tag/dto/TagResponse.java`:

```java
package com.phraseforge.phraseforge_api.tag.dto;

public record TagResponse(
        Long id,
        String name) {
}
```

`tag/dto/CreateTagRequest.java`:

```java
package com.phraseforge.phraseforge_api.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTagRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name) {
}
```

`tag/dto/UpdateTagRequest.java`:

```java
package com.phraseforge.phraseforge_api.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTagRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name) {
}
```

- [ ] **Step 6: Create TagMapper**

`tag/TagMapper.java`:

```java
package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }
}
```

- [ ] **Step 7: Write service test**

`tag/TagServiceTest.java`:

```java
package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private com.phraseforge.phraseforge_api.phrase.PhraseTagRepository phraseTagRepository;

    @Spy
    private TagMapper tagMapper;

    @InjectMocks
    private TagService tagService;

    @Test
    void create_savesTag() {
        when(tagRepository.existsByName("mind")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = tagService.create(new CreateTagRequest("mind"));

        assertThat(response.name()).isEqualTo("mind");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(tagRepository.existsByName("mind")).thenReturn(true);

        assertThatThrownBy(() -> tagService.create(new CreateTagRequest("mind")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    void update_missingTag_throwsNotFound() {
        when(tagRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tagService.update(99L, new CreateTagRequest("x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 8: Create the service**

`tag/TagService.java`:

```java
package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.PhraseTagRepository;
import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
import com.phraseforge.phraseforge_api.tag.dto.UpdateTagRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final PhraseTagRepository phraseTagRepository;

    public TagService(TagRepository tagRepository, TagMapper tagMapper,
                      PhraseTagRepository phraseTagRepository) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
        this.phraseTagRepository = phraseTagRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<TagResponse> list(Pageable pageable) {
        Page<Tag> page = tagRepository.findAll(pageable);
        return PagedResponse.from(page.map(tagMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public TagResponse getById(Long id) {
        return tagMapper.toResponse(findByIdOrThrow(id));
    }

    @Transactional
    public TagResponse create(CreateTagRequest request) {
        ensureNameAvailable(request.name());
        Tag tag = tagRepository.save(new Tag(request.name()));
        return tagMapper.toResponse(tag);
    }

    @Transactional
    public TagResponse update(Long id, UpdateTagRequest request) {
        Tag tag = findByIdOrThrow(id);
        if (!tag.getName().equals(request.name()) && tagRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Tag already exists: " + request.name());
        }
        tag.setName(request.name());
        return tagMapper.toResponse(tag);
    }

    @Transactional
    public void delete(Long id) {
        findByIdOrThrow(id);
        phraseTagRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
    }

    private Tag findByIdOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
    }

    private void ensureNameAvailable(String name) {
        if (tagRepository.existsByName(name)) {
            throw new DuplicateResourceException("Tag already exists: " + name);
        }
    }
}
```

- [ ] **Step 9: Create the controller**

`tag/TagController.java`:

```java
package com.phraseforge.phraseforge_api.tag;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
import com.phraseforge.phraseforge_api.tag.dto.UpdateTagRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public PagedResponse<TagResponse> list(Pageable pageable) {
        return tagService.list(pageable);
    }

    @GetMapping("/{id}")
    public TagResponse getById(@PathVariable Long id) {
        return tagService.getById(id);
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @PutMapping("/{id}")
    public TagResponse update(@PathVariable Long id,
                              @Valid @RequestBody UpdateTagRequest request) {
        return tagService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 10: Create controller test**

`tag/TagControllerTest.java`:

```java
package com.phraseforge.phraseforge_api.tag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phraseforge.phraseforge_api.tag.dto.CreateTagRequest;
import com.phraseforge.phraseforge_api.tag.dto.TagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TagService tagService;

    @Test
    void getById_returnsTag() throws Exception {
        when(tagService.getById(1L)).thenReturn(new TagResponse(1L, "mind"));

        mockMvc.perform(get("/api/v1/tags/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("mind")));
    }

    @Test
    void create_validRequest_returnsCreated() throws Exception {
        when(tagService.create(any(CreateTagRequest.class))).thenReturn(new TagResponse(1L, "mind"));

        String body = objectMapper.writeValueAsString(new CreateTagRequest("mind"));

        mockMvc.perform(post("/api/v1/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("mind")));
    }
}
```

- [ ] **Step 11: Run all tests, then commit**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: all green.

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/tag/ backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/phrase/PhraseTag.java backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/phrase/PhraseTagRepository.java backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/tag/
git commit -m "feat(backend): add tag domain with join entity and CRUD"
```

### Educational summary

- **Java/Spring concepts:** same layered pattern applied to a simpler entity (`Tag` has no slug); service-layer uniqueness check for the `name` column.
- **Architectural decision:** tags are managed independently and linked to phrases only through phrase create/update.
- **Study before proceeding:** `@Modifying` deletes require transaction boundaries; referential integrity between `phrase_tags` and `tags`.

---

## Task 8: Phrase domain — entity, repository, DTOs, mapper, specifications

**Files:**
- Create: `.../phrase/Phrase.java`
- Create: `.../phrase/PhraseRepository.java`
- Create: `.../phrase/PhraseSpecifications.java`
- Create: `.../phrase/dto/AuthorRef.java`
- Create: `.../phrase/dto/CategoryRef.java`
- Create: `.../phrase/dto/TagRef.java`
- Create: `.../phrase/dto/PhraseSummaryResponse.java`
- Create: `.../phrase/dto/PhraseResponse.java`
- Create: `.../phrase/dto/CreatePhraseRequest.java`
- Create: `.../phrase/dto/UpdatePhraseRequest.java`
- Create: `.../phrase/PhraseMapper.java`
- Test: `.../phrase/PhraseRepositoryTest.java`, `.../phrase/PhraseSpecificationsTest.java`

**Interfaces:**
- Consumes: `Author`, `Category`, `Tag`, join entities from Tasks 4/6/7, `SlugUtil` (not used here).
- Produces:
  - `Phrase` entity (extends `AuditableEntity`): `Long getId()`, `String getContent()`, `Author getAuthor()`, `Integer getYear()`, `String getLanguage()`, `String getSource()`, `List<PhraseCategory> getPhraseCategories()`, `List<PhraseTag> getPhraseTags()`, plus `getCategories()`/`getTags()` convenience (mapped lists of `Category`/`Tag`), and setters.
  - `PhraseRepository extends JpaRepository<Phrase, Long>`: `boolean existsByContentAndAuthor_Id(String content, Long authorId)`, `boolean existsByContentAndAuthor_IdAndIdNot(String content, Long authorId, Long excludeId)`, `long count()`, `Optional<Phrase> findWithDetailsById(Long id)`.
  - `PhraseSpecifications.filter(String query, Long authorId, Long categoryId, Long tagId, String language)` returning `Specification<Phrase>`.
  - DTOs and `PhraseMapper` (component): `PhraseSummaryResponse toSummary(Phrase)`, `PhraseResponse toResponse(Phrase)`.

- [ ] **Step 1: Write the failing entity+repository test**

`phrase/PhraseRepositoryTest.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PhraseRepositoryTest {

    @Autowired
    private PhraseRepository phraseRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    void existsByContentAndAuthorId_detectsDuplicates() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        phraseRepository.save(new Phrase("Shared content", author, null, "en", null));

        boolean exists = phraseRepository.existsByContentAndAuthor_Id("Shared content", author.getId());
        boolean otherAuthor = phraseRepository.existsByContentAndAuthor_Id("Shared content", 999L);

        assertThat(exists).isTrue();
        assertThat(otherAuthor).isFalse();
    }

    @Test
    void existsByContentAndAuthorIdAndIdNot_excludesGivenPhrase() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        Phrase first = phraseRepository.save(new Phrase("Shared content", author, null, "en", null));
        Phrase second = phraseRepository.save(new Phrase("Shared content", author, null, "en", null));

        boolean excludingFirst = phraseRepository.existsByContentAndAuthor_IdAndIdNot("Shared content", author.getId(), first.getId());
        boolean excludingAll = phraseRepository.existsByContentAndAuthor_IdAndIdNot("Shared content", author.getId(), second.getId());

        assertThat(excludingFirst).isTrue();   // second still matches
        assertThat(excludingAll).isTrue();     // first still matches
    }

    @Test
    void existsByContentAndAuthorIdAndIdNot_returnsFalseWhenNoOtherMatch() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        Phrase only = phraseRepository.save(new Phrase("Unique content", author, null, "en", null));

        boolean found = phraseRepository.existsByContentAndAuthor_IdAndIdNot("Unique content", author.getId(), only.getId());

        assertThat(found).isFalse();
    }

    @Test
    void count_returnsTotal() {
        Author author = authorRepository.save(new Author("Test Author", "test-author", null, null, null));
        phraseRepository.save(new Phrase("One", author, null, "en", null));
        phraseRepository.save(new Phrase("Two", author, null, "pt", null));

        assertThat(phraseRepository.count()).isEqualTo(2L);
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=PhraseRepositoryTest`
Expected: COMPILATION FAILURE — `Phrase` missing.

- [ ] **Step 3: Create the Phrase entity**

`phrase/Phrase.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.common.AuditableEntity;
import com.phraseforge.phraseforge_api.tag.Tag;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "phrases")
public class Phrase extends AuditableEntity {

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "year")
    private Integer year;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "source", length = 300)
    private String source;

    @OneToMany(mappedBy = "phrase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhraseCategory> phraseCategories = new ArrayList<>();

    @OneToMany(mappedBy = "phrase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PhraseTag> phraseTags = new ArrayList<>();

    protected Phrase() {
    }

    public Phrase(String content, Author author, Integer year, String language, String source) {
        this.content = content;
        this.author = author;
        this.year = year;
        this.language = language;
        this.source = source;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<PhraseCategory> getPhraseCategories() {
        return phraseCategories;
    }

    public List<PhraseTag> getPhraseTags() {
        return phraseTags;
    }

    public List<Category> getCategories() {
        return phraseCategories.stream().map(PhraseCategory::getCategory).toList();
    }

    public List<Tag> getTags() {
        return phraseTags.stream().map(PhraseTag::getTag).toList();
    }
}
```

- [ ] **Step 4: Create the repository**

`phrase/PhraseRepository.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PhraseRepository
        extends JpaRepository<Phrase, Long>, JpaSpecificationExecutor<Phrase> {

    boolean existsByContentAndAuthor_Id(String content, Long authorId);

    /**
     * Duplicate check for updates: true if another phrase (id != excludeId)
     * has the same content under the same author.
     */
    boolean existsByContentAndAuthor_IdAndIdNot(String content, Long authorId, Long excludeId);

    /**
     * Loads a phrase with its author, categories, and tags in one query
     * (avoids N+1 when serializing the detail view).
     */
    @EntityGraph(attributePaths = {"author", "phraseCategories.category", "phraseTags.tag"})
    Optional<Phrase> findWithDetailsById(Long id);
}
```

Note: listing queries use `findAll(Specification, Pageable)` from `JpaSpecificationExecutor` combined with a custom `@EntityGraph` variant added in Task 9 Step 1.

- [ ] **Step 5: Run the test to verify it passes**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=PhraseRepositoryTest`
Expected: PASS.

- [ ] **Step 6: Create the specifications**

`phrase/PhraseSpecifications.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PhraseSpecifications {

    private PhraseSpecifications() {
    }

    public static Specification<Phrase> filter(String query, Long authorId,
                                               Long categoryId, Long tagId, String language) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                Join<Phrase, ?> authorJoin = root.join("author", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), like),
                        cb.like(cb.lower(authorJoin.get("name")), like)));
            }

            if (authorId != null) {
                predicates.add(cb.equal(root.get("author").get("id"), authorId));
            }

            if (categoryId != null) {
                root.join("phraseCategories", JoinType.LEFT).get("category");
                cq.distinct(true);
                predicates.add(cb.equal(root.get("phraseCategories").get("category").get("id"), categoryId));
            }

            if (tagId != null) {
                predicates.add(cb.equal(root.get("phraseTags").get("tag").get("id"), tagId));
            }

            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(root.get("language"), language));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

Note: this first version joins `phraseCategories`/`phraseTags` separately per predicate and can produce duplicate rows. Task 9 Step 2 replaces `filter(...)` with the corrected single-join + `cq.distinct(true)` version, and Task 9 Step 3's integration tests verify no duplicates are returned when filtering by category/tag.

- [ ] **Step 7: Create the DTOs**

`phrase/dto/AuthorRef.java`:

```java
package com.phraseforge.phraseforge_api.phrase.dto;

public record AuthorRef(
        Long id,
        String name,
        String slug) {
}
```

`phrase/dto/CategoryRef.java`:

```java
package com.phraseforge.phraseforge_api.phrase.dto;

public record CategoryRef(
        Long id,
        String name,
        String slug) {
}
```

`phrase/dto/TagRef.java`:

```java
package com.phraseforge.phraseforge_api.phrase.dto;

public record TagRef(
        Long id,
        String name) {
}
```

`phrase/dto/PhraseSummaryResponse.java`:

```java
package com.phraseforge.phraseforge_api.phrase.dto;

import java.time.Instant;
import java.util.List;

public record PhraseSummaryResponse(
        Long id,
        String content,
        Integer year,
        String language,
        String source,
        AuthorRef author,
        List<CategoryRef> categories,
        List<TagRef> tags,
        Instant createdAt) {
}
```

`phrase/dto/PhraseResponse.java`:

```java
package com.phraseforge.phraseforge_api.phrase.dto;

import java.time.Instant;
import java.util.List;

public record PhraseResponse(
        Long id,
        String content,
        Integer year,
        String language,
        String source,
        AuthorRef author,
        List<CategoryRef> categories,
        List<TagRef> tags,
        Instant createdAt,
        Instant updatedAt) {
}
```

`phrase/dto/CreatePhraseRequest.java`:

```java
package com.phraseforge.phraseforge_api.phrase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public record CreatePhraseRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 10000, message = "Content must be at most 10000 characters")
        String content,

        @NotNull(message = "Author is required")
        Long authorId,

        @Min(value = -10000, message = "Year is out of range")
        @Max(value = 10000, message = "Year is out of range")
        Integer year,

        @NotBlank(message = "Language is required")
        @Size(max = 10, message = "Language must be at most 10 characters")
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be an ISO 639-1 code")
        String language,

        @Size(max = 300, message = "Source must be at most 300 characters")
        String source,

        Set<Long> categoryIds,

        Set<Long> tagIds) {

    public CreatePhraseRequest {
        categoryIds = categoryIds == null ? new HashSet<>() : categoryIds;
        tagIds = tagIds == null ? new HashSet<>() : tagIds;
    }
}
```

`phrase/dto/UpdatePhraseRequest.java`:

```java
package com.phraseforge.phraseforge_api.phrase.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public record UpdatePhraseRequest(
        @NotBlank(message = "Content is required")
        @Size(max = 10000, message = "Content must be at most 10000 characters")
        String content,

        @NotNull(message = "Author is required")
        Long authorId,

        @Min(value = -10000, message = "Year is out of range")
        @Max(value = 10000, message = "Year is out of range")
        Integer year,

        @NotBlank(message = "Language is required")
        @Size(max = 10, message = "Language must be at most 10 characters")
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "Language must be an ISO 639-1 code")
        String language,

        @Size(max = 300, message = "Source must be at most 300 characters")
        String source,

        Set<Long> categoryIds,

        Set<Long> tagIds) {

    public UpdatePhraseRequest {
        categoryIds = categoryIds == null ? new HashSet<>() : categoryIds;
        tagIds = tagIds == null ? new HashSet<>() : tagIds;
    }
}
```

- [ ] **Step 8: Create the mapper**

`phrase/PhraseMapper.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.phrase.dto.AuthorRef;
import com.phraseforge.phraseforge_api.phrase.dto.CategoryRef;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import com.phraseforge.phraseforge_api.phrase.dto.TagRef;
import com.phraseforge.phraseforge_api.tag.Tag;
import org.springframework.stereotype.Component;

@Component
public class PhraseMapper {

    public PhraseSummaryResponse toSummary(Phrase phrase) {
        return new PhraseSummaryResponse(
                phrase.getId(),
                phrase.getContent(),
                phrase.getYear(),
                phrase.getLanguage(),
                phrase.getSource(),
                authorRef(phrase.getAuthor()),
                phrase.getCategories().stream().map(this::categoryRef).toList(),
                phrase.getTags().stream().map(this::tagRef).toList(),
                phrase.getCreatedAt());
    }

    public PhraseResponse toResponse(Phrase phrase) {
        return new PhraseResponse(
                phrase.getId(),
                phrase.getContent(),
                phrase.getYear(),
                phrase.getLanguage(),
                phrase.getSource(),
                authorRef(phrase.getAuthor()),
                phrase.getCategories().stream().map(this::categoryRef).toList(),
                phrase.getTags().stream().map(this::tagRef).toList(),
                phrase.getCreatedAt(),
                phrase.getUpdatedAt());
    }

    private AuthorRef authorRef(Author author) {
        return new AuthorRef(author.getId(), author.getName(), author.getSlug());
    }

    private CategoryRef categoryRef(Category category) {
        return new CategoryRef(category.getId(), category.getName(), category.getSlug());
    }

    private TagRef tagRef(Tag tag) {
        return new TagRef(tag.getId(), tag.getName());
    }
}
```

- [ ] **Step 9: Run full suite, then commit**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: green.

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/phrase/ backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/phrase/
git commit -m "feat(backend): add phrase entity, repository, specifications, DTOs, and mapper"
```

### Educational summary

- **Java/Spring concepts:** `JpaSpecificationExecutor` + `Specification` for dynamic filters; `@EntityGraph` to eager-fetch author/categories/tags and avoid N+1; derived `existsByContentAndAuthor_Id...` queries; stream mapping into records.
- **Architectural decision:** Specifications keep the filter logic declarative and testable instead of hand-written queries per combination.
- **Study before completion of this task:** JPA Criteria API basics (`root`, `cb`, `Predicate`), entity graphs and join fetching.

---

## Task 9: Phrase service and controller + author/category phrases endpoints

**Files:**
- Modify: `.../phrase/PhraseRepository.java` (add `@EntityGraph` `findAll` for listings)
- Modify: `.../phrase/PhraseSpecifications.java` (finalize join handling)
- Modify: `.../author/AuthorController.java` (add `/api/v1/authors/{id}/phrases`)
- Modify: `.../category/CategoryController.java` (add `/api/v1/categories/{id}/phrases`)
- Create: `.../phrase/PhraseService.java`
- Create: `.../phrase/PhraseController.java`
- Test: `.../phrase/PhraseServiceTest.java`, `.../phrase/PhraseControllerTest.java`, `.../phrase/PhraseSpecificationsTest.java`

**Interfaces:**
- Consumes: all Task 8 artifacts; `AuthorRepository`, `CategoryRepository`, `TagRepository`, `PhraseCategoryRepository`.
- Produces:
  - `PhraseService`: `PagedResponse<PhraseSummaryResponse> list(String query, Long authorId, Long categoryId, Long tagId, String language, Pageable pageable)`, `PhraseResponse getById(Long)`, `PhraseResponse create(CreatePhraseRequest)`, `PhraseResponse update(Long, UpdatePhraseRequest)`, `void delete(Long)`, `PhraseResponse random()`.
  - `PhraseController` at `/api/v1/phrases` — **`/random` declared before `/{id}`** — with `GET ""` (filters + pagination), `GET "/random"`, `GET "/{id}"`, `POST ""`, `PUT "/{id}"`, `DELETE "/{id}"`.
  - Author/category controllers get a `GET "/{id}/phrases"` returning `PagedResponse<PhraseSummaryResponse>`.

- [ ] **Step 1: Add @EntityGraph listing to PhraseRepository**

Replace the repository body with:

```java
package com.phraseforge.phraseforge_api.phrase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PhraseRepository
        extends JpaRepository<Phrase, Long>, JpaSpecificationExecutor<Phrase> {

    boolean existsByContentAndAuthor_Id(String content, Long authorId);

    /**
     * Duplicate check for updates: true if another phrase (id != excludeId)
     * has the same content under the same author. Runs as a single SQL query
     * instead of loading and filtering phrases in memory.
     */
    boolean existsByContentAndAuthor_IdAndIdNot(String content, Long authorId, Long excludeId);

    @EntityGraph(attributePaths = {"author", "phraseCategories.category", "phraseTags.tag"})
    Optional<Phrase> findWithDetailsById(Long id);

    /**
     * Listings eagerly fetch author + categories + tags so the paged JSON
     * does not trigger N+1 SELECTs. Overrides the inherited method.
     */
    @Override
    @EntityGraph(attributePaths = {"author", "phraseCategories.category", "phraseTags.tag"})
    Page<Phrase> findAll(Specification<Phrase> spec, Pageable pageable);
}
```

- [ ] **Step 2: Finalize PhraseSpecifications join handling**

Replace the body of `filter(...)` with this corrected version (single join reused by multiple predicates — avoids the duplicate-row trap):

```java
    public static Specification<Phrase> filter(String query, Long authorId,
                                               Long categoryId, Long tagId, String language) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                var authorJoin = root.join("author", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), like),
                        cb.like(cb.lower(authorJoin.get("name")), like)));
            }

            if (authorId != null) {
                predicates.add(cb.equal(root.join("author").get("id"), authorId));
            }

            boolean hasCollectionFilter = categoryId != null || tagId != null;
            if (hasCollectionFilter) {
                cq.distinct(true);
            }

            if (categoryId != null) {
                var catJoin = root.join("phraseCategories", JoinType.LEFT);
                predicates.add(cb.equal(catJoin.get("category").get("id"), categoryId));
            }

            if (tagId != null) {
                var tagJoin = root.join("phraseTags", JoinType.LEFT);
                predicates.add(cb.equal(tagJoin.get("tag").get("id"), tagId));
            }

            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(root.get("language"), language));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
```

Note: the final `Predicate` import is still needed at the top of the file; `JoinType` remains imported.

- [ ] **Step 3: Write the failing filter integration test**

`phrase/PhraseSpecificationsTest.java` — `@DataJpaTest` against an empty DB
(fixtures built by the test); covers every filter, combinations, and verifies
pagination does not return duplicate phrases when filtering by category/tag:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.category.CategoryRepository;
import com.phraseforge.phraseforge_api.tag.Tag;
import com.phraseforge.phraseforge_api.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PhraseSpecificationsTest {

    @Autowired
    private PhraseRepository phraseRepository;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TagRepository tagRepository;

    private Author authorA;
    private Author authorB;
    private Category philosophy;
    private Category stoicism;
    private Tag mind;
    private Tag strength;

    @BeforeEach
    void setUp() {
        authorA = authorRepository.save(new Author("Test Author A", "test-author-a", null, null, null));
        authorB = authorRepository.save(new Author("Test Author B", "test-author-b", null, null, null));
        philosophy = categoryRepository.save(new Category("Philosophy", "philosophy", null));
        stoicism = categoryRepository.save(new Category("Stoicism", "stoicism", null));
        mind = tagRepository.save(new Tag("mind"));
        strength = tagRepository.save(new Tag("strength"));

        // Phrase 1: authorA, philosophy, mind
        savePhrase("The unexamined life", authorA, "en", List.of(philosophy), List.of(mind));
        // Phrase 2: authorA, philosophy AND stoicism, mind AND strength (multi-join: dedup must hold)
        savePhrase("What stands in the way becomes the way", authorA, "en", List.of(philosophy, stoicism), List.of(mind, strength));
        // Phrase 3: authorB, stoicism, strength
        savePhrase("Apenas os instruídos são livres", authorB, "pt", List.of(stoicism), List.of(strength));
    }

    private void savePhrase(String content, Author author, String language,
                            List<Category> categories, List<Tag> tags) {
        Phrase phrase = new Phrase(content, author, null, language, null);
        categories.forEach(c -> phrase.getPhraseCategories().add(new PhraseCategory(phrase, c)));
        tags.forEach(t -> phrase.getPhraseTags().add(new PhraseTag(phrase, t)));
        phraseRepository.save(phrase);
    }

    private List<Long> findIds(Specification<Phrase> spec) {
        Page<Phrase> page = phraseRepository.findAll(spec, PageRequest.of(0, 20));
        return page.getContent().stream().map(Phrase::getId).toList();
    }

    @Test
    void query_matchesContent() {
        List<Long> ids = findIds(PhraseSpecifications.filter("unexamined", null, null, null, null));
        assertThat(ids).hasSize(1);
    }

    @Test
    void query_matchesAuthorName() {
        List<Long> ids = findIds(PhraseSpecifications.filter("Author B", null, null, null, null));
        assertThat(ids).hasSize(1);
    }

    @Test
    void authorId_filtersByAuthor() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, authorB.getId(), null, null, null));
        assertThat(ids).hasSize(1);
    }

    @Test
    void categoryId_filtersByCategory() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, philosophy.getId(), null, null));
        assertThat(ids).hasSize(2);
    }

    @Test
    void tagId_filtersByTag() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, null, mind.getId(), null));
        assertThat(ids).hasSize(2);
    }

    @Test
    void language_filtersByLanguage() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, null, null, "pt"));
        assertThat(ids).hasSize(1);
    }

    @Test
    void combinations_authorAndCategoryAndTag() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, authorA.getId(), stoicism.getId(), mind.getId(), "en"));
        assertThat(ids).hasSize(1);
    }

    @Test
    void categoryFilter_doesNotDuplicatePhrases() {
        // Phrase 2 belongs to BOTH philosophy and stoicism and has two tags.
        // A cartesian join would return it twice; distinct() must collapse it.
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, stoicism.getId(), null, null));
        assertThat(ids).hasSize(2);
        assertThat(ids).doesNotHaveDuplicates();
    }

    @Test
    void tagFilter_doesNotDuplicatePhrases() {
        List<Long> ids = findIds(PhraseSpecifications.filter(null, null, null, strength.getId(), null));
        assertThat(ids).hasSize(2);
        assertThat(ids).doesNotHaveDuplicates();
    }
}
```

- [ ] **Step 4: Run the filter test to verify it fails**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=PhraseSpecificationsTest`
Expected: FAIL (either compile error because the final `filter`/`findAll` does not
exist yet, or assertion failures on duplicates before `distinct` is wired in Step 2).

- [ ] **Step 5: Write the failing service test**

`phrase/PhraseServiceTest.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.UpdatePhraseRequest;
import com.phraseforge.phraseforge_api.tag.Tag;
import com.phraseforge.phraseforge_api.tag.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PhraseServiceTest {

    @Mock
    private PhraseRepository phraseRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private com.phraseforge.phraseforge_api.category.CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private PhraseMapper phraseMapper;

    @InjectMocks
    private PhraseService phraseService;

    @Test
    void create_savesPhrase() {
        Author author = new Author("Test Author", "test-author", null, null, null);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(phraseRepository.existsByContentAndAuthor_Id("Shared content", 1L)).thenReturn(false);
        when(phraseRepository.save(any(Phrase.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(phraseMapper.toResponse(any(Phrase.class)))
                .thenReturn(new PhraseResponse(1L, "Shared content", null, "en", null, null, null, null, null, null));

        PhraseResponse response = phraseService.create(
                new CreatePhraseRequest("Shared content", 1L, null, "en", null, Set.of(), Set.of()));

        assertThat(response.content()).isEqualTo("Shared content");
        verify(phraseRepository).save(any(Phrase.class));
    }

    @Test
    void create_duplicateContentAndAuthor_throwsConflict() {
        when(authorRepository.findById(1L)).thenReturn(Optional.of(new Author("Test Author", "test-author", null, null, null)));
        when(phraseRepository.existsByContentAndAuthor_Id("Shared content", 1L)).thenReturn(true);

        assertThatThrownBy(() -> phraseService.create(
                new CreatePhraseRequest("Shared content", 1L, null, "en", null, Set.of(), Set.of())))
                .isInstanceOf(DuplicateResourceException.class);
        verify(phraseRepository, never()).save(any(Phrase.class));
    }

    @Test
    void update_duplicateContentAndAuthor_throwsConflict() {
        Phrase existing = new Phrase("Original content", new Author("Test Author", "test-author", null, null, null), null, "en", null);
        when(phraseRepository.findWithDetailsById(99L)).thenReturn(Optional.of(existing));
        when(phraseRepository.existsByContentAndAuthor_IdAndIdNot("Shared content", 1L, 99L)).thenReturn(true);

        assertThatThrownBy(() -> phraseService.update(99L,
                new UpdatePhraseRequest("Shared content", 1L, null, "en", null, Set.of(), Set.of())))
                .isInstanceOf(DuplicateResourceException.class);
        verify(phraseRepository, never()).save(any(Phrase.class));
    }

    @Test
    void update_missingPhrase_throwsNotFound() {
        when(phraseRepository.findWithDetailsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> phraseService.update(99L,
                new UpdatePhraseRequest("x", 1L, null, "en", null, Set.of(), Set.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void random_emptyDatabase_throwsNotFound() {
        when(phraseRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> phraseService.random())
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [ ] **Step 6: Run to verify it fails**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=PhraseServiceTest`
Expected: COMPILATION FAILURE — `PhraseService` missing.

- [ ] **Step 7: Create the service**

`phrase/PhraseService.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.author.Author;
import com.phraseforge.phraseforge_api.author.AuthorRepository;
import com.phraseforge.phraseforge_api.category.Category;
import com.phraseforge.phraseforge_api.category.CategoryRepository;
import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.exception.DuplicateResourceException;
import com.phraseforge.phraseforge_api.exception.ResourceNotFoundException;
import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import com.phraseforge.phraseforge_api.phrase.dto.UpdatePhraseRequest;
import com.phraseforge.phraseforge_api.tag.Tag;
import com.phraseforge.phraseforge_api.tag.TagRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Set;

@Service
public class PhraseService {

    private final PhraseRepository phraseRepository;
    private final AuthorRepository authorRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PhraseMapper phraseMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhraseService(PhraseRepository phraseRepository,
                         AuthorRepository authorRepository,
                         CategoryRepository categoryRepository,
                         TagRepository tagRepository,
                         PhraseMapper phraseMapper) {
        this.phraseRepository = phraseRepository;
        this.authorRepository = authorRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.phraseMapper = phraseMapper;
    }

    @Transactional(readOnly = true)
    public PagedResponse<PhraseSummaryResponse> list(String query, Long authorId, Long categoryId,
                                                     Long tagId, String language, Pageable pageable) {
        Specification<Phrase> spec = PhraseSpecifications.filter(query, authorId, categoryId, tagId, language);
        Page<Phrase> page = phraseRepository.findAll(spec, pageable);
        return PagedResponse.from(page.map(phraseMapper::toSummary));
    }

    @Transactional(readOnly = true)
    public PhraseResponse getById(Long id) {
        return phraseMapper.toResponse(findWithDetailsOrThrow(id));
    }

    @Transactional
    public PhraseResponse create(CreatePhraseRequest request) {
        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + request.authorId()));
        ensureNotDuplicate(request.content(), author.getId());

        Phrase phrase = new Phrase(request.content(), author, request.year(), request.language(), request.source());
        attachCategories(phrase, request.categoryIds());
        attachTags(phrase, request.tagIds());
        return phraseMapper.toResponse(phraseRepository.save(phrase));
    }

    @Transactional
    public PhraseResponse update(Long id, UpdatePhraseRequest request) {
        Phrase phrase = findWithDetailsOrThrow(id);
        ensureNotDuplicateExcluding(request.content(), request.authorId(), id);

        Author author = authorRepository.findById(request.authorId())
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + request.authorId()));
        phrase.setContent(request.content());
        phrase.setAuthor(author);
        phrase.setYear(request.year());
        phrase.setLanguage(request.language());
        phrase.setSource(request.source());
        replaceCategories(phrase, request.categoryIds());
        replaceTags(phrase, request.tagIds());
        return phraseMapper.toResponse(phrase);
    }

    @Transactional
    public void delete(Long id) {
        Phrase phrase = findWithDetailsOrThrow(id);
        phraseRepository.delete(phrase);
    }

    @Transactional(readOnly = true)
    public PhraseResponse random() {
        long total = phraseRepository.count();
        if (total == 0) {
            throw new ResourceNotFoundException("No phrases available");
        }
        int randomIndex = secureRandom.nextInt((int) total);
        Page<Phrase> page = phraseRepository.findAll(PageRequest.of(randomIndex, 1));
        Phrase phrase = page.getContent().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No phrases available"));
        return phraseMapper.toResponse(phrase);
    }

    private void ensureNotDuplicate(String content, Long authorId) {
        if (phraseRepository.existsByContentAndAuthor_Id(content, authorId)) {
            throw new DuplicateResourceException(
                    "Phrase with the same content already exists for this author");
        }
    }

    private void ensureNotDuplicateExcluding(String content, Long authorId, Long excludeId) {
        // Runs at the database level; excludes the phrase being updated so an
        // unchanged phrase does not trip its own duplicate check.
        if (phraseRepository.existsByContentAndAuthor_IdAndIdNot(content, authorId, excludeId)) {
            throw new DuplicateResourceException(
                    "Phrase with the same content already exists for this author");
        }
    }

    private Phrase findWithDetailsOrThrow(Long id) {
        return phraseRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Phrase not found: " + id));
    }

    private void attachCategories(Phrase phrase, Set<Long> categoryIds) {
        categoryIds.forEach(id -> {
            Category category = categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
            phrase.getPhraseCategories().add(new PhraseCategory(phrase, category));
        });
    }

    private void attachTags(Phrase phrase, Set<Long> tagIds) {
        tagIds.forEach(id -> {
            Tag tag = tagRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
            phrase.getPhraseTags().add(new PhraseTag(phrase, tag));
        });
    }

    private void replaceCategories(Phrase phrase, Set<Long> categoryIds) {
        phrase.getPhraseCategories().clear();
        attachCategories(phrase, categoryIds);
    }

    private void replaceTags(Phrase phrase, Set<Long> tagIds) {
        phrase.getPhraseTags().clear();
        attachTags(phrase, tagIds);
    }
}
```

Note: `findAll(spec)` with no `Pageable` returns a `List<Phrase>`; the `Pageable`-less overload is used only in `ensureNotDuplicateExcluding` (small data, MVP scale). The unused `Function` import should be removed.

- [ ] **Step 8: Run the service test to verify it passes**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=PhraseServiceTest`
Expected: PASS.

- [ ] **Step 9: Write the failing controller test**

`phrase/PhraseControllerTest.java`:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PhraseController.class)
class PhraseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PhraseService phraseService;

    @Test
    void getById_returnsPhrase() throws Exception {
        when(phraseService.getById(1L)).thenReturn(
                new PhraseResponse(1L, "Know thyself.", null, "en", null, null, null, null, null, null));

        mockMvc.perform(get("/api/v1/phrases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", is("Know thyself.")));
    }

    @Test
    void random_returnsPhrase() throws Exception {
        when(phraseService.random()).thenReturn(
                new PhraseResponse(2L, "Be good.", null, "en", null, null, null, null, null, null));

        mockMvc.perform(get("/api/v1/phrases/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)));
    }

    @Test
    void create_invalidRequest_returnsBadRequest() throws Exception {
        // Empty content and missing authorId violate Bean Validation.
        mockMvc.perform(post("/api/v1/phrases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\",\"authorId\":null,\"language\":\"en\"}"))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 10: Run to verify it fails**

Run: `cd backend/phraseforge-api && ./mvnw test -Dtest=PhraseControllerTest`
Expected: FAIL — controller missing (or 404 on `/random` if only partially wired).

- [ ] **Step 11: Create the controller**

`phrase/PhraseController.java` — note `/random` is declared BEFORE `/{id}` so it never gets captured by the path variable:

```java
package com.phraseforge.phraseforge_api.phrase;

import com.phraseforge.phraseforge_api.common.PagedResponse;
import com.phraseforge.phraseforge_api.phrase.dto.CreatePhraseRequest;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseResponse;
import com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse;
import com.phraseforge.phraseforge_api.phrase.dto.UpdatePhraseRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/phrases")
public class PhraseController {

    private final PhraseService phraseService;

    public PhraseController(PhraseService phraseService) {
        this.phraseService = phraseService;
    }

    @GetMapping
    public PagedResponse<PhraseSummaryResponse> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String language,
            Pageable pageable) {
        return phraseService.list(query, authorId, categoryId, tagId, language, pageable);
    }

    @GetMapping("/random")
    public PhraseResponse random() {
        return phraseService.random();
    }

    @GetMapping("/{id}")
    public PhraseResponse getById(@PathVariable Long id) {
        return phraseService.getById(id);
    }

    @PostMapping
    public ResponseEntity<PhraseResponse> create(@Valid @RequestBody CreatePhraseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(phraseService.create(request));
    }

    @PutMapping("/{id}")
    public PhraseResponse update(@PathVariable Long id,
                                 @Valid @RequestBody UpdatePhraseRequest request) {
        return phraseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        phraseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 12: Add author/category phrases endpoints**

> Adding `PhraseService` as a constructor dependency to these controllers changes
> their `@WebMvcTest` beans. Add to BOTH `AuthorControllerTest` and
> `CategoryControllerTest`:

```java
    @MockitoBean
    private com.phraseforge.phraseforge_api.phrase.PhraseService phraseService;
```

`AuthorController.java` — add method and dependency:

```java
    private final com.phraseforge.phraseforge_api.phrase.PhraseService phraseService;
    // in constructor: this.phraseService = phraseService;

    @GetMapping("/{id}/phrases")
    public com.phraseforge.phraseforge_api.common.PagedResponse<com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse> phrasesByAuthor(
            @PathVariable Long id, Pageable pageable) {
        return phraseService.list(null, id, null, null, null, pageable);
    }
```

`CategoryController.java` — add method and dependency:

```java
    private final com.phraseforge.phraseforge_api.phrase.PhraseService phraseService;
    // in constructor: this.phraseService = phraseService;

    @GetMapping("/{id}/phrases")
    public com.phraseforge.phraseforge_api.common.PagedResponse<com.phraseforge.phraseforge_api.phrase.dto.PhraseSummaryResponse> phrasesByCategory(
            @PathVariable Long id, Pageable pageable) {
        return phraseService.list(null, null, id, null, null, pageable);
    }
```

- [ ] **Step 13: Run full suite, then commit**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: all green (both new controller tests and previously passing tests).

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/phrase/ backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/author/AuthorController.java backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/category/CategoryController.java backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/phrase/
git commit -m "feat(backend): add phrase service and controller with filters, random, and nested endpoints"
```

### Educational summary

- **Java/Spring concepts:** `distinct(true)` + explicit joins to avoid duplicate rows from collection joins; `@EntityGraph` on a `findAll(Specification, Pageable)` override; `SecureRandom` for the random-phrase offset; controller route ordering so `/random` is not captured by `/{id}`.
- **Architectural decision:** the duplicate-on-update rule is pushed into a single repository query (`existsByContentAndAuthor_IdAndIdNot`) instead of filtering in memory.
- **Study before proceeding:** Criteria join semantics, `cq.distinct`, and why a single query beats loading everything into memory.

---

## Task 10: Exception handling + OpenAPI config + seed migration

**Files:**
- Create: `.../exception/ApiError.java`
- Create: `.../exception/ApiExceptionHandler.java`
- Create: `.../config/OpenApiConfig.java`
- Create: `backend/phraseforge-api/src/main/resources/db/migration/V7__seed_data.sql`
- Test: `.../exception/ApiExceptionHandlerTest.java` (or extend a controller test)

**Interfaces:**
- Produces: `@RestControllerAdvice` mapping validation/not-found/conflict/type-mismatch/generic errors to the `ApiError` body; `OpenAPI` bean; seed data.

- [ ] **Step 1: Create ApiError**

`exception/ApiError.java`:

```java
package com.phraseforge.phraseforge_api.exception;

import java.time.Instant;

public record ApiError(
        int status,
        String message,
        Instant timestamp) {

    public static ApiError of(int status, String message) {
        return new ApiError(status, message, Instant.now());
    }
}
```

- [ ] **Step 2: Create the global exception handler**

`exception/ApiExceptionHandler.java` — consistent responses, no stack traces to the client:

```java
package com.phraseforge.phraseforge_api.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.badRequest().body(ApiError.of(400, message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "Invalid request parameters"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "Invalid parameter: " + ex.getName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "Malformed request body"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleConflict(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Catches unique-constraint races that slip past service checks.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Resource conflicts with existing data"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal server error"));
    }
}
```

- [ ] **Step 3: Create OpenApiConfig**

`config/OpenApiConfig.java`:

```java
package com.phraseforge.phraseforge_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI phraseforgeOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PhraseForge API")
                .description("REST API for the PhraseForge quote library (MVP)")
                .version("1.0.0"));
    }
}
```

- [ ] **Step 4: Write the exception-handler test**

`exception/ApiExceptionHandlerTest.java`:

```java
package com.phraseforge.phraseforge_api.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void notFound_mapsTo404() {
        var response = handler.handleNotFound(new ResourceNotFoundException("Author not found: 9"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Author not found: 9");
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void conflict_mapsTo409() {
        var response = handler.handleConflict(new DuplicateResourceException("Tag already exists: x"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().status()).isEqualTo(409);
    }
}
```

- [ ] **Step 5: Write the seed migration**

`V7__seed_data.sql` — schema-invariant inserts (ids assigned by Hibernate on test runs via repository tests are NOT relevant here; this migration is applied to MySQL in dev and to H2 in tests — the SQL must be portable). Authors and quotes use realistic content; years follow the MVP rule: `year` omitted (NULL) where dating is uncertain (ancient figures). Categories/tags link via the join tables.

```sql
-- Authors
INSERT INTO authors (name, slug, birth_year, death_year, biography, created_at, updated_at)
VALUES
('Marcus Aurelius', 'marcus-aurelius', 121, 180, 'Roman emperor and Stoic philosopher. His private notes, the Meditations, are a landmark of Stoic thought.', NOW(), NOW()),
('Socrates', 'socrates', -470, -399, 'Athenian philosopher credited as a founder of Western philosophy. Known for the Socratic method and his trial and execution.', NOW(), NOW()),
('Aristotle', 'aristotle', -384, -322, 'Greek philosopher and polymath, student of Plato and tutor of Alexander the Great. Founder of the Peripatetic school.', NOW(), NOW()),
('Friedrich Nietzsche', 'friedrich-nietzsche', 1844, 1900, 'German philosopher and cultural critic whose work influenced modern existentialism and postmodern thought.', NOW(), NOW()),
('Simone de Beauvoir', 'simone-de-beauvoir', 1908, 1986, 'French existentialist philosopher, writer, and feminist theorist, author of The Second Sex.', NOW(), NOW()),
('Albert Einstein', 'albert-einstein', 1879, 1955, 'German-born theoretical physicist who developed the theory of relativity. Nobel laureate in Physics 1921.', NOW(), NOW()),
('Virginia Woolf', 'virginia-woolf', 1882, 1941, 'English modernist writer, pioneer of the stream-of-consciousness narrative.', NOW(), NOW()),
('Epictetus', 'epictetus', NULL, NULL, 'Greek Stoic philosopher, born a slave. His teachings were recorded by his pupil Arrian in the Discourses and the Enchiridion. Exact years are uncertain, so none are recorded.', NOW(), NOW());

-- Categories
INSERT INTO categories (name, slug, description, created_at, updated_at)
VALUES
('Filosofia', 'filosofia', 'Pensamento filosófico de diversas tradições.', NOW(), NOW()),
('Estoicismo', 'estoicismo', 'Filosofia prática greco-romana centrada na virtude e no controle das emoções.', NOW(), NOW()),
('Existencialismo', 'existencialismo', 'Filosofia centrada na liberdade, na escolha e no sentido da existência.', NOW(), NOW()),
('Ciência', 'ciencia', 'Frase sobre ciência, conhecimento e o universo.', NOW(), NOW()),
('Literatura', 'literatura', 'Frase de obras literárias e escritores.', NOW(), NOW()),
('Motivação', 'motivacao', 'Frases que inspiram ação e superação.', NOW(), NOW()),
('Sabedoria', 'sabedoria', 'Frases de sabedoria prática e reflexões sobre a vida.', NOW(), NOW());

-- Tags
INSERT INTO tags (name, created_at, updated_at)
VALUES
('mente', NOW(), NOW()),
('força', NOW(), NOW()),
('controle', NOW(), NOW()),
('resiliência', NOW(), NOW()),
('sabedoria', NOW(), NOW()),
('ação', NOW(), NOW()),
('liberdade', NOW(), NOW()),
('mudança', NOW(), NOW()),
('identidade', NOW(), NOW()),
('conhecimento', NOW(), NOW()),
('verdade', NOW(), NOW()),
('coragem', NOW(), NOW()),
('tempo', NOW(), NOW()),
('paz', NOW(), NOW());

-- Phrases (year intentionally NULL where dating is uncertain — MVP does not model approximate dates)
INSERT INTO phrases (content, author_id, year, language, source, created_at, updated_at)
VALUES
('Você tem poder sobre sua mente, não sobre os eventos externos. Perceba isso e encontrará força.',
 (SELECT id FROM authors WHERE slug = 'marcus-aurelius'), 170, 'pt', 'Meditações', NOW(), NOW()),
('Aquilo que se interpõe no caminho torna-se o caminho.',
 (SELECT id FROM authors WHERE slug = 'marcus-aurelius'), NULL, 'pt', 'Meditações', NOW(), NOW()),
('Conhece-te a ti mesmo.',
 (SELECT id FROM authors WHERE slug = 'socrates'), NULL, 'pt', NULL, NOW(), NOW()),
('A vida não examinada não vale a pena ser vivida.',
 (SELECT id FROM authors WHERE slug = 'socrates'), NULL, 'pt', NULL, NOW(), NOW()),
('Somos o que fazemos repetidamente. A excelência, então, não é um ato, mas um hábito.',
 (SELECT id FROM authors WHERE slug = 'aristotle'), NULL, 'pt', NULL, NOW(), NOW()),
('O que não nos mata nos torna mais fortes.',
 (SELECT id FROM authors WHERE slug = 'friedrich-nietzsche'), 1888, 'pt', 'Crepúsculo dos Ídolos', NOW(), NOW()),
('Quem tem um porquê para viver pode suportar quase qualquer como.',
 (SELECT id FROM authors WHERE slug = 'friedrich-nietzsche'), NULL, 'pt', NULL, NOW(), NOW()),
('Não se nasce mulher, torna-se mulher.',
 (SELECT id FROM authors WHERE slug = 'simone-de-beauvoir'), 1949, 'pt', 'O Segundo Sexo', NOW(), NOW()),
('A imaginação é mais importante que o conhecimento.',
 (SELECT id FROM authors WHERE slug = 'albert-einstein'), NULL, 'pt', NULL, NOW(), NOW()),
('A lógica levará você de A a B. A imaginação levará você a qualquer lugar.',
 (SELECT id FROM authors WHERE slug = 'albert-einstein'), NULL, 'en', NULL, NOW(), NOW()),
('Não se encontra paz evitando a vida.',
 (SELECT id FROM authors WHERE slug = 'virginia-woolf'), NULL, 'pt', NULL, NOW(), NOW()),
('No fundo do inverno, aprendi enfim que dentro de mim havia um verão invencível.',
 (SELECT id FROM authors WHERE slug = 'virginia-woolf'), NULL, 'pt', NULL, NOW(), NOW()),
('Não são os acontecimentos que perturbam as pessoas, mas sim seus julgamentos a respeito deles.',
 (SELECT id FROM authors WHERE slug = 'epictetus'), NULL, 'pt', 'Enchiridion', NOW(), NOW()),
('Apenas os instruídos são livres.',
 (SELECT id FROM authors WHERE slug = 'epictetus'), NULL, 'pt', 'Discursos', NOW(), NOW());

-- Phrase <-> Category links
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'estoicismo' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'marcus-aurelius');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'filosofia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'socrates');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'filosofia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'aristotle');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'filosofia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'friedrich-nietzsche');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'existencialismo' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'simone-de-beauvoir');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'ciencia' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'albert-einstein');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'literatura' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'virginia-woolf');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'estoicismo' WHERE p.author_id = (SELECT id FROM authors WHERE slug = 'epictetus');
INSERT INTO phrase_categories (phrase_id, category_id)
SELECT p.id, c.id FROM phrases p JOIN categories c ON c.slug = 'sabedoria' WHERE p.author_id IN (SELECT id FROM authors WHERE slug IN ('epictetus', 'socrates'));

-- Phrase <-> Tag links
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'mente' WHERE p.content LIKE 'Você tem poder%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'resiliência' WHERE p.content LIKE 'Aquilo que se interpõe%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'sabedoria' WHERE p.content LIKE 'Conhece-te%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'sabedoria' WHERE p.content LIKE 'A vida não examinada%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'ação' WHERE p.content LIKE 'Somos o que fazemos%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'força' WHERE p.content LIKE 'O que não nos mata%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'identidade' WHERE p.content LIKE 'Não se nasce mulher%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'conhecimento' WHERE p.content LIKE 'A imaginação é mais%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'paz' WHERE p.content LIKE 'Não se encontra paz%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'coragem' WHERE p.content LIKE 'No fundo do inverno%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'controle' WHERE p.content LIKE 'Não são os acontecimentos%';
INSERT INTO phrase_tags (phrase_id, tag_id)
SELECT p.id, t.id FROM phrases p JOIN tags t ON t.name = 'liberdade' WHERE p.content LIKE 'Apenas os instruídos%';
```

- [ ] **Step 6: Run full suite, then commit**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: green (V7 seed is EXCLUDED from the H2 test context via `spring.flyway.target=6`; tests use only V1–V6 schema migrations).

```bash
git add backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/exception/ backend/phraseforge-api/src/main/java/com/phraseforge/phraseforge_api/config/OpenApiConfig.java backend/phraseforge-api/src/main/resources/db/migration/V7__seed_data.sql backend/phraseforge-api/src/test/java/com/phraseforge/phraseforge_api/exception/
git commit -m "feat(backend): add global exception handling, OpenAPI config, and seed data"
```

### Educational summary

- **Java/Spring concepts:** `@RestControllerAdvice` + `@ExceptionHandler` mapping domain exceptions to consistent HTTP responses; `ApiError` record body; `DataIntegrityViolationException` as a safety net for unique-constraint races; springdoc `OpenAPI` bean.
- **Architectural decision:** error contract `{status, message, timestamp}` is uniform across 400/404/409/500 and never leaks stack traces.
- **Study before proceeding:** exception handler precedence, `@ResponseStatus` vs handler methods.

---

## Task 11: Backend end-to-end verification against real MySQL

**Files:** none (verification only)

**Interfaces:** — 

- [ ] **Step 1: Start MySQL via Docker**

Create `docker-compose.yml` and `.env.example` at the repo root NOW (full wiring happens in Task 22; a minimal db service is needed here):

`/home/jovi/Documentos/ws/phraseforge-java/docker-compose.yml`:

```yaml
services:
  db:
    image: mysql:8.4
    container_name: phraseforge-db
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: ${DB_NAME:-phraseforge}
      MYSQL_USER: ${DB_USERNAME:-phraseforge}
      MYSQL_PASSWORD: ${DB_PASSWORD:-phraseforge}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-phraseforge-root}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-u", "root", "-p$${MYSQL_ROOT_PASSWORD}"]
      interval: 5s
      timeout: 5s
      retries: 20
      start_period: 30s

volumes:
  mysql_data:
```

`/home/jovi/Documentos/ws/phraseforge-java/.env.example`:

```
# MySQL
DB_HOST=localhost
DB_PORT=3306
DB_NAME=phraseforge
DB_USERNAME=phraseforge
DB_PASSWORD=phraseforge
MYSQL_ROOT_PASSWORD=phraseforge-root

# Backend
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

- [ ] **Step 2: Start and wait for MySQL health**

Run: `docker compose up -d db && docker compose ps`
Wait for the `db` service to report `healthy` (poll `docker compose ps`).

- [ ] **Step 3: Run the app against MySQL and hit endpoints**

Run: `cd backend/phraseforge-api && DB_HOST=localhost DB_PORT=3306 DB_NAME=phraseforge DB_USERNAME=phraseforge DB_PASSWORD=phraseforge ./mvnw spring-boot:run`
Wait for startup, then in another shell:

```bash
curl -s http://localhost:8080/api/v1/phrases/random
curl -s "http://localhost:8080/api/v1/phrases?query=mente&page=0&size=5"
curl -s http://localhost:8080/api/v1/authors
curl -s http://localhost:8080/api/v1/categories
curl -s http://localhost:8080/api/v1/tags
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui.html
```

Expected: JSON responses with seed data; `/api/v1/authors/999` returns 404 `ApiError`; swagger returns 200. Verify the `V7__seed_data.sql` applied on MySQL (no Flyway error in startup logs).

- [ ] **Step 4: Stop the app and MySQL**

Stop the spring-boot process; run `docker compose down` (volume preserved) — it will be reused in Task 22.

- [ ] **Step 5: Commit the compose/dotenv files**

```bash
git add docker-compose.yml .env.example
git commit -m "chore(infra): add docker-compose for MySQL and .env.example"
```

### Educational summary

- **Java/Spring concepts:** end-to-end verification against a real MySQL (Flyway + seed applied); docker-compose service health.
- **Architectural decision:** MySQL healthcheck + compose volume; `.env.example` keeps credentials out of the repo.
- **Study before proceeding:** how Flyway behaves on an already-populated volume.

---

## Task 12: Frontend scaffold — deps, Tailwind, tokens, base structure

**Files:**
- Modify: `frontend/package.json` (add deps)
- Modify: `frontend/index.html` (lang, title)
- Modify: `frontend/vite.config.ts` (tailwind plugin + dev proxy)
- Modify: `frontend/src/index.css` (Tailwind v4 + design tokens)
- Modify: `frontend/src/main.tsx` (QueryClientProvider + RouterProvider)
- Create: `frontend/src/App.tsx` (router)
- Create: `frontend/src/types/models.ts`
- Delete: `frontend/src/App.css`, Vite template assets referenced by it
- Create: `frontend/src/lib/utils.ts`

**Interfaces:**
- Produces: `types/models.ts` (`Author`, `AuthorSummary`, `Category`, `CategorySummary`, `Tag`, `Phrase`, `PhraseSummary`, `Paged<T>`, `AuthorRef`, `CategoryRef`, `TagRef`); `lib/utils.ts` (`copyToClipboard`, `formatYear`); Tailwind token classes (`bg-paper`, `bg-card`, `text-ink`, `text-ink-muted`, `text-ink-faint`, `border-hair`, `border-subtle`).

- [ ] **Step 1: Install frontend dependencies**

Run: `cd frontend && npm install react-router-dom @tanstack/react-query tailwindcss @tailwindcss/vite`

(Adds React Router 7, TanStack Query 5, Tailwind v4 + its Vite plugin. React 19 already present.)

- [ ] **Step 2: Update package.json scripts are already fine** — no change needed. Verify `build` = `tsc -b && vite build` and `lint` = `oxlint` still present.

- [ ] **Step 3: Rewrite index.html**

`frontend/index.html`:

```html
<!doctype html>
<html lang="pt-BR">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>PhraseForge</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>
```

- [ ] **Step 4: Rewrite vite.config.ts**

`frontend/vite.config.ts` — dev proxy so the app can call `/api/v1` without CORS during development:

```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 5: Rewrite index.css with design tokens**

`frontend/src/index.css` — tokens transcribed from the prototype (docs/prototype):

```css
@import 'tailwindcss';
@import url('https://fonts.googleapis.com/css2?family=Lora:ital,wght@0,400;0,500;0,600;1,400;1,500&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600&display=swap');

@theme {
  --color-paper: #f9f8f6;
  --color-card: #ffffff;
  --color-ink: #111110;
  --color-ink-muted: #6b6b68;
  --color-ink-faint: #a8a8a4;
  --color-hair: #e4e3e0;
  --color-hair-subtle: #eeede9;
  --font-serif: 'Lora', serif;
  --font-sans: 'Inter', system-ui, sans-serif;
}

@layer base {
  body {
    background: var(--color-paper);
    color: var(--color-ink);
    font-family: var(--font-sans);
    font-size: 15px;
    line-height: 1.6;
    -webkit-font-smoothing: antialiased;
  }
  ::selection {
    background: var(--color-ink);
    color: var(--color-paper);
  }
  button {
    cursor: pointer;
    font-family: inherit;
  }
}
```

- [ ] **Step 6: Create the model types**

`frontend/src/types/models.ts`:

```ts
export interface Author {
  id: number
  name: string
  slug: string
  birthYear: number | null
  deathYear: number | null
  biography: string | null
  phraseCount: number
  createdAt: string
  updatedAt: string
}

export interface AuthorSummary {
  id: number
  name: string
  slug: string
  birthYear: number | null
  deathYear: number | null
  phraseCount: number
}

export interface Category {
  id: number
  name: string
  slug: string
  description: string | null
  phraseCount: number
}

export interface CategorySummary {
  id: number
  name: string
  slug: string
  phraseCount: number
}

export interface Tag {
  id: number
  name: string
}

export interface AuthorRef {
  id: number
  name: string
  slug: string
}

export interface CategoryRef {
  id: number
  name: string
  slug: string
}

export interface TagRef {
  id: number
  name: string
}

export interface Phrase {
  id: number
  content: string
  year: number | null
  language: string
  source: string | null
  author: AuthorRef
  categories: CategoryRef[]
  tags: TagRef[]
  createdAt: string
  updatedAt: string
}

export type PhraseSummary = Phrase

export interface Paged<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface PhraseFilters {
  query?: string
  authorId?: number
  categoryId?: number
  tagId?: number
  language?: string
  page?: number
  size?: number
}

export interface AuthorPayload {
  name: string
  birthYear: number | null
  deathYear: number | null
  biography: string | null
}

export interface CategoryPayload {
  name: string
  description: string | null
}

export interface TagPayload {
  name: string
}

export interface PhrasePayload {
  content: string
  authorId: number
  year: number | null
  language: string
  source: string | null
  categoryIds: number[]
  tagIds: number[]
}
```

Note: `PhraseSummary` is aliased to `Phrase` for MVP simplicity (the API returns the same shape from list and detail). The `*Payload` interfaces are shared by the services (Task 13) and the reusable admin forms (Task 21) — they live here so no component imports payload types from a service module.

- [ ] **Step 7: Create utils**

`frontend/src/lib/utils.ts`:

```ts
export async function copyToClipboard(text: string): Promise<void> {
  await navigator.clipboard.writeText(text)
}

export function formatYear(year: number | null): string {
  return year === null ? '' : String(year)
}
```

- [ ] **Step 8: Create the router and providers**

`frontend/src/main.tsx`:

```tsx
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RouterProvider } from 'react-router-dom'
import { router } from './App'
import './index.css'

const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
```

`frontend/src/App.tsx` — placeholder routes are filled by later tasks:

```tsx
import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from './layouts/PublicLayout'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <div>Home</div> },
      { path: 'explore', element: <div>Explore</div> },
    ],
  },
])
```

- [ ] **Step 9: Delete stale template files and verify build**

Run: `rm frontend/src/App.css` (and any template assets under `frontend/public` that are unused).

Run: `cd frontend && npm run build && npm run lint`
Expected: build + lint pass.

- [ ] **Step 10: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/index.html frontend/vite.config.ts frontend/src/
git commit -m "feat(frontend): scaffold Vite app with Tailwind tokens, router, and types"
```

### Educational summary

- **Frontend/TS concepts:** Tailwind v4 `@theme` tokens translating prototype CSS vars; Vite dev proxy removing CORS from the equation; `createBrowserRouter`;
- **Architectural decision:** centralized types mirror the API's DTOs; shared payload interfaces keep services and forms consistent.
- **Study before proceeding:** Tailwind v4 theme configuration, Vite proxy semantics.

---

## Task 13: Frontend HTTP client and API service modules

**Files:**
- Create: `frontend/src/services/api.ts`
- Create: `frontend/src/services/phrases.ts`
- Create: `frontend/src/services/authors.ts`
- Create: `frontend/src/services/categories.ts`
- Create: `frontend/src/services/tags.ts`

**Interfaces:**
- Consumes: `types/models.ts`.
- Produces: `services/api.ts` (`apiFetch<T>`), and per-domain modules with functions:
  - `phrases.ts`: `getPhrases(filters)`, `getPhrase(id)`, `getRandomPhrase()`, `createPhrase(body)`, `updatePhrase(id, body)`, `deletePhrase(id)`
  - `authors.ts`: `getAuthors(page, size)`, `getAuthor(id)`, `getAuthorPhrases(id, page, size)`, `createAuthor(body)`, `updateAuthor(id, body)`, `deleteAuthor(id)`
  - `categories.ts`: `getCategories(page, size)`, `getCategory(id)`, `getCategoryPhrases(id, page, size)`, `createCategory(body)`, `updateCategory(id, body)`, `deleteCategory(id)`
  - `tags.ts`: `getTags(page, size)`, `createTag(body)`, `updateTag(id, body)`, `deleteTag(id)`

- [ ] **Step 1: Create the centralized client**

`frontend/src/services/api.ts` — single fetch wrapper, no HTTP in components:

```ts
const BASE_URL = import.meta.env.VITE_API_URL ?? '/api/v1'

export class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export interface ApiErrorBody {
  status: number
  message: string
  timestamp: string
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (!res.ok) {
    let message = `Request failed with status ${res.status}`
    try {
      const body = (await res.json()) as ApiErrorBody
      message = body.message ?? message
    } catch {
      // non-JSON error body; keep generic message
    }
    throw new ApiError(res.status, message)
  }

  if (res.status === 204) {
    return undefined as T
  }

  return res.json() as Promise<T>
}

export function get<T>(path: string): Promise<T> {
  return request<T>(path)
}

export function post<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: 'POST', body: JSON.stringify(body) })
}

export function put<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, { method: 'PUT', body: JSON.stringify(body) })
}

export function del<T>(path: string): Promise<T> {
  return request<T>(path, { method: 'DELETE' })
}
```

- [ ] **Step 2: Create phrases service**

`frontend/src/services/phrases.ts`:

```ts
import { del, get, post, put } from './api'
import type { Paged, Phrase, PhraseFilters, PhrasePayload } from '../types/models'

function buildQuery(filters: PhraseFilters): string {
  const params = new URLSearchParams()
  if (filters.query) params.set('query', filters.query)
  if (filters.authorId) params.set('authorId', String(filters.authorId))
  if (filters.categoryId) params.set('categoryId', String(filters.categoryId))
  if (filters.tagId) params.set('tagId', String(filters.tagId))
  if (filters.language) params.set('language', filters.language)
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 20))
  return params.toString()
}

export function getPhrases(filters: PhraseFilters = {}): Promise<Paged<Phrase>> {
  return get<Paged<Phrase>>(`/phrases?${buildQuery(filters)}`)
}

export function getPhrase(id: number): Promise<Phrase> {
  return get<Phrase>(`/phrases/${id}`)
}

export function getRandomPhrase(): Promise<Phrase> {
  return get<Phrase>('/phrases/random')
}

export function createPhrase(body: PhrasePayload): Promise<Phrase> {
  return post<Phrase>('/phrases', body)
}

export function updatePhrase(id: number, body: PhrasePayload): Promise<Phrase> {
  return put<Phrase>(`/phrases/${id}`, body)
}

export function deletePhrase(id: number): Promise<void> {
  return del<void>(`/phrases/${id}`)
}
```

- [ ] **Step 3: Create authors service**

`frontend/src/services/authors.ts`:

```ts
import { del, get, post, put } from './api'
import type { Author, AuthorPayload, AuthorSummary, Paged, Phrase } from '../types/models'

export function getAuthors(page = 0, size = 20): Promise<Paged<AuthorSummary>> {
  return get<Paged<AuthorSummary>>(`/authors?page=${page}&size=${size}`)
}

export function getAuthor(id: number): Promise<Author> {
  return get<Author>(`/authors/${id}`)
}

export function getAuthorPhrases(id: number, page = 0, size = 20): Promise<Paged<Phrase>> {
  return get<Paged<Phrase>>(`/authors/${id}/phrases?page=${page}&size=${size}`)
}

export function createAuthor(body: AuthorPayload): Promise<Author> {
  return post<Author>('/authors', body)
}

export function updateAuthor(id: number, body: AuthorPayload): Promise<Author> {
  return put<Author>(`/authors/${id}`, body)
}

export function deleteAuthor(id: number): Promise<void> {
  return del<void>(`/authors/${id}`)
}
```

- [ ] **Step 4: Create categories service**

`frontend/src/services/categories.ts`:

```ts
import { del, get, post, put } from './api'
import type { Category, CategoryPayload, CategorySummary, Paged, Phrase } from '../types/models'

export function getCategories(page = 0, size = 50): Promise<Paged<CategorySummary>> {
  return get<Paged<CategorySummary>>(`/categories?page=${page}&size=${size}`)
}

export function getCategory(id: number): Promise<Category> {
  return get<Category>(`/categories/${id}`)
}

export function getCategoryPhrases(id: number, page = 0, size = 20): Promise<Paged<Phrase>> {
  return get<Paged<Phrase>>(`/categories/${id}/phrases?page=${page}&size=${size}`)
}

export function createCategory(body: CategoryPayload): Promise<Category> {
  return post<Category>('/categories', body)
}

export function updateCategory(id: number, body: CategoryPayload): Promise<Category> {
  return put<Category>(`/categories/${id}`, body)
}

export function deleteCategory(id: number): Promise<void> {
  return del<void>(`/categories/${id}`)
}
```

- [ ] **Step 5: Create tags service**

`frontend/src/services/tags.ts`:

```ts
import { del, get, post, put } from './api'
import type { Paged, Tag, TagPayload } from '../types/models'

export function getTags(page = 0, size = 100): Promise<Paged<Tag>> {
  return get<Paged<Tag>>(`/tags?page=${page}&size=${size}`)
}

export function createTag(body: TagPayload): Promise<Tag> {
  return post<Tag>('/tags', body)
}

export function updateTag(id: number, body: TagPayload): Promise<Tag> {
  return put<Tag>(`/tags/${id}`, body)
}

export function deleteTag(id: number): Promise<void> {
  return del<void>(`/tags/${id}`)
}
```

- [ ] **Step 6: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/services/
git commit -m "feat(frontend): add centralized HTTP client and API service modules"
```

### Educational summary

- **Frontend/TS concepts:** a single `fetch` wrapper (`api.ts`) with typed helpers and centralized error handling; per-domain service modules.
- **Architectural decision:** components never call `fetch` directly — all HTTP flows through services, so the API surface is easy to mock/replace.
- **Study before proceeding:** `URLSearchParams`, handling `204` responses, typed error bodies.

---

## Task 14: Frontend TanStack Query hooks

**Files:**
- Create: `frontend/src/hooks/usePhrases.ts`
- Create: `frontend/src/hooks/useAuthors.ts`
- Create: `frontend/src/hooks/useCategories.ts`
- Create: `frontend/src/hooks/useTags.ts`

**Interfaces:**
- Consumes: service modules + `types/models.ts`.
- Produces:
  - `usePhrases(filters)`, `usePhrase(id)`, `useRandomPhrase()`, `useCreatePhrase()`, `useUpdatePhrase()`, `useDeletePhrase()`
  - `useAuthors(page, size)`, `useAuthor(id)`, `useAuthorPhrases(id, page, size)`, `useCreateAuthor()`, `useUpdateAuthor()`, `useDeleteAuthor()`
  - `useCategories(page, size)`, `useCategory(id)`, `useCategoryPhrases(id, page, size)`, `useCreateCategory()`, `useUpdateCategory()`, `useDeleteCategory()`
  - `useTags(page, size)`, `useCreateTag()`, `useUpdateTag()`, `useDeleteTag()`

- [ ] **Step 1: Create phrase hooks**

`frontend/src/hooks/usePhrases.ts`:

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createPhrase,
  deletePhrase,
  getPhrase,
  getPhrases,
  getRandomPhrase,
  updatePhrase,
  type PhrasePayload,
} from '../services/phrases'
import type { PhraseFilters } from '../types/models'

const PHRASE_KEYS = {
  all: ['phrases'] as const,
  list: (filters: PhraseFilters) => ['phrases', 'list', filters] as const,
  detail: (id: number) => ['phrases', 'detail', id] as const,
  random: ['phrases', 'random'] as const,
}

export function usePhrases(filters: PhraseFilters = {}) {
  return useQuery({
    queryKey: PHRASE_KEYS.list(filters),
    queryFn: () => getPhrases(filters),
  })
}

export function usePhrase(id: number) {
  return useQuery({
    queryKey: PHRASE_KEYS.detail(id),
    queryFn: () => getPhrase(id),
  })
}

export function useRandomPhrase() {
  return useQuery({
    queryKey: PHRASE_KEYS.random,
    queryFn: getRandomPhrase,
  })
}

export function useCreatePhrase() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: PhrasePayload) => createPhrase(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['phrases'] })
    },
  })
}

export function useUpdatePhrase(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: PhrasePayload) => updatePhrase(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['phrases'] })
    },
  })
}

export function useDeletePhrase() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deletePhrase(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['phrases'] })
      qc.invalidateQueries({ queryKey: ['authors'] })
    },
  })
}
```

- [ ] **Step 2: Create author hooks**

`frontend/src/hooks/useAuthors.ts`:

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createAuthor,
  deleteAuthor,
  getAuthor,
  getAuthorPhrases,
  getAuthors,
  updateAuthor,
  type AuthorPayload,
} from '../services/authors'

export function useAuthors(page = 0, size = 20) {
  return useQuery({
    queryKey: ['authors', 'list', page, size],
    queryFn: () => getAuthors(page, size),
  })
}

export function useAuthor(id: number) {
  return useQuery({
    queryKey: ['authors', 'detail', id],
    queryFn: () => getAuthor(id),
  })
}

export function useAuthorPhrases(id: number, page = 0, size = 20) {
  return useQuery({
    queryKey: ['authors', id, 'phrases', page, size],
    queryFn: () => getAuthorPhrases(id, page, size),
  })
}

export function useCreateAuthor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: AuthorPayload) => createAuthor(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['authors'] }),
  })
}

export function useUpdateAuthor(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: AuthorPayload) => updateAuthor(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['authors'] }),
  })
}

export function useDeleteAuthor() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteAuthor(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['authors'] }),
  })
}
```

- [ ] **Step 3: Create category hooks**

`frontend/src/hooks/useCategories.ts`:

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createCategory,
  deleteCategory,
  getCategories,
  getCategory,
  getCategoryPhrases,
  updateCategory,
  type CategoryPayload,
} from '../services/categories'

export function useCategories(page = 0, size = 50) {
  return useQuery({
    queryKey: ['categories', 'list', page, size],
    queryFn: () => getCategories(page, size),
  })
}

export function useCategory(id: number) {
  return useQuery({
    queryKey: ['categories', 'detail', id],
    queryFn: () => getCategory(id),
  })
}

export function useCategoryPhrases(id: number, page = 0, size = 20) {
  return useQuery({
    queryKey: ['categories', id, 'phrases', page, size],
    queryFn: () => getCategoryPhrases(id, page, size),
  })
}

export function useCreateCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CategoryPayload) => createCategory(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}

export function useUpdateCategory(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CategoryPayload) => updateCategory(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}

export function useDeleteCategory() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteCategory(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['categories'] }),
  })
}
```

- [ ] **Step 4: Create tag hooks**

`frontend/src/hooks/useTags.ts`:

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createTag,
  deleteTag,
  getTags,
  updateTag,
  type TagPayload,
} from '../services/tags'

export function useTags(page = 0, size = 100) {
  return useQuery({
    queryKey: ['tags', 'list', page, size],
    queryFn: () => getTags(page, size),
  })
}

export function useCreateTag() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: TagPayload) => createTag(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
}

export function useUpdateTag(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: TagPayload) => updateTag(id, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
}

export function useDeleteTag() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteTag(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tags'] }),
  })
}
```

- [ ] **Step 5: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/hooks/
git commit -m "feat(frontend): add TanStack Query hooks for all resources"
```

### Educational summary

- **Frontend/TS concepts:** TanStack Query hooks (`useQuery`, `useMutation`, `useQueryClient`) with structured query keys and cache invalidation.
- **Architectural decision:** every server call has a hook; components consume data through hooks, decoupling UI from data fetching.
- **Study before proceeding:** query key design, optimistic updates, invalidateQueries.

---

## Task 15: Frontend shared layout and components

**Files:**
- Create: `frontend/src/layouts/PublicLayout.tsx`
- Create: `frontend/src/components/Header.tsx`
- Create: `frontend/src/components/Footer.tsx`
- Create: `frontend/src/components/QuoteCard.tsx`
- Create: `frontend/src/components/Chip.tsx`
- Create: `frontend/src/components/PillButton.tsx`
- Create: `frontend/src/components/SearchInput.tsx`
- Create: `frontend/src/components/Pagination.tsx`
- Create: `frontend/src/components/EmptyState.tsx`
- Create: `frontend/src/components/ErrorState.tsx`
- Create: `frontend/src/components/Loading.tsx`
- Create: `frontend/src/lib/useCopy.ts`

**Interfaces:**
- Consumes: `types/models.ts`, `lib/utils.ts`, `lib/useCopy.ts`.
- Produces: layout + presentational components reused by all pages. `useCopy.ts` provides `useCopy()` returning `{ copiedId, copy(text, id) }`.

- [ ] **Step 1: Create the copy hook**

`frontend/src/lib/useCopy.ts`:

```ts
import { useCallback, useRef, useState } from 'react'
import { copyToClipboard } from './utils'

export function useCopy() {
  const [copiedId, setCopiedId] = useState<string | null>(null)
  const timer = useRef<number | null>(null)

  const copy = useCallback(async (text: string, id: string) => {
    await copyToClipboard(text)
    setCopiedId(id)
    if (timer.current) window.clearTimeout(timer.current)
    timer.current = window.setTimeout(() => setCopiedId(null), 2000)
  }, [])

  return { copiedId, copy }
}
```

- [ ] **Step 2: Create the Header**

`frontend/src/components/Header.tsx` — sticky 52px, Lora wordmark, PT-BR nav, mobile hamburger:

```tsx
import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'

const navLinks = [
  { to: '/explore', label: 'Explorar' },
  { to: '/autores', label: 'Autores' },
  { to: '/categorias', label: 'Categorias' },
]

export default function Header() {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  return (
    <header className="sticky top-0 z-50 border-b border-hair-subtle bg-paper">
      <div className="mx-auto flex h-[52px] max-w-[1040px] items-center justify-between px-8">
        <button
          onClick={() => navigate('/')}
          className="font-serif text-base font-medium tracking-tight text-ink"
        >
          PhraseForge
        </button>

        <nav className="hidden items-center gap-1 md:flex">
          {navLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `rounded px-3 py-1.5 text-[13px] transition-colors ${
                  isActive ? 'font-medium text-ink' : 'text-ink-muted hover:text-ink'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        <button
          className="flex text-ink md:hidden"
          onClick={() => setOpen((v) => !v)}
          aria-label="Menu"
        >
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            {open ? (
              <>
                <line x1="3" y1="3" x2="15" y2="15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                <line x1="15" y1="3" x2="3" y2="15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
              </>
            ) : (
              <>
                <line x1="3" y1="5" x2="15" y2="5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                <line x1="3" y1="9" x2="15" y2="9" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
                <line x1="3" y1="13" x2="15" y2="13" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
              </>
            )}
          </svg>
        </button>
      </div>

      {open && (
        <div className="flex flex-col border-t border-hair-subtle px-8 py-4 md:hidden">
          {navLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                `border-b border-hair-subtle py-2.5 text-left text-[15px] ${
                  isActive ? 'font-medium text-ink' : 'text-ink-muted'
                }`
              }
            >
              {link.label}
            </NavLink>
          ))}
        </div>
      )}
    </header>
  )
}
```

- [ ] **Step 3: Create the Footer**

`frontend/src/components/Footer.tsx`:

```tsx
import { Link } from 'react-router-dom'

export default function Footer() {
  return (
    <footer className="border-t border-hair-subtle py-6 text-center">
      <Link to="/admin" className="text-xs text-ink-faint transition-colors hover:text-ink-muted">
        Admin
      </Link>
    </footer>
  )
}
```

- [ ] **Step 4: Create PublicLayout**

`frontend/src/layouts/PublicLayout.tsx`:

```tsx
import { Outlet, useLocation } from 'react-router-dom'
import { useEffect } from 'react'
import Header from '../components/Header'
import Footer from '../components/Footer'

export default function PublicLayout() {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }, [pathname])

  return (
    <div className="flex min-h-screen flex-col bg-paper">
      <Header />
      <main className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
```

- [ ] **Step 5: Create presentational components**

`frontend/src/components/Chip.tsx`:

```tsx
import type { ReactNode } from 'react'

export default function Chip({ children }: { children: ReactNode }) {
  return (
    <span className="rounded-[99px] border border-hair px-3 py-0.5 text-xs text-ink-muted">
      {children}
    </span>
  )
}
```

`frontend/src/components/PillButton.tsx`:

```tsx
import type { ReactNode } from 'react'

export default function PillButton({
  children,
  onClick,
  active = false,
}: {
  children: ReactNode
  onClick?: () => void
  active?: boolean
}) {
  return (
    <button
      onClick={onClick}
      className={`rounded-[99px] border px-3.5 py-1.5 text-[13px] transition-all ${
        active
          ? 'border-ink bg-ink font-medium text-paper'
          : 'border-hair text-ink-muted hover:border-ink-muted hover:text-ink'
      }`}
    >
      {children}
    </button>
  )
}
```

`frontend/src/components/SearchInput.tsx`:

```tsx
export default function SearchInput({
  value,
  onChange,
  placeholder,
}: {
  value: string
  onChange: (v: string) => void
  placeholder?: string
}) {
  return (
    <div className="relative max-w-[480px]">
      <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-sm text-ink-faint">
        ⌕
      </span>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded bg-card border border-hair py-2.5 pl-9 pr-3.5 text-sm text-ink outline-none transition-colors focus:border-ink-muted"
      />
    </div>
  )
}
```

`frontend/src/components/QuoteCard.tsx` — the Explore grid card (no favorites; only copy):

```tsx
import { Link } from 'react-router-dom'
import type { Phrase } from '../types/models'
import { formatYear } from '../lib/utils'
import { useCopy } from '../lib/useCopy'

export default function QuoteCard({ phrase }: { phrase: Phrase }) {
  const { copiedId, copy } = useCopy()
  const label = phrase.categories.length > 0 ? phrase.categories[0].name : ''

  const handleCopy = async () => {
    await copy(`"${phrase.content}" — ${phrase.author.name}`, String(phrase.id))
  }

  return (
    <div className="group flex flex-col gap-5 bg-paper p-8 transition-colors hover:bg-card">
      <Link to={`/frases/${phrase.id}`} className="flex flex-1 flex-col gap-5">
        <p className="font-serif italic leading-[1.65] text-ink">“{phrase.content}”</p>
        <div>
          <p className="mb-0.5 text-sm font-medium text-ink">— {phrase.author.name}</p>
          <p className="text-xs text-ink-faint">
            {label}
            {phrase.year !== null ? ` · ${formatYear(phrase.year)}` : ''}
          </p>
        </div>
      </Link>
      <button
        onClick={handleCopy}
        className="self-start text-xs text-ink-faint transition-colors hover:text-ink"
      >
        {copiedId === String(phrase.id) ? '✓ Copiada' : '⎘ Copiar'}
      </button>
    </div>
  )
}
```

`frontend/src/components/Pagination.tsx`:

```tsx
import type { Paged } from '../types/models'

export default function Pagination({
  data,
  onPage,
}: {
  data: Paged<unknown>
  onPage: (page: number) => void
}) {
  if (data.totalPages <= 1) return null
  return (
    <div className="mt-10 flex items-center justify-center gap-4 text-[13px]">
      <button
        disabled={data.page === 0}
        onClick={() => onPage(data.page - 1)}
        className="text-ink-muted transition-colors disabled:opacity-40 hover:text-ink"
      >
        ← Anterior
      </button>
      <span className="text-ink-faint">
        {data.page + 1} / {data.totalPages}
      </span>
      <button
        disabled={data.page >= data.totalPages - 1}
        onClick={() => onPage(data.page + 1)}
        className="text-ink-muted transition-colors disabled:opacity-40 hover:text-ink"
      >
        Próxima →
      </button>
    </div>
  )
}
```

`frontend/src/components/EmptyState.tsx`:

```tsx
export default function EmptyState({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="py-16 text-center text-ink-faint">
      <p className="mb-1 font-serif text-lg text-ink">{title}</p>
      {subtitle && <p className="text-sm">{subtitle}</p>}
    </div>
  )
}
```

`frontend/src/components/ErrorState.tsx`:

```tsx
export default function ErrorState({ message }: { message: string }) {
  return (
    <div className="py-16 text-center">
      <p className="mb-1 font-serif text-lg text-ink">Algo deu errado</p>
      <p className="text-sm text-ink-faint">{message}</p>
    </div>
  )
}
```

`frontend/src/components/Loading.tsx`:

```tsx
export default function Loading() {
  return (
    <div className="py-16 text-center text-sm text-ink-faint">Carregando…</div>
  )
}
```

- [ ] **Step 6: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/layouts/ frontend/src/components/ frontend/src/lib/useCopy.ts
git commit -m "feat(frontend): add shared layout, header, footer, and presentational components"
```

### Educational summary

- **Frontend/React concepts:** layout routes with `Outlet`; reusable presentational components (QuoteCard, Chip, PillButton, Pagination, states); custom `useCopy` hook for clipboard feedback.
- **Architectural decision:** shared components keep pages small and the design system consistent.
- **Study before proceeding:** React Router nested routes, `useEffect` scroll restoration, hook design.

---

## Task 16: Home page (random quote)

**Files:**
- Create: `frontend/src/pages/Home.tsx`
- Modify: `frontend/src/App.tsx` (register route)

**Interfaces:**
- Consumes: `useRandomPhrase`, `useCopy`, `formatYear`, `Chip`/`PillButton` styles inline.
- Produces: route `/` rendering one centered random phrase with actions (Nova Frase, Copiar, Compartilhar).

- [ ] **Step 1: Create the Home page**

`frontend/src/pages/Home.tsx` — matches prototype layout: centered column, uppercase micro-label, Lora italic quote, author + year, action row, "Nova Frase" pill + counter:

```tsx
import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useRandomPhrase } from '../hooks/usePhrases'
import { useCopy } from '../lib/useCopy'
import { formatYear } from '../lib/utils'
import { getRandomPhrase } from '../services/phrases'

export default function Home() {
  const { data, isLoading, isError, refetch } = useRandomPhrase()
  const { copiedId, copy } = useCopy()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [visible, setVisible] = useState(true)

  const nextQuote = async () => {
    setVisible(false)
    await new Promise((r) => setTimeout(r, 280))
    queryClient.removeQueries({ queryKey: ['phrases', 'random'] })
    await refetch()
    setVisible(true)
  }

  if (isLoading) {
    return <main className="flex min-h-[calc(100vh-52px)] items-center justify-center text-sm text-ink-faint">Carregando…</main>
  }

  if (isError || !data) {
    return (
      <main className="flex min-h-[calc(100vh-52px)] items-center justify-center">
        <p className="text-sm text-ink-faint">Não foi possível carregar a frase.</p>
      </main>
    )
  }

  const phrase = data
  const label = phrase.categories.length > 0 ? phrase.categories[0].name : ''
  const quoteText = `"${phrase.content}" — ${phrase.author.name}`

  const copyQuote = () => copy(quoteText, String(phrase.id))
  const share = () => copy(window.location.href, 'share')

  return (
    <main className="flex min-h-[calc(100vh-52px)] flex-col items-center justify-center px-8 py-16">
      <div
        className={`w-full max-w-[680px] transition-opacity duration-300 ${visible ? 'opacity-100' : 'opacity-0'}`}
      >
        {label && (
          <p className="mb-10 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">
            {label}
          </p>
        )}

        <blockquote className="mb-10 font-serif text-[clamp(1.375rem,3vw,2rem)] font-normal italic leading-[1.55] tracking-[-0.015em] text-ink">
          “{phrase.content}”
        </blockquote>

        <div className="mb-14">
          <button
            onClick={() => navigate(`/autores/${phrase.author.id}`)}
            className="mb-0.5 block text-left text-[15px] font-medium text-ink transition-opacity hover:opacity-60"
          >
            — {phrase.author.name}
          </button>
          <span className="text-[13px] text-ink-faint">{formatYear(phrase.year)}</span>
        </div>

        <div className="mb-16 flex items-center gap-6">
          <button onClick={copyQuote} className="flex items-center gap-1.5 text-[13px] text-ink-muted transition-colors hover:text-ink">
            <span>{copiedId === String(phrase.id) ? '✓' : '⎘'}</span>
            {copiedId === String(phrase.id) ? 'Copiada' : 'Copiar'}
          </button>
          <button onClick={share} className="flex items-center gap-1.5 text-[13px] text-ink-muted transition-colors hover:text-ink">
            <span>↗</span>
            {copiedId === 'share' ? 'Link copiado' : 'Compartilhar'}
          </button>
        </div>

        <div className="flex items-center gap-6">
          <button
            onClick={nextQuote}
            className="rounded-[99px] border border-hair px-5.5 py-2 text-[13px] font-medium text-ink transition-all hover:bg-ink hover:text-paper"
          >
            Nova Frase
          </button>
        </div>
      </div>
    </main>
  )
}
```

- [ ] **Step 2: Register the route in App.tsx**

`frontend/src/App.tsx`:

```tsx
import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from './layouts/PublicLayout'
import Home from './pages/Home'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'explore', element: <div>Explore</div> },
    ],
  },
])
```

- [ ] **Step 3: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/pages/Home.tsx frontend/src/App.tsx
git commit -m "feat(frontend): add Home page with random quote"
```

### Educational summary

- **Frontend/React concepts:** server-state refresh via `removeQueries` + `refetch` to fetch a new random quote; CSS transition for the quote swap.
- **Architectural decision:** Home is a single-query page; no favorites (V1).
- **Study before proceeding:** TanStack Query cache key management.

---

## Task 17: Explore page

**Files:**
- Create: `frontend/src/pages/Explore.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `usePhrases`, `useCategories`, `SearchInput`, `PillButton`, `QuoteCard`, `Pagination`, `EmptyState`, `Loading`, `ErrorState`.

- [ ] **Step 1: Create the Explore page**

`frontend/src/pages/Explore.tsx` — Lora heading, search, category pills, result count, hairline grid of QuoteCards, pagination:

```tsx
import { useState } from 'react'
import { useCategories } from '../hooks/useCategories'
import { usePhrases } from '../hooks/usePhrases'
import SearchInput from '../components/SearchInput'
import PillButton from '../components/PillButton'
import QuoteCard from '../components/QuoteCard'
import Pagination from '../components/Pagination'
import EmptyState from '../components/EmptyState'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'

const PAGE_SIZE = 12

export default function Explore() {
  const [query, setQuery] = useState('')
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [page, setPage] = useState(0)

  const { data: categories } = useCategories(0, 50)
  const { data, isLoading, isError, error } = usePhrases({
    query: query || undefined,
    categoryId: categoryId ?? undefined,
    page,
    size: PAGE_SIZE,
  })

  const countLabel = data
    ? `${data.totalElements} ${data.totalElements === 1 ? 'frase' : 'frases'}`
    : ''

  return (
    <main className="mx-auto max-w-[1040px] px-8 py-16">
      <div className="mb-12">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          Explorar
        </h1>
        <p className="text-[15px] text-ink-muted">
          Descubra ideias, pensamentos e palavras de diferentes pensadores.
        </p>
      </div>

      <div className="mb-7">
        <SearchInput
          value={query}
          onChange={(v) => {
            setQuery(v)
            setPage(0)
          }}
          placeholder="Buscar frases, autores ou temas..."
        />
      </div>

      {categories && categories.content.length > 0 && (
        <div className="mb-10 flex flex-wrap gap-2">
          <PillButton active={categoryId === null} onClick={() => { setCategoryId(null); setPage(0) }}>
            Todas
          </PillButton>
          {categories.content.map((c) => (
            <PillButton
              key={c.id}
              active={categoryId === c.id}
              onClick={() => { setCategoryId(c.id); setPage(0) }}
            >
              {c.name}
            </PillButton>
          ))}
        </div>
      )}

      <p className="mb-8 text-[13px] text-ink-faint">{countLabel}</p>

      {isLoading && <Loading />}
      {isError && <ErrorState message={error instanceof Error ? error.message : 'Erro desconhecido'} />}

      {data && data.content.length === 0 && (
        <EmptyState title="Nenhuma frase encontrada" subtitle="Tente outra busca ou filtro." />
      )}

      {data && data.content.length > 0 && (
        <div className="grid grid-cols-1 gap-px overflow-hidden rounded border border-hair-subtle bg-hair-subtle sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((phrase) => (
            <QuoteCard key={phrase.id} phrase={phrase} />
          ))}
        </div>
      )}

      {data && <Pagination data={data} onPage={setPage} />}
    </main>
  )
}
```

- [ ] **Step 2: Register the route**

`frontend/src/App.tsx` — replace the Explore placeholder:

```tsx
import Explore from './pages/Explore'
// ...
      { path: 'explore', element: <Explore /> },
```

- [ ] **Step 3: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/pages/Explore.tsx frontend/src/App.tsx
git commit -m "feat(frontend): add Explore page with search, filters, and pagination"
```

### Educational summary

- **Frontend/React concepts:** controlled inputs driving query params; category pill filters; pagination via `usePhrases`.
- **Architectural decision:** filters map 1:1 to API query params — the frontend adds no filter logic of its own.
- **Study before proceeding:** debouncing search input (optional improvement, not required for MVP).

---

## Task 18: Quote Detail page

**Files:**
- Create: `frontend/src/pages/QuoteDetail.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `usePhrase`, `usePhrases` (related by author), `useCopy`, `Chip`, `formatYear`.

- [ ] **Step 1: Create the Quote Detail page**

`frontend/src/pages/QuoteDetail.tsx` — back link, category label, quote, author, chip row (categories, tags, language, source), actions (Copiar, Compartilhar), "Mais de {autor}" related list:

```tsx
import { Link, useParams } from 'react-router-dom'
import { usePhrase, usePhrases } from '../hooks/usePhrases'
import { useCopy } from '../lib/useCopy'
import { formatYear } from '../lib/utils'
import Chip from '../components/Chip'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'

export default function QuoteDetail() {
  const { id } = useParams<{ id: string }>()
  const phraseId = Number(id)
  const { data, isLoading, isError, error } = usePhrase(phraseId)
  const { copiedId, copy } = useCopy()

  const relatedQuery = usePhrases(
    data ? { authorId: data.author.id, size: 5 } : undefined,
  )
  const related = relatedQuery.data?.content.filter((p) => p.id !== phraseId) ?? []

  if (isLoading) return <main className="mx-auto max-w-[680px] px-8 py-20"><Loading /></main>
  if (isError || !data) {
    return <main className="mx-auto max-w-[680px] px-8 py-20"><ErrorState message={error instanceof Error ? error.message : 'Frase não encontrada'} /></main>
  }

  const quoteText = `"${data.content}" — ${data.author.name}`
  const label = data.categories.length > 0 ? data.categories[0].name : ''

  return (
    <main className="mx-auto max-w-[680px] px-8 py-20">
      <Link to="/explore" className="mb-12 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-ink">
        ← Voltar
      </Link>

      {label && (
        <p className="mb-8 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">
          {label}
        </p>
      )}

      <blockquote className="mb-10 font-serif text-[clamp(1.375rem,3vw,2rem)] font-normal italic leading-[1.55] tracking-[-0.015em] text-ink">
        “{data.content}”
      </blockquote>

      <div className="mb-12">
        <Link to={`/autores/${data.author.id}`} className="mb-0.5 block text-base font-medium text-ink transition-opacity hover:opacity-60">
          — {data.author.name}
        </Link>
        <span className="text-[13px] text-ink-faint">{formatYear(data.year)}</span>
      </div>

      <div className="mb-12 flex flex-wrap gap-2">
        {data.categories.map((c) => (
          <Chip key={c.id}>{c.name}</Chip>
        ))}
        {data.tags.map((t) => (
          <Chip key={t.id}>#{t.name}</Chip>
        ))}
        <span className="self-center text-xs text-ink-faint">
          {data.language.toUpperCase()}
          {data.source ? ` · ${data.source}` : ''}
        </span>
      </div>

      <div className="mb-20 flex gap-3 border-t border-hair-subtle pt-8">
        <button
          onClick={() => copy(quoteText, 'quote')}
          className={`rounded border px-4 py-2 text-[13px] transition-all ${
            copiedId === 'quote' ? 'border-ink bg-ink font-medium text-paper' : 'border-hair text-ink-muted hover:border-ink-muted hover:text-ink'
          }`}
        >
          {copiedId === 'quote' ? '✓ Copiada' : '⎘ Copiar Frase'}
        </button>
        <button
          onClick={() => copy(window.location.href, 'share')}
          className="rounded border border-hair px-4 py-2 text-[13px] text-ink-muted transition-all hover:border-ink-muted hover:text-ink"
        >
          {copiedId === 'share' ? 'Link copiado' : '↗ Compartilhar'}
        </button>
      </div>

      {related.length > 0 && (
        <section>
          <p className="mb-6 text-xs font-semibold uppercase tracking-[0.08em] text-ink-faint">
            Mais de {data.author.name}
          </p>
          <div className="flex flex-col">
            {related.map((p) => (
              <Link
                key={p.id}
                to={`/frases/${p.id}`}
                className="border-b border-hair-subtle py-5 transition-opacity hover:opacity-60 first:border-t"
              >
                <p className="font-serif italic leading-[1.6] text-ink">“{p.content}”</p>
              </Link>
            ))}
          </div>
        </section>
      )}
    </main>
  )
}
```

- [ ] **Step 2: Register the route**

`frontend/src/App.tsx`:

```tsx
import QuoteDetail from './pages/QuoteDetail'
// ...
      { path: 'frases/:id', element: <QuoteDetail /> },
```

- [ ] **Step 3: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/pages/QuoteDetail.tsx frontend/src/App.tsx
git commit -m "feat(frontend): add Quote Detail page with related phrases"
```

### Educational summary

- **Frontend/React concepts:** reading URL params with `useParams`; combining detail + related-list queries.
- **Architectural decision:** related phrases reuse the author filter of the phrases endpoint.
- **Study before proceeding:** parallel queries and loading states.

---

## Task 19: Authors and Author Detail pages

**Files:**
- Create: `frontend/src/pages/Authors.tsx`
- Create: `frontend/src/pages/AuthorDetail.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `useAuthors`, `useAuthor`, `useAuthorPhrases`, `SearchInput`, `Pagination`, `QuoteCard`, `EmptyState`, `Loading`, `ErrorState`, `formatYear`.

- [ ] **Step 1: Create the Authors page**

`frontend/src/pages/Authors.tsx` — Lora heading, search (client-side over loaded page), hairline list rows with name + count:

```tsx
import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthors } from '../hooks/useAuthors'
import SearchInput from '../components/SearchInput'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'

export default function Authors() {
  const [query, setQuery] = useState('')
  const { data, isLoading, isError, error } = useAuthors(0, 100)

  const results = useMemo(() => {
    if (!data) return []
    const q = query.trim().toLowerCase()
    if (!q) return data.content
    return data.content.filter(
      (a) =>
        a.name.toLowerCase().includes(q) ||
        (a.birthYear !== null && String(a.birthYear).includes(q)),
    )
  }, [data, query])

  return (
    <main className="mx-auto max-w-[720px] px-8 py-16">
      <div className="mb-12">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          Autores
        </h1>
        <p className="text-[15px] text-ink-muted">Explore os pensadores por trás das palavras.</p>
      </div>

      <div className="mb-10">
        <SearchInput value={query} onChange={setQuery} placeholder="Buscar autores..." />
      </div>

      {isLoading && <Loading />}
      {isError && <ErrorState message={error instanceof Error ? error.message : 'Erro desconhecido'} />}

      <div className="flex flex-col">
        {results.map((author, i) => (
          <Link
            key={author.id}
            to={`/autores/${author.id}`}
            className={`flex items-baseline justify-between gap-8 py-6 transition-opacity hover:opacity-60 ${
              i === 0 ? '' : 'border-t border-hair-subtle'
            }`}
          >
            <div>
              <p className="mb-0.5 font-serif text-lg text-ink">{author.name}</p>
              <p className="text-[13px] text-ink-muted">
                {author.birthYear !== null && author.deathYear !== null
                  ? `${formatYear(author.birthYear)}–${formatYear(author.deathYear)}`
                  : author.biography?.slice(0, 90) ?? ''}
              </p>
            </div>
            <span className="whitespace-nowrap text-xs text-ink-faint">
              {author.phraseCount} {author.phraseCount === 1 ? 'frase' : 'frases'}
            </span>
          </Link>
        ))}
      </div>
    </main>
  )
}
```

- [ ] **Step 2: Create the Author Detail page**

`frontend/src/pages/AuthorDetail.tsx` — back link, name, dates, biography, phrase count, phrase list:

```tsx
import { Link, useParams } from 'react-router-dom'
import { useAuthor, useAuthorPhrases } from '../hooks/useAuthors'
import { formatYear } from '../lib/utils'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'
import Pagination from '../components/Pagination'
import { useState } from 'react'

export default function AuthorDetail() {
  const { id } = useParams<{ id: string }>()
  const authorId = Number(id)
  const [page, setPage] = useState(0)

  const { data: author, isLoading, isError, error } = useAuthor(authorId)
  const phrasesQuery = useAuthorPhrases(authorId, page, 10)

  if (isLoading) return <main className="mx-auto max-w-[680px] px-8 py-20"><Loading /></main>
  if (isError || !author) {
    return <main className="mx-auto max-w-[680px] px-8 py-20"><ErrorState message={error instanceof Error ? error.message : 'Autor não encontrado'} /></main>
  }

  const years =
    author.birthYear !== null && author.deathYear !== null
      ? `${formatYear(author.birthYear)}–${formatYear(author.deathYear)}`
      : ''

  return (
    <main className="mx-auto max-w-[680px] px-8 py-20">
      <Link to="/autores" className="mb-12 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-ink">
        ← Autores
      </Link>

      <div className="mb-14 border-b border-hair-subtle pb-12">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,3rem)] font-normal tracking-[-0.02em] text-ink">
          {author.name}
        </h1>
        {years && <p className="mb-7 text-[13px] text-ink-faint">{years}</p>}
        {author.biography && (
          <p className="text-[15px] leading-[1.75] text-ink-muted">{author.biography}</p>
        )}
      </div>

      <p className="mb-6 text-xs font-semibold uppercase tracking-[0.08em] text-ink-faint">
        Frases de {author.name}
      </p>

      {phrasesQuery.isLoading && <Loading />}
      {phrasesQuery.isError && <ErrorState message="Não foi possível carregar as frases." />}

      {phrasesQuery.data && phrasesQuery.data.content.length === 0 && (
        <p className="py-8 text-sm text-ink-faint">Nenhuma frase registrada para este autor.</p>
      )}

      <div className="flex flex-col">
        {phrasesQuery.data?.content.map((phrase, i) => (
          <Link
            key={phrase.id}
            to={`/frases/${phrase.id}`}
            className={`flex flex-col gap-4 py-7 transition-opacity hover:opacity-60 ${
              i === 0 ? '' : 'border-t border-hair-subtle'
            }`}
          >
            <p className="font-serif text-[17px] italic leading-[1.65] text-ink">“{phrase.content}”</p>
            <div className="flex flex-wrap gap-2">
              {phrase.categories.map((c) => (
                <span key={c.id} className="rounded-[99px] border border-hair-subtle px-2.5 py-0.5 text-[11px] text-ink-faint">
                  {c.name}
                </span>
              ))}
            </div>
          </Link>
        ))}
      </div>

      {phrasesQuery.data && <Pagination data={phrasesQuery.data} onPage={setPage} />}
    </main>
  )
}
```

- [ ] **Step 3: Register the routes**

`frontend/src/App.tsx`:

```tsx
import Authors from './pages/Authors'
import AuthorDetail from './pages/AuthorDetail'
// ...
      { path: 'autores', element: <Authors /> },
      { path: 'autores/:id', element: <AuthorDetail /> },
```

- [ ] **Step 4: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/pages/Authors.tsx frontend/src/pages/AuthorDetail.tsx frontend/src/App.tsx
git commit -m "feat(frontend): add Authors and Author Detail pages"
```

### Educational summary

- **Frontend/React concepts:** list pages with client-side search over a loaded page; detail pages composing author + phrase queries.
- **Architectural decision:** author search is client-side for MVP (small dataset); server-side search is a V2 option.
- **Study before proceeding:** `useMemo` for derived filtered lists.

---

## Task 20: Categories and Category Detail pages

**Files:**
- Create: `frontend/src/pages/Categories.tsx`
- Create: `frontend/src/pages/CategoryDetail.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: `useCategories`, `useCategory`, `useCategoryPhrases`, `Pagination`, `QuoteCard`, `EmptyState`, `Loading`, `ErrorState`.

- [ ] **Step 1: Create the Categories page**

`frontend/src/pages/Categories.tsx` — Lora heading, hairline list rows with name, description, count, arrow:

```tsx
import { Link } from 'react-router-dom'
import { useCategories } from '../hooks/useCategories'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'

export default function Categories() {
  const { data, isLoading, isError, error } = useCategories(0, 100)

  return (
    <main className="mx-auto max-w-[680px] px-8 py-16">
      <div className="mb-14">
        <h1 className="font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          Categorias
        </h1>
      </div>

      {isLoading && <Loading />}
      {isError && <ErrorState message={error instanceof Error ? error.message : 'Erro desconhecido'} />}

      <div className="flex flex-col">
        {data?.content.map((category, i) => (
          <Link
            key={category.id}
            to={`/categorias/${category.id}`}
            className={`flex items-center justify-between py-5 transition-opacity hover:opacity-50 ${
              i === 0 ? '' : 'border-t border-hair-subtle'
            }`}
          >
            <div>
              <p className="font-serif text-xl font-normal tracking-[-0.01em] text-ink">
                {category.name}
              </p>
              {category.description && (
                <p className="mt-0.5 text-[13px] text-ink-muted">{category.description}</p>
              )}
            </div>
            <div className="flex items-center gap-4">
              <span className="text-xs text-ink-faint">
                {category.phraseCount} {category.phraseCount === 1 ? 'frase' : 'frases'}
              </span>
              <span className="text-sm text-ink-faint">→</span>
            </div>
          </Link>
        ))}
      </div>
    </main>
  )
}
```

- [ ] **Step 2: Create the Category Detail page**

`frontend/src/pages/CategoryDetail.tsx` — category header + its phrases:

```tsx
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useCategory, useCategoryPhrases } from '../hooks/useCategories'
import QuoteCard from '../components/QuoteCard'
import Pagination from '../components/Pagination'
import EmptyState from '../components/EmptyState'
import Loading from '../components/Loading'
import ErrorState from '../components/ErrorState'

export default function CategoryDetail() {
  const { id } = useParams<{ id: string }>()
  const categoryId = Number(id)
  const [page, setPage] = useState(0)

  const { data: category, isLoading, isError, error } = useCategory(categoryId)
  const phrasesQuery = useCategoryPhrases(categoryId, page, 12)

  if (isLoading) return <main className="mx-auto max-w-[1040px] px-8 py-20"><Loading /></main>
  if (isError || !category) {
    return <main className="mx-auto max-w-[1040px] px-8 py-20"><ErrorState message={error instanceof Error ? error.message : 'Categoria não encontrada'} /></main>
  }

  return (
    <main className="mx-auto max-w-[1040px] px-8 py-16">
      <Link to="/categorias" className="mb-12 inline-flex items-center gap-1.5 text-[13px] text-ink-faint transition-colors hover:text-ink">
        ← Categorias
      </Link>

      <div className="mb-10">
        <h1 className="mb-2 font-serif text-[clamp(2rem,4vw,2.75rem)] font-normal tracking-[-0.02em] text-ink">
          {category.name}
        </h1>
        {category.description && (
          <p className="text-[15px] text-ink-muted">{category.description}</p>
        )}
      </div>

      <p className="mb-8 text-[13px] text-ink-faint">
        {category.phraseCount} {category.phraseCount === 1 ? 'frase' : 'frases'}
      </p>

      {phrasesQuery.isLoading && <Loading />}
      {phrasesQuery.isError && <ErrorState message="Não foi possível carregar as frases." />}
      {phrasesQuery.data && phrasesQuery.data.content.length === 0 && (
        <EmptyState title="Nenhuma frase nesta categoria" />
      )}

      {phrasesQuery.data && phrasesQuery.data.content.length > 0 && (
        <div className="grid grid-cols-1 gap-px overflow-hidden rounded border border-hair-subtle bg-hair-subtle sm:grid-cols-2 lg:grid-cols-3">
          {phrasesQuery.data.content.map((phrase) => (
            <QuoteCard key={phrase.id} phrase={phrase} />
          ))}
        </div>
      )}

      {phrasesQuery.data && <Pagination data={phrasesQuery.data} onPage={setPage} />}
    </main>
  )
}
```

- [ ] **Step 3: Register the routes**

`frontend/src/App.tsx`:

```tsx
import Categories from './pages/Categories'
import CategoryDetail from './pages/CategoryDetail'
// ...
      { path: 'categorias', element: <Categories /> },
      { path: 'categorias/:id', element: <CategoryDetail /> },
```

- [ ] **Step 4: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/pages/Categories.tsx frontend/src/pages/CategoryDetail.tsx frontend/src/App.tsx
git commit -m "feat(frontend): add Categories and Category Detail pages"
```

### Educational summary

- **Frontend/React concepts:** category listing with counts; category detail reusing the phrases grid.
- **Architectural decision:** counts come from the API's summary DTO, not client-side computation.
- **Study before proceeding:** reusing shared grid components across pages.

---

## Task 21: Admin area (full CRUD)

**Files:**
- Create: `frontend/src/layouts/AdminLayout.tsx`
- Create: `frontend/src/components/admin/StatCard.tsx`
- Create: `frontend/src/components/admin/FormField.tsx`
- Create: `frontend/src/components/admin/ActionBar.tsx`
- Create: `frontend/src/components/admin/EntityTable.tsx`
- Create: `frontend/src/components/admin/AuthorForm.tsx`
- Create: `frontend/src/components/admin/CategoryForm.tsx`
- Create: `frontend/src/components/admin/TagForm.tsx`
- Create: `frontend/src/components/admin/PhraseForm.tsx`
- Create: `frontend/src/pages/admin/Dashboard.tsx`
- Create: `frontend/src/pages/admin/AdminPhrases.tsx`
- Create: `frontend/src/pages/admin/AdminAuthors.tsx`
- Create: `frontend/src/pages/admin/AdminCategories.tsx`
- Create: `frontend/src/pages/admin/AdminTags.tsx`
- Modify: `frontend/src/App.tsx`

**Interfaces:**
- Consumes: hooks + services + shared components.
- Produces: `/admin` routes with sidebar (Painel, Frases, Autores, Categorias, Tags). Full CRUD for all four resources — create, read/view, update, and delete — reusing a single form component per resource for both create and edit. No auth (MVP).

- [ ] **Step 1: Create AdminLayout**

`frontend/src/layouts/AdminLayout.tsx` — sticky admin top bar + sidebar + Outlet:

```tsx
import { Link, NavLink, Outlet } from 'react-router-dom'

const navItems = [
  { to: '/admin', label: 'Painel', end: true },
  { to: '/admin/frases', label: 'Frases' },
  { to: '/admin/autores', label: 'Autores' },
  { to: '/admin/categorias', label: 'Categorias' },
  { to: '/admin/tags', label: 'Tags' },
]

export default function AdminLayout() {
  return (
    <div className="min-h-screen bg-paper">
      <div className="flex h-[52px] items-center justify-between border-b border-hair-subtle bg-paper px-8">
        <Link to="/" className="font-serif text-base font-medium tracking-tight text-ink">
          PhraseForge
        </Link>
        <span className="text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">Admin</span>
        <Link to="/" className="text-[13px] text-ink-muted transition-colors hover:text-ink">
          ← Voltar ao site
        </Link>
      </div>

      <div className="flex">
        <aside className="hidden w-[200px] shrink-0 border-r border-hair-subtle p-4 pt-8 md:block">
          <p className="mb-4 px-2 text-[11px] font-semibold uppercase tracking-[0.1em] text-ink-faint">
            Admin
          </p>
          <nav className="flex flex-col gap-0.5">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  `rounded px-2 py-1.5 text-left text-sm transition-colors ${
                    isActive ? 'bg-hair-subtle font-medium text-ink' : 'text-ink-muted hover:text-ink'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </aside>

        <main className="min-w-0 flex-1 p-8 md:p-10">
          <Outlet />
        </main>
      </div>

      <div className="border-t border-hair-subtle p-4 md:hidden">
        <nav className="flex flex-wrap gap-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className="rounded border border-hair px-3 py-1 text-[13px] text-ink-muted"
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Create admin helper components**

`frontend/src/components/admin/StatCard.tsx`:

```tsx
export default function StatCard({ label, value }: { label: string; value: number | string }) {
  return (
    <div className="rounded border border-hair bg-card p-6">
      <p className="mb-2 text-xs text-ink-faint">{label}</p>
      <p className="font-serif text-3xl font-normal text-ink">{value}</p>
    </div>
  )
}
```

`frontend/src/components/admin/FormField.tsx`:

```tsx
import type { ReactNode } from 'react'

export default function FormField({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <div>
      <label className="mb-1.5 block text-[13px] font-medium text-ink-muted">{label}</label>
      {children}
    </div>
  )
}
```

`frontend/src/components/admin/ActionBar.tsx`:

```tsx
import type { ReactNode } from 'react'

export default function ActionBar({
  title,
  action,
}: {
  title: string
  action?: ReactNode
}) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <h2 className="font-serif text-[1.75rem] font-normal text-ink">{title}</h2>
      {action}
    </div>
  )
}
```

`frontend/src/components/admin/EntityTable.tsx`:

```tsx
import type { ReactNode } from 'react'

export default function EntityTable({ children }: { children: ReactNode }) {
  return (
    <div className="overflow-hidden rounded border border-hair">
      <div className="flex flex-col">{children}</div>
    </div>
  )
}
```

- [ ] **Step 3: Create the reusable AuthorForm (used for create AND edit)**

`frontend/src/components/admin/AuthorForm.tsx`:

```tsx
import { useState } from 'react'
import FormField from './FormField'
import type { Author, AuthorPayload } from '../../types/models'

export default function AuthorForm({
  author,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  author?: Author | null
  submitLabel: string
  onSubmit: (payload: AuthorPayload) => Promise<void>
  onCancel: () => void
}) {
  const [name, setName] = useState(author?.name ?? '')
  const [birthYear, setBirthYear] = useState(author?.birthYear != null ? String(author.birthYear) : '')
  const [deathYear, setDeathYear] = useState(author?.deathYear != null ? String(author.deathYear) : '')
  const [biography, setBiography] = useState(author?.biography ?? '')
  const [submitting, setSubmitting] = useState(false)

  const submit = async () => {
    setSubmitting(true)
    try {
      await onSubmit({
        name,
        birthYear: birthYear === '' ? null : Number(birthYear),
        deathYear: deathYear === '' ? null : Number(deathYear),
        biography: biography === '' ? null : biography,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mb-10 flex flex-col gap-5 rounded border border-hair bg-card p-6">
      <h3 className="font-serif text-lg text-ink">{author ? 'Editar Autor' : 'Novo Autor'}</h3>
      <FormField label="Nome">
        <input value={name} onChange={(e) => setName(e.target.value)}
          className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none focus:border-ink-muted" />
      </FormField>
      <div className="grid grid-cols-2 gap-5">
        <FormField label="Nascimento (opcional)">
          <input value={birthYear} onChange={(e) => setBirthYear(e.target.value)} type="number"
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
        <FormField label="Falecimento (opcional)">
          <input value={deathYear} onChange={(e) => setDeathYear(e.target.value)} type="number"
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
      </div>
      <FormField label="Biografia (opcional)">
        <textarea value={biography} onChange={(e) => setBiography(e.target.value)} rows={4}
          className="w-full resize-y rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
      </FormField>
      <div className="flex gap-3">
        <button onClick={submit} disabled={submitting}
          className="rounded bg-ink px-6 py-2.5 text-sm font-medium text-paper transition-opacity hover:opacity-80 disabled:opacity-50">
          {submitLabel}
        </button>
        {onCancel && (
          <button onClick={onCancel}
            className="rounded border border-hair px-6 py-2.5 text-sm text-ink-muted hover:text-ink">
            Cancelar
          </button>
        )}
      </div>
    </div>
  )
}
```

Note: the `AuthorPayload`, `CategoryPayload`, `TagPayload`, and `PhrasePayload`
interfaces live in `types/models.ts` (added in Task 12) so forms and services
share one definition. Import them from `../types/models` — never from a service module.

- [ ] **Step 4: Create the reusable CategoryForm and TagForm**

`frontend/src/components/admin/CategoryForm.tsx`:

```tsx
import { useState } from 'react'
import FormField from './FormField'
import type { Category, CategoryPayload } from '../../types/models'

export default function CategoryForm({
  category,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  category?: Category | null
  submitLabel: string
  onSubmit: (payload: CategoryPayload) => Promise<void>
  onCancel: () => void
}) {
  const [name, setName] = useState(category?.name ?? '')
  const [description, setDescription] = useState(category?.description ?? '')
  const [submitting, setSubmitting] = useState(false)

  const submit = async () => {
    setSubmitting(true)
    try {
      await onSubmit({
        name,
        description: description === '' ? null : description,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mb-10 flex flex-col gap-5 rounded border border-hair bg-card p-6">
      <h3 className="font-serif text-lg text-ink">{category ? 'Editar Categoria' : 'Nova Categoria'}</h3>
      <FormField label="Nome">
        <input value={name} onChange={(e) => setName(e.target.value)}
          className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none focus:border-ink-muted" />
      </FormField>
      <FormField label="Descrição (opcional)">
        <input value={description} onChange={(e) => setDescription(e.target.value)}
          className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
      </FormField>
      <div className="flex gap-3">
        <button onClick={submit} disabled={submitting}
          className="rounded bg-ink px-6 py-2.5 text-sm font-medium text-paper transition-opacity hover:opacity-80 disabled:opacity-50">
          {submitLabel}
        </button>
        {onCancel && (
          <button onClick={onCancel}
            className="rounded border border-hair px-6 py-2.5 text-sm text-ink-muted hover:text-ink">
            Cancelar
          </button>
        )}
      </div>
    </div>
  )
}
```

`frontend/src/components/admin/TagForm.tsx`:

```tsx
import { useState } from 'react'
import FormField from './FormField'
import type { Tag, TagPayload } from '../../types/models'

export default function TagForm({
  tag,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  tag?: Tag | null
  submitLabel: string
  onSubmit: (payload: TagPayload) => Promise<void>
  onCancel: () => void
}) {
  const [name, setName] = useState(tag?.name ?? '')
  const [submitting, setSubmitting] = useState(false)

  const submit = async () => {
    setSubmitting(true)
    try {
      await onSubmit({ name })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mb-10 flex flex-col gap-5 rounded border border-hair bg-card p-6">
      <h3 className="font-serif text-lg text-ink">{tag ? 'Editar Tag' : 'Nova Tag'}</h3>
      <FormField label="Nome">
        <input value={name} onChange={(e) => setName(e.target.value)}
          className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none focus:border-ink-muted" />
      </FormField>
      <div className="flex gap-3">
        <button onClick={submit} disabled={submitting}
          className="rounded bg-ink px-6 py-2.5 text-sm font-medium text-paper transition-opacity hover:opacity-80 disabled:opacity-50">
          {submitLabel}
        </button>
        {onCancel && (
          <button onClick={onCancel}
            className="rounded border border-hair px-6 py-2.5 text-sm text-ink-muted hover:text-ink">
            Cancelar
          </button>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 5: Create the reusable PhraseForm**

`frontend/src/components/admin/PhraseForm.tsx`:

```tsx
import { useState } from 'react'
import FormField from './FormField'
import type { Author, Category, Phrase, PhrasePayload, Tag } from '../../types/models'

export default function PhraseForm({
  phrase,
  authors,
  categories,
  tags,
  submitLabel,
  onSubmit,
  onCancel,
}: {
  phrase?: Phrase | null
  authors: Author[]
  categories: Category[]
  tags: Tag[]
  submitLabel: string
  onSubmit: (payload: PhrasePayload) => Promise<void>
  onCancel: () => void
}) {
  const [content, setContent] = useState(phrase?.content ?? '')
  const [authorId, setAuthorId] = useState(phrase ? String(phrase.author.id) : '')
  const [year, setYear] = useState(phrase?.year != null ? String(phrase.year) : '')
  const [language, setLanguage] = useState(phrase?.language ?? 'pt')
  const [source, setSource] = useState(phrase?.source ?? '')
  const [categoryIds, setCategoryIds] = useState<number[]>(
    phrase ? phrase.categories.map((c) => c.id) : [],
  )
  const [tagIds, setTagIds] = useState<number[]>(
    phrase ? phrase.tags.map((t) => t.id) : [],
  )
  const [submitting, setSubmitting] = useState(false)

  const toggle = (list: number[], value: number): number[] =>
    list.includes(value) ? list.filter((v) => v !== value) : [...list, value]

  const submit = async () => {
    setSubmitting(true)
    try {
      await onSubmit({
        content,
        authorId: Number(authorId),
        year: year === '' ? null : Number(year),
        language,
        source: source === '' ? null : source,
        categoryIds,
        tagIds,
      })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mb-10 flex flex-col gap-5 rounded border border-hair bg-card p-6">
      <h3 className="font-serif text-lg text-ink">{phrase ? 'Editar Frase' : 'Nova Frase'}</h3>
      <FormField label="Conteúdo">
        <textarea value={content} onChange={(e) => setContent(e.target.value)} rows={4}
          className="w-full resize-y rounded border border-hair bg-card px-3 py-2.5 font-serif italic leading-[1.6] text-ink outline-none focus:border-ink-muted" />
      </FormField>
      <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
        <FormField label="Autor">
          <select value={authorId} onChange={(e) => setAuthorId(e.target.value)}
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none">
            <option value="">Selecione…</option>
            {authors.map((a) => (
              <option key={a.id} value={a.id}>{a.name}</option>
            ))}
          </select>
        </FormField>
        <FormField label="Idioma">
          <input value={language} onChange={(e) => setLanguage(e.target.value)}
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
        <FormField label="Ano (opcional)">
          <input value={year} onChange={(e) => setYear(e.target.value)} type="number"
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
        <FormField label="Fonte (opcional)">
          <input value={source} onChange={(e) => setSource(e.target.value)}
            className="w-full rounded border border-hair bg-card px-3 py-2.5 text-sm text-ink outline-none" />
        </FormField>
      </div>
      <FormField label="Categorias">
        <div className="flex flex-wrap gap-2">
          {categories.map((c) => (
            <button key={c.id} type="button"
              onClick={() => setCategoryIds((prev) => toggle(prev, c.id))}
              className={`rounded-[99px] border px-3 py-1 text-xs transition-colors ${
                categoryIds.includes(c.id) ? 'border-ink bg-ink text-paper' : 'border-hair text-ink-muted hover:text-ink'
              }`}>
              {c.name}
            </button>
          ))}
        </div>
      </FormField>
      <FormField label="Tags">
        <div className="flex flex-wrap gap-2">
          {tags.map((t) => (
            <button key={t.id} type="button"
              onClick={() => setTagIds((prev) => toggle(prev, t.id))}
              className={`rounded-[99px] border px-3 py-1 text-xs transition-colors ${
                tagIds.includes(t.id) ? 'border-ink bg-ink text-paper' : 'border-hair text-ink-muted hover:text-ink'
              }`}>
              {t.name}
            </button>
          ))}
        </div>
      </FormField>
      <div className="flex gap-3">
        <button onClick={submit} disabled={submitting}
          className="rounded bg-ink px-6 py-2.5 text-sm font-medium text-paper transition-opacity hover:opacity-80 disabled:opacity-50">
          {submitLabel}
        </button>
        {onCancel && (
          <button onClick={onCancel}
            className="rounded border border-hair px-6 py-2.5 text-sm text-ink-muted hover:text-ink">
            Cancelar
          </button>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 6: Create the Dashboard**

`frontend/src/pages/admin/Dashboard.tsx`:

```tsx
import { Link } from 'react-router-dom'
import { usePhrases } from '../../hooks/usePhrases'
import { useAuthors } from '../../hooks/useAuthors'
import { useCategories } from '../../hooks/useCategories'
import { useTags } from '../../hooks/useTags'
import StatCard from '../../components/admin/StatCard'
import Loading from '../../components/Loading'

export default function Dashboard() {
  const phrases = usePhrases({ page: 0, size: 1 })
  const authors = useAuthors(0, 1)
  const categories = useCategories(0, 1)
  const tags = useTags(0, 1)
  const recent = usePhrases({ page: 0, size: 5 })

  return (
    <div>
      <h2 className="mb-8 font-serif text-[1.75rem] font-normal text-ink">Painel</h2>

      <div className="mb-10 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Total de Frases" value={phrases.data?.totalElements ?? '—'} />
        <StatCard label="Autores" value={authors.data?.totalElements ?? '—'} />
        <StatCard label="Categorias" value={categories.data?.totalElements ?? '—'} />
        <StatCard label="Tags" value={tags.data?.totalElements ?? '—'} />
      </div>

      <div className="overflow-hidden rounded border border-hair">
        <div className="flex items-center justify-between border-b border-hair-subtle px-5 py-4">
          <p className="text-sm font-medium text-ink">Frases Recentes</p>
          <Link to="/admin/frases" className="text-[13px] text-ink-muted hover:text-ink">
            Ver todas →
          </Link>
        </div>
        {recent.isLoading && <Loading />}
        {recent.data?.content.map((p, i) => (
          <div key={p.id}
            className={`flex justify-between gap-4 px-5 py-3.5 ${i < (recent.data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <p className="flex-1 truncate text-sm text-ink">“{p.content}”</p>
            <p className="shrink-0 text-[13px] text-ink-faint">{p.author.name}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 7: Create AdminPhrases (create / list+view / update / delete)**

`frontend/src/pages/admin/AdminPhrases.tsx` — a single `PhraseForm` handles create (no `phrase`) and edit (with `phrase`); rows link to the public detail (read/view):

```tsx
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCreatePhrase, useDeletePhrase, usePhrases, useUpdatePhrase } from '../../hooks/usePhrases'
import { useAuthors } from '../../hooks/useAuthors'
import { useCategories } from '../../hooks/useCategories'
import { useTags } from '../../hooks/useTags'
import ActionBar from '../../components/admin/ActionBar'
import EntityTable from '../../components/admin/EntityTable'
import PhraseForm from '../../components/admin/PhraseForm'
import Loading from '../../components/Loading'
import type { Phrase, PhrasePayload } from '../../types/models'

export default function AdminPhrases() {
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState<Phrase | null>(null)
  const [message, setMessage] = useState('')

  const { data, isLoading } = usePhrases({ page, size: 10 })
  const createPhrase = useCreatePhrase()
  const deletePhrase = useDeletePhrase()
  const updatePhrase = useUpdatePhrase(editing?.id ?? 0)

  const authors = useAuthors(0, 100)
  const categories = useCategories(0, 100)
  const tags = useTags(0, 100)

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const handleSubmit = async (payload: PhrasePayload) => {
    try {
      if (editing) {
        await updatePhrase.mutateAsync(payload)
        flash('Frase atualizada com sucesso.')
      } else {
        await createPhrase.mutateAsync(payload)
        flash('Frase salva com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  const handleDelete = async (id: number) => {
    if (window.confirm('Excluir esta frase?')) {
      await deletePhrase.mutateAsync(id)
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Frases"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Nova Frase
          </button>
        }
      />

      {message && (
        <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>
      )}

      <PhraseForm
        key={editing?.id ?? 'new'}
        phrase={editing}
        authors={authors.data?.content ?? []}
        categories={categories.data?.content ?? []}
        tags={tags.data?.content ?? []}
        submitLabel={editing ? 'Atualizar Frase' : 'Salvar Frase'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <EntityTable>
        {data?.content.map((p, i) => (
          <div key={p.id}
            className={`flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-card ${i < (data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm text-ink">“{p.content}”</p>
              <p className="text-[13px] text-ink-faint">{p.author.name} · {p.language.toUpperCase()}</p>
            </div>
            <div className="flex shrink-0 gap-3 text-xs">
              <Link to={`/frases/${p.id}`} className="text-ink-muted hover:text-ink">Ver</Link>
              <button onClick={() => setEditing(p)} className="text-ink-muted hover:text-ink">Editar</button>
              <button onClick={() => handleDelete(p.id)} className="text-ink-faint hover:text-ink">Excluir</button>
            </div>
          </div>
        ))}
      </EntityTable>

      {data && data.totalPages > 1 && (
        <div className="mt-6 flex justify-center gap-4 text-[13px]">
          <button disabled={page === 0} onClick={() => setPage(page - 1)} className="text-ink-muted disabled:opacity-40">← Anterior</button>
          <span className="text-ink-faint">{page + 1} / {data.totalPages}</span>
          <button disabled={page >= data.totalPages - 1} onClick={() => setPage(page + 1)} className="text-ink-muted disabled:opacity-40">Próxima →</button>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 8: Create AdminAuthors (full CRUD)**

`frontend/src/pages/admin/AdminAuthors.tsx`:

```tsx
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuthors, useCreateAuthor, useDeleteAuthor, useUpdateAuthor } from '../../hooks/useAuthors'
import ActionBar from '../../components/admin/ActionBar'
import EntityTable from '../../components/admin/EntityTable'
import AuthorForm from '../../components/admin/AuthorForm'
import Loading from '../../components/Loading'
import type { Author, AuthorPayload } from '../../types/models'

export default function AdminAuthors() {
  const { data, isLoading } = useAuthors(0, 100)
  const createAuthor = useCreateAuthor()
  const deleteAuthor = useDeleteAuthor()

  const [editing, setEditing] = useState<Author | null>(null)
  const [message, setMessage] = useState('')

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const updateAuthor = useUpdateAuthor(editing?.id ?? 0)

  const handleSubmit = async (payload: AuthorPayload) => {
    try {
      if (editing) {
        await updateAuthor.mutateAsync(payload)
        flash('Autor atualizado com sucesso.')
      } else {
        await createAuthor.mutateAsync(payload)
        flash('Autor salvo com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Autores"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Novo Autor
          </button>
        }
      />

      {message && <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>}

      <AuthorForm
        key={editing?.id ?? 'new'}
        author={editing}
        submitLabel={editing ? 'Atualizar Autor' : 'Salvar Autor'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <EntityTable>
        {data?.content.map((a, i) => (
          <div key={a.id}
            className={`flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-card ${i < (data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <div>
              <p className="text-[15px] font-medium text-ink">{a.name}</p>
              <p className="text-[13px] text-ink-faint">{a.phraseCount} {a.phraseCount === 1 ? 'frase' : 'frases'}</p>
            </div>
            <div className="flex shrink-0 gap-3 text-xs">
              <Link to={`/autores/${a.id}`} className="text-ink-muted hover:text-ink">Ver</Link>
              <button onClick={() => setEditing(a)} className="text-ink-muted hover:text-ink">Editar</button>
              <button onClick={() => { if (window.confirm('Excluir este autor?')) deleteAuthor.mutate(a.id) }} className="text-ink-faint hover:text-ink">Excluir</button>
            </div>
          </div>
        ))}
      </EntityTable>
    </div>
  )
}
```

- [ ] **Step 9: Create AdminCategories (full CRUD)**

`frontend/src/pages/admin/AdminCategories.tsx`:

```tsx
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useCategories, useCreateCategory, useDeleteCategory, useUpdateCategory } from '../../hooks/useCategories'
import ActionBar from '../../components/admin/ActionBar'
import EntityTable from '../../components/admin/EntityTable'
import CategoryForm from '../../components/admin/CategoryForm'
import Loading from '../../components/Loading'
import type { Category, CategoryPayload } from '../../types/models'

export default function AdminCategories() {
  const { data, isLoading } = useCategories(0, 100)
  const createCategory = useCreateCategory()
  const deleteCategory = useDeleteCategory()

  const [editing, setEditing] = useState<Category | null>(null)
  const [message, setMessage] = useState('')

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const updateCategory = useUpdateCategory(editing?.id ?? 0)

  const handleSubmit = async (payload: CategoryPayload) => {
    try {
      if (editing) {
        await updateCategory.mutateAsync(payload)
        flash('Categoria atualizada com sucesso.')
      } else {
        await createCategory.mutateAsync(payload)
        flash('Categoria salva com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Categorias"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Nova Categoria
          </button>
        }
      />

      {message && <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>}

      <CategoryForm
        key={editing?.id ?? 'new'}
        category={editing}
        submitLabel={editing ? 'Atualizar Categoria' : 'Salvar Categoria'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <EntityTable>
        {data?.content.map((c, i) => (
          <div key={c.id}
            className={`flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-card ${i < (data?.content.length ?? 1) - 1 ? 'border-b border-hair-subtle' : ''}`}>
            <div>
              <p className="text-[15px] text-ink">{c.name}</p>
              <p className="text-[13px] text-ink-faint">{c.phraseCount} {c.phraseCount === 1 ? 'frase' : 'frases'}</p>
            </div>
            <div className="flex shrink-0 gap-3 text-xs">
              <Link to={`/categorias/${c.id}`} className="text-ink-muted hover:text-ink">Ver</Link>
              <button onClick={() => setEditing(c)} className="text-ink-muted hover:text-ink">Editar</button>
              <button onClick={() => { if (window.confirm('Excluir esta categoria?')) deleteCategory.mutate(c.id) }} className="text-ink-faint hover:text-ink">Excluir</button>
            </div>
          </div>
        ))}
      </EntityTable>
    </div>
  )
}
```

- [ ] **Step 10: Create AdminTags (full CRUD)**

`frontend/src/pages/admin/AdminTags.tsx`:

```tsx
import { useState } from 'react'
import { useTags, useCreateTag, useDeleteTag, useUpdateTag } from '../../hooks/useTags'
import ActionBar from '../../components/admin/ActionBar'
import TagForm from '../../components/admin/TagForm'
import Loading from '../../components/Loading'
import type { Tag, TagPayload } from '../../types/models'

export default function AdminTags() {
  const { data, isLoading } = useTags(0, 100)
  const createTag = useCreateTag()
  const deleteTag = useDeleteTag()

  const [editing, setEditing] = useState<Tag | null>(null)
  const [message, setMessage] = useState('')

  const flash = (msg: string) => {
    setMessage(msg)
    window.setTimeout(() => setMessage(''), 3000)
  }

  const updateTag = useUpdateTag(editing?.id ?? 0)

  const handleSubmit = async (payload: TagPayload) => {
    try {
      if (editing) {
        await updateTag.mutateAsync(payload)
        flash('Tag atualizada com sucesso.')
      } else {
        await createTag.mutateAsync(payload)
        flash('Tag salva com sucesso.')
      }
      setEditing(null)
    } catch (err) {
      flash(err instanceof Error ? err.message : 'Erro ao salvar.')
    }
  }

  return (
    <div className="max-w-[900px]">
      <ActionBar
        title="Tags"
        action={
          <button onClick={() => setEditing(null)}
            className="rounded bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-80">
            + Nova Tag
          </button>
        }
      />

      {message && <div className="mb-6 rounded border border-hair bg-card px-4 py-3 text-sm text-ink-muted">{message}</div>}

      <TagForm
        key={editing?.id ?? 'new'}
        tag={editing}
        submitLabel={editing ? 'Atualizar Tag' : 'Salvar Tag'}
        onSubmit={handleSubmit}
        onCancel={() => setEditing(null)}
      />

      {isLoading && <Loading />}

      <div className="flex flex-wrap gap-2">
        {data?.content.map((t) => (
          <div key={t.id} className="flex items-center gap-2 rounded-[99px] border border-hair px-3 py-1.5">
            <button onClick={() => setEditing(t)} className="text-[13px] text-ink-muted hover:text-ink">{t.name}</button>
            <button onClick={() => { if (window.confirm('Excluir esta tag?')) deleteTag.mutate(t.id) }}
              className="text-xs text-ink-faint hover:text-ink">×</button>
          </div>
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 11: Register admin routes**

`frontend/src/App.tsx` — full router:

```tsx
import { createBrowserRouter } from 'react-router-dom'
import PublicLayout from './layouts/PublicLayout'
import AdminLayout from './layouts/AdminLayout'
import Home from './pages/Home'
import Explore from './pages/Explore'
import QuoteDetail from './pages/QuoteDetail'
import Authors from './pages/Authors'
import AuthorDetail from './pages/AuthorDetail'
import Categories from './pages/Categories'
import CategoryDetail from './pages/CategoryDetail'
import Dashboard from './pages/admin/Dashboard'
import AdminPhrases from './pages/admin/AdminPhrases'
import AdminAuthors from './pages/admin/AdminAuthors'
import AdminCategories from './pages/admin/AdminCategories'
import AdminTags from './pages/admin/AdminTags'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <PublicLayout />,
    children: [
      { index: true, element: <Home /> },
      { path: 'explore', element: <Explore /> },
      { path: 'frases/:id', element: <QuoteDetail /> },
      { path: 'autores', element: <Authors /> },
      { path: 'autores/:id', element: <AuthorDetail /> },
      { path: 'categorias', element: <Categories /> },
      { path: 'categorias/:id', element: <CategoryDetail /> },
    ],
  },
  {
    path: '/admin',
    element: <AdminLayout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'frases', element: <AdminPhrases /> },
      { path: 'autores', element: <AdminAuthors /> },
      { path: 'categorias', element: <AdminCategories /> },
      { path: 'tags', element: <AdminTags /> },
    ],
  },
])
```

- [ ] **Step 12: Verify build + lint, then commit**

Run: `cd frontend && npm run build && npm run lint`
Expected: pass.

```bash
git add frontend/src/layouts/AdminLayout.tsx frontend/src/components/admin/ frontend/src/pages/admin/ frontend/src/App.tsx frontend/src/types/models.ts frontend/src/services/
git commit -m "feat(frontend): add full CRUD admin area with reusable forms"
```

### Educational summary

- **Frontend/React concepts:** full CRUD with a single reusable form per resource (create vs edit driven by an `editing` state); Rules of Hooks respected by binding update hooks at the top level.
- **Architectural decision:** create and edit share one form; read/view links to the public detail pages; delete confirms inline.
- **Study before proceeding:** controlled form state, mutation error handling, `key` remount to reset forms.

## Task 22: Docker Compose full stack + backend Dockerfile

**Files:**
- Modify: `docker-compose.yml` (add backend service)
- Create: `backend/phraseforge-api/Dockerfile`
- Create: `backend/phraseforge-api/.dockerignore`

**Interfaces:**
- Produces: two-service compose (db + backend), backend waits for healthy MySQL via `depends_on: condition: service_healthy`. No sleep hacks.

- [ ] **Step 1: Create the backend Dockerfile**

`backend/phraseforge-api/Dockerfile` — multi-stage (build with Maven image, run on Temurin JRE):

```dockerfile
# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Create .dockerignore**

`backend/phraseforge-api/.dockerignore`:

```
target/
.mvn/
*.log
```

- [ ] **Step 3: Extend docker-compose.yml**

Replace `docker-compose.yml` with the full two-service definition:

```yaml
services:
  db:
    image: mysql:8.4
    container_name: phraseforge-db
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: ${DB_NAME:-phraseforge}
      MYSQL_USER: ${DB_USERNAME:-phraseforge}
      MYSQL_PASSWORD: ${DB_PASSWORD:-phraseforge}
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-phraseforge-root}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-u", "root", "-p$${MYSQL_ROOT_PASSWORD}"]
      interval: 5s
      timeout: 5s
      retries: 20
      start_period: 30s

  backend:
    build: ./backend/phraseforge-api
    container_name: phraseforge-backend
    restart: unless-stopped
    environment:
      DB_HOST: db
      DB_PORT: 3306
      DB_NAME: ${DB_NAME:-phraseforge}
      DB_USERNAME: ${DB_USERNAME:-phraseforge}
      DB_PASSWORD: ${DB_PASSWORD:-phraseforge}
      CORS_ALLOWED_ORIGINS: ${CORS_ALLOWED_ORIGINS:-http://localhost:5173}
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy

volumes:
  mysql_data:
```

- [ ] **Step 4: Start the full stack and verify**

Run: `docker compose up -d --build`
Wait for both services. Verify:

```bash
docker compose ps
curl -s http://localhost:8080/api/v1/phrases/random
curl -s http://localhost:8080/api/v1/phrases?page=0&size=3
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/swagger-ui.html
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health
docker compose logs backend | grep -i flyway
```

Expected: `docker compose ps` shows `db (healthy)` and `backend (running)`; API returns seed data; swagger 200; health UP; Flyway logs show the 7 migrations applied (including V7 seed).

- [ ] **Step 5: Run backend tests once more in container-free mode**

Run: `cd backend/phraseforge-api && ./mvnw test`
Expected: green (H2-based tests unaffected by Docker).

- [ ] **Step 6: Commit**

```bash
git add docker-compose.yml backend/phraseforge-api/Dockerfile backend/phraseforge-api/.dockerignore
git commit -m "feat(infra): add backend Dockerfile and full docker-compose with MySQL healthcheck"
```

### Educational summary

- **Java/Spring/Docker concepts:** multi-stage Dockerfile (Maven build → JRE runtime); compose `depends_on: condition: service_healthy` removes the need for `sleep`; Flyway applies V1–V7 on MySQL.
- **Architectural decision:** the healthcheck gate is the only ordering mechanism.
- **Study before proceeding:** Docker multi-stage builds, healthcheck semantics.

---

## Task 23: README and documentation

**Files:**
- Create: `/home/jovi/Documentos/ws/phraseforge-java/README.md`

**Interfaces:** —

- [ ] **Step 1: Write the README**

`README.md` — document objective, stack, architecture, how to run (db/backend/frontend), env vars, API docs, project structure, prototype reference, roadmap, and the year-model limitation:

```markdown
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

As migrations Flyway (V1–V7) criam o schema e inserem dados de demonstração.
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
defina `VITE_API_URL`.

### Tudo de uma vez (Docker)

```bash
docker compose up -d --build
```

Backend disponível em `http://localhost:8080`, banco em `localhost:3306`.

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
| `VITE_API_URL` | Base URL da API para o frontend | `/api/v1` |

Nunca commite credenciais reais.

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
desenvolvimento usam MySQL.

## Roadmap

**MVP** — Catálogo de frases · Autores · Categorias · Tags · Pesquisa ·
Filtros · Frase aleatória · CRUD administrativo · API REST

**V1** — Autenticação · JWT · Usuários · Roles · Favoritos

**V2** — Ranking · Frase do dia · Recomendações · Histórico · Sugestões ·
Recursos avançados da API
```

- [ ] **Step 2: Verify nothing sensitive is tracked**

Run: `cd /home/jovi/Documentos/ws/phraseforge-java && git status --porcelain && git ls-files | grep -E '\.env$|secrets' || echo "no env files tracked"`

Expected: `.env` not in `git ls-files`; only `.env.example` present.

- [ ] **Step 3: Final full check — backend tests, frontend build/lint, compose up**

Run:

```bash
cd backend/phraseforge-api && ./mvnw test
cd ../.. && docker compose up -d --build
curl -s http://localhost:8080/api/v1/phrases/random
cd frontend && npm run build && npm run lint
```

Expected: all green. API returns seed data through Docker.

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "docs: add project README with run instructions and roadmap"
```

---

### Educational summary

- **Documentation concepts:** README covers run instructions, env vars, API surface, architecture, and the deliberate year-model limitation.
- **Architectural decision:** the prototype is documented as the visual source of truth; schema vs prototype conflicts are resolved in favor of the schema with explicit notes.
- **Study before proceeding:** nothing — this closes the MVP loop.

## Self-Review Notes

- **Spec coverage:** every spec section maps to a task — migrations (Task 2), entities/repos (Tasks 3–8), services/DTOs/validation/exceptions (Tasks 5–10), controllers (Tasks 5/6/7/9), tests (Tasks 4–10), OpenAPI (Task 10), frontend structure (12–15), pages (16–20), admin (21), docker (11/22), README (23).
- **Flyway sequence:** V1–V6 schema + V7 seed, exactly as the user required (no arbitrary V8).
- **H2 test isolation:** tests run V1–V6 only (`spring.flyway.target=6` in test properties); V7 seed excluded from the H2 context. Repository/integration tests build their own fixtures with neutral names and never depend on seeded data. MySQL runtime/dev applies V1–V7.
- **Admin CRUD (correction applied):** full CRUD for Phrases, Authors, Categories, Tags — create, read/view (links to public detail pages), update (reusable form with `editing` state), and delete. One form component per resource reused for create and edit; payload types shared from `types/models.ts`.
- **Phrase duplicate on update (correction applied):** `PhraseService.update` uses the derived `existsByContentAndAuthor_IdAndIdNot(content, authorId, id)` repository query — no in-memory filtering of all phrases.
- **Phrase filter coverage (correction applied):** `PhraseSpecificationsTest` covers `query`, `authorId`, `categoryId`, `tagId`, `language`, combinations, and asserts pagination returns no duplicate phrases when filtering by category/tag (multi-join fixture).
- **Year model:** SMALLINT numeric; `phrases.year` is the year associated with the phrase/source, not necessarily when it was spoken/written; uncertain dates are NULL; BCE/CE not modeled. Documented in README + spec.
- **Type consistency:** `PagedResponse<T>` used everywhere; `Paged<T>` mirrors it on the frontend; `PhraseSummaryResponse` shape == `PhraseSummary` alias; `AuthorRef/CategoryRef/TagRef` identical names across TS/Java.
- **Educational workflow:** each of the 23 tasks ends with an `### Educational summary` block (implemented, concepts, decisions, tests, study topics); the Global Constraints mandate the summary after every completed task.
- **Known deliberate simplifications:** `PhraseSummary` aliased to `Phrase`; Home random re-fetch via `removeQueries` + `refetch`; author search client-side over the loaded page (server-side search is a V2 option).
