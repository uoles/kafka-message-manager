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

// Ключ истории сообщений в localStorage.
window.KafkaMessageManager.storageKey = 'kafkaMessageHistory';
