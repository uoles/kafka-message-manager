# Спецификация: web-интерфейс аутентификации

**Дата:** 2026-08-04  
**Статус:** Предлагаемая спецификация  
**Связанная функциональность:** `.claude/specs/2026-08-04-add-spring-boot-security.md`

## 1. Цель

Добавить в существующий Thymeleaf/Bootstrap web-интерфейс страницы регистрации, входа и выхода пользователя, 
совместимые с локальной Spring Security/JWT-аутентификацией.

После реализации пользователь должен иметь возможность:

- открыть страницу входа без JWT;
- зарегистрировать новую учётную запись с ролью `USER`;
- войти через `POST /api/v1/auth/login`;
- безопасно использовать полученный JWT для вызовов `/api/kafka/**`;
- открыть защищённый интерфейс Kafka после успешного входа;
- увидеть понятные ошибки регистрации, входа, `401` и `403`;
- выйти из web-интерфейса и удалить клиентское состояние аутентификации.

## 2. Текущее состояние

Существующий web-интерфейс:

- контролируется `WebController`;
- отображается Thymeleaf-шаблоном `src/main/resources/templates/index.html`;
- доступен по `/web/`, `/web/index`, `/web/send-message`;
- использует классические JavaScript-файлы в `src/main/resources/templates/static/`;
- вызывает REST API с адресами `/api/kafka/send` и `/api/kafka/consumers/**`;
- хранит историю сообщений в `localStorage` под ключом `kafkaMessageHistory`.

Security API из связанной спецификации:

- `POST /api/v1/auth/register`;
- `POST /api/v1/auth/login`;
- защищённые API требуют `Authorization: Bearer <JWT>`;
- роли: `USER`, `MODERATOR`, `ADMIN`.

## 3. Архитектурное решение

Использовать отдельную публичную страницу `/web/login` и stateless JWT bearer flow:

- страницы `/web/login` и `/web/register` доступны без аутентификации;
- после login JWT хранится в `sessionStorage`, а не в URL или `localStorage`;
- API wrapper добавляет `Authorization: Bearer <token>` к same-origin API-запросам;
- при `401` token удаляется, пользователь перенаправляется на `/web/login`;
- logout очищает JWT, сведения текущего пользователя и историю текущей browser session;
- refresh token в рамках этой спецификации не добавляется;
- cookies для authentication не использовать в этой итерации, чтобы не смешивать stateless bearer flow с CSRF-моделью.

### Ограничения решения

`sessionStorage` доступен JavaScript-коду текущего origin и поэтому требует:

- HTTPS в production;
- строгого CSP;
- Thymeleaf escaping всех пользовательских значений;
- отсутствия стороннего скрипта, которому не доверяет приложение;
- запрета логирования токена.

Переход на HttpOnly SameSite cookie должен быть отдельной спецификацией с CSRF token strategy.

## 4. Маршруты web-интерфейса

Добавить в `WebController`:

- `GET /web/login` → `login`;
- `GET /web/register` → `register`;
- существующие `/web/`, `/web/index`, `/web/send-message` сохранить;
- при отсутствии authentication для защищённых web-страниц направлять на `/web/login` или возвращать согласованный 
browser response;
- после login возвращать пользователя на первоначально запрошенный маршрут, если это безопасный same-origin URL.

Не делать `/` публичным alias без отдельного решения: README и существующий `WebController` сейчас не дают явного 
mapping для bare root.

## 5. Страница входа

Создать:

```text
src/main/resources/templates/login.html
```

Страница должна содержать:

- поле `username` с `autocomplete="username"`;
- поле `password` с `autocomplete="current-password"`;
- кнопку входа;
- ссылку на `/web/register`;
- область доступных для пользователя сообщений об ошибке;
- состояние loading/disabled для кнопки;
- доступную HTML-разметку: label, `aria-live`, keyboard navigation, focus management;
- Bootstrap 5 styling в стиле текущего `index.html`.

Алгоритм login:

