/**
 * Sistema de notificaciones (campanita + sonido + toast).
 *
 * Fuentes de notificaciones:
 *  1. Mensajes flash del servidor tras agregar / modificar / eliminar (#flashMessages).
 *  2. Notificaciones pendientes del servidor (endpoint REST).
 *  3. Notificaciones en tiempo real por WebSocket (/topic/notifications).
 *
 * Un mismo mensaje que llega por varias fuentes se registra y suena una sola vez.
 */
document.addEventListener('DOMContentLoaded', function () {
    const STORAGE_KEY = 'notificationHistory';
    const MAX_ITEMS = 30;
    const DEDUPE_MS = 2 * 60 * 1000; // mismo mensaje en menos de 2 min = duplicado
    const SOUND_URL = '/audio/notification-sound.mp3';

    const badge = document.getElementById('notificationBadge');
    const list = document.getElementById('notificationList');
    const subtitle = document.getElementById('notificationSubtitle');
    const tray = document.getElementById('notificationTray');
    const btnClear = document.getElementById('clearNotifications');
    const btnRead = document.getElementById('markAllRead');

    // ---------- Estado ----------
    let history = [];
    try {
        history = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
        if (!Array.isArray(history)) history = [];
    } catch (e) {
        history = [];
    }
    // Compatibilidad con historial antiguo (sin id / fecha / leído)
    history = history.map(n => ({
        id: n.id || (Date.now() + Math.random()).toString(36),
        type: n.type || 'info',
        message: n.message || '',
        timestamp: n.timestamp || '',
        at: n.at || Date.now(),
        read: n.read !== undefined ? n.read : true
    }));

    const save = () => localStorage.setItem(STORAGE_KEY, JSON.stringify(history.slice(0, MAX_ITEMS)));

    // ---------- Sonido ----------
    const audio = new Audio(SOUND_URL);
    audio.preload = 'auto';
    audio.volume = 0.7;
    let lastSoundAt = 0;
    const playSound = () => {
        const now = Date.now();
        if (now - lastSoundAt < 800) return; // evita sonidos superpuestos
        lastSoundAt = now;
        try {
            audio.currentTime = 0;
            const p = audio.play();
            if (p && p.catch) {
                p.catch(() => {
                    // El navegador bloquea el audio hasta la primera interacción: se reintenta en el siguiente clic
                    const retry = () => { audio.play().catch(() => {}); };
                    document.addEventListener('click', retry, { once: true });
                });
            }
        } catch (e) { /* sin audio */ }
    };

    // ---------- Utilidades ----------
    const TYPE_META = {
        success: { icon: 'fa-check', cls: 'notif-success', swal: 'success' },
        error: { icon: 'fa-times', cls: 'notif-error', swal: 'error' },
        warning: { icon: 'fa-exclamation', cls: 'notif-warning', swal: 'warning' },
        appointment: { icon: 'fa-calendar-check', cls: 'notif-info', swal: 'info' },
        info: { icon: 'fa-info', cls: 'notif-info', swal: 'info' }
    };
    const meta = (type) => TYPE_META[type] || TYPE_META.info;

    const escapeHtml = (str) => String(str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;').replace(/'/g, '&#39;');

    const timeAgo = (at) => {
        const diff = Math.floor((Date.now() - at) / 1000);
        if (diff < 60) return 'Hace un momento';
        if (diff < 3600) return `Hace ${Math.floor(diff / 60)} min`;
        if (diff < 86400) return `Hace ${Math.floor(diff / 3600)} h`;
        return new Date(at).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });
    };

    // ---------- Render ----------
    const updateUI = () => {
        const unread = history.filter(n => !n.read).length;

        if (badge) {
            badge.textContent = unread > 9 ? '9+' : String(unread);
            badge.classList.toggle('d-none', unread === 0);
        }
        if (tray) tray.classList.toggle('has-unread', unread > 0);
        if (subtitle) {
            subtitle.textContent = unread > 0
                ? `${unread} sin leer`
                : (history.length ? 'Todo al día' : 'Sin novedades');
        }
        if (!list) return;

        if (history.length === 0) {
            list.innerHTML = '<li class="notif-empty"><i class="fas fa-bell-slash"></i><div>No hay notificaciones recientes</div></li>';
            return;
        }
        list.innerHTML = history.map(n => {
            const m = meta(n.type);
            return `
                <li class="notif-item ${n.read ? '' : 'unread'}">
                    <span class="notif-icon ${m.cls}"><i class="fas ${m.icon}"></i></span>
                    <div class="notif-body">
                        <div class="notif-msg">${escapeHtml(n.message)}</div>
                        <div class="notif-time">${timeAgo(n.at)}${n.timestamp ? ' · ' + escapeHtml(n.timestamp) : ''}</div>
                    </div>
                </li>`;
        }).join('');
    };

    const isDuplicate = (message) => history.some(n => n.message === message && (Date.now() - n.at) < DEDUPE_MS);

    const showToast = (notif) => {
        if (!window.Swal) return;
        Swal.fire({
            toast: true,
            position: 'top-end',
            timer: 4500,
            showConfirmButton: false,
            icon: meta(notif.type).swal,
            title: notif.message,
            timerProgressBar: true,
            customClass: { popup: 'notif-toast' }
        });
    };

    /** Registra una notificación (con sonido y toast) evitando duplicados. */
    const handleNewNotif = (notif, opts = {}) => {
        if (!notif || !notif.message) return false;
        const message = String(notif.message).trim();
        if (isDuplicate(message)) {
            // Ya está en la campanita (p. ej. llegó por WebSocket justo antes de cambiar de página).
            // Si es el resultado de una acción del usuario (flash), igual se muestra y suena.
            if (opts.force) {
                if (opts.sound !== false) playSound();
                if (opts.toast !== false) showToast({ type: notif.type || 'info', message });
                ringBell();
            }
            return false;
        }

        const item = {
            id: (Date.now() + Math.random()).toString(36),
            type: notif.type || 'info',
            message,
            timestamp: notif.timestamp || new Date().toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' }),
            at: Date.now(),
            read: false
        };
        history.unshift(item);
        history = history.slice(0, MAX_ITEMS);
        save();
        updateUI();

        if (opts.sound !== false) playSound();
        if (opts.toast !== false) showToast(item);

        ringBell();
        return true;
    };

    function ringBell() {
        if (!tray) return;
        tray.classList.remove('ring');
        void tray.offsetWidth;
        tray.classList.add('ring');
    }

    // API global para que otras pantallas puedan notificar
    window.PetClinicNotify = (type, message) => handleNewNotif({ type, message });

    // ---------- Acciones del panel ----------
    if (btnClear) {
        btnClear.addEventListener('click', function (e) {
            e.preventDefault();
            history = [];
            save();
            updateUI();
        });
    }
    const markAllRead = () => {
        if (!history.some(n => !n.read)) return;
        history.forEach(n => { n.read = true; });
        save();
        updateUI();
    };
    if (btnRead) btnRead.addEventListener('click', (e) => { e.preventDefault(); markAllRead(); });
    // Al cerrar el panel, todo lo visto queda como leído
    if (tray) tray.addEventListener('hidden.bs.dropdown', markAllRead);

    // Sincroniza la campanita entre pestañas abiertas
    window.addEventListener('storage', (e) => {
        if (e.key !== STORAGE_KEY) return;
        try { history = JSON.parse(e.newValue || '[]'); } catch (_) { history = []; }
        updateUI();
    });

    updateUI();
    setInterval(updateUI, 60000); // refresca los "Hace X min"

    // ---------- 1. Mensajes flash (agregar / modificar / eliminar) ----------
    const flash = document.getElementById('flashMessages');
    if (flash) {
        const pares = [['success', flash.dataset.success], ['error', flash.dataset.error], ['info', flash.dataset.info]];
        let mostrado = false;
        pares.forEach(([type, msg]) => {
            if (msg && msg.trim()) {
                handleNewNotif({ type, message: msg }, { force: true });
                mostrado = true;
            }
        });
        // Si ya se mostró como toast, se ocultan los banners de éxito/info duplicados
        if (mostrado && window.Swal) {
            const textos = pares.filter(([t, m]) => t !== 'error' && m).map(([, m]) => m.trim());
            document.querySelectorAll('.flash-alert, .alert-success, .alert-info').forEach(el => {
                if (el.classList.contains('flash-alert') || textos.some(t => el.textContent.includes(t))) el.remove();
            });
        }
    }

    // ---------- 2. Pendientes del servidor ----------
    const apiUrl = window.NOTIFICACION_API_URL || '/recepcionista/api/ui-notifications';
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 4000);
    fetch(apiUrl, { method: 'GET', cache: 'no-store', signal: controller.signal })
        .then(res => res.ok ? res.json() : [])
        .then(data => {
            if (Array.isArray(data)) data.forEach(n => handleNewNotif(n));
        })
        .catch(() => { /* se ignora para no romper la UI */ })
        .finally(() => clearTimeout(timeoutId));

    // ---------- 3. Tiempo real (WebSocket) ----------
    const connectWS = () => {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn('SockJS/Stomp no cargados: sin notificaciones en tiempo real.');
            return;
        }
        const socket = new SockJS('/ws-notifications');
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;
        stompClient.connect({}, () => {
            stompClient.subscribe('/topic/notifications', (msg) => {
                try { handleNewNotif(JSON.parse(msg.body)); } catch (e) { /* mensaje inválido */ }
            });
        }, () => setTimeout(connectWS, 5000));
    };
    connectWS();
});
