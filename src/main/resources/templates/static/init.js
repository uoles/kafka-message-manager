// Инициализация обработчиков страницы после загрузки всех модулей.
const initApp = window.KafkaMessageManager;
const initState = initApp.state;

// Подключение обработчиков формы отправки и формы консьюмера.
initState.form.addEventListener('submit', initApp.form.sendMessage);
initState.consumerForm.addEventListener('submit', initApp.consumers.createConsumer);
initState.consumerTabs.addEventListener('click', event => {
    const button = event.target.closest('[data-delete-consumer]');
    if (button) initApp.consumers.deleteConsumer(button.dataset.deleteConsumer);
});
document.getElementById('consumers-tab').addEventListener('shown.bs.tab', initApp.consumers.loadConsumers);
initApp.consumers.loadConsumers().catch(() => {});

// Останавливает запланированные опросы перед закрытием страницы.
window.addEventListener('beforeunload', () => initState.consumerTimers.forEach(timer => clearTimeout(timer)));
initState.clearBtn.addEventListener('click', initApp.form.clearForm);
initState.clearHistoryBtn.addEventListener('click', initApp.history.clearHistory);
initState.historyListDiv.addEventListener('click', event => {
    const button = event.target.closest('.resend-btn');
    if (!button || button.disabled) return;
    initApp.form.resendMessage(initState.messagesHistory[Number(button.dataset.historyIndex)]);
});

initApp.history.loadHistory();

document.getElementById('messageText').addEventListener('keydown', event => {
    if ((event.ctrlKey || event.metaKey) && event.key === 'Enter') { event.preventDefault(); initApp.form.sendMessage(event); }
});

if (!initState.messagesHistory.length) {
    setTimeout(() => {
        if (!initState.messagesHistory.length) initApp.history.addToHistory({ status: 'info', topic: 'Welcome', kafkaAddress: '-', headers: '', messageText: 'Welcome to Kafka Message Producer! Send your first message using the form on the left.', responseMessage: 'New messages will appear here with the newest on top.', timestamp: Date.now() });
    }, 500);
}
