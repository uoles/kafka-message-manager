# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working in this repository.

## Project overview

`kafka-message-manager` is a Spring Boot 3.5 service for sending messages to Kafka and managing dynamic Kafka consumers through a JSON API and a Thymeleaf/Bootstrap browser UI. The project uses Maven and requires JDK 23. Application code is rooted at `ru.uoles.kafka.sender`; keep new Spring components below that package unless component scanning is deliberately changed.

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

The Maven build has no separate lint or formatting plugin. `spring-boot-starter-test` is present, but there are currently no `src/test` sources, so `mvn test` may report that no tests were found. Use a JDK 23 toolchain; `pom.xml` sets both compiler source and target to `23`.

The Docker stack advertises Kafka to the host at `localhost:29092`, exposes Kafka UI at `http://localhost:8089/`, and uses `kafka:9092` for broker connections from other containers. The application listens on port `8080`. The web controller currently maps the UI under `/web/`, `/web/index`, and `/web/send-message`; the README's bare `/` URL is not mapped by `WebController`.

## Architecture

### Application and HTTP layers

- `Application` is the Spring Boot entry point.
- `MessageController` owns `/api/kafka` and delegates all Kafka operations to services/managers. Endpoints are:
  - `POST /api/kafka/send` for a validated `MessageRequest` containing `topic`, `kafkaAddress`, `messageText`, and optional comma-separated `headers` (`name=value,name2=value2`).
  - `POST /api/kafka/consumers`, `GET /api/kafka/consumers`, and `GET /api/kafka/consumers/{id}` for dynamic consumer lifecycle and status.
  - `GET /api/kafka/consumers/{id}/messages?after=&limit=` for cursor-based message polling.
  - `DELETE /api/kafka/consumers/{id}` to stop and remove a consumer and its persisted messages.
  - `GET /api/kafka/health` for a simple health response.
- `WebController` returns the `index` Thymeleaf view for `/web/send-message`, `/web/`, and `/web/index`. `ThymeleafConfig` explicitly configures the classpath template resolver, UTF-8 engine, view resolver, and static resource handling.
- `src/main/resources/templates/index.html` is a self-contained UI with Bootstrap tabs for sending messages, browser-local message history, and dynamic consumers. Classic scripts are loaded in dependency order: `app-state.js`, `history.js`, `consumers.js`, `form.js`, then `init.js`. Bootstrap must remain loaded before them because resend and tab behavior use its API.

### Kafka sending

`KafkaMessageServiceImpl` creates a producer factory and `KafkaTemplate` for each request, using the caller-provided `bootstrap.servers` value and string serializers. Producer settings include `acks=all`, retries, retry backoff, linger, request timeout, and delivery timeout. `HeaderUtils` parses and validates optional `name=value` headers before creating the `ProducerRecord`. Producer resources are destroyed in `finally`; check this service as well as `application.properties` when changing producer behavior.

The broker address is intentionally request-scoped rather than a single application-wide Kafka setting. Do not replace it with the local Docker address without changing the API and UI contract.

### Dynamic consumers

`ConsumerManager` owns process-local `ManagedConsumer` instances and their `KafkaMessageListenerContainer`s. It limits the number of consumers and bounds each in-memory `ConsumerMessageBuffer`. Consumers use generated group IDs, string deserializers, `latest` offset reset, and auto-commit. Startup restores persisted definitions and retained messages; shutdown stops containers while preserving definitions for the next startup.

Each received record gets a local sequence number. `GET .../messages` treats `after` as an exclusive cursor and returns `oldestSequence`, `nextSequence`, and cumulative `droppedCount` along with messages. Clients must advance their cursor from returned sequences and account for buffer eviction. Kafka headers are retained in message responses and persistence.

### SQLite persistence and migrations

`ConsumersRepository` and `MessagesRepository`, coordinated by `ConsumersInfoServiceImpl`, persist consumer definitions and received records through `JdbcTemplate`. SQLite is configured by the single source of truth `spring.datasource.url` (currently `jdbc:sqlite:database/kafka-info.db`); `SQLiteDatabaseConfig` exposes the configured JDBC access.

Liquibase owns schema creation and tracking through `src/main/resources/liquibase/changelog-master.xml`, which includes formatted SQL migrations under `src/main/resources/liquibase/scripts/tables/`. Update the changelog when changing the `consumers` or `messages` schema; do not duplicate table creation in Java. The messages table is tied to consumers with cascading deletion.

## Repository conventions

- Keep DTOs in `ru.uoles.kafka.sender.model`, controllers in `controller`, Kafka integration in `kafka`, repositories in `kafka.repository`, services in `kafka.service`, consumer runtime code in `kafka.consumer`, and infrastructure configuration in `config`.
- Match the existing Lombok style (`@RequiredArgsConstructor`, `@Slf4j`, and DTO conventions) and Java 23 configuration in nearby code.
- Keep request validation and HTTP mapping in controllers, Kafka orchestration in services/managers, and SQL access in repositories.
- When changing Docker broker topology or advertised ports, update both `docker/kafka/README.md` and this file. The repository-specific rules in `.claude/rules/` contain additional project conventions; consult `.claude/rules/README.md` when a change touches architecture, API design, security, migrations, or the web UI.
