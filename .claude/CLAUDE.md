# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

`kafka-message-manager` is a small Spring Boot 3.5 service for sending messages to Kafka through both a JSON API and a Thymeleaf web form. The application is configured for Java 23 and Maven.

## Development commands

Run these commands from the repository root:

```bash
# Compile and package (also runs tests)
mvn clean package

# Run the application in development mode
mvn spring-boot:run

# Run all tests without packaging
mvn test

# Run one test class
mvn -Dtest=ClassNameTest test

# Run one test method
mvn -Dtest=ClassNameTest#methodName test

# Start the local Kafka, Zookeeper, and Kafka UI stack
docker compose -f docker/kafka/docker-compose.yml up -d

# Stop the local Kafka stack
docker compose -f docker/kafka/docker-compose.yml down
```

There is currently no separate lint or formatting plugin configured in `pom.xml`; `mvn test` and `mvn package` are the available Maven verification commands. The project requires a JDK 23 toolchain (`maven.compiler.source`/`target` are both `23`). The repository currently has no `src/test` sources, so a test command may report that no tests were found.

When the Docker stack is running, the externally advertised Kafka bootstrap address is `localhost:29092`, Kafka UI is at `http://localhost:8089/`, and the Spring application listens on `http://localhost:8080/`.

## Architecture

- `Application` is the Spring Boot entry point. Component scanning starts at `ru.uoles.kafka.sender`, so application classes should remain under that package (or configuration must be changed deliberately).
- `MessageController` exposes the REST API under `/api/kafka`. `POST /api/kafka/send` accepts a validated `MessageRequest` (`topic`, `kafkaAddress`, and `messageText`), delegates to `KafkaMessageService`, and returns a `MessageResponse`; consumer resources are managed through `POST/GET /api/kafka/consumers`, `GET /api/kafka/consumers/{id}`, `GET /api/kafka/consumers/{id}/messages?after=&limit=`, and `DELETE /api/kafka/consumers/{id}`. `GET /api/kafka/health` is a simple service health response.
- `KafkaMessageService` creates a Kafka producer factory and `KafkaTemplate` dynamically for each send request using the caller-provided bootstrap address. It uses string serializers, `acks=all`, retries, bounded acknowledgement waiting, and deterministic cleanup. `KafkaConsumerManager` owns bounded consumer containers and in-memory message buffers, while `KafkaInfoRepository` persists consumer definitions and received messages in SQLite at the URL configured by `spring.datasource.url` (currently `jdbc:sqlite:database/kafka-info.db`). The `consumers` table stores consumer identity, broker/topic/group configuration, lifecycle state, dropped-message count, and sequence high-water mark; the `messages` table stores received records and JSON Kafka headers with a cascading consumer foreign key. Consumers and their retained message logs are restored during application startup, explicit deletion removes their database rows and messages, and application shutdown stops containers without deleting persisted definitions.
- Consumer message polling uses an exclusive local sequence cursor. Responses include `oldestSequence`, `nextSequence`, and cumulative `droppedCount`; clients must advance their cursor using returned message sequences and account for buffer eviction. Each received record is persisted when the Kafka listener receives it, including its local sequence and headers.
- Liquibase is configured through `liquibase-core` and `spring.liquibase.change-log=classpath:liquibase/changelog-master.xml`. The master changelog includes formatted SQL files under `src/main/resources/liquibase/scripts/tables/`; migrations own creation of the `consumers` and `messages` tables. `SQLiteDatabaseConfig` only exposes `JdbcTemplate`; do not duplicate table creation in Java when changing the schema. `spring.datasource.url` is the single source of truth for the SQLite database location; there is no `KafkaPersistenceProperties` class or `kafka.persistence.*` configuration.
- `WebController` maps `/web/send-message`, `/web/`, `/web/index`, and (through the class-level `/web` mapping) the corresponding web paths to the `index` Thymeleaf view. `ThymeleafConfig` explicitly wires the classpath template resolver, engine, and UTF-8 view resolver; template files live in `src/main/resources/templates/`.
- `index.html` is a self-contained browser UI. It posts JSON to `/api/kafka/send`, including optional comma-separated `name=value` Kafka headers, performs client-side validation, and stores/renders message history in browser `localStorage` under `kafkaMessageHistory`; history is not persisted by the server or Kafka. The page uses Bootstrap 5.0.2 tabs for sending, message history, and dynamic Kafka consumers. The Message history tab renders a responsive table with Resend controls and headers. The Consumers tab creates process-local consumers with bootstrap address/topic, generates inner tabs, and polls buffered message tables. Resend repopulates topic, broker, headers, and message, then activates the send tab without sending automatically. Browser logic is split by responsibility into `app-state.js`, `history.js`, `consumers.js`, `form.js`, and `init.js` under `src/main/resources/templates/static/`; `index.html` loads them in that dependency order after Bootstrap, and `ThymeleafConfig` serves them under `/static/`.
- `application.properties` sets port 8080, Thymeleaf behavior, logging levels, Kafka producer retries, the SQLite `spring.datasource.url`, and Liquibase changelog settings. The service currently overrides the relevant producer settings when constructing its dynamic producer, so check both the properties file and `KafkaMessageService` when changing Kafka behavior.
- Liquibase changelogs live under `src/main/resources/liquibase/`: `changelog-master.xml` includes formatted SQL migrations for the `consumers` and `messages` tables. Liquibase owns schema creation and tracking through its changelog tables; do not duplicate migration DDL in Java configuration. The SQLite database path is taken only from `spring.datasource.url`.
- `SQLiteDatabaseConfig` provides `JdbcTemplate` over Spring Boot's configured `DataSource`; repository code is in `ru.uoles.kafka.sender.repository.KafkaInfoRepository`. There is no separate persistence-properties class or `kafka.persistence.*` configuration.
- The browser scripts are classic scripts loaded from `index.html` in dependency order: `app-state.js`, `history.js`, `consumers.js`, `form.js`, and `init.js`. Keep Bootstrap loaded before these scripts because resend uses the Bootstrap tab API.
- `docker/kafka/docker-compose.yml` provides a single-broker Confluent Kafka 6.2.4 setup backed by Zookeeper plus Kafka UI. The host uses `localhost:29092`; containers use the internal `kafka:9092` address.

## Request flow

1. A browser submits the form or another client posts JSON to `/api/kafka/send`.
2. Spring validation checks `MessageRequest`'s `@NotBlank` fields.
3. `MessageController` passes the three request values to `KafkaMessageService`.
4. The service creates a producer for the requested bootstrap address, sends the value to the requested topic, waits briefly for completion, and cleans up producer resources.
5. The controller returns a status payload; the web page adds the result to local history and renders it newest-first.

## Configuration and conventions

- Keep API DTOs in `ru.uoles.kafka.sender.model`, HTTP endpoints in `controller`, Kafka integration in `service`, and infrastructure beans in `config`.
- Use the existing Lombok style (`@Data`, `@RequiredArgsConstructor`, `@Slf4j`) consistently with nearby classes.
- The request intentionally accepts a Kafka address per request; do not assume the configured local broker is the only target when modifying the API or UI.
- The Docker README documents the local Kafka endpoints; update it and this file if the compose topology or advertised ports change.
