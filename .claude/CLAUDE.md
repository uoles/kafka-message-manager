# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working in this repository.

## Project overview

`kafka-message-manager` is a Spring Boot 3.5 service for sending messages to Kafka and managing dynamic Kafka consumers through a JSON API and a Thymeleaf/Bootstrap browser UI. The project uses Maven and requires JDK 23. Application code is rooted at `ru.uoles.kafka.sender`; keep new Spring components below that package unless component scanning is deliberately changed. Authentication uses local SQLite-backed users and roles with BCrypt passwords and stateless JWT bearer tokens.

## Development commands

Run commands from the repository root:

```bash
# Compile, run verification, and package
mvn clean package

# Run all tests
mvn test

# Run one test class or method
mvn -Dtest=ClassNameTest test
mvn -Dtest=ClassNameTest#methodName test

# Start the application
mvn spring-boot:run

# Start the local Kafka, Zookeeper, and Kafka UI stack
docker compose -f docker/kafka/docker-compose.yml up -d

# Stop the local Kafka stack
docker compose -f docker/kafka/docker-compose.yml down

# Check the service health endpoint
curl http://localhost:8080/api/kafka/health
```

The Maven build has no separate lint or formatting plugin. `spring-boot-starter-test` provides the JUnit 5, Mockito, AssertJ, and MockMvc stack; repository, service, and controller tests are under `src/test/java/ru/uoles/kafka/sender`, with controller tests using `@WebMvcTest` and mocked Kafka collaborators so they do not require Kafka or a database. Use a JDK 23 toolchain; `pom.xml` sets both compiler source and target to `23`.

The Docker stack advertises Kafka to the host at `localhost:29092`, exposes Kafka UI at `http://localhost:8089/`, and uses `kafka:9092` for broker connections from other containers. The application listens on port `8080`. The web controller maps public auth pages at `/web/login` and `/web/register`, and the protected UI under `/web/`, `/web/index`, and `/web/send-message`; the README's bare `/` URL is not mapped by `WebController`. The browser stores JWT state only in sessionStorage, sends bearer tokens through the shared auth wrapper, and clears token/user/history state on logout.

## Architecture

### Application and HTTP layers

- `Application` is the Spring Boot entry point.
- `MessageController` owns `/api/kafka` and delegates all Kafka operations to services/managers. Consumer ownership is propagated from the JWT subject; `USER` access is restricted to their own consumers, while `MODERATOR` and `ADMIN` retain global consumer access. Endpoints are:
  - `POST /api/kafka/send` for a validated `MessageRequest` containing `topic`, `kafkaAddress`, `messageText`, and optional comma-separated `headers` (`name=value,name2=value2`).
  - `POST /api/kafka/consumers`, `GET /api/kafka/consumers`, and `GET /api/kafka/consumers/{id}` for dynamic consumer lifecycle and status.
  - `GET /api/kafka/consumers/{id}/messages?after=&limit=` for cursor-based message polling.
  - `DELETE /api/kafka/consumers/{id}` to stop and remove a consumer and its persisted messages.
  - `GET /api/kafka/health` for a simple health response.
- `AuthController` owns public `POST /api/v1/auth/register` and `POST /api/v1/auth/login` endpoints. Registration assigns only the `USER` role; login returns a short-lived JWT bearer token.
- `SecurityConfig` permits auth endpoints, the Kafka health endpoint, and static resources, while protecting other `/api/**` and `/web/**` routes. JWT properties and security allowlists are externalized in `application.properties`/environment variables.
- Security tests using `@WebMvcTest` disable filters only for legacy controller behavior tests; dedicated security tests must exercise the filter chain and role restrictions.
- `WebController` returns the `index` Thymeleaf view for `/web/send-message`, `/web/`, and `/web/index`. `ThymeleafConfig` explicitly configures the classpath template resolver, UTF-8 engine, view resolver, and static resource handling.
- `src/main/resources/templates/index.html` is a self-contained UI with Bootstrap tabs for sending messages, browser-local message history, and dynamic consumers. Classic scripts are loaded in dependency order: `app-state.js`, `history.js`, `consumers.js`, `form.js`, then `init.js`. Bootstrap must remain loaded before them because resend and tab behavior use its API.

### Kafka sending

`KafkaMessageServiceImpl` creates a producer factory and `KafkaTemplate` for each request, using the caller-provided `bootstrap.servers` value and string serializers. Producer settings include `acks=all`, retries, retry backoff, linger, request timeout, and delivery timeout. `HeaderUtils` parses and validates optional `name=value` headers before creating the `ProducerRecord`. The send future is awaited with a 10-second timeout; interruption restores the thread flag, timeout becomes a runtime failure, and producer resources are destroyed in `finally`. The dynamic producer hardcodes its retry settings, so `spring.kafka.producer.retries` in `application.properties` does not control these request-scoped producers.

The broker address is intentionally request-scoped rather than a single application-wide Kafka setting. Do not replace it with the local Docker address without changing the API and UI contract. The browser's `clearForm()` currently resets the address to `localhost:9092`, while the Docker host address and initial form value are `localhost:29092`; preserve or correct this inconsistency deliberately when changing the UI.

### Persistence and response behavior

