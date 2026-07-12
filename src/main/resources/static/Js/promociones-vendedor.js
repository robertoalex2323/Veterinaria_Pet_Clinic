(() => {
  function init() {
    const container = document.getElementById('content') || document;
    const table = container.querySelector('table.table');
    if (!table) return;

    // Hover de filas
    table.querySelectorAll('tbody tr').forEach((tr) => {
      tr.addEventListener('mouseenter', () => tr.classList.add('pc-row-hover'));
      tr.addEventListener('mouseleave', () => tr.classList.remove('pc-row-hover'));
    });

    // Confirm delete (seguro si el dataset.nombre no existe)
    table.querySelectorAll('a.btn-outline-danger').forEach((btn) => {
      // Si ya tiene onclick, no lo pisamos. (por compatibilidad con tu HTML)
      if (btn.getAttribute('onclick')) return;

      btn.addEventListener('click', (e) => {
        const nombre = btn.dataset.nombre || 'esta promoción';
        const ok = window.confirm(`¿Eliminar la promoción de ${nombre}?`);
        if (!ok) e.preventDefault();
      });
    });

    // Tooltip simple en badges (si hay bootstrap JS no hace falta, pero ayuda)
    table.querySelectorAll('span.badge').forEach((badge) => {
      if (badge.getAttribute('title')) return;
      const texto = badge.textContent?.trim();
      if (texto) badge.setAttribute('title', texto);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

