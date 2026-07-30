// Управление динамическими Kafka-консьюмерами и их сообщениями.
const consumerApp = window.KafkaMessageManager;
const consumerState = consumerApp.state;

// Создаёт консьюмер через REST API.
async function createConsumer(event) {
    event.preventDefault();
    const button = document.getElementById('createConsumerBtn');
    button.disabled = true;
    try {
        const response = await fetch('/api/kafka/consumers', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ bootstrapAddress: document.getElementById('consumerBootstrapAddress').value.trim(), topic: document.getElementById('consumerTopic').value.trim() }) });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || 'Failed to create consumer');
        consumerState.consumers.set(data.id, data);
        consumerState.consumerCursors.set(data.id, 0);
        renderConsumers();
        pollConsumer(data.id);
        consumerState.consumerForm.reset();
    } catch (error) { alert(error.message); } finally { button.disabled = false; }
}

// Перерисовывает список вкладок созданных консьюмеров.
function renderConsumers() {
    if (!consumerState.consumers.size) { consumerState.consumerTabs.innerHTML = '<div class="text-muted">No consumers created.</div>'; return; }
    const nav = [...consumerState.consumers.values()].map((consumer, index) => `<li class="nav-item"><button class="nav-link ${index === 0 ? 'active' : ''}" data-bs-toggle="tab" data-bs-target="#consumer-pane-${consumer.id}" type="button">${consumerApp.escapeHtml(consumer.topic)}</button></li>`).join('');
    const panes = [...consumerState.consumers.values()].map((consumer, index) => `<section class="tab-pane fade ${index === 0 ? 'show active' : ''}" id="consumer-pane-${consumer.id}"><div class="d-flex justify-content-between align-items-center mb-3"><div><span class="badge bg-${consumer.status === 'RUNNING' ? 'success' : consumer.status === 'ERROR' ? 'danger' : 'secondary'}">${consumerApp.escapeHtml(consumer.status)}</span> <span class="text-muted">${consumerApp.escapeHtml(consumer.bootstrapAddress)} · group ${consumerApp.escapeHtml(consumer.groupId)}</span>${consumer.lastError ? `<div class="text-danger small">${consumerApp.escapeHtml(consumer.lastError)}</div>` : ''}</div><button class="btn btn-outline-danger btn-sm" data-delete-consumer="${consumer.id}">Delete</button></div><div id="consumer-messages-${consumer.id}" class="table-responsive"><div class="text-muted py-3">Waiting for messages...</div></div></section>`).join('');
    consumerState.consumerTabs.innerHTML = `<ul class="nav nav-pills mb-3">${nav}</ul><div class="tab-content">${panes}</div>`;
    consumerState.consumers.forEach((consumer, id) => renderConsumerMessages(id));
}

// Отображает сообщения выбранного консьюмера в таблице.
function renderConsumerMessages(id) {
    const container = document.getElementById(`consumer-messages-${id}`);
    if (!container) return;
    const messages = consumerState.consumerMessages.get(id) || [];
    if (!messages.length) return;
    const rows = messages.map(message => `<tr><td>${consumerApp.escapeHtml(new Date(message.timestamp).toLocaleString())}</td><td>${message.partition}</td><td>${message.offset}</td><td>${consumerApp.escapeHtml(message.key || '')}</td><td class="message-text">${consumerApp.escapeHtml(message.value || '')}</td><td>${consumerApp.escapeHtml((message.headers || []).map(header => `${header.name}=${header.value}`).join(', '))}</td></tr>`).join('');
    container.innerHTML = `<table class="table table-sm table-hover"><thead><tr><th>Time</th><th>Partition</th><th>Offset</th><th>Key</th><th>Value</th><th>Headers</th></tr></thead><tbody>${rows}</tbody></table>`;
}

// Запрашивает новые сообщения и планирует следующий опрос.
async function pollConsumer(id) {
    if (consumerState.consumerInFlight.has(id)) return;
    consumerState.consumerInFlight.add(id);
    try {
        const response = await fetch(`/api/kafka/consumers/${id}/messages?after=${consumerState.consumerCursors.get(id) || 0}&limit=100`);
        if (response.status === 404) { removeConsumerLocally(id); return; }
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || 'Consumer unavailable');
        if (!consumerState.consumers.get(id)) return;
        const container = document.getElementById(`consumer-messages-${id}`);
        const messages = data.messages || [];
        const knownMessages = consumerState.consumerMessages.get(id) || [];
        const knownSequences = new Set(knownMessages.map(message => message.sequence));
        consumerState.consumerMessages.set(id, knownMessages.concat(messages.filter(message => !knownSequences.has(message.sequence))));
        renderConsumerMessages(id);
        if (messages.length) consumerState.consumerCursors.set(id, Math.max(...messages.map(message => message.sequence)));
        consumerState.consumerErrors.delete(id);
        if (data.droppedCount > 0 && container) container.insertAdjacentHTML('afterbegin', '<div class="alert alert-warning py-2">Some older messages were dropped because the consumer buffer is full.</div>');
    } catch (error) { consumerState.consumerErrors.set(id, error.message); } finally { consumerState.consumerInFlight.delete(id); }
    if (consumerState.consumers.has(id)) consumerState.consumerTimers.set(id, setTimeout(() => pollConsumer(id), 2000));
}

// Удаляет консьюмера и его локальные данные.
function removeConsumerLocally(id) {
    clearTimeout(consumerState.consumerTimers.get(id));
    consumerState.consumerTimers.delete(id);
    consumerState.consumerInFlight.delete(id);
    consumerState.consumers.delete(id);
    consumerState.consumerCursors.delete(id);
    consumerState.consumerErrors.delete(id);
    consumerState.consumerMessages.delete(id);
    renderConsumers();
}

// Синхронизирует список консьюмеров с сервером.
async function loadConsumers() {
    const response = await fetch('/api/kafka/consumers');
    if (!response.ok) return;
    const serverConsumers = await response.json();
    const serverIds = new Set(serverConsumers.map(consumer => consumer.id));
    [...consumerState.consumers.keys()].filter(id => !serverIds.has(id)).forEach(removeConsumerLocally);
    serverConsumers.forEach(consumer => {
        consumerState.consumers.set(consumer.id, consumer);
        if (!consumerState.consumerCursors.has(consumer.id)) consumerState.consumerCursors.set(consumer.id, 0);
    });
    renderConsumers();
    serverConsumers.forEach(consumer => { if (!consumerState.consumerTimers.has(consumer.id)) pollConsumer(consumer.id); });
}

// Удаляет консьюмера на сервере и в браузере.
function deleteConsumer(id) {
    clearTimeout(consumerState.consumerTimers.get(id));
    consumerState.consumerTimers.delete(id);
    fetch(`/api/kafka/consumers/${id}`, { method: 'DELETE' }).then(response => {
        if (response.ok || response.status === 404) removeConsumerLocally(id);
        else throw new Error('Failed to delete consumer');
    }).catch(error => consumerState.consumerErrors.set(id, error.message));
}

consumerApp.consumers = { createConsumer, renderConsumers, pollConsumer, loadConsumers, deleteConsumer };