1. запретить обычную отправку формы;
2. проверить обязательные поля на клиенте;
3. отправить JSON на `/api/v1/auth/login`;
4. проверить HTTP status и структуру ответа;
5. сохранить только `accessToken`, `tokenType`, `expiresIn` и безопасные user fields в `sessionStorage`;
6. не сохранять password, response body целиком или error stack trace;
7. перенаправить на `returnUrl` только если это same-origin относительный путь; иначе использовать `/web/send-message`;
8. при `401` показать единое сообщение `Неверное имя пользователя или пароль` без username enumeration;
9. при сетевой ошибке показать нейтральное сообщение о недоступности сервиса.

## 6. Страница регистрации

Создать:

```text
src/main/resources/templates/register.html
```

Поля:

- `username`, `autocomplete="username"`;
- `displayName`, если поле поддерживается API;
- `password`, `autocomplete="new-password"`;
- `passwordConfirmation`, только клиентское поле;
- согласованное сообщение о минимальной длине пароля — не менее 12 символов;
- кнопка регистрации;
- ссылка на `/web/login`.

Алгоритм регистрации:

1. проверить username/password/passwordConfirmation на клиенте;
2. не отправлять `passwordConfirmation` на сервер;
3. отправить JSON на `/api/v1/auth/register`;
4. при `201` показать успешное сообщение и перейти на login либо автоматически использовать безопасный token response 
согласно API-контракту;
5. предпочтительно направлять на login с предварительно заполненным только username, но не password;
6. при `409` показать нейтральную ошибку о невозможности регистрации username;
7. при `400` отображать ошибки валидации без внутренних деталей;
8. не отображать и не сохранять password/hash/token в HTML, URL, localStorage или логах.

Публичная регистрация не должна позволять выбрать роль. UI не должен содержать поле role.

## 7. Auth state и JavaScript

Добавить отдельный классический script:

```text
src/main/resources/templates/static/auth.js
```

или расширить `app-state.js`, сохранив существующий порядок загрузки.

Предлагаемый API:

```javascript
AuthState.getToken()
AuthState.getUser()
AuthState.isAuthenticated()
AuthState.save(response)
AuthState.clear()
AuthState.loginUrl(returnUrl)
AuthState.redirectToLogin()
AuthState.authenticatedFetch(url, options)
```

Требования:

- `sessionStorage` key names должны быть константами;
- не принимать token из query string или fragment;
- не добавлять Authorization к внешним CDN-запросам;
- добавлять header только для same-origin `/api/**`;
- не мутировать caller-owned `options` неожиданно;
- при `401` выполнить одно перенаправление без redirect loop;
- при `403` показать сообщение о недостаточных правах;
- при logout очистить auth state;
- не выводить token в `console.log`, DOM или error text;
- корректно работать с пустым/повреждённым `sessionStorage`.

Существующие `form.js`, `consumers.js` и другие API-вызовы должны использовать общий auth-aware wrapper, 
а не собирать Authorization header независимо.

## 8. Изменения главной страницы

Обновить `index.html`:

- показать имя пользователя и роли, если они есть в auth state;
- добавить кнопку `Logout`;
- скрывать или отключать consumer controls для `USER`;
- показывать consumer controls для `MODERATOR`/`ADMIN`;
- не считать visibility control заменой server-side authorization;
- добавить `auth.js` до `form.js`, `consumers.js` и `init.js` либо обеспечить эквивалентный порядок;
- при открытии страницы без token не показывать рабочую форму дольше, чем необходимо для redirect;
- при открытии с истёкшим token корректно перевести пользователя на login.

Роль должна использоваться только для UI-удобства. Все права остаются защищёнными Spring Security и `@PreAuthorize` на сервере.

## 9. Обработка ошибок

UI должен различать:

| HTTP | Поведение |
|---|---|
| `400` | Показать ошибку валидации запроса |
| `401` | Очистить token и перейти на login |
| `403` | Показать отсутствие прав, token не удалять автоматически |
| `409` | Показать конфликт регистрации |
| `5xx` | Показать нейтральную ошибку сервера |
| network error | Показать сообщение о недоступности сервиса |

Не отображать пользователю:

- stack trace;
- SQL;
- Java exception class names;
- Kafka connection details;
- password hash;
- JWT contents;
- private key or security configuration.

## 10. Безопасность браузера

Обязательные требования:

