(() => {

  function init() {
    const table = document.querySelector('.producto-card table.table');
    if (!table) return;

    table.querySelectorAll('tbody tr').forEach((tr) => {
      tr.addEventListener('mouseenter', () => tr.classList.add('pc-row-hover'));
      tr.addEventListener('mouseleave', () => tr.classList.remove('pc-row-hover'));
    });

    table.querySelectorAll('a.btn-outline-danger').forEach((btn) => {
      btn.addEventListener('click', (e) => {
        const nombre = btn.getAttribute('data-nombre') || 'este producto';

        const ok = window.confirm(`¿Eliminar el producto "${nombre}"?`);
        if (!ok) e.preventDefault();
      });
    });


  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

