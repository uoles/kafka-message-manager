# Спецификация: добавление Spring Security и ролевой аутентификации

**Дата:** 2026-08-04  
**Статус:** Предлагаемая спецификация  
**Проект:** `kafka-message-manager`

## 1. Цель и ожидаемый результат

Добавить в приложение аутентификацию и авторизацию на базе Spring Boot 3.5 / Spring Security 6 с хранением пользователей
и ролей в существующей SQLite-базе данных. Схема базы данных должна создаваться и изменяться только через Liquibase.

После реализации:

- пользователь может зарегистрироваться и войти в приложение;
- защищённые API- и web-ресурсы доступны только аутентифицированным пользователям;
- права ограничиваются ролями `ADMIN`, `USER` и `MODERATOR`;
- пароли хранятся только в виде BCrypt-хэшей;
- данные пользователей, ролей и связей пользователей с ролями сохраняются в SQLite;
- изменения схемы воспроизводимы при запуске через Liquibase;
- неаутентифицированные и неавторизованные запросы получают предсказуемые HTTP-ответы без раскрытия внутренних деталей.

## 2. Область изменений

### Входит в задачу

- зависимости Spring Security и JWT/OAuth2 Resource Server либо согласованный серверный механизм выдачи токенов;
- доменная модель пользователя и роли;
- таблицы пользователей, ролей и связи пользователей с ролями;
- Liquibase changelog и rollback для новой схемы;
- endpoint регистрации и endpoint входа;
- `UserDetailsService`/репозиторий пользователей;
- `PasswordEncoder` на BCrypt;
- `SecurityFilterChain`;
- правила доступа к API и web-интерфейсу;
- методовая авторизация через `@PreAuthorize` для операций, требующих конкретной роли;
- тесты конфигурации безопасности, регистрации, входа, ролевых ограничений и миграций;
- документация API и переменных окружения.

### Не входит в задачу

- внешняя OAuth2/Identity-платформа и федерация пользователей;
- восстановление пароля по электронной почте;
- MFA/2FA;
- полноценная административная панель управления пользователями;
- хранение паролей в открытом виде или обратимое шифрование паролей;
- изменение Kafka-протокола и структуры таблиц `consumers`/`received_messages`, если это не требуется правилами доступа.

## 3. Рекомендуемая архитектура

### 3.1. Аутентификация

Для stateless JSON API использовать JWT Bearer tokens:

- `POST /api/auth/register` — создаёт пользователя и возвращает безопасный результат регистрации;
- `POST /api/auth/login` — проверяет логин и пароль и возвращает access token;
- остальные `/api/**` требуют `Authorization: Bearer <token>`;
- JWT signing secret или ключи передаются через переменные окружения/секреты deployment-среды, а не хранятся в Git;
- срок жизни access token ограничивается конфигурацией, например `15 минут`;
- refresh token не добавлять в первую итерацию без отдельного требования. Если refresh token понадобится, его следует хранить в БД в хэшированном/отозванном состоянии и описать отдельной спецификацией.

Для Thymeleaf web-интерфейса выбрать один согласованный режим:

1. **Рекомендуемый для текущего приложения:** session-based login с form login для `/web/**`, если браузерная UI должна работать напрямую без JavaScript-хранилища JWT.
2. JWT Bearer для `/api/**` оставить stateless.

Security-конфигурация должна явно разделять API и web-цепочки либо иметь единый фильтр с ясными правилами. 
Нельзя помещать JWT в URL или логировать токены.

### 3.2. Пользователи и роли

Роли представить отдельной сущностью/таблицей и связывать с пользователями через таблицу many-to-many:

- `ADMIN` — полный доступ к управлению пользователями, ролями и системными операциями;
- `MODERATOR` — управление динамическими consumer-ресурсами и просмотр/модерация данных в пределах API;
- `USER` — отправка сообщений, проверка health и доступ к разрешённым собственным пользовательским операциям.

В Spring Security authorities использовать единый формат `ROLE_ADMIN`, `ROLE_MODERATOR`, `ROLE_USER`, при этом в БД 
хранить базовое имя роли без префикса или зафиксировать другой формат в одном месте. Не смешивать `ADMIN` и `ROLE_ADMIN` в проверках.

### 3.3. Начальный администратор

Не создавать встроенного администратора с известным паролем в исходном коде. Предусмотреть один из безопасных вариантов:

- отдельный bootstrap-механизм, принимающий логин и пароль через секреты окружения при первом запуске;
- ручной SQL/операционный скрипт, который получает заранее вычисленный BCrypt-хэш;
- одноразовая команда администратора.

Выбранный вариант должен быть явно зафиксирован до реализации и не должен выводить пароль или JWT в логи.

## 4. Предлагаемая структура компонентов

Пакеты сохранять под `ru.uoles.kafka.sender`:

