# PhraseForge — Authentication and Favorites Design

**Status:** Proposed for V1

**Date:** 2026-08-17

**Base:** stabilized MVP on `fix/mvp-stabilization`

## 1. Objective

V1 adds user accounts, JWT authentication, role-based authorization, and personal
favorites without changing the public catalog behavior delivered by the MVP.
Administrative writes, which are open in the MVP, become restricted to
administrators.

The product label "V1" is independent from both the existing `/api/v1` URL and
Flyway migration numbers. The API remains under `/api/v1`; new database migrations
continue after the existing `V7__seed_data.sql`.

## 2. Scope

### Included

- Public registration and login.
- Short-lived JWT access tokens.
- Rotating, revocable refresh sessions.
- Logout from the current session.
- Current-user profile (`/users/me`).
- Two roles: `USER` and `ADMIN`.
- Backend enforcement of administrative permissions.
- Add, remove, and list the authenticated user's favorite phrases.
- Favorite state on phrase cards and phrase detail.
- Login, registration, authenticated navigation, and favorites pages.
- Route guards for the administrative frontend.
- A documented, one-time first-administrator bootstrap mechanism.
- Security, service, repository, controller, and frontend behavior tests.

### Not included

- Social login, OAuth client login, passkeys, or multi-factor authentication.
- Password reset or email verification.
- Multiple roles per user or custom permission management.
- Administrative user-management screens.
- Public user profiles, follows, sharing, or favorite counts.
- Ranking, recommendations, history, and quote-of-the-day behavior (V2).
- Access-token denylisting. Short access-token lifetime limits the logout window;
  refresh sessions are revoked immediately.

## 3. Actors and permissions

| Capability | Visitor | `USER` | `ADMIN` |
| --- | --- | --- | --- |
| Read phrases, authors, categories, and tags | Yes | Yes | Yes |
| Register and log in | Yes | Yes | Yes |
| Read own profile | No | Yes | Yes |
| Add, remove, and list own favorites | No | Yes | Yes |
| Create, update, and delete catalog data | No | No | Yes |
| Open `/admin` | No | No | Yes |

Every new account receives `USER`. The role is a single string-backed enum on the
user record. A roles table and multiple-role assignments are deliberately deferred.
Backend authorization is authoritative; hiding frontend controls is only a user
experience measure.

## 4. Authentication model

### Access token

- Signed JWT sent as `Authorization: Bearer <token>`.
- Default lifetime: 15 minutes, configurable by environment.
- Required claims: `sub` (user ID), `role`, `iss`, `iat`, `exp`, and `jti`.
- The signing secret is supplied only through environment/configuration and must
  contain at least 256 bits of entropy.
- The frontend keeps the access token in memory, never in `localStorage` or
  `sessionStorage`.

### Refresh session

- The refresh credential is an opaque, cryptographically random value, not a JWT.
- Only its SHA-256 hash is stored in the database.
- It is delivered through an `HttpOnly` cookie with `SameSite=Strict`, an auth-only
  path, and `Secure=true` outside local development.
- Default lifetime: 30 days, configurable by environment.
- Every successful refresh revokes the previous token and issues a replacement in
  the same token family.
- Reuse of an already replaced token revokes the entire family.
- Logout revokes the current refresh session and expires the cookie. Already issued
  access tokens remain usable only until their short expiration.

Because catalog mutations use bearer tokens rather than ambient cookies, they are
not subject to cookie-based CSRF. Refresh and logout additionally require an exact
allowed `Origin`; the strict refresh cookie is never sent in a cross-site context.
CORS must use explicit origins and credentials rather than wildcards.

### Passwords and authentication errors

- Passwords are encoded through Spring Security's `PasswordEncoder`; plaintext is
  never logged or persisted.
- Initial policy: at least 12 characters and at most 72 UTF-8 bytes (the BCrypt
  input limit).
