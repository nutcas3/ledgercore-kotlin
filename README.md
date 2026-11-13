# LedgerCore

Embedded double-entry ledger engine (Kotlin + Spring Boot 4 + Exposed + PostgreSQL).

## Run

```bash
docker compose up -d postgres
./gradlew bootRun   # requires JDK 25
```

## API (all endpoints except `/v1/health` require HMAC-SHA256 auth)

- `POST /v1/accounts` — `{currency, openingBalance?}` → creates an account (amounts in major units, stored as minor units)
- `GET /v1/accounts/{id}` — balance
- `GET /v1/accounts/{id}/entries?page=&limit=` — posting history
- `POST /v1/transfers` — `{sourceAccountId, destinationAccountId, amount, currency}` + `Idempotency-Key` header → atomic double-entry transfer

## Auth

Every signed request needs:

- `X-Timestamp`: epoch milliseconds (±5 min skew)
- `X-Signature`: hex of `HMAC-SHA256(secret, "METHOD\nURI\nTIMESTAMP\nsha256hex(body)")`

Secret via `LEDGERCORE_HMAC_SECRET` env var.

## Design notes

- Money is `Long` minor units in domain and DB; BigDecimal only at the API edge.
- Transfers run in a single SERIALIZABLE `newSuspendedTransaction`; account rows lock via `SELECT ... FOR UPDATE` in sorted id order; serialization failures retry, unique-key violations map to idempotent replay.
- Postings are append-only; a `JournalEntry` enforces debits = credits per currency at construction.
