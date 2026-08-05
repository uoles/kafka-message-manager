// Общее состояние приложения и ссылки на элементы страницы.
// Глобальный объект-модуль, общий для всех файлов приложения.
window.KafkaMessageManager = window.KafkaMessageManager || {};

// Ссылки на DOM и изменяемое состояние пользовательского интерфейса.
window.KafkaMessageManager.state = {
    messagesHistory: [],
    form: document.getElementById('kafkaForm'),
    sendBtn: document.getElementById('sendBtn'),
    clearBtn: document.getElementById('clearBtn'),
    clearHistoryBtn: document.getElementById('clearHistoryBtn'),
    historyListDiv: document.getElementById('historyList'),
    headersInput: document.getElementById('headers'),
    consumerForm: document.getElementById('consumerForm'),
    consumerTabs: document.getElementById('consumerTabs'),
    consumers: new Map(),
    consumerTimers: new Map(),
    consumerCursors: new Map(),
    consumerInFlight: new Set(),
    consumerErrors: new Map(),
    consumerMessages: new Map()
};

// Ключ истории сообщений в localStorage. AuthState делает его пользовательским.
window.KafkaMessageManager.storageKey = window.KafkaMessageManager.auth
    ? window.KafkaMessageManager.auth.historyKey()
    : 'kafkaMessageHistory';

// Обновляет ключ истории после входа пользователя.
window.KafkaMessageManager.refreshStorageKey = function () {
    window.KafkaMessageManager.storageKey = window.KafkaMessageManager.auth.historyKey();
};

window.KafkaMessageManager.apiFetch = window.KafkaMessageManager.auth.authenticatedFetch;
window.KafkaMessageManager.showApiError = function (response) {
    if (response && response.status === 403) return 'You do not have permission to perform this action.';
    return 'The request could not be completed.';
};

// Проверяет роль пользователя без привязки к формату роли в JWT-ответе.
window.KafkaMessageManager.hasRole = function (role) {
    const user = window.KafkaMessageManager.auth.getUser();
    return Boolean(user && Array.isArray(user.roles) && user.roles.some(value => value === role || value === `ROLE_${role}`));
};

// Обновляет элементы профиля и оставляет USER доступ к созданию consumer.
window.KafkaMessageManager.updateAuthUi = function () {
    const user = window.KafkaMessageManager.auth.getUser();
    const name = document.getElementById('currentUsername');
    const roles = document.getElementById('currentRoles');
    if (name) name.textContent = user ? user.username : '';
    if (roles) roles.textContent = user && user.roles ? user.roles.join(', ') : '';
    const canCreate = Boolean(user && Array.isArray(user.roles) && user.roles.some(value =>
        ['USER', 'ROLE_USER', 'MODERATOR', 'ROLE_MODERATOR', 'ADMIN', 'ROLE_ADMIN'].includes(value)));
    const controls = document.getElementById('consumerControls');
    if (controls) controls.classList.toggle('d-none', !canCreate);
};

window.KafkaMessageManager.canManageConsumers = function () {
    return window.KafkaMessageManager.hasRole('MODERATOR') || window.KafkaMessageManager.hasRole('ADMIN');
};

window.KafkaMessageManager.canAccessConsumers = function () {
    return Boolean(window.KafkaMessageManager.auth.getUser());
};

window.KafkaMessageManager.updateAuthUi();
