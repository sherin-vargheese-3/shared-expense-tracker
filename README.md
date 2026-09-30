# Shared Expense Tracker

A web app for a group of people to track shared expenses and settle up.

- Create groups and add members
- Record expenses split **equally** or by **exact amounts**
- See every member's **balance** (what they owe or are owed)
- Get the **fewest payments** that settle all debts, and record them with one click
- Remove members once they're settled up, or delete a group entirely
- Everything is persisted in PostgreSQL and survives restarts

| Layer    | Stack                                                                    |
|----------|--------------------------------------------------------------------------|
| Frontend | React 19 + TypeScript, Vite, MUI, TanStack Query, React Router           |
| Backend  | Spring Boot 3.5, Java 21, Spring Data JPA, Jakarta Validation, Springdoc |
| Database | PostgreSQL 16, Flyway migrations                                         |
| Tests    | JUnit 5, Mockito, Testcontainers · Vitest, React Testing Library, MSW    |
| Runtime  | Docker + Docker Compose (nginx serves the SPA and proxies the API)       |

## Quick start

Requires Docker.

```bash
docker compose up --build
node scripts/seed-demo.mjs        # optional: creates a populated "Goa trip" group
```

| URL                                   | What                  |
|---------------------------------------|-----------------------|
| http://localhost:3000                 | Web app               |
| http://localhost:8080/swagger-ui.html | API docs (Swagger UI) |
| http://localhost:8080/actuator/health | Backend health        |
| `localhost:5433`                      | PostgreSQL (`expenses` / `expenses`) |

Data lives in the `postgres-data` volume; `docker compose down -v` wipes it.
Ports and credentials can be overridden via a `.env` file — see [`.env.example`](.env.example).

## Local development

Requires Java 21 and Node 20+ (Maven is provided by the wrapper).

```bash
docker compose up -d postgres                  # database only, on localhost:5433
cd backend  && ./mvnw spring-boot:run          # API on http://localhost:8080
cd frontend && npm install && npm run dev      # UI on http://localhost:5173 (proxies /api to :8080)
```

## Tests

```bash
cd backend  && ./mvnw verify   # unit, web-slice and Testcontainers integration tests (needs Docker)
cd frontend && npm test        # component and page tests against a mocked API (MSW)
cd frontend && npm run lint && npm run build
```

| Suite | What it covers |
|---|---|
| `SplitCalculatorTest`, `MoneyTest` | Equal-split rounding; randomized check that shares always sum to the total and differ by ≤ 1 cent |
| `SettlementPlannerTest` | Every plan zeroes all balances; plan size equals an independent brute-force minimum on 300 random groups; a case where greedy is suboptimal; 20-balance worst case runs well under 2s |
| `*ServiceTest` (Mockito) | Every business validation rule |
| `*ControllerTest` (`@WebMvcTest`) | Bean Validation, field paths in errors, malformed JSON |
| `*ApiIntegrationTest` (Testcontainers) | Full HTTP → PostgreSQL flows, including settling up end-to-end |
| Frontend (Vitest + RTL + MSW) | Money parsing, expense form (live split preview, exact-split remaining counter, server error mapping), group page, recording suggested payments |

## API

