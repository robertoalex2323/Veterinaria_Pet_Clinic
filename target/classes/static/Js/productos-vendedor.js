(() => {

  function init() {
    const table = document.querySelector('.producto-card table.table');
    if (!table) return;

    // Resaltar filas al pasar mouse 
    table.querySelectorAll('tbody tr').forEach((tr) => {
      tr.addEventListener('mouseenter', () => tr.classList.add('pc-row-hover'));
      tr.addEventListener('mouseleave', () => tr.classList.remove('pc-row-hover'));
    });

    // Confirmación mejorada para eliminación: si existe el dataset nombre.
    table.querySelectorAll('a.btn-outline-danger').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const nombre = btn.getAttribute('data-nombre') || 'este producto';
        // Si el atributo onclick ya trae confirm, lo respetamos.
        // Cancelamos solo si el usuario no confirma.
        const ok = window.confirm(`¿Eliminar el producto "${nombre}"?`);
        if (!ok) e.preventDefault();
      });
    });

    // Si hay alertas, que cierren rápido y se vean más limpias (opcional)
    // (Se maneja también por el layout global, pero esto es redundante/ligero)
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

