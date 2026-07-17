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

let debounceTimer = null;

    const ocultarSugerencias = () => {
      if (sugerencias) sugerencias.style.display = 'none';
      if (listaSugerencias) listaSugerencias.innerHTML = '';
    };

    const pintarSugerencias = (items) => {
      if (!listaSugerencias || !sugerencias) return;
      listaSugerencias.innerHTML = '';

      if (!items || items.length === 0) {
        ocultarSugerencias();
        return;
      }

      items.forEach((item) => {
        const li = document.createElement('li');
        li.innerHTML =
          '<span class="suf-nombre"></span>' +
          '<span class="suf-telefono"></span>';
        li.querySelector('.suf-nombre').textContent = item.nombre || '';
        li.querySelector('.suf-telefono').textContent = item.telefono || '';
        li.addEventListener('click', () => {
          input.value = item.nombre || item.telefono || '';
          ocultarSugerencias();
          goSearch();
        });
        listaSugerencias.appendChild(li);
      });

      sugerencias.style.display = 'block';
    };

    const buscarSugerencias = async (q) => {
      if (!q) {
        ocultarSugerencias();
        return;
      }
      try {
        const resp = await fetch('/vendedor/api/clientes/sugerencias?q=' + encodeURIComponent(q));
        if (!resp.ok) {
          ocultarSugerencias();
          return;
        }
        const data = await resp.json();
        pintarSugerencias(data);
      } catch (e) {
        console.error('Error al buscar sugerencias:', e);
        ocultarSugerencias();
      }
    };

    if (listaSugerencias && sugerencias && input) {
      input.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        const q = input.value.trim();
        debounceTimer = setTimeout(() => buscarSugerencias(q), 300);
      });

      document.addEventListener('click', (e) => {
        if (!sugerencias.contains(e.target) && e.target !== input) {
          ocultarSugerencias();
        }
      });
    }
  });
})();