Received consumer records are persisted by `ReceivedMessagesRepository` in the `received_messages` table, defined by `TABLE.RECEIVED_MESSAGES.sql`; this is separate from browser send history. Dynamic consumers store nullable `user_id` ownership in `consumers`; USER list/read/messages/delete operations are owner-scoped, while legacy null-owner rows remain available to elevated roles. Consumers use `latest` offset reset, so records published before assignment are not replayed. Browser history is stored only in `localStorage` under `kafkaMessageHistory`. `MessageController` returns `MessageResponse` for sends, maps send failures to HTTP 500, and has a 400 handler for malformed header arguments when the exception reaches it. Bean-validation failures use Spring's default validation response because no dedicated validation handler is defined. Resend only repopulates the form and never sends automatically.

### Dynamic consumers

`ConsumerManager` owns process-local `ManagedConsumer` instances and their `KafkaMessageListenerContainer`s. It limits the number of consumers and bounds each in-memory `ConsumerMessageBuffer`. Consumers use generated group IDs, string deserializers, `latest` offset reset, and auto-commit. Startup restores persisted definitions and retained messages; shutdown stops containers while preserving definitions for the next startup.

Each received record gets a local sequence number. `GET .../messages` treats `after` as an exclusive cursor and returns `oldestSequence`, `nextSequence`, and cumulative `droppedCount` along with messages. Clients must advance their cursor from returned sequences and account for buffer eviction. Kafka headers are retained in message responses and persistence.

### SQLite persistence and migrations

`ConsumersRepository` and `ReceivedMessagesRepository`, coordinated by `ConsumersInfoServiceImpl`, persist consumer definitions and received records through `JdbcTemplate`. SQLite is configured by the single source of truth `spring.datasource.url` (currently `jdbc:sqlite:database/kafka-info.db`); `SQLiteDatabaseConfig` exposes the configured JDBC access.

Liquibase owns schema creation and tracking through `src/main/resources/liquibase/changelog-master.xml`, which includes formatted SQL migrations under `src/main/resources/liquibase/scripts/tables/`. The received-message schema is `received_messages`, defined in `TABLE.RECEIVED_MESSAGES.sql`; update the changelog when changing the `consumers` or `received_messages` schema and do not duplicate table creation in Java. The received-message table is tied to consumers with cascading deletion.

There are uncommitted persistence-renaming changes in the working tree: `MessagesRepository` was renamed to `ReceivedMessagesRepository`, and the `messages` table/migration was renamed to `received_messages`. Treat these changes as part of the current repository state when modifying persistence code.

The application does not persist browser send history on the server. The UI stores it in `localStorage` under `kafkaMessageHistory`.

### Error and response behavior

`MessageController` returns `MessageResponse` for sends, catches send failures as HTTP 500, and has a 400 handler for malformed header input when that exception reaches the handler. Bean-validation failures from `@Valid` use Spring's default validation response because no dedicated validation handler is defined. Resend only repopulates the form and does not send automatically.

## Repository conventions

- Keep DTOs in `ru.uoles.kafka.sender.model`, controllers in `controller`, Kafka integration in `kafka`, repositories in `kafka.repository`, services in `kafka.service`, consumer runtime code in `kafka.consumer`, and infrastructure configuration in `config`.
- Match the existing Lombok style (`@RequiredArgsConstructor`, `@Slf4j`, and DTO conventions) and Java 23 configuration in nearby code.
- Keep request validation and HTTP mapping in controllers, Kafka orchestration in services/managers, and SQL access in repositories.
- Keep JavaScript comments and UI behavior aligned with the current browser contract; static scripts are classic scripts, not modules.
- When changing Docker broker topology or advertised ports, update both `docker/kafka/README.md` and this file. The repository-specific rules in `.claude/rules/` contain additional project conventions; consult `.claude/rules/README.md` when a change touches architecture, API design, security, migrations, or the web UI.
- `README.md` currently advertises the bare `/` web URL and says the service sends/receives messages; verify those claims against `WebController` and the actual API before relying on them.
- `application.properties` has a stale `logging.level.com.example.kafkaproducer` entry; use the real `ru.uoles.kafka.sender` package when adjusting logging. Security configuration includes `security.jwt.issuer`, `security.jwt.audience`, `security.jwt.access-token-ttl`, external key properties, registration enablement, CORS origins, and broker allowlist properties; never commit production key material.

### Authentication and authorization

Users, roles, and user-role links are created by the Liquibase migration `TABLE.SECURITY.sql` in SQLite tables `users`, `roles`, and `user_roles`. Roles are stored as `USER`, `MODERATOR`, and `ADMIN` and mapped to Spring authorities with the `ROLE_` prefix. `USER` may create dynamic consumers; listing, inspection, message polling, and deletion remain restricted to `MODERATOR` and `ADMIN` until consumer ownership is implemented. BCrypt is used for password hashing. API consumers must send a valid JWT in the `Authorization: Bearer` header; CSRF is disabled for this stateless bearer-token model. The initial security implementation generates an RSA key pair at startup when no external key material is configured, which is suitable only for local development because restarting invalidates tokens; production deployments must provide stable external signing keys and HTTPS.

Public authentication routes are `/api/v1/auth/register` and `/api/v1/auth/login`. `/api/kafka/health` remains public for health probes. Other `/api/**` and `/web/**` routes require authentication; role-specific restrictions should be added with method security as consumer ownership and the browser login flow are completed. Do not log passwords, password hashes, JWTs, signing keys, or authorization headers.
- Do not assume `spring.kafka.producer.retries` affects dynamic producers; `KafkaMessageServiceImpl` supplies request-scoped properties directly.
