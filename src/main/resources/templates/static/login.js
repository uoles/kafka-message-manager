// Обработчик публичной формы входа.
(function () {
    const form = document.getElementById('loginForm');
    if (!form) return;
    const message = document.getElementById('loginMessage');
    const button = document.getElementById('loginButton');
    const show = (text, kind) => { message.textContent = text; message.className = `alert alert-${kind}`; };
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        button.disabled = true;
        try {
            const response = await fetch('/api/v1/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: form.username.value.trim(), password: form.password.value }) });
            if (response.status === 401) { show('Invalid username or password.', 'danger'); return; }
            if (!response.ok) { show('The service could not complete sign in.', 'danger'); return; }
            const data = await response.json();
            window.KafkaMessageManager.auth.save(data);
            window.location.assign(window.KafkaMessageManager.auth.safeReturnUrl(new URLSearchParams(window.location.search).get('returnUrl')));
        } catch (error) { show('The service is unavailable. Please try again.', 'danger'); }
        finally { button.disabled = false; }
    });
}());
