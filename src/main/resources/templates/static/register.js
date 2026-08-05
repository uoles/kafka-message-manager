// Обработчик публичной формы регистрации.
(function () {
    const form = document.getElementById('registerForm');
    if (!form) return;
    const message = document.getElementById('registerMessage');
    const button = document.getElementById('registerButton');
    const show = (text, kind) => { message.textContent = text; message.className = `alert alert-${kind}`; };
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        if (form.password.value !== form.passwordConfirmation.value) { show('Passwords must match.', 'danger'); return; }
        button.disabled = true;
        try {
            const response = await fetch('/api/v1/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: form.username.value.trim(), displayName: form.displayName.value.trim(), password: form.password.value }) });
            if (response.status === 409) { show('This username cannot be registered.', 'danger'); return; }
            if (response.status === 400) { show('Please check the entered values.', 'danger'); return; }
            if (!response.ok) { show('The service could not complete registration.', 'danger'); return; }
            window.location.assign(`/web/login?registeredUsername=${encodeURIComponent(form.username.value.trim())}`);
        } catch (error) { show('The service is unavailable. Please try again.', 'danger'); }
        finally { button.disabled = false; }
    });
}());
