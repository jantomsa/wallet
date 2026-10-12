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
