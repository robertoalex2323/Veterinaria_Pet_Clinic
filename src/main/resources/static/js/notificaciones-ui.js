document.addEventListener('DOMContentLoaded', function() {
    console.log("Sistema de Notificaciones Iniciado...");
    const notificationBadge = document.getElementById('notificationBadge'); 
    const notificationList = document.getElementById('notificationList');   

    let history = JSON.parse(localStorage.getItem('notificationHistory') || '[]');

    const updateUI = () => {
        if (!notificationList) return;
        
        if (history.length === 0) {
            notificationList.innerHTML = '<li class="p-4 text-center text-muted small notification-empty-state"><i class="fas fa-bell-slash d-block mb-2 fa-2x opacity-25"></i>No hay notificaciones recientes</li>';
            if (notificationBadge) notificationBadge.classList.add('d-none');
        } else {
            if (notificationBadge) {
                notificationBadge.textContent = history.length;
                notificationBadge.classList.remove('d-none');
            }
            
            notificationList.innerHTML = history.map(n => `
                <li class="dropdown-item d-flex align-items-center border-bottom py-2">
                    <div class="me-3">
                        <i class="fas ${n.type === 'success' ? 'fa-check-circle text-success' : 
                                         n.type === 'warning' ? 'fa-exclamation-triangle text-warning' : 'fa-info-circle text-info'} fa-fw"></i>
                    </div>
                    <div class="flex-grow-1">
                        <div class="small text-dark fw-bold" style="white-space: normal;">${n.message}</div>
                        <div class="text-muted" style="font-size: 0.7rem;">${n.timestamp}</div>
                    </div>
                </li>
            `).join('');
        }
    };

    // 1. Mostrar historial inmediatamente al cargar la página
    updateUI();

    const handleNewNotif = (notif, playSound = true) => {
        if (playSound) {
            // Sonido de alerta
            const audio = new Audio('/audio/notification-sound.mp3');
            audio.volume = 0.7;
            audio.play().catch(() => {
                console.log("Sonido bloqueado por el navegador. Se requiere interacción del usuario.");
            });

            // Toast Visual
            if (window.Swal) {
                Swal.fire({
                    toast: true,
                    position: 'top-end',
                    timer: 5000,
                    showConfirmButton: false,
                    icon: notif.type, 
                    title: notif.message, 
                    timerProgressBar: true
                });
            }
        }

        // Guardar en historial
        history.unshift(notif);
        if (history.length > 10) history = history.slice(0, 10);
        localStorage.setItem('notificationHistory', JSON.stringify(history));
        updateUI();
    };

    const connectWS = () => {
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.error("Librerías SockJS o Stomp no cargadas. Revisa tu sidebar_recepcionista.html");
            return;
        }
        const socket = new SockJS('/ws-notifications');
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            console.log("WebSocket Conectado");
            stompClient.subscribe('/topic/notifications', (msg) => handleNewNotif(JSON.parse(msg.body)));
        }, () => {
            console.warn("WebSocket desconectado. Reintentando...");
            setTimeout(connectWS, 5000);
        });
    };

    // 2. Recuperar pendientes del servidor (por ejemplo, después de refrescar por cancelar una cita)
    fetch('/recepcionista/agenda/api/ui-notifications', { method: 'GET', cache: 'no-store' })
        .then(res => {
            if (!res.ok) throw new Error("Error al obtener notificaciones");
            return res.json();
        })
        .then(data => {
            if (Array.isArray(data) && data.length > 0) {
                data.forEach(notif => handleNewNotif(notif, true));
            }
            // No llamamos a updateUI() aquí porque handleNewNotif ya lo hace
        })
        .catch(err => console.warn("No hay notificaciones nuevas pendientes en el servidor."));

    connectWS();
});