- Email is trimmed and normalized to lowercase before lookup and persistence.
- Login always returns the same generic error for an unknown email, wrong password,
  disabled user, or invalid credentials.
- Login and registration receive basic per-client throttling. A distributed rate
  limiter is an infrastructure concern beyond this single-instance V1.

## 5. Data model and migrations

### `V8__create_users.sql`

`users`

- `id BIGINT` primary key, auto-increment.
- `email VARCHAR(254)` required and unique after normalization.
- `password_hash VARCHAR(255)` required.
- `display_name VARCHAR(100)` required.
- `role VARCHAR(20)` required (`USER` or `ADMIN`).
- `enabled BOOLEAN` required, default true.
- `created_at` and `updated_at` required.

### `V9__create_refresh_tokens.sql`

`refresh_tokens`

- `id BIGINT` primary key, auto-increment.
- `user_id BIGINT` required, foreign key to `users`.
- `token_hash CHAR(64)` required and unique.
- `family_id CHAR(36)` required and indexed.
- `expires_at`, `created_at`, and nullable `revoked_at`.
- Nullable `replaced_by_id`, self-referencing the replacement token.
- Indexes on `user_id`, `family_id`, and `expires_at`.

### `V10__create_favorites.sql`

`favorites`

- `id BIGINT` primary key, auto-increment.
- `user_id BIGINT` required, foreign key to `users`.
- `phrase_id BIGINT` required, foreign key to `phrases`.
- `created_at` required.
- Unique constraint on `(user_id, phrase_id)`.
- Index on `(user_id, created_at)` for the paginated favorites page.
- Deleting a user deletes their refresh sessions and favorites. Deleting a phrase
  deletes its favorite links. Application services remain responsible for normal
  catalog deletion flows.

No migration already committed or applied is renamed or edited.

## 6. API contracts

Every path below is relative to the existing `/api/v1` base path.

### Authentication

| Method | Path | Authentication | Result |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | Public | `201`, user and access token; sets refresh cookie |
| `POST` | `/auth/login` | Public | `200`, user and access token; sets refresh cookie |
| `POST` | `/auth/refresh` | Refresh cookie | `200`, rotates session and returns access token |
| `POST` | `/auth/logout` | Refresh cookie | `204`, revokes session and clears cookie |
| `GET` | `/users/me` | Bearer token | `200`, current user |

Registration accepts `email`, `displayName`, and `password`. Authentication responses
return `accessToken`, `expiresIn`, and a safe user representation. Password hashes,
refresh tokens, and internal security state are never serialized.

### Favorites

| Method | Path | Result |
| --- | --- | --- |
| `GET` | `/users/me/favorites?page=&size=` | Paginated phrase summaries |
| `PUT` | `/users/me/favorites/{phraseId}` | `204`; idempotently adds the phrase |
| `DELETE` | `/users/me/favorites/{phraseId}` | `204`; idempotently removes the phrase |

Phrase summary and detail responses gain `favorited: boolean`. It is `false` for an
anonymous request and is calculated in one batch for the IDs in an authenticated
page, avoiding one favorite query per phrase.

### Catalog authorization

- Existing catalog `GET` endpoints remain public.
- Existing catalog `POST`, `PUT`, and `DELETE` endpoints require `ADMIN`.
- `/actuator/health` remains public. Other actuator endpoints are not exposed.
- OpenAPI endpoints remain public in local development; production exposure is
  controlled by configuration.
- Authentication failures use `401`; authenticated users without permission use
  `403`; duplicate email uses `409`; invalid input uses `400`.
- API errors gain a stable machine-readable `code` while retaining `status`,
  `message`, and `timestamp`.

## 7. Frontend behavior

- `/login` and `/cadastro` are public routes.
- `/favoritos` requires authentication and preserves the intended destination when
  redirecting a visitor to login.
- `/admin/**` requires `ADMIN`; a `USER` is sent to a forbidden page or the home
  page with a clear message.
