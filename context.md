# Multicurrency Wallet

REST API in Java acting as a multicurrency bank account. Balances are kept per currency;
the total balance can be converted to any currency using the daily exchange rates
of the Czech National Bank (CNB).

Homework assignment for Wultra.

## Features
- Deposit money in any currency.
- Withdraw money in any currency (only up to the balance of that currency).
- Get the total balance of all currencies converted to a selected currency.

## Tech stack
- Java 25, Spring Boot 4.1, Maven (wrapper included)
- Spring Web MVC, Bean Validation
- springdoc-openapi – OpenAPI description + Swagger UI
- H2 in-memory database (PostgreSQL mode) accessed via `JdbcTemplate`
- CNB daily exchange rates as the rate source

## Repository structure
```
src/main/java/com/wultra/wallet/   application code
src/main/resources/
  application.properties           configuration (H2 datasource)
src/test/java/com/wultra/wallet/   tests
```

Data lives in memory only and is lost on restart.

## Build & run
```
./mvnw spring-boot:run
```
Tests: `./mvnw test`
