let messagesHistory = [];
const form = document.getElementById('kafkaForm');
const sendBtn = document.getElementById('sendBtn');
const clearBtn = document.getElementById('clearBtn');
const clearHistoryBtn = document.getElementById('clearHistoryBtn');
const historyListDiv = document.getElementById('historyList');
const headersInput = document.getElementById('headers');

function loadHistory() {
    try {
        const saved = localStorage.getItem('kafkaMessageHistory');
        const parsed = saved ? JSON.parse(saved) : [];
        messagesHistory = Array.isArray(parsed) ? parsed : [];
    } catch (error) {
        messagesHistory = [];
    }
    renderHistory();
}

function saveHistory() { localStorage.setItem('kafkaMessageHistory', JSON.stringify(messagesHistory)); }

function addToHistory(message) {
    messagesHistory.unshift(message);
    saveHistory();
    renderHistory();
}

function clearHistory() {
    if (confirm('Are you sure you want to clear all message history?')) {
        messagesHistory = [];
        saveHistory();
        renderHistory();
    }
}

function updateStats() {
    document.getElementById('totalCount').textContent = messagesHistory.length;
    document.getElementById('successCount').textContent = messagesHistory.filter(m => m.status === 'success').length;
    document.getElementById('errorCount').textContent = messagesHistory.filter(m => m.status === 'error').length;
}

