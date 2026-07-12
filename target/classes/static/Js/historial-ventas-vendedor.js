(() => {
  document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('busqueda');
    const btn = document.getElementById('btnBuscar');
    const listaSugerencias = document.getElementById('listaSugerencias');
    const sugerencias = document.getElementById('sugerencias');

    const goSearch = () => {
      const q = (input && input.value ? input.value.trim() : '');
      const url = new URL(window.location.href);
      url.pathname = '/vendedor/ventas/historial';
      url.searchParams.set('q', q);
      url.searchParams.set('page', '0');
      window.location.href = url.toString();
    };

    if (btn) btn.addEventListener('click', goSearch);
    if (input) {
      input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') goSearch();
      });
    }

    // Si en el futuro el backend devuelve sugerencias universales (opcional)
    // este JS puede pintar resultados. (No depende de endpoint obligatorio.)
    if (listaSugerencias && sugerencias) {
      // No implementado: requiere formato/endpoint de sugerencias.
      sugerencias.style.display = 'none';
    }
  });
})();

