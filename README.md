# library-lending

A small Spring Boot service for lending library books. It models books and loans,
enforces the lending rules (copy availability, no duplicate active loan per member),
and exposes a paginated, role-protected REST API.

## Tech stack

| | |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.8 (Web MVC, Data JPA, Security, Validation) |
| Database | PostgreSQL, schema owned by Flyway |
| API docs | springdoc-openapi 3.1.0 (Swagger UI) |
| Build | Maven (wrapper committed) |
| Tests | JUnit 5, Mockito, Spring Security Test, Testcontainers |

## Getting started

Requirements: JDK 25 and Docker (for the database and for the test suite).

Start a database:

```bash
docker run --name library-db -p 5432:5432 \
  -e POSTGRES_DB=library -e POSTGRES_USER=library -e POSTGRES_PASSWORD=library \
  -d postgres:17-alpine
```

Then run the service:

```bash
./mvnw spring-boot:run
```

The datasource defaults to `jdbc:postgresql://localhost:5432/library` with user and
password `library`, and each part is overridable through `DATABASE_URL`,
`DATABASE_USERNAME` and `DATABASE_PASSWORD`.

The service starts on **port 8081**. Swagger UI is at
<http://localhost:8081/swagger-ui.html>, the OpenAPI document at
`/v3/api-docs`. Both sit behind authentication like every other path — see
[Security](#security).

Build and test:

```bash
./mvnw verify     # compile + run all tests
./mvnw test       # tests only
```

Docker must be running for `verify`: the persistence tests start a real PostgreSQL
container.

## Schema

Flyway owns the schema. Migrations live in `src/main/resources/db/migration` and are
append-only — a schema change is a new `V<n>__<description>.sql`, never an edit to a
file that has already run. Hibernate is set to `ddl-auto: validate` in every profile,
including tests, so a migration that drifts from the entities fails the build rather
than the deploy.

## Domain model

**Book** — `title`, `isbn`, `copies` (total number of physical copies). The ISBN is the
natural key and carries a database-level unique constraint.

**Loan** — links a book to a member, with a `dueDate` and a status:

| Status | Meaning |
|---|---|
| `ACTIVE` | Currently lent out |
| `RETURNED` | Given back |

### Lending rules

- **No copies available → `409 Conflict`.** A book is unavailable when its number of
  `ACTIVE` loans has reached `copies`. The book row is read under a pessimistic write
  lock, so two requests for the last copy cannot both succeed.
- **Duplicate active loan → `409 Conflict`.** The same member cannot hold two
  `ACTIVE` loans of the same book.
- **Duplicate ISBN → `409 Conflict`.** Guarded by a pre-check for the friendly message
  and by the unique constraint for the guarantee; a race that slips past the pre-check
  produces the same `409`.
- **Returning twice → `409 Conflict`.** A `RETURNED` loan cannot be returned again.
- **Unknown book or loan id → `404 Not Found`.**

## API

All endpoints require authentication. Write operations require the
`LIBRARIAN` role; reads are open to any authenticated user.

### Books — `/api/v1/books`

| Method | Path | Role | Success |
|---|---|---|---|
| `POST` | `/api/v1/books` | `LIBRARIAN` | `201 Created` |
| `GET` | `/api/v1/books/{id}` | authenticated | `200 OK` |
| `GET` | `/api/v1/books` | authenticated | `200 OK` |

### Loans — `/api/v1/loans`

| Method | Path | Role | Success |
|---|---|---|---|
| `POST` | `/api/v1/loans` | `LIBRARIAN` | `201 Created` |
| `PATCH` | `/api/v1/loans/{id}/return` | `LIBRARIAN` | `200 OK` |
| `GET` | `/api/v1/loans/{id}` | authenticated | `200 OK` |
| `GET` | `/api/v1/loans` | authenticated | `200 OK` |

### Pagination and sorting

The list endpoints accept the standard Spring Data parameters and default to
20 items per page sorted by `createdAt` descending:

```
GET /api/v1/loans?page=0&size=20&sort=dueDate,asc
```

### Request and response shapes

Create a loan:

```http
POST /api/v1/loans
Content-Type: application/json

{
  "bookId": "11111111-1111-1111-1111-111111111111",
  "memberId": "22222222-2222-2222-2222-222222222222",
  "dueDate": "2026-12-31"
}
```

`dueDate` must be in the future; all three fields are required.

A successful response carries the payload at the root — there is no envelope. The HTTP
status is what says the call succeeded:

```json
{
  "id": "33333333-3333-3333-3333-333333333333",
  "bookId": "11111111-1111-1111-1111-111111111111",
  "bookTitle": "Dune",
  "memberId": "22222222-2222-2222-2222-222222222222",
  "dueDate": "2026-12-31",
  "status": "ACTIVE",
  "createdAt": "2026-08-30T10:00:00Z",
  "updatedAt": "2026-08-30T10:00:00Z"
}
```

A paginated body carries `content` plus `page`, `size`, `totalElements`,
`totalPages`, `first`, `last`, and `sort`.

### Errors

Failures are RFC 9457 problem documents, served as `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "No available copies for book. bookId=..., title=Dune",
  "instance": "/api/v1/loans",
  "code": "NO_AVAILABLE_COPIES"
}
```

`type`, `title`, `status`, `detail` and `instance` are the standard members. `code` is
the application extension clients branch on, and it is present on every error. A second
extension, `errors` (field → message), appears only on `VALIDATION_ERROR` responses.

Error codes in use: `RESOURCE_NOT_FOUND`, `VALIDATION_ERROR`,
`BUSINESS_RULE_VIOLATION`, `NO_AVAILABLE_COPIES`, `DUPLICATE_ACTIVE_LOAN`,
`DUPLICATE_ISBN`, `UNAUTHORIZED`, `FORBIDDEN`, `METHOD_NOT_ALLOWED`,
`INTERNAL_SERVER_ERROR`.

Unexpected exceptions are logged server-side and answered with a generic
`500 INTERNAL_SERVER_ERROR` — internal details are never sent to the client.

## Project layout

```
com.eyup.library
├── config       SecurityConfig
├── controller   HTTP layer, @PreAuthorize, no business logic
├── domain       Loan, Book, LoanStatus — plain records, no JPA
├── dto          Request and response records
├── entity       JPA entities (BookEntity, LoanEntity)
├── exception    BusinessException hierarchy + GlobalExceptionHandler
├── mapper       entity → domain → response
├── repository   Spring Data JPA repositories
└── service      Business rules, @Transactional boundaries

src/main/resources/db/migration   Flyway migrations
```

A few conventions worth knowing before you add code:

- **Entities never leave the service layer.** Services return `domain` records;
  controllers map those to DTOs. This keeps lazy proxies out of Jackson.
- **Entities have no setters.** State changes go through intent-named methods such
  as `markReturned()`.
- **No response envelope.** Controllers return the DTO itself; the HTTP status carries
  success or failure.
- **The HTTP status lives in the exception handler**, not in the service. Services
  throw a typed `BusinessException`; `GlobalExceptionHandler` maps the error code
  to a status and builds the `ProblemDetail`.
- **Constructor injection only**, no field `@Autowired`, no Lombok.
- **A write guarded by a count takes a lock.** Read-check-write is a race otherwise,
  and every lock-guarded flow carries a concurrent test.

## Security

Stateless HTTP Basic, CSRF disabled, method security via `@PreAuthorize`.

⚠️ **No user store is configured.** With no `UserDetailsService`, Spring Boot falls
back to a single generated user (`user`, with a password printed to the console at
startup) that holds no roles — so out of the box you can read, but every
`LIBRARIAN` endpoint answers `403`. Wire in your own `UserDetailsService` (or an
in-memory user with `ROLE_LIBRARIAN`) before using this beyond the tests. HTTP
Basic over plain HTTP is fine for local development only.

## Tests

44 tests, all green:

- **Service tests** (`LoanServiceTest`, `BookServiceTest`) — Mockito unit tests of
  the business rules, no Spring context.
- **Controller tests** (`LoanControllerTest`, `BookControllerTest`) — MockMvc with a
  mocked service, covering status codes, root-level response fields, problem bodies,
  validation failures, and role enforcement for both `LIBRARIAN` and a plain user.
- **Repository tests** (`BookRepositoryTest`, `LoanRepositoryTest`) — `@DataJpaTest`
  slices against real PostgreSQL via Testcontainers, so every derived query, the
  migrations, and the unique constraint are actually executed.
- **Concurrency test** (`LoanServiceConcurrencyTest`) — races two requests for the last
  copy of a book and asserts exactly one loan is created. It fails if the pessimistic
  lock is removed.

```bash
./mvnw verify
```

CI runs `./mvnw --batch-mode verify` on every push and pull request
(`.github/workflows/ci.yml`), including the Testcontainers-backed tests.

## Known limitations

- No overdue handling. The status was removed rather than left unreachable; adding it
  back means shipping the transition (a scheduled job flipping past-due `ACTIVE` loans)
  in the same change.
- No update or delete endpoints for books, and no member entity — `memberId` is an
  opaque UUID that is not validated against anything.
- No seed data and no non-default profile; the schema is created by Flyway on first
  start against whatever database the datasource points at.