```text
config/
  SecurityConfig.java
  SecurityProperties.java
  JwtProperties.java
controller/
  AuthController.java
  ...
model/
  RegisterRequest.java
  LoginRequest.java
  AuthResponse.java
  UserResponse.java
security/
  model/
    SecurityUser.java
  service/
    AuthService.java
    AuthServiceImpl.java
    JwtTokenService.java
    JwtTokenServiceImpl.java
    CustomUserDetailsService.java
    CustomUserDetailsServiceImpl.java
  repository/
    UserRepository.java
    RoleRepository.java
```

Названия могут быть адаптированы к существующей структуре `kafka.repository`, `kafka.service` и DTO-конвенциям проекта, но границы ответственности сохранить:

- controller — HTTP, валидация и коды ответа;
- service — регистрация, вход, назначение ролей и бизнес-правила;
- repository — SQL через `JdbcTemplate`;
- config/security — фильтры, encoder, JWT и user details;
- Liquibase — единственный источник DDL.

## 5. API-контракт

### 5.1. Регистрация

`POST /api/auth/register`

Пример запроса:

```json
{
  "username": "user@example.com",
  "password": "strong-password",
  "displayName": "Иван"
}
```

Требования:

- `username` обязателен, нормализуется по согласованному правилу и должен быть уникальным;
- пароль не менее 12 символов, не возвращается и не логируется;
- публичная регистрация назначает только `USER`;
- роль `ADMIN` или `MODERATOR` нельзя указать в теле публичного запроса;
- повторный username возвращает `409 Conflict` без раскрытия лишних сведений;
- ответ не должен содержать пароль или password hash.

### 5.2. Вход

`POST /api/auth/login`

Пример запроса:

```json
{
  "username": "user@example.com",
  "password": "strong-password"
}
```