function formatTime(timestamp) {
    return new Date(timestamp).toLocaleString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

function getStatusIcon(status) { return status === 'success' ? '&#9989;' : status === 'error' ? '&#10060;' : '&#8505;&#65039;'; }

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function canResend(message) {
    return Boolean(message && message.topic && message.kafkaAddress && message.messageText && message.kafkaAddress !== '-');
}

function renderHistory() {
    if (!messagesHistory.length) {
        historyListDiv.innerHTML = '<div class="text-center text-muted py-5">No messages yet.<br>Send your first message!</div>';
        updateStats();
        return;
    }

    const rows = messagesHistory.map((msg, index) => {
        const statusClass = msg.status === 'success' ? 'success' : msg.status === 'error' ? 'danger' : 'info';
        const statusTextClass = msg.status === 'info' ? ' text-dark' : '';
        const resendDisabled = canResend(msg) ? '' : ' disabled';
        return `<tr class="message-row">
            <td><span class="badge bg-${statusClass}${statusTextClass}">${getStatusIcon(msg.status)} ${escapeHtml((msg.status || 'info').toUpperCase())}</span></td>
            <td class="text-nowrap small text-muted">${formatTime(msg.timestamp)}</td>
            <td class="font-monospace text-break">${escapeHtml(msg.topic)}</td>
            <td class="font-monospace text-break">${escapeHtml(msg.kafkaAddress)}</td>
            <td class="font-monospace text-break">${escapeHtml(msg.headers || '') || '&mdash;'}</td>
            <td><div class="message-text bg-light rounded p-2">${escapeHtml(msg.messageText)}</div></td>
            <td><div class="response-text bg-light rounded p-2">${escapeHtml(msg.responseMessage || '')}</div></td>
            <td class="text-end"><button type="button" class="btn btn-outline-primary btn-sm resend-btn" data-history-index="${index}"${resendDisabled} aria-label="Resend message">Resend</button></td>
        </tr>`;
    }).join('');

    historyListDiv.innerHTML = `<div class="table-responsive"><table class="table table-hover align-middle mb-0"><caption class="visually-hidden">Kafka message history</caption><thead class="table-light"><tr><th scope="col">Status</th><th scope="col">Time</th><th scope="col">Topic</th><th scope="col">Kafka address</th><th scope="col">Headers</th><th scope="col">Message</th><th scope="col">Response</th><th scope="col"><span class="visually-hidden">Actions</span></th></tr></thead><tbody>${rows}</tbody></table></div>`;
    updateStats();
    historyListDiv.scrollTop = 0;
}

function showTemporaryMessage(type, text) {
    const tempMessage = { status: type, topic: 'System', kafkaAddress: '-', messageText: text, responseMessage: text, timestamp: Date.now() };
    addToHistory(tempMessage);
    setTimeout(() => {
        const index = messagesHistory.indexOf(tempMessage);
        if (index !== -1) {
            messagesHistory.splice(index, 1);
            saveHistory();
            renderHistory();
        }
    }, 5000);
}

function clearForm() {
    document.getElementById('topic').value = '';
    document.getElementById('kafkaAddress').value = 'localhost:9092';
    document.getElementById('messageText').value = '';
    headersInput.value = '';
    showTemporaryMessage('info', 'Form has been cleared');
}

function validateForm() {
    const topic = document.getElementById('topic').value.trim();
    const kafkaAddress = document.getElementById('kafkaAddress').value.trim();
    const messageText = document.getElementById('messageText').value.trim();
    const headers = headersInput.value.trim();
    if (headers && !headers.split(',').every(entry => {
        const separator = entry.indexOf('=');
        return separator > 0 && entry.substring(0, separator).trim() && entry.substring(separator + 1).trim();
    })) {
        showTemporaryMessage('error', 'Invalid Kafka headers. Expected: name=value,name2=value2');
        return false;
    }
    if (!topic) { showTemporaryMessage('error', 'Please enter a topic name'); return false; }
    if (!kafkaAddress) { showTemporaryMessage('error', 'Please enter a Kafka address'); return false; }
    if (!messageText) { showTemporaryMessage('error', 'Please enter a message text'); return false; }
    if (!/^[a-zA-Z0-9.-]+:\d+$/.test(kafkaAddress)) { showTemporaryMessage('error', 'Invalid Kafka address format. Expected: host:port (e.g., localhost:9092)'); return false; }
    return true;
}

function resendMessage(message) {
    if (!canResend(message)) return;
    document.getElementById('topic').value = message.topic;
    document.getElementById('kafkaAddress').value = message.kafkaAddress;
    document.getElementById('messageText').value = message.messageText;
    headersInput.value = message.headers || '';
    bootstrap.Tab.getOrCreateInstance(document.getElementById('send-tab')).show();
    document.getElementById('messageText').focus();
}

async function sendMessage(event) {
    event.preventDefault();
    if (!validateForm()) return;
    const topic = document.getElementById('topic').value.trim();
    const kafkaAddress = document.getElementById('kafkaAddress').value.trim();
    const messageText = document.getElementById('messageText').value.trim();
    const headers = headersInput.value.trim();
    sendBtn.disabled = true;
    const originalButtonText = sendBtn.innerHTML;
    sendBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Sending...';
    const startTime = Date.now();
    try {
        const response = await fetch('/api/kafka/send', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ topic, kafkaAddress, messageText, headers }) });
        const data = await response.json();
        const duration = Date.now() - startTime;
        addToHistory({ status: response.ok && data.status === 'success' ? 'success' : 'error', topic, kafkaAddress, headers, messageText, responseMessage: response.ok && data.status === 'success' ? `${data.message} (${duration}ms)` : (data.message || 'Unknown error'), timestamp: Date.now() });
    } catch (error) {
        addToHistory({ status: 'error', topic, kafkaAddress, messageText, headers, responseMessage: `Connection error: ${error.message}`, timestamp: Date.now() });
    } finally {
        sendBtn.disabled = false;
        sendBtn.innerHTML = originalButtonText;
    }
}

form.addEventListener('submit', sendMessage);
clearBtn.addEventListener('click', clearForm);
clearHistoryBtn.addEventListener('click', clearHistory);
historyListDiv.addEventListener('click', event => {
    const button = event.target.closest('.resend-btn');
    if (!button || button.disabled) return;
    const message = messagesHistory[Number(button.dataset.historyIndex)];
    resendMessage(message);
});
loadHistory();
document.getElementById('messageText').addEventListener('keydown', event => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') { event.preventDefault(); sendMessage(event); }
});
if (!messagesHistory.length) {
    setTimeout(() => {
        if (!messagesHistory.length) addToHistory({ status: 'info', topic: 'Welcome', kafkaAddress: '-', headers: '', messageText: 'Welcome to Kafka Message Producer! Send your first message using the form on the left.', responseMessage: 'New messages will appear here with the newest on top.', timestamp: Date.now() });
    }, 500);
}
