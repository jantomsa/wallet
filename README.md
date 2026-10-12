# Multicurrency Wallet

REST API acting as a multicurrency bank account, using daily exchange rates
of the Czech National Bank (CNB).

## Build & run
Requires JDK 25:
```
./mvnw spring-boot:run
```

Or with Docker (no local JDK needed):
```
docker build -t wallet .
docker run -p 8080:8080 wallet
```

## API overview

| Method | Path | Description |
|---|---|---|
| `PUT` | `/v1/wallet/deposit` | Deposit money into a wallet (currency must match the wallet) |
| `PUT` | `/v1/wallet/withdraw` | Withdraw money from a wallet (max. the wallet balance) |
| `GET` | `/v1/wallet/user/{userId}/currency/{currency}` | Total balance of all user's wallets converted to `currency` |

All requests below (plus error examples) are also in [`wallet.http`](wallet.http) – runnable with one click
in IntelliJ IDEA (HTTP Client) or VS Code (REST Client).

The application starts with one sample wallet (`src/main/resources/data.sql`):
user `905a48a6-ffcf-430e-9e92-44bfa08427ac`, EUR wallet `fcb58bd7-f09a-47f2-a92d-89e20095a4da` with balance 1.00 EUR.

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
On invalid input - null value in required parameter, invalid currency will end with 400 error without any detail message. 
To have exception handled will require to handle them in "WultraWalletExceptionHandler". The "Insufficient funds" is situation exception ad is handled with custom exception.
Some situational exceptions are handled and explained to user. 

### 7. CZK. CNB quotes rates against CZK. How do you treat CZK itself?
I threat the CZK as rate multiplier with 1, so it will flow into common logic, but it will do noting. 

## Optional extra points
1. Exposure to AI - not done.
2. Docker file and docker-compose added.
3. Logging provided, health endpoints are accessible (http://localhost:8080/actuator/health/liveness)
4. 

## Limitations: 
- There is no security
- There is no limitation on currencies, you can add any currency, but in CNB is only some of them.
- 