Успешный ответ `200 OK`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "<uuid>",
    "username": "user@example.com",
    "roles": ["USER"]
  }
}
```

Неверные credentials должны возвращать единый `401 Unauthorized`, не сообщая, существует ли username.

### 5.3. Ошибки безопасности

- отсутствие credentials: `401 Unauthorized`;
- истёкший/невалидный JWT: `401 Unauthorized`;
- корректный пользователь без требуемой роли: `403 Forbidden`;
- ошибки должны иметь единый JSON-формат с timestamp и message без stack trace;
- в production не возвращать исключения, SQL и внутреннюю конфигурацию.

## 6. Матрица доступа

Начальная матрица должна быть уточнена на этапе реализации и покрыта тестами:

| Ресурс | USER | MODERATOR | ADMIN |
|---|---:|---:|---:|
| `POST /api/auth/register` | public | public | public |
| `POST /api/auth/login` | public | public | public |
| `GET /api/kafka/health` | authenticated | authenticated | authenticated |
| `POST /api/kafka/send` | разрешено | разрешено | разрешено |
| `GET /api/kafka/consumers` | запрещено | разрешено | разрешено |
| `GET /api/kafka/consumers/{id}` | запрещено | разрешено | разрешено |
| `GET /api/kafka/consumers/{id}/messages` | запрещено | разрешено | разрешено |
| `POST /api/kafka/consumers` | запрещено | разрешено | разрешено |
| `DELETE /api/kafka/consumers/{id}` | запрещено | разрешено | разрешено |
| управление пользователями/ролями | запрещено | запрещено | разрешено |
| `/web/**` | authenticated | authenticated | authenticated |
| статические ресурсы и страницы входа | public | public | public |

Если требуется ownership-модель для consumer-ресурсов, добавить `owner_id` в таблицу consumers отдельной миграцией и проверять ownership в service layer, а не только в URL.

## 7. Схема SQLite и Liquibase

Добавить отдельные formatted SQL migrations в `src/main/resources/liquibase/scripts/tables/` и включить их в `liquibase/changelog-master.xml`. Не создавать таблицы через Java-конфигурацию или `schema.sql`.

Минимальная схема:

```sql
users (
    id TEXT PRIMARY KEY NOT NULL,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_name TEXT,
    enabled INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
)

roles (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL UNIQUE
)

user_roles (
    user_id TEXT NOT NULL,
    role_id TEXT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
)
```

Требования к миграциям:

- UUID хранить в согласованном формате, совместимом с существующими SQLite-repository;
- timestamps хранить в едином формате проекта, предпочтительно epoch milliseconds;
- включить индексы для `users.username`, `user_roles.role_id` и внешних ключей;
- включить `PRAGMA foreign_keys=ON` в существующую инфраструктуру SQLite;
- добавить rollback для каждого changeset;
- seed ролей `USER`, `MODERATOR`, `ADMIN` должен быть идемпотентным (`INSERT ... WHERE NOT EXISTS` или эквивалент);
- не добавлять пароль администратора в migration;
- проверить запуск на чистой БД и upgrade существующей БД.

Пример порядка включения:

```xml
<include file="scripts/tables/TABLE.USERS.sql" relativeToChangelogFile="true"/>
<include file="scripts/tables/TABLE.ROLES.sql" relativeToChangelogFile="true"/>
<include file="scripts/tables/TABLE.USER_ROLES.sql" relativeToChangelogFile="true"/>
```

Можно объединить таблицы в одну migration только если это соответствует текущей структуре changeset-ов; 
отдельные таблицы предпочтительнее для rollback и сопровождения.

## 8. Конфигурация

Добавить внешние настройки, например:

```properties
security.jwt.issuer=${SECURITY_JWT_ISSUER:kafka-message-manager}
security.jwt.access-token-ttl=PT15M
security.jwt.secret=${SECURITY_JWT_SECRET:}
security.registration.enabled=${SECURITY_REGISTRATION_ENABLED:true}
```

Требования:

- в production `SECURITY_JWT_SECRET` обязателен и должен иметь достаточную энтропию;
- не использовать слабый дефолтный secret в production;
- не коммитить credentials;
- добавить профиль/проверку конфигурации для dev и production;
- отключение CSRF допустимо только для stateless API; для session-based web form login CSRF должен оставаться включённым или иметь обоснованную защиту;
- включить security headers: `X-Content-Type-Options`, `X-Frame-Options`, HSTS при HTTPS и CSP с учётом Thymeleaf/Bootstrap;
- CORS не разрешать глобально: origins, methods и headers должны быть внешней конфигурацией.

## 9. Безопасность и эксплуатационные требования

- BCrypt с настроенным work factor, соответствующим production latency;
- rate limiting или throttling для login/register рассмотреть как обязательное deployment-требование;
- не логировать пароль, access token, Authorization header и password hash;
- при логировании username учитывать, что это потенциально персональные данные;
- защита от username enumeration через единые сообщения и статус login-ошибок;
- ограничить размер request body и валидировать все поля;
- отключить открытый actuator, оставить только необходимые health/readiness endpoints;
- проверить, что consumer API не позволяет горизонтально эскалировать права через UUID;
- ревизия ошибок и audit logging для выдачи ролей, блокировки пользователя и административных операций.

## 10. Тестовая стратегия

Добавить unit и MVC-тесты без запуска Kafka:

### Unit-тесты

- регистрация хэширует пароль BCrypt и не сохраняет plaintext;
- duplicate username отклоняется;
- публичная регистрация всегда получает `USER`;
- login с правильным паролем создаёт JWT;
- неверный пароль и неизвестный пользователь дают одинаковую ошибку;
- disabled пользователь не проходит аутентификацию;
- role mapping преобразует БД-роли в `ROLE_*` корректно;
- JWT service проверяет issuer, subject, срок действия и authorities;
- repository выполняет ожидаемые SQLite-запросы и обработку пустого результата.

### MockMvc/Security-тесты

- public register/login доступны без token;
- защищённые endpoints без token возвращают `401`;
- `USER` может отправлять сообщения, но получает `403` при consumer administration;
- `MODERATOR` может управлять consumer endpoints, но не управлять ролями пользователей;
- `ADMIN` имеет административный доступ;
- invalid/expired token возвращает `401`;
- ошибки не содержат stack trace, SQL, hash или secret;
- CSRF/CORS/security headers соответствуют выбранной API/web-модели.

### Liquibase/интеграционные проверки

- миграции применяются на чистой SQLite-базе;
- миграции применяются поверх текущих таблиц `consumers` и `received_messages` без повреждения данных;
- rollback удаляет только добавленные security-объекты;
- повторный запуск Liquibase идемпотентен;
- foreign keys и cascade delete работают для `user_roles`.

## 11. План реализации

1. Уточнить режим web-аутентификации и способ bootstrap первого администратора.
2. Добавить зависимости Spring Security/JWT и конфигурационные properties.
3. Добавить Liquibase changeset-ы для пользователей, ролей, связей и seed ролей.
4. Реализовать модели, repository и `CustomUserDetailsService`.
5. Реализовать BCrypt encoder, auth service, registration/login DTO и controller.
6. Реализовать JWT выдачу/проверку и `SecurityFilterChain`.
7. Включить `@EnableMethodSecurity` и расставить `@PreAuthorize` на чувствительных операциях.
8. Защитить существующие API и web routes согласно матрице доступа; проверить browser flow.
9. Добавить обработчики `401/403`, security headers, CORS/CSRF и конфигурационные проверки.
10. Добавить unit, MockMvc и Liquibase/integration tests.
11. Обновить README, настройки окружения и эксплуатационную документацию.
12. Выполнить `mvn test`, `mvn clean package` и проверить запуск на чистой SQLite-базе.

## 12. Критерии готовности

- Пользователь регистрируется только с ролью `USER` и может войти с корректным паролем.
- Пароль в базе представлен только BCrypt-хэшем.
- JWT содержит идентификатор пользователя и authorities, имеет issuer и ограниченный TTL.
- Все защищённые endpoint-ы возвращают `401/403` согласно матрице.
- `ADMIN`, `MODERATOR` и `USER` реально различаются в проверках доступа.
- SQLite-схема пользователей и ролей создаётся только Liquibase и имеет rollback.
- Существующие Kafka consumer/message таблицы и операции не ломаются.
- Токены, пароли, хэши и секреты не попадают в логи/ответы.
- Unit/MVC/integration tests проходят, включая security-negative cases.
- Документация содержит обязательные environment variables и процедуру создания первого администратора.
