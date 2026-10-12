# Multicurrency Wallet

REST API acting as a multicurrency bank account, using daily exchange rates
of the Czech National Bank (CNB).

## Build & run
```
./mvnw spring-boot:run
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

