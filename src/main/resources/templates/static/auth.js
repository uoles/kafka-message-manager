// Безопасное состояние JWT-аутентификации и общий wrapper API-запросов.
(function () {
    'use strict';

    const app = window.KafkaMessageManager = window.KafkaMessageManager || {};
    const TOKEN_KEY = 'kafkaAuthToken';
    const USER_KEY = 'kafkaAuthUser';
    const EXPIRY_KEY = 'kafkaAuthExpiry';
    const HISTORY_PREFIX = 'kafkaMessageHistory:';

    function read(key) {
        try { return sessionStorage.getItem(key); } catch (error) { return null; }
    }

    function write(key, value) {
        try { sessionStorage.setItem(key, value); } catch (error) { /* storage unavailable */ }
    }

    function remove(key) {
        try { sessionStorage.removeItem(key); } catch (error) { /* storage unavailable */ }
    }

    function getToken() {
        const token = read(TOKEN_KEY);
        const expiry = Number(read(EXPIRY_KEY));
        if (!token || !Number.isFinite(expiry) || expiry <= Date.now()) {
            if (token || expiry) clear();
            return null;
        }
        return token;
    }

    function getUser() {
        try { return JSON.parse(read(USER_KEY) || 'null'); } catch (error) { return null; }
    }

    function save(response) {
        if (!response || typeof response.accessToken !== 'string' || response.accessToken.length === 0) {
            throw new Error('Authentication response is invalid');
        }
        const user = response.user && typeof response.user === 'object' ? {
            id: response.user.id || '', username: response.user.username || '',
            roles: Array.isArray(response.user.roles) ? response.user.roles : []
        } : null;
        write(TOKEN_KEY, response.accessToken);
        write(USER_KEY, JSON.stringify(user));
        write(EXPIRY_KEY, String(Date.now() + Math.max(1, Number(response.expiresIn) || 900) * 1000));
    }

    function clear() {
        remove(TOKEN_KEY); remove(USER_KEY); remove(EXPIRY_KEY);
    }

    function historyKey() {
        const user = getUser();
        return `${HISTORY_PREFIX}${String((user && (user.id || user.username)) || 'anonymous').toLowerCase()}`;
    }

    function safeReturnUrl(value) {
        if (!value || value[0] !== '/' || value.startsWith('//') || value.includes('\\')) return '/web/send-message';
        return value;
    }

    function loginUrl(returnUrl) {
        return `/web/login?returnUrl=${encodeURIComponent(safeReturnUrl(returnUrl))}`;
    }

    function redirectToLogin() {
        if (!window.location.pathname.startsWith('/web/login')) {
            window.location.assign(loginUrl(window.location.pathname + window.location.search));
        }
    }

    async function authenticatedFetch(url, options) {
        const target = new URL(url, window.location.origin);
        const request = { ...(options || {}), headers: { ...((options && options.headers) || {}) } };
        if (target.origin === window.location.origin && target.pathname.startsWith('/api/')) {
            const token = getToken();
            if (!token) { redirectToLogin(); throw new Error('Authentication required'); }
            request.headers.Authorization = `Bearer ${token}`;
        }
        const response = await window.fetch(target.toString(), request);
        if (response.status === 401) { clear(); redirectToLogin(); }
        return response;
    }

    function logout() {
        const key = historyKey();
        clear();
        try { localStorage.removeItem(key); } catch (error) { /* storage unavailable */ }
        window.location.assign('/web/login');
    }

    app.auth = { TOKEN_KEY, USER_KEY, getToken, getUser, save, clear, historyKey, safeReturnUrl, loginUrl, redirectToLogin, authenticatedFetch, logout };
    app.storageKey = historyKey();
}());
