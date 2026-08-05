// Работа с историей отправленных сообщений и её отображением.
const app = window.KafkaMessageManager;
const state = app.state;
if (app.auth) app.storageKey = app.auth.historyKey();

// Загружает историю из локального хранилища браузера.
function loadHistory() {
    try {
        const saved = localStorage.getItem(app.storageKey);
        const parsed = saved ? JSON.parse(saved) : [];
        state.messagesHistory = Array.isArray(parsed) ? parsed : [];
    } catch (error) {
        state.messagesHistory = [];
    }
    renderHistory();
}

// Сохраняет текущую историю в локальное хранилище.
function saveHistory() { localStorage.setItem(app.storageKey, JSON.stringify(state.messagesHistory)); }

// Добавляет сообщение в начало истории и обновляет экран.
function addToHistory(message) {
    state.messagesHistory.unshift(message);
    saveHistory();
    renderHistory();
}

// Очищает историю после подтверждения пользователя.
function clearHistory() {
    if (confirm('Are you sure you want to clear all message history?')) {
        state.messagesHistory = [];
        saveHistory();
        renderHistory();
    }
}

// Обновляет счётчики сообщений разных статусов.
function updateStats() {
    document.getElementById('totalCount').textContent = state.messagesHistory.length;
    document.getElementById('successCount').textContent = state.messagesHistory.filter(m => m.status === 'success').length;
    document.getElementById('errorCount').textContent = state.messagesHistory.filter(m => m.status === 'error').length;
}

// Форматирует время сообщения для таблицы истории.
function formatTime(timestamp) {
    return new Date(timestamp).toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

// Возвращает HTML-значок для статуса сообщения.
function getStatusIcon(status) { return status === 'success' ? '&#9989;' : status === 'error' ? '&#10060;' : '&#8505;&#65039;'; }

// Экранирует пользовательский текст перед вставкой в HTML.
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Проверяет, достаточно ли данных для повторной отправки сообщения.
function canResend(message) { return Boolean(message && message.topic && message.kafkaAddress && message.messageText && message.kafkaAddress !== '-'); }

// Перерисовывает таблицу истории и её пустое состояние.
function renderHistory() {
    if (!state.messagesHistory.length) {
        state.historyListDiv.innerHTML = '<div class="text-center text-muted py-5">No messages yet.<br>Send your first message!</div>';
        updateStats();
        return;
    }
    const rows = state.messagesHistory.map((msg, index) => {
        const statusClass = msg.status === 'success' ? 'success' : msg.status === 'error' ? 'danger' : 'info';
        const statusTextClass = msg.status === 'info' ? ' text-dark' : '';
        const resendDisabled = canResend(msg) ? '' : ' disabled';
        return `<tr class="message-row"><td><span class="badge bg-${statusClass}${statusTextClass}">${getStatusIcon(msg.status)} ${escapeHtml((msg.status || 'info').toUpperCase())}</span></td><td class="text-nowrap small text-muted">${formatTime(msg.timestamp)}</td><td class="font-monospace text-break">${escapeHtml(msg.topic)}</td><td class="font-monospace text-break">${escapeHtml(msg.kafkaAddress)}</td><td class="font-monospace text-break">${escapeHtml(msg.headers || '') || '&mdash;'}</td><td><div class="message-text bg-light rounded p-2">${escapeHtml(msg.messageText)}</div></td><td><div class="response-text bg-light rounded p-2">${escapeHtml(msg.responseMessage || '')}</div></td><td class="text-end"><button type="button" class="btn btn-outline-primary btn-sm resend-btn" data-history-index="${index}"${resendDisabled} aria-label="Resend message">Resend</button></td></tr>`;
    }).join('');
    state.historyListDiv.innerHTML = `<div class="table-responsive"><table class="table table-hover align-middle mb-0"><caption class="visually-hidden">Kafka message history</caption><thead class="table-light"><tr><th scope="col">Status</th><th scope="col">Time</th><th scope="col">Topic</th><th scope="col">Kafka address</th><th scope="col">Headers</th><th scope="col">Message</th><th scope="col">Response</th><th scope="col"><span class="visually-hidden">Actions</span></th></tr></thead><tbody>${rows}</tbody></table></div>`;
    updateStats();
    state.historyListDiv.scrollTop = 0;
}

// Показывает информационное сообщение и удаляет его через пять секунд.
function showTemporaryMessage(type, text) {
    const tempMessage = { status: type, topic: 'System', kafkaAddress: '-', messageText: text, responseMessage: text, timestamp: Date.now() };
    addToHistory(tempMessage);
    setTimeout(() => {
        const index = state.messagesHistory.indexOf(tempMessage);
        if (index !== -1) {
            state.messagesHistory.splice(index, 1);
            saveHistory();
            renderHistory();
        }
    }, 5000);
}

app.history = { loadHistory, saveHistory, addToHistory, clearHistory, renderHistory, showTemporaryMessage, canResend };
app.escapeHtml = escapeHtml;