All endpoints are under `/api/groups`. Full schemas and examples are in Swagger UI.

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/groups` | Create a group, optionally with members `{name, members?: [names]}` |
| `GET` | `/api/groups` | List groups (newest first) with member counts |
| `GET` | `/api/groups/{id}` | Group with its (active) members |
| `DELETE` | `/api/groups/{id}` | Delete the group and everything in it |
| `POST` | `/api/groups/{id}/members` | Add a member `{name}` |
| `GET` | `/api/groups/{id}/members` | List active members |
| `DELETE` | `/api/groups/{id}/members/{memberId}` | Remove a member (balance must be zero) |
| `POST` | `/api/groups/{id}/expenses` | Add an expense (see below) |
| `GET` | `/api/groups/{id}/expenses` | List expenses with splits (newest first) |
| `GET` | `/api/groups/{id}/expenses/{expenseId}` | One expense |
| `GET` | `/api/groups/{id}/balances` | Every member's net balance |
| `GET` | `/api/groups/{id}/suggested-payments` | Fewest payments to settle all debts |
| `POST` | `/api/groups/{id}/settlements` | Record a payment `{fromMemberId, toMemberId, amount}` |
| `GET` | `/api/groups/{id}/settlements` | List payments (newest first) |

```jsonc
// POST /api/groups/1/expenses
{
  "description": "Hotel",
  "amount": 250.50,
  "paidBy": 1,
  "splitType": "EXACT",            // or "EQUAL" (then omit per-member amounts)
  "splits": [
    { "memberId": 1, "amount": 100.25 },
    { "memberId": 3, "amount": 150.25 }
  ]
}
```

### Errors

Errors are [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem details. Validation failures list every
invalid field, using JSON paths so clients can highlight the exact input:

```json
{
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "errors": [
    { "field": "paidBy", "message": "member 9 is not in this group" },
    { "field": "splits", "message": "split amounts add up to 99.99 but the expense amount is 100.00" }
  ]
}
```

| Status | When |
|---|---|
| 400 | Invalid input: missing/blank fields, non-positive amounts, more than 2 decimals, wrong JSON types, unknown split type, members not in the group, duplicate participants, exact splits that don't add up, paying yourself |
| 404 | Unknown group or expense |
| 409 | Member name already used in the group (case-insensitive); removing a member who still owes or is owed money |
| 500 | Unexpected error (details are logged, never returned) |

## Design decisions

### Money is integer cents
Amounts cross the API as decimals with at most two fraction digits, but are stored as `BIGINT` cents and all
arithmetic is done on `long`. Floating point never touches a balance, which is what makes "balances always
sum to exactly zero" a guarantee rather than a hope. Input with a third decimal place is rejected, not rounded.
The frontend does the same with integer cents (`src/lib/money.ts`).

### Equal splits are rounded deterministically
`100.00 / 3` becomes `33.34 + 33.33 + 33.33`: everyone gets the floor, and leftover cents go one each to members
in ascending id order. The result doesn't depend on request order, shares never differ by more than one cent,
and they always sum to the total. The frontend mirrors this rule so the preview matches what is stored.

### Balances are derived, never stored
A member's balance is `paid − owed + settlements sent − settlements received`, computed from four aggregate
SQL queries on every read. There is no running total to drift out of sync or to update transactionally, and
editing/deleting expenses later needs no balance bookkeeping. The service asserts the sum is zero and fails
loudly if it ever isn't. At much larger scale this could be cached or materialized.

### Suggested payments are truly minimal
Greedy "largest debtor pays largest creditor" needs at most *n − 1* payments but is not always optimal — e.g.
balances `[-8, -3, -2, +3, +4, +6]` take 5 payments greedily, but 4 if `-3 → +3` is settled separately.

The minimum is *n − k*, where *k* is the largest number of disjoint groups whose balances each sum to zero
(each group of size *s* settles in *s − 1* payments). This is NP-hard in general, so `SettlementPlanner` solves
it exactly with a bitmask DP over subsets (O(2ⁿ·n)) for up to **20 members with non-zero balances**, then
settles each zero-sum group greedily. Above 20 it falls back to plain greedy (still ≤ *n − 1*). The planner is
verified against an independent brute-force search in tests.

### Validation lives in two layers
Jakarta Bean Validation handles shape (required, positive, ≤ 2 decimals, lengths); the services handle rules
that need the database (membership, duplicates, exact-split totals). Both produce the same `errors[]` format,
and the service collects all problems before failing so the UI can show them at once. Database constraints
(`CHECK`s, unique index on `(group_id, lower(name))`) are the last line of defence.

All input errors return **400** — including business-rule failures like an exact split that doesn't add up —
so clients have one error shape to map onto form fields.

### Removing members and deleting groups
A member can only be removed once their balance is **exactly zero**; otherwise their debt would vanish and
the remaining balances would no longer sum to zero. The API returns 409 with who owes what, and the UI
explains this before offering the button.

- A member with **no expenses or payments** (e.g. added by mistake) is deleted outright.
- A member **with history** is *soft-removed* (`removed_at` is set): they disappear from the member list,
  balances, member counts and forms, and can't be used in new expenses or payments, but past expenses and
  payments still show their name. Their name becomes available again (the unique index only covers active
  members). Responses flag them (`removed` on members, `memberRemoved` on splits) and the UI shows
  "Bob (former member)", so history can't be confused with a new member of the same name.

Deleting a group is permanent and removes its members, expenses, splits and payments. The UI asks for
confirmation and warns if balances are unsettled. Rows are deleted in dependency order in one transaction,
because Postgres checks foreign keys after each cascaded delete.

### Other choices
- **Overpaying is allowed.** A settlement larger than the debt simply reverses who owes whom; balances still
  sum to zero.
- **The payer need not be a participant** (e.g. paying for someone else's ticket).
- **Member names are unique per group, case-insensitively**, so balances and payment suggestions are
  unambiguous.
- **Flyway owns the schema**; Hibernate runs with `ddl-auto: validate`.

## Project structure

```
backend/src/main/java/com/edstem/expense/
  group/        groups and members
  expense/      expenses, splits, SplitCalculator
  settlement/   recorded payments
  balance/      BalanceCalculator, SettlementPlanner, balances & suggestions API
  common/       Money, error handling, OpenAPI config
backend/src/main/resources/db/migration/   Flyway migrations

frontend/src/
  api/          typed API client, TanStack Query hooks, error helpers
  lib/          money and date helpers
  pages/        GroupsPage, GroupPage
  components/   dialogs and cards
  test/         MSW server, render helpers, fixtures

scripts/seed-demo.mjs   demo data via the public API
```

## Limitations and next steps

- Expenses and settlements can't be edited or deleted yet. Balances are derived, so this is purely an
  API/UI addition.
- Removed members can't be restored (re-adding the name creates a new member).
- No authentication (out of scope); any client can modify any group.
- Lists are not paginated.
- Single currency only.
