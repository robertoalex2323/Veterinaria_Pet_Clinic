// Funciones globales
document.addEventListener('DOMContentLoaded', function() {
    // Auto-cerrar alertas
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.3s';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 300);
        }, 5000);
    });
    
    // Tooltips
    const tooltips = document.querySelectorAll('[data-tooltip]');
    tooltips.forEach(el => {
        el.addEventListener('mouseenter', (e) => {
            const tooltip = document.createElement('div');
            tooltip.className = 'custom-tooltip';
            tooltip.textContent = el.dataset.tooltip;
            document.body.appendChild(tooltip);
            const rect = el.getBoundingClientRect();
            tooltip.style.top = rect.top - 30 + 'px';
            tooltip.style.left = rect.left + (rect.width/2) - (tooltip.offsetWidth/2) + 'px';
            setTimeout(() => tooltip.remove(), 2000);
        });
    });
});

// Formatear moneda
function formatMoney(amount) {
    return new Intl.NumberFormat('es-PE', {
        style: 'currency',
        currency: 'PEN',
        minimumFractionDigits: 2
    }).format(amount);
}

// Formatear fecha
function formatDate(date) {
    return new Date(date).toLocaleDateString('es-PE');
}

// Confirmar acción
function confirmAction(message) {
    return confirm(message || '¿Estás seguro de realizar esta acción?');
}