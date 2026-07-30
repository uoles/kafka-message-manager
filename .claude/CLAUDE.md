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
- `MessageController` exposes the REST API under `/api/kafka`. `POST /api/kafka/send` accepts a validated `MessageRequest` (`topic`, `kafkaAddress`, and `messageText`), delegates to `KafkaMessageService`, and returns a `MessageResponse`; failures are converted to an HTTP 500 response. `GET /api/kafka/health` is a simple service health response.
- `KafkaMessageService` creates a Kafka producer factory and `KafkaTemplate` dynamically for each send request using the caller-provided bootstrap address. It uses string serializers, `acks=all`, three retries, and a one-second wait before destroying the template/factory. Changes to send semantics, error handling, or producer lifecycle belong here rather than in the controller.
- `WebController` maps `/web/send-message`, `/web/`, `/web/index`, and (through the class-level `/web` mapping) the corresponding web paths to the `index` Thymeleaf view. `ThymeleafConfig` explicitly wires the classpath template resolver, engine, and UTF-8 view resolver; template files live in `src/main/resources/templates/`.
- `index.html` is a self-contained browser UI. It posts JSON to `/api/kafka/send`, including optional comma-separated `name=value` Kafka headers, performs client-side validation, and stores/renders message history in browser `localStorage` under `kafkaMessageHistory`; history is not persisted by the server or Kafka. The page uses Bootstrap 5.0.2 tabs: the Send message tab contains the form, while the Message history tab renders a responsive table with Resend controls and headers. Resend repopulates topic, broker, headers, and message, then activates the send tab without sending automatically. Application JavaScript lives in `src/main/resources/templates/static/index.js` and is served at `/static/index.js` through the resource handler in `ThymeleafConfig`.
- `application.properties` sets port 8080, Thymeleaf behavior, logging levels, and a default producer retry count. The service currently overrides the relevant producer settings when constructing its dynamic producer, so check both files when changing Kafka behavior.
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