- HTTPS в production;
- CSP, совместимая с Bootstrap и локальными static scripts;
- `X-Frame-Options: DENY`;
- `X-Content-Type-Options: nosniff`;
- безопасное escaping пользовательских значений;
- никакого `innerHTML` с неэкранированным API/history data;
- не хранить auth token в `localStorage`;
- при logout очищать auth-related state и пользовательскую историю;
- не использовать third-party JavaScript для auth state;
- не передавать credentials в URL;
- не логировать формы, password и response tokens.

Если browser history остаётся в `localStorage`, ключ должен быть scoped к нормализованному user ID/username либо 
история должна очищаться при смене пользователя. Нельзя показывать предыдущему пользователю данные другого пользователя
на общем компьютере.

## 11. Server-side изменения

Изменить `WebController`:

- добавить mappings `/web/login` и `/web/register`;
- добавить Javadoc на новые methods;
- не выполнять authentication в controller;
- сохранить controller thin.

Изменить `SecurityConfig`:

- разрешить `/web/login`, `/web/register`, их static assets и auth API;
- защищать `/web/`, `/web/index`, `/web/send-message`;
- для browser navigation использовать redirect на login, а для `/api/**` — JSON `401`;
- исключить redirect loop для `/web/login`, `/web/register`, `/api/v1/auth/**`.

## 12. Тестовая стратегия

### MVC tests

Добавить `WebAuthControllerTest`:

- `/web/login` возвращает view `login`;
- `/web/register` возвращает view `register`;
- защищённые web routes без authentication дают redirect/response согласно SecurityConfig;
- auth pages доступны anonymous;
- static auth script доступен;
- неизвестные web routes дают `404`.

### JavaScript tests

Если в проект добавляется JS test tooling, покрыть:

- save/read/clear auth state;
- malformed storage data;
- Authorization header для same-origin API;
- отсутствие header для CDN/external URL;
- один redirect при `401`;
- handling `403`, `409`, `400`, network failures;
- logout cleanup;
- safe returnUrl validation;
- password confirmation validation;
- отсутствие token/password в DOM и logs.

Если JS test tooling не добавляется, выполнить browser/manual smoke checklist и зафиксировать его в README.

### Integration tests

Проверить через MockMvc:

- anonymous GET login/register;
- anonymous protected page redirect;
- login API token can call protected Kafka API;
- invalid token redirects/returns appropriate response;
- role USER does not see/use consumer controls and receives server-side `403`;
- MODERATOR/ADMIN can use consumer controls if backend role restrictions permit;
- logout makes browser state unauthenticated.

## 13. README и документация

Обновить README:

- `/web/login` и `/web/register`;
- login/register workflow;
- sessionStorage warning;
- logout behavior;
- required HTTPS/CSP production settings;
- role-dependent UI behavior;
- distinction between UI hiding and server-side authorization.

## 14. Критерии готовности

- Login и registration pages доступны без JWT.
- Главная Kafka page защищена и перенаправляет anonymous user на login.
- Login page получает JWT через `/api/v1/auth/login`.
- JWT не попадает в URL, localStorage, DOM или logs.
- API requests отправляют Bearer header только для same-origin API.
- `401` очищает token и переводит на login.
- `403` показывает понятное сообщение без очистки действительного token.
- Logout очищает token, user state и user-scoped browser history.
- UI корректно отображает роли, но server-side authorization остаётся обязательным.
- Password confirmation никогда не отправляется на API.
- MVC/security tests проходят.
- README и `.claude/CLAUDE.md` описывают новый web auth flow.
- В production используются HTTPS и CSP.

## 15. Порядок реализации

1. Добавить `WebController` mappings для `/web/login` и `/web/register`.
2. Создать Thymeleaf templates `login.html` и `register.html`.
3. Создать `auth.js` и общий authenticated fetch wrapper.
4. Подключить auth script к login/register/index templates.
5. Обновить `form.js`, `consumers.js`, `history.js` и `init.js` для auth-aware поведения.
6. Обновить `SecurityConfig` для anonymous auth pages и browser redirect/JSON API distinction.
7. Реализовать role-aware UI controls без замены server authorization.
8. Добавить MVC/security tests и manual browser smoke tests.
9. Обновить README и `.claude/CLAUDE.md`.
10. Выполнить `mvn test`, затем проверить login → protected page → API call → logout flow в браузере.
