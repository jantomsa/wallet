# AI Usage

## Tools
- **Claude (Anthropic)** – used as a chat partner and as an agent with access to the repository folder.
  Used for: discussing design options, reviewing my code before commits, writing tests, boilerplate
  (DTOs, CNB client, Dockerfile), and drafting parts of the README.
- **IntelliJ IDEA** – my own coding, refactoring, running the app and tests.

## How I steered it
- I decided the direction first and used the AI as a reviewer: typical prompts were *"rate my solution first"*,
  *"check my changes before commit"*, *"what could be a better approach?"* – and only then *"implement it"*.
- I worked in small steps, one assignment decision (1–7) at a time, with a commit per step.
- I rejected or reduced suggestions I found too complex (see below) and kept the code style I wanted
  (Lombok classes instead of records, fields instead of constructor injection for `@Value`, my own comments).
- Agent configuration committed in the repo: [`CLAUDE.md`](CLAUDE.md) – project context, commands, architecture,
  and the rules the agent must keep (e.g. `schema.sql` and entities in sync, `BigDecimal` only, shared test context).

## Where the AI was wrong, incomplete or over-engineered
- **Storage.** The AI first proposed an in-memory `ConcurrentHashMap` with manual locking. I wanted the DB to handle
  concurrency, so I chose H2 + JPA with a pessimistic lock – the AI then pointed out the JPA pitfalls
  (lost update without a lock, `PESSIMISTIC_READ` vs `PESSIMISTIC_WRITE`).
- **Over-engineering.** The AI proposed returning a list of all used rates with their dates; I reverted it to a single
  `ratesValidForDate` - (counting with same date in responses). For CNB outages it proposed a "last known good" fallback with stale rates – I rejected it,
  stale rates must never be used, the API returns 503 instead.
- **Cache refresh.** The AI's first version cleared the cache on a fixed schedule; my idea was to compare the cached
  `validFor` with today and only refresh when it is outdated – that is the final solution.
- **Unverified assumptions.** The AI could not build the project or call CNB from its environment, so it could not
  verify the build on JDK 25 (Lombok annotation processing) or CNB's weekend behaviour – I verified these myself.

## What the AI caught in my code
- I had rounded the intermediate CZK amount to 2 decimals – the AI showed a concrete case where it gave a wrong result
  (72 USD → EUR = 64.45 instead of 64.44). Now only the final total is rounded.
- `@Digits(integer = 19)` did not fit the DB column `DECIMAL(19, 4)`; `PESSIMISTIC_READ` would deadlock on PostgreSQL;
  a missing user creation line in the Dockerfile.

## What I decided / wrote myself
- Data model: one wallet = one currency owned by a user; no conversion on deposit/withdraw (currency must match).
- H2 in-memory DB with JPA and a row lock (`SELECT ... FOR UPDATE`) for concurrency.
- `java.util.Currency` as the currency type, `BigDecimal` everywhere, banker's rounding to 2 decimals, configurable.
- Returning `ratesValidForDate` so the client knows which day's rates were used.
- Caching: in-memory `@Cacheable`, refresh only around CNB's 14:30 publication and only if the cached rates are
  not from today; no stale rates – 503 when CNB is down and nothing is cached.
- The whole app in `Europe/Prague` time zone.
- Which tests to have: the assignment workflow over HTTP, concurrent withdrawals, simulated deadlock.
