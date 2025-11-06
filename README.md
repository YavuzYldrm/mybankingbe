# MyBanking (Spring Boot)

JWT-secured banking backend with roles, accounts, transfers, fees, and consistent JSON errors.

## Features

* JWT auth (`USER`, `ADMIN`)
* Accounts: list mine, admin balances, deposit, withdraw
* Transfer by destination account number
* Pluggable `FeePolicy` (e.g., 1% for credit-card accounts)
* Global JSON error handler

## Stack

Java 17, Spring Boot 3, Spring Security 6, JPA/Hibernate, H2 (dev), JUnit5/Mockito/AssertJ, Maven.

## Run

```bash
mvn spring-boot:run
# or
mvn clean package && java -jar target/mybanking-*.jar
```

`application.yml` (dev essentials):

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:mybanking;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
  jpa:
    hibernate.ddl-auto: update
jwt:
  secret: change-me
  expires-in-seconds: 3600
```

H2 console: `/h2`.

## Key Endpoints (base `/api`)

Auth:

* `POST /auth/login` → `{ token, expiresAt, roles }`

Accounts (USER):

* `GET /accounts/detail`
* `POST /accounts/{accountId}/deposit` `{ amount }`
* `POST /accounts/{accountId}/withdraw` `{ amount }`

Accounts (ADMIN):

* `GET /accounts/admin/balances`

Transfer (USER):

* `POST /transactions/transfer` `{ fromAccountId, toAccountNumber, amount }`

## Error Shape

```json
{"timestamp":"…","path":"/…","status":409,"code":"INSUFFICIENT_BALANCE","message":"…","details":[],"traceId":"…"}
```

## Security

* Bearer JWT. Subject = userId (UUID). Roles claim used by Spring.
* Simple option: permit admin route in security and check role in controller, or disable anonymous to get `401`.

## Tests

```bash
mvn test
```

Unit, MVC (`@WebMvcTest`), and E2E (`@SpringBootTest`) over H2.

## Notes

Use a real DB and secret manager in prod (e.g., Azure Key Vault). Consider Flyway/Liquibase and rate limiting.