- Application startup attempts one refresh request to restore a session.
- The API client attaches the in-memory bearer token and performs at most one
  coordinated refresh/retry after a `401`; concurrent failures share the same
  refresh operation.
- Failed refresh clears the session. It must not create a retry loop.
- The header shows login/register actions to visitors and profile, favorites, role-
  appropriate admin access, and logout to authenticated users.
- Phrase cards and detail show a favorite action. A visitor who selects it is sent
  to login and returned to the original page afterward.
- Optimistic favorite updates roll back and show an error if the API call fails.
- Loading, empty, unauthorized, forbidden, and expired-session states are explicit.

## 8. First administrator

The application supports optional `APP_BOOTSTRAP_ADMIN_EMAIL` and
`APP_BOOTSTRAP_ADMIN_PASSWORD` values. When both are present and no administrator
exists, startup creates one administrator using the regular password encoder.

The bootstrap is idempotent: it never changes an existing account or resets a
password. The values are never committed, printed, or included in API responses.
Production instructions require removing the bootstrap password from the runtime
environment after the first successful creation.

## 9. Testing strategy

The current test profile stops Flyway at version 6 to exclude seed data. That would
also exclude every V1 schema migration. V1 therefore removes the version-6 target,
applies the complete migration chain, and cleans seeded business rows before tests
that require an empty fixture database. Existing migrations remain covered.

Required coverage:

- Migration validation through V10 on H2 in MySQL mode.
- A targeted MySQL/Testcontainers integration test for constraints and migrations.
- Registration normalization, duplicate email, password encoding, and safe output.
- Login success and generic failure behavior.
- JWT validation for missing, malformed, expired, and incorrectly signed tokens.
- Refresh rotation, expiration, logout, and reuse-family revocation.
- `401` versus `403` behavior and the complete permission matrix.
- Favorite idempotency, ownership isolation, pagination, and deleted phrases.
- Existing public reads remain anonymous; every administrative mutation is denied
  to visitors and regular users.
- Frontend build/lint plus tests for session restore, single refresh retry, route
  guards, and optimistic favorite rollback.
- Docker smoke test through the frontend reverse proxy.

## 10. Configuration

New runtime configuration is environment-backed and documented without real values:

- `JWT_SECRET`
- `JWT_ISSUER` (default `phraseforge`)
- `JWT_ACCESS_TTL` (default `15m`)
- `REFRESH_TOKEN_TTL` (default `30d`)
- `COOKIE_SECURE` (default true outside local development)
- `APP_BOOTSTRAP_ADMIN_EMAIL` (optional)
- `APP_BOOTSTRAP_ADMIN_PASSWORD` (optional)

Startup fails clearly when a required signing secret is missing or too weak. Test
configuration uses an explicit test-only secret.

## 11. Acceptance criteria

V1 is complete when:

1. A visitor can register, log in, refresh a session, and log out without tokens
   being persisted in browser storage.
2. Public catalog reads continue to work without authentication.
3. Visitors receive `401` and regular users receive `403` for administrative
   mutations; administrators can complete the existing CRUD flows.
4. Users can add/remove favorites from cards and detail, and see only their own
   paginated favorites.
5. Refresh rotation and reuse detection are covered by automated tests.
6. The complete Flyway chain runs in tests and on MySQL without modifying migration
   history.
7. Backend tests, frontend build/lint/tests, Compose validation, and Docker smoke
   tests pass.
8. OpenAPI and README document authentication, permissions, environment variables,
   first-admin bootstrap, and favorites.

## 12. Execution sequence

1. Database migrations and test-database strategy.
2. User domain, password encoding, and first-admin bootstrap.
3. JWT access authentication and refresh-session lifecycle.
4. Route authorization and security error contracts.
5. Favorites domain and API.
6. Frontend session foundation and authentication screens.
7. Frontend authorization and favorites experience.
8. Security hardening, full regression, Docker smoke test, and documentation.

Each sequence item is independently validated and committed before the next begins.
