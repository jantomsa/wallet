# Multicurrency Wallet

REST API acting as a multicurrency bank account, using daily exchange rates
of the Czech National Bank (CNB).

## Build & run
Requires JDK 25:
```
./mvnw spring-boot:run
```

Or with Docker Compose (no local JDK needed):
```
docker compose up --build
```

## API overview

| Method | Path | Description |
|---|---|---|
| `POST` | `/v1/wallet` | Create a wallet in one currency for a user (starts with balance 0) |
| `PUT` | `/v1/wallet/deposit` | Deposit money into a wallet (currency must match the wallet) |
| `PUT` | `/v1/wallet/withdraw` | Withdraw money from a wallet (max. the wallet balance) |
| `GET` | `/v1/wallet/user/{userId}/currency/{currency}` | Total balance of all user's wallets converted to `currency` |

All requests below (plus error examples) are also in [`wallet.http`](wallet.http) – runnable with one click
in IntelliJ IDEA (HTTP Client) or VS Code (REST Client).

The application starts with one sample wallet (`src/main/resources/data.sql`):
user `905a48a6-ffcf-430e-9e92-44bfa08427ac`, EUR wallet `fcb58bd7-f09a-47f2-a92d-89e20095a4da` with balance 1.00 EUR.

**Create a USD wallet**
```
curl -X POST 'http://localhost:8080/v1/wallet' \
  -H 'Content-Type: application/json' \
  -d '{"userId": "905a48a6-ffcf-430e-9e92-44bfa08427ac", "currency": "USD", "name": "Travel money"}'
```
```json
{ "id": "<new wallet id>", "userId": "905a48a6-ffcf-430e-9e92-44bfa08427ac", "currency": "USD", "name": "Travel money", "amount": 0 }
```

**Deposit 1 EUR**
```
curl -X PUT 'http://localhost:8080/v1/wallet/deposit' \
  -H 'Content-Type: application/json' \
  -d '{"walletId": "fcb58bd7-f09a-47f2-a92d-89e20095a4da", "amount": 1, "currency": "EUR"}'
```

**Withdraw 1 EUR**
```
curl -X PUT 'http://localhost:8080/v1/wallet/withdraw' \
  -H 'Content-Type: application/json' \
  -d '{"walletId": "fcb58bd7-f09a-47f2-a92d-89e20095a4da", "amount": 1, "currency": "EUR"}'
```

**Total balance in USD**
```
curl 'http://localhost:8080/v1/wallet/user/905a48a6-ffcf-430e-9e92-44bfa08427ac/currency/USD'
```
```json
{ "balance": 1.17, "currency": "USD", "ratesValidForDate": "2026-10-09" }
```
(the balance depends on the current CNB rates)

**Error response** (e.g. withdrawing more than the balance)
```json
{ "code": "INSUFFICIENT_FUNDS", "message": "Insufficient funds in wallet 'fcb58bd7-f09a-47f2-a92d-89e20095a4da'" }
```

## API documentation (OpenAPI)
Generated at runtime by springdoc-openapi. With the application running on the default port:

| What | URL |
|---|---|
| Swagger UI (interactive, try requests from the browser) | http://localhost:8080/swagger-ui.html |
| OpenAPI spec – JSON | http://localhost:8080/v3/api-docs |
| OpenAPI spec – YAML | http://localhost:8080/v3/api-docs.yaml |

## Design decisions

**Wallet model.** One wallet = one currency, owned by a user (`userId`). A user can have wallets in several
currencies (`POST /v1/wallet`). Deposits and withdrawals are never converted – the request currency must match
the wallet currency, otherwise `400 CURRENCY_MISMATCH`. Conversion happens only when computing the total balance.

### 1. Missing rates (weekends, public holidays)
In response is date, for which is deposit calculated. It seems that date "validFor" 
returned in CNB api is always same for all results and based on that I am using only findFirst. 
Otherwise, I would return list of currencies with dates. Let the client decide what to do. 

```json
{ "balance": 12.80, "currency": "USD", "ratesValidForDate": "2026-10-09" }
```

### 2. Rate units (100 JPY, 100 HUF)
CNB quotes some currencies per multiple units (e.g. 100 JPY = 21.045 CZK), so every conversion uses the quoted amount into CZK first, then to target.

### 3. CNB unavailable. What does the API do when CNB cannot be reached? Does it cache rates?
Ive added the in-memory cache. Ive found that rates are refreshed once per day around 14:30. So the TTL for cache is 3min from that time for hour. It will try to get refreshed on working days.
If data are cached for day and CNB is down, it will still serve data. Timezone is set to be sure, that cron is running at correct time.

### 4. Money precision. How do you represent amounts, and what rounding rules apply to conversions?
All amounts and CNB rates are "BigDecimal", never "double" (CNB JSON numbers are parsed directly into "BigDecimal").
- Deposits and withdrawals are stored exactly as sent – they are never rounded. Input is validated to max 17 digits
  before and 2 after the decimal point, which matches the DB column `DECIMAL(19, 2)`.
- Ive tried to put the rounding and scale into property, but its not used everywhere unfortunately. Some of it is in annotations and so on. 

### 5. Concurrency. What happens with simultaneous withdrawals from the same balance?
I was not sure, which storage to use. At first wanted to use just plain Map as storage. But later it was nicer to use H2 in memory database and use 
more real-world scenario, where we are leaving these thing of Pessimistic locking to DB. Goal of my solution is to lock row for update on select - (SELECT x ... FOR UPDATE).
This query has locking in name, so developer will not use it without knowledge. 

### 6. Errors. What do clients receive for invalid input, unknown currency, or insufficient funds?
Business errors return `{ "code": "...", "message": "..." }` with the HTTP status of the error code:

| Situation | Response |
|---|---|
| Wallet not found | `404 ENTITY_NOT_FOUND` |
| Currency does not match the wallet | `400 CURRENCY_MISMATCH` |
| Currency not published by CNB (e.g. `XAU`) | `400 UNSUPPORTED_CURRENCY` |
| Withdrawal above the balance | `409 INSUFFICIENT_FUNDS` |
| CNB unavailable and no rates cached | `503 RATES_UNAVAILABLE` |
| Invalid input (missing field, negative amount, invalid ISO code, malformed JSON) | `400`, Spring's default error body |

Invalid input is rejected by validation, but does not use the `{code, message}` format yet – that would need
handlers for the validation exceptions in `WultraWalletExceptionHandler` (see Limitations).

### 7. CZK. CNB quotes rates against CZK. How do you treat CZK itself?
I threat the CZK as rate multiplier with 1, so it will flow into common logic, but it will do noting. 

## Optional extra points
1. Exposure to AI - not done.
2. Docker file and docker-compose added.
3. Logging provided, health endpoints are accessible (http://localhost:8080/actuator/health/liveness)


## Limitations
- No authentication/authorization – anyone who knows a wallet id can use it.
- Wallets can be created in any ISO currency, but CNB publishes rates only for some of them – a wallet in such a currency
  makes the total balance fail with `400 UNSUPPORTED_CURRENCY`.
- Invalid input returns Spring's default 400 body, not the `{code, message}` format.
- The same scale (2 decimals) is used for all currencies (JPY would naturally have 0, BHD 3).
- A balance is stored, not derived from a ledger of transactions – there is no history.
- If CNB publishes after 15:00, the new rates are picked up the next working day.