# library-lending

A small Spring Boot service for lending library books. It models books and loans,
enforces the lending rules (copy availability, no duplicate active loan per member),
and exposes a paginated, role-protected REST API.

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.8 (Web MVC, Data JPA, Security, Validation) |
| Database | H2, in-memory (`create-drop`) |
| API docs | springdoc-openapi 3.1.0 (Swagger UI) |
| Build | Maven |
| Tests | JUnit 5, Mockito, Spring Security Test |

## Getting started

Requirements: JDK 21 and Maven (or use the Maven wrapper if you add one).

```bash
mvn spring-boot:run
```

The service starts on **port 8081**. Swagger UI is at
<http://localhost:8081/swagger-ui.html>, the OpenAPI document at
`/v3/api-docs`. Both sit behind authentication like every other path — see
[Security](#security).

Build and test:

```bash
mvn clean verify     # compile + run all tests
mvn test             # tests only
```

The database is in-memory and recreated on every start, so no setup is needed —
and nothing survives a restart.

## Domain model

**Book** — `title`, `isbn`, `copies` (total number of physical copies).

**Loan** — links a book to a member, with a `dueDate` and a status:

| Status | Meaning |
|---|---|
| `ACTIVE` | Currently lent out |
| `RETURNED` | Given back |
| `OVERDUE` | Past its due date and not returned |

### Lending rules

- **No copies available → `409 Conflict`.** A book is unavailable when the number
  of its loans that are not `RETURNED` has reached `copies`.
- **Duplicate active loan → `409 Conflict`.** The same member cannot hold two
  `ACTIVE` loans of the same book.
- **Returning twice → `409 Conflict`.** Only an `ACTIVE` (or `OVERDUE`) loan can be
  returned; a `RETURNED` loan cannot be returned again.
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

Every successful response is wrapped in a common envelope:

```json
{
  "success": true,
  "data": {
    "id": "33333333-3333-3333-3333-333333333333",
    "bookId": "11111111-1111-1111-1111-111111111111",
    "bookTitle": "Dune",
    "memberId": "22222222-2222-2222-2222-222222222222",
    "dueDate": "2026-12-31",
    "status": "ACTIVE",
    "createdAt": "2026-08-30T10:00:00Z",
    "updatedAt": "2026-08-30T10:00:00Z"
  },
  "timestamp": "2026-08-30T10:00:00Z"
}
```

A paginated `data` object carries `content` plus `page`, `size`, `totalElements`,
`totalPages`, `first`, `last`, and `sort`.

### Errors

Failures return a separate error body, never the envelope above:

```json
{
  "code": "NO_AVAILABLE_COPIES",
  "message": "No available copies for book. bookId=..., title=Dune",
  "status": 409,
  "path": "/api/v1/loans",
  "validationErrors": {},
  "timestamp": "2026-08-30T10:00:00Z"
}
```

`validationErrors` is populated (field → message) only for `VALIDATION_ERROR`
responses from bean validation. Error codes in use: `RESOURCE_NOT_FOUND`,
`VALIDATION_ERROR`, `BUSINESS_RULE_VIOLATION`, `NO_AVAILABLE_COPIES`,
`DUPLICATE_ACTIVE_LOAN`, `UNAUTHORIZED`, `FORBIDDEN`, `METHOD_NOT_ALLOWED`,
`INTERNAL_SERVER_ERROR`.

Unexpected exceptions are logged server-side and answered with a generic
`500 INTERNAL_SERVER_ERROR` — internal details are never sent to the client.

## Project layout

```
com.eyup.library
├── api          ApiResponse / ApiError envelopes
├── config       SecurityConfig
├── controller   HTTP layer, @PreAuthorize, no business logic
├── domain       Loan, Book, LoanStatus — plain records, no JPA
├── dto          Request and response records
├── entity       JPA entities (BookEntity, LoanEntity)
├── exception    BusinessException hierarchy + GlobalExceptionHandler
├── mapper       entity → domain → response
├── repository   Spring Data JPA repositories
└── service      Business rules, @Transactional boundaries
```

A few conventions worth knowing before you add code:

- **Entities never leave the service layer.** Services return `domain` records;
  controllers map those to DTOs. This keeps lazy proxies out of Jackson.
- **Entities have no setters.** State changes go through intent-named methods such
  as `markReturned()`.
- **The HTTP status lives in the exception handler**, not in the service. Services
  throw a typed `BusinessException`; `GlobalExceptionHandler` maps the error code
  to a status.
- **Constructor injection only**, no field `@Autowired`, no Lombok.

## Security

Stateless HTTP Basic, CSRF disabled, method security via `@PreAuthorize`.

⚠️ **No user store is configured.** With no `UserDetailsService`, Spring Boot falls
back to a single generated user (`user`, with a password printed to the console at
startup) that holds no roles — so out of the box you can read, but every
`LIBRARIAN` endpoint answers `403`. Wire in your own `UserDetailsService` (or an
in-memory user with `ROLE_LIBRARIAN`) before using this beyond the tests. HTTP
Basic over plain HTTP is fine for local development only.

## Tests

31 tests, all green:

- **Service tests** (`LoanServiceTest`, `BookServiceTest`) — Mockito unit tests of
  the business rules, no Spring context.
- **Controller tests** (`LoanControllerTest`, `BookControllerTest`) — MockMvc with a
  mocked service, covering status codes, the response envelope, validation
  failures, and role enforcement for both `LIBRARIAN` and a plain user.

```bash
mvn test
```

## Known limitations

- `OVERDUE` is modelled and honoured by the availability check, but nothing
  transitions a loan into it yet — a scheduled job that flips past-due `ACTIVE`
  loans would close that gap.
- Availability is checked and then written inside one transaction, but there is no
  optimistic lock on `BookEntity`; two concurrent requests for the last copy could
  both succeed.
- No update or delete endpoints for books, and no member entity — `memberId` is an
  opaque UUID that is not validated against anything.
- In-memory H2 only; no migrations (Flyway/Liquibase) and no persistent profile.
