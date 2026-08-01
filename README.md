# Kafka Message Manager

Небольшое Spring Boot-приложение для отправки сообщений в Kafka и управления динамическими Kafka-потребителями через REST API и веб-интерфейс на Thymeleaf и Bootstrap.

## Требования

- JDK 23;
- Maven;
- Docker Compose — для запуска локального Kafka-стека.

Приложение использует Spring Boot 3.5 и собирается Maven. Исходный код находится в пакете `ru.uoles.kafka.sender`.

## Запуск локального Kafka

Запустите Zookeeper, Kafka и Kafka UI:

```bash
docker compose -f docker/kafka/docker-compose.yml up -d
```

Параметры локального стека:

- Kafka для подключения с хоста: `localhost:29092`;
- Kafka внутри Docker-сети: `kafka:9092`;
- Kafka UI: <http://localhost:8089/>;
- Zookeeper: `localhost:22181`.

Остановить стек можно командой:

```bash
docker compose -f docker/kafka/docker-compose.yml down
```

## Запуск приложения

```bash
mvn spring-boot:run
```

Приложение использует порт `8080`.

Веб-интерфейс доступен по адресам:

- <http://localhost:8080/web/>;
- <http://localhost:8080/web/index>;
- <http://localhost:8080/web/send-message>.

Корневой адрес <http://localhost:8080/> отдельным контроллером не обрабатывается.

## Сборка и проверка

```bash
# Запустить тесты
mvn test

# Собрать приложение и выполнить проверку
mvn clean package

# Запустить отдельный тестовый класс
mvn -Dtest=ClassNameTest test

# Запустить отдельный тестовый метод
mvn -Dtest=ClassNameTest#methodName test
```

В проекте подключен `spring-boot-starter-test`, однако исходники тестов в `src/test` пока отсутствуют. Отдельный lint- или formatting-плагин в Maven не настроен.

## REST API

Базовый путь API: `/api/kafka`.

### Проверка состояния

```http
GET /api/kafka/health
```

Пример:

```bash
curl http://localhost:8080/api/kafka/health
```

### Отправка сообщения

```http
POST /api/kafka/send
Content-Type: application/json
```

Тело запроса:

```json
{
  "topic": "TEST_TOPIC",
  "kafkaAddress": "localhost:29092",
  "messageText": "Пример сообщения",
  "headers": "source=readme,environment=local"
}
```

Поля `topic`, `kafkaAddress` и `messageText` обязательны. Поле `headers` необязательно и принимает Kafka-заголовки в формате:

```text
name=value,name2=value2
```

Имена и значения заголовков не должны быть пустыми, а имена должны быть уникальными. Значения передаются в Kafka в UTF-8.

Пример через `curl`:

```bash
curl -X POST http://localhost:8080/api/kafka/send \
  -H "Content-Type: application/json" \
  -d '{"topic":"TEST_TOPIC","kafkaAddress":"localhost:29092","messageText":"Пример сообщения","headers":"source=readme"}'
```

Producer создаётся для каждого запроса и использует адрес Kafka из тела запроса. Поэтому приложение может отправлять сообщения в разные Kafka-кластеры без изменения конфигурации приложения. Для отправки в локальный Docker Kafka с хоста используйте `localhost:29092`.

### Управление Kafka-потребителями

Создать потребителя:

```http
POST /api/kafka/consumers
Content-Type: application/json
```

```json
{
  "bootstrapAddress": "localhost:29092",
  "topic": "TEST_TOPIC"
}
```

Получить список потребителей:

```http
GET /api/kafka/consumers
```

Получить одного потребителя:

```http
GET /api/kafka/consumers/{id}
```

Получить сообщения потребителя:

```http
GET /api/kafka/consumers/{id}/messages?after=0&limit=100
```

Параметр `after` — исключающий локальный курсор: возвращаются сообщения с последовательностью больше указанного значения. Ответ также содержит:

- `oldestSequence` — самая старая последовательность, доступная в буфере;
- `nextSequence` — следующая последовательность, которая будет назначена сообщению;
- `droppedCount` — накопленное число сообщений, вытесненных из ограниченного буфера.

Удалить потребителя:

```http
DELETE /api/kafka/consumers/{id}
```

Приложение ограничивает число одновременно работающих потребителей и размер их оперативных буферов. При перезапуске сохранённые определения потребителей и журнал полученных сообщений восстанавливаются. Удаление потребителя удаляет его конфигурацию и связанные сообщения.

## Хранение данных

Конфигурации потребителей и полученные Kafka-сообщения сохраняются в SQLite:

```text
jdbc:sqlite:database/kafka-info.db
```

Схема управляется Liquibase:

- основной changelog: `src/main/resources/liquibase/changelog-master.xml`;
- таблица потребителей: `consumers`;
- таблица полученных сообщений: `received_messages`;
- миграция журнала сообщений: `src/main/resources/liquibase/scripts/tables/TABLE.RECEIVED_MESSAGES.sql`.

Не создавайте таблицы вручную в Java-коде — изменения схемы должны оформляться Liquibase-миграциями.

История отправленных через веб-форму сообщений не сохраняется на сервере. Она хранится в браузере в `localStorage` под ключом `kafkaMessageHistory`. Кнопка повторной отправки только заполняет форму данными из истории и не отправляет сообщение автоматически.

## Веб-интерфейс

Страница содержит три вкладки:

- **Отправка сообщения** — топик, адрес брокера, текст и необязательные заголовки;
- **История сообщений** — локальная история отправок с возможностью повторно заполнить форму;
- **Consumers** — создание потребителей и просмотр полученных сообщений.

JavaScript-файлы находятся в `src/main/resources/templates/static/` и загружаются в следующем порядке:

1. `app-state.js`;
2. `history.js`;
3. `consumers.js`;
4. `form.js`;
5. `init.js`.

Bootstrap 5.0.2 подключается из CDN до этих скриптов.

## Структура проекта

- `controller` — REST- и web-контроллеры;
- `model` — DTO и ответы API;
- `kafka/service` — отправка сообщений и сервисы работы с данными потребителей;
- `kafka/consumer` — динамические Kafka-контейнеры и буферы сообщений;
- `kafka/repository` — JDBC-репозитории SQLite;
- `config` — конфигурация Thymeleaf и JDBC;
- `src/main/resources/liquibase` — миграции базы данных;
- `src/main/resources/templates` — Thymeleaf-шаблон и browser UI.

Дополнительные правила разработки находятся в `.claude/rules/`, а подробные инструкции для Claude Code — в `.claude/CLAUDE.md`.
