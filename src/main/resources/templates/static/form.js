// Валидация формы и отправка сообщений в Kafka.
const formApp = window.KafkaMessageManager;
const formState = formApp.state;

// Очищает поля формы отправки сообщения.
function clearForm() {
    document.getElementById('topic').value = '';
    document.getElementById('kafkaAddress').value = 'localhost:9092';
    document.getElementById('messageText').value = '';
    formState.headersInput.value = '';
    formApp.history.showTemporaryMessage('info', 'Form has been cleared');
}

// Проверяет обязательные поля и формат Kafka-заголовков и адреса.
function validateForm() {
    const topic = document.getElementById('topic').value.trim();
    const kafkaAddress = document.getElementById('kafkaAddress').value.trim();
    const messageText = document.getElementById('messageText').value.trim();
    const headers = formState.headersInput.value.trim();
    if (headers && !headers.split(',').every(entry => { const separator = entry.indexOf('='); return separator > 0 && entry.substring(0, separator).trim() && entry.substring(separator + 1).trim(); })) {
        formApp.history.showTemporaryMessage('error', 'Invalid Kafka headers. Expected: name=value,name2=value2'); return false;
    }
    if (!topic) { formApp.history.showTemporaryMessage('error', 'Please enter a topic name'); return false; }
    if (!kafkaAddress) { formApp.history.showTemporaryMessage('error', 'Please enter a Kafka address'); return false; }
    if (!messageText) { formApp.history.showTemporaryMessage('error', 'Please enter a message text'); return false; }
    if (!/^[a-zA-Z0-9.-]+:\d+$/.test(kafkaAddress)) { formApp.history.showTemporaryMessage('error', 'Invalid Kafka address format. Expected: host:port (e.g., localhost:9092)'); return false; }
    return true;
}

// Заполняет форму данными выбранного сообщения без отправки.
function resendMessage(message) {
    if (!formApp.history.canResend(message)) return;
    document.getElementById('topic').value = message.topic;
    document.getElementById('kafkaAddress').value = message.kafkaAddress;
    document.getElementById('messageText').value = message.messageText;
    formState.headersInput.value = message.headers || '';
    bootstrap.Tab.getOrCreateInstance(document.getElementById('send-tab')).show();
    document.getElementById('messageText').focus();
}

// Отправляет сообщение в Kafka через REST API.
async function sendMessage(event) {
    event.preventDefault();
    if (!validateForm()) return;
    const topic = document.getElementById('topic').value.trim();
    const kafkaAddress = document.getElementById('kafkaAddress').value.trim();
    const messageText = document.getElementById('messageText').value.trim();
    const headers = formState.headersInput.value.trim();
    formState.sendBtn.disabled = true;
    const originalButtonText = formState.sendBtn.innerHTML;
    formState.sendBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Sending...';
    const startTime = Date.now();
    try {
        const response = await formApp.auth.authenticatedFetch('/api/kafka/send', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ topic, kafkaAddress, messageText, headers }) });
        const data = await response.json();
        const duration = Date.now() - startTime;
        formApp.history.addToHistory({ status: response.ok && data.status === 'success' ? 'success' : 'error', topic, kafkaAddress, headers, messageText, responseMessage: response.ok && data.status === 'success' ? `${data.message} (${duration}ms)` : (data.message || 'Unknown error'), timestamp: Date.now() });
    } catch (error) {
        formApp.history.addToHistory({ status: 'error', topic, kafkaAddress, messageText, headers, responseMessage: `Connection error: ${error.message}`, timestamp: Date.now() });
    } finally {
        formState.sendBtn.disabled = false;
        formState.sendBtn.innerHTML = originalButtonText;
    }
}

formApp.form = { clearForm, validateForm, resendMessage, sendMessage };
