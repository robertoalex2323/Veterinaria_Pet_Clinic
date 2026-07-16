(() => {
    const STORAGE_KEY = "vetNotificationSettings";
    const defaults = {
        criticalAlerts: true,
        appointmentReminders: true,
        pharmacyRequests: true,
        vaccineReminders: false,
        alertSound: true
    };

    const read = () => {
        try { return { ...defaults, ...JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}") }; }
        catch (_) { return { ...defaults }; }
    };

    const save = partial => {
        const next = { ...read(), ...partial };
        localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
        return next;
    };

    const categoryFor = notification => {
        const message = String(notification.message || "").toLowerCase();
        if (notification.type === "critical" || message.includes("crític") || message.includes("critico")) return "criticalAlerts";
        if (message.includes("vacuna")) return "vaccineReminders";
        if (message.includes("receta") || message.includes("farmacia") || message.includes("medicamento")) return "pharmacyRequests";
        return "appointmentReminders";
    };

    const notify = notification => {
        const settings = read();
        if (!settings[categoryFor(notification)]) return;

        if (settings.alertSound) {
            const audio = new Audio("/audio/notification-sound.mp3");
            audio.volume = .55;
            audio.play().catch(() => {});
        }

        const toast = document.createElement("div");
        toast.className = "vet-notification-toast";
        toast.setAttribute("role", "alert");
        toast.textContent = notification.message || "Nueva notificación";
        document.body.appendChild(toast);
        window.setTimeout(() => toast.remove(), 5500);
    };

    const connect = () => {
        if (!window.SockJS || !window.Stomp) return;
        const client = window.Stomp.over(new SockJS("/ws-notifications"));
        client.debug = null;
        client.connect({}, () => client.subscribe("/topic/notifications", event => notify(JSON.parse(event.body))),
            () => window.setTimeout(connect, 5000));
    };

    document.addEventListener("DOMContentLoaded", () => {
        document.querySelectorAll("[data-notification-setting]").forEach(input => {
            const key = input.dataset.notificationSetting;
            input.checked = read()[key];
            input.addEventListener("change", () => save({ [key]: input.checked }));
        });

        fetch("/veterinaria/api/ui-notifications", { cache: "no-store" })
            .then(response => response.ok ? response.json() : [])
            .then(items => Array.isArray(items) && items.forEach(notify))
            .catch(() => {});
        connect();
    });
})();
