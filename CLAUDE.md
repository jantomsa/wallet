# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Multicurrency wallet REST API (Wultra homework assignment). Java 25, Spring Boot 4.1, Maven wrapper, Lombok, Spring Data JPA on in-memory H2 (PostgreSQL mode), CNB (Czech National Bank) daily rates as the FX source. `README.md` holds the design decisions (missing rates, rate units, caching, precision, concurrency, errors, CZK) — keep it in sync when changing behavior in those areas.

## Commands

```
./mvnw spring-boot:run                                   # run on :8080 (Swagger UI: /swagger-ui.html)
./mvnw test                                              # all tests
./mvnw test -Dtest=WalletServiceTest                     # single test class
./mvnw test -Dtest=WalletServiceTest#methodName          # single test method
./mvnw package -DskipTests                               # build jar
docker compose up --build                                # run in Docker (no local JDK needed)
```

`wallet.http` contains runnable sample requests (IntelliJ HTTP Client / VS Code REST Client). Seed data in `src/main/resources/data.sql` (user `905a48a6-…`, one EUR wallet).

## Architecture

Request flow: `WalletControllerV1` (`/v1/wallet`) → `WalletService` → `WalletRepository` / `CurrencyRateService`.

- **Wallet model**: one `WalletEntity` row = one wallet in a single currency owned by a `userId`. Deposit/withdraw never convert — the request currency must match the wallet currency (`CURRENCY_MISMATCH` otherwise). Only the total-balance endpoint converts, summing all of a user's wallets into the target currency.
- **Schema**: defined in `schema.sql` (with a non-negative amount CHECK); Hibernate runs with `ddl-auto=validate`, so entity changes require matching `schema.sql` changes.
- **Concurrency**: deposit/withdraw load the wallet via `WalletRepository.findWithLock` (`PESSIMISTIC_WRITE`, i.e. `SELECT … FOR UPDATE`) inside `@Transactional`. H2 `LOCK_TIMEOUT=10000` in the datasource URL. `ConcurrentMoneyOperationsTest` verifies no overdraw under parallel withdrawals.
- **Exchange rates**: `CnbApiClient` (RestClient, `/cnbapi/exrates/daily`) → `CurrencyRateService.loadCurrentRates()` is `@Cacheable("cnbRates")` with no args (cache key `SimpleKey.EMPTY`) and returns an `ExchangeRates` snapshot carrying CNB's `validFor` date (returned to clients as `ratesValidForDate`). Failure to reach CNB → `RATES_UNAVAILABLE`, but a previously cached snapshot keeps being served.
- **Cache refresh**: `CnbRatesCacheEvictionJob` runs on cron `cnb.rates.cacheEvictCronFrom1430` (every 3 min 14:30–14:59, Mon–Fri) and clears the cache only if the cached rates aren't for today. The JVM default time zone is forced to `application.timezone` (Europe/Prague) by `TimeZoneConfig`, so `LocalDate.now()` and cron agree with CNB.
- **Conversion** (`ExchangeRates.convert`): always via CZK — `amount * rate / units` (CNB quotes some currencies per 100 units, e.g. JPY/HUF), then CZK → target. CZK itself is a synthetic rate of 1/1 (`CzkRate.CZK`). Unknown currency → `UNSUPPORTED_CURRENCY`.
- **Money**: always `BigDecimal`. Intermediate conversion uses `MathContext.DECIMAL128`; only the final total is rounded using `application.globalNumberScale` / `application.globalRoundingMode` (HALF_EVEN, scale 2). Stored amounts are `DECIMAL(19,2)` and inputs are validated to match; deposits/withdrawals are never rounded.
- **Errors**: business errors throw `WultraWalletException` with an `ErrorCode` (which carries the HTTP status); `WultraWalletExceptionHandler` maps it to `ErrorDetail {code, message}`. Bean-validation failures currently fall through to Spring's default 400.

## Tests

Spring Boot integration tests (`WalletWorkflowTest`, `ConcurrentMoneyOperationsTest`) use a real context + H2 and mock `CnbApiClient` with `@MockitoBean`; keep the same mock-bean set across these classes so they share one cached Spring context. HTTP-level tests use RestAssured. Service-level tests are plain Mockito unit tests.
