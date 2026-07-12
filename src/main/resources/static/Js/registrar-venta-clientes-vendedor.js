(() => {
  document.addEventListener('DOMContentLoaded', () => {

    const metodo = document.getElementById('metodoPago');
    const qrMetodoPago = document.getElementById('qrMetodoPago');
    const qrImg = document.getElementById('qrImg');

    if (metodo && qrMetodoPago && qrImg) {
      const getQr = (value) => {
        if (value === 'Yape') return '/Imagen/Iconos/qr-yape.png';
        if (value === 'Plin') return '/Imagen/Iconos/qr-plin.png';
        return null;
      };

      const render = () => {
        const qr = getQr(metodo.value);
        if (!qr) {
          qrMetodoPago.style.display = 'none';
          qrImg.src = '';
          return;
        }
        qrMetodoPago.style.display = 'block';
        qrImg.src = qr;
      };

      metodo.addEventListener('change', render);
      render();
    }

    // ================= Autocompletado de cliente =================
    const nombreInput = document.getElementById('clienteNombre');
    const telefonoInput = document.getElementById('clienteTelefono');
    const emailInput = document.getElementById('clienteEmail');
    const direccionInput = document.getElementById('clienteDireccion');
    const sugerenciasBox = document.getElementById('clienteSugerencias');

    if (!nombreInput || !sugerenciasBox) return;

    const BUSCAR_URL = '/vendedor/ventas/clientes/buscar';
    let debounceTimer = null;
    let controller = null;
    let activeIndex = -1;
    let items = [];

    function cerrarSugerencias() {
      sugerenciasBox.classList.add('d-none');
      sugerenciasBox.innerHTML = '';
      activeIndex = -1;
      items = [];
    }

    function seleccionarCliente(cliente) {
      nombreInput.value = cliente.nombre || '';
      if (telefonoInput && cliente.telefono) telefonoInput.value = cliente.telefono;
      if (emailInput && cliente.email) emailInput.value = cliente.email;
      if (direccionInput && cliente.direccion) direccionInput.value = cliente.direccion;
      cerrarSugerencias();
    }

    function renderSugerencias(clientes) {
      sugerenciasBox.innerHTML = '';
      items = clientes;
      activeIndex = -1;

      if (!clientes.length) {
        const vacio = document.createElement('div');
        vacio.className = 'autocomplete-empty';
        vacio.textContent = 'Sin coincidencias. Se registrará como cliente nuevo.';
        sugerenciasBox.appendChild(vacio);
        sugerenciasBox.classList.remove('d-none');
        return;
      }

      clientes.forEach((cliente) => {
        const item = document.createElement('div');
        item.className = 'autocomplete-item';
        item.setAttribute('role', 'option');

        const nombre = document.createElement('div');
        nombre.className = 'ac-nombre';
        nombre.textContent = cliente.nombre || '(sin nombre)';

        const meta = document.createElement('div');
        meta.className = 'ac-meta';
        const partes = [cliente.telefono, cliente.email].filter(Boolean);
        meta.textContent = partes.join(' · ');

        item.appendChild(nombre);
        item.appendChild(meta);
        item.addEventListener('click', () => seleccionarCliente(cliente));

        sugerenciasBox.appendChild(item);
      });

      sugerenciasBox.classList.remove('d-none');
    }

    function marcarActivo() {
      const nodos = sugerenciasBox.querySelectorAll('.autocomplete-item');
      nodos.forEach((n, i) => n.classList.toggle('active', i === activeIndex));
      if (activeIndex >= 0 && nodos[activeIndex]) {
        nodos[activeIndex].scrollIntoView({ block: 'nearest' });
      }
    }

    async function buscarClientes(query) {
      if (controller) controller.abort();
      controller = new AbortController();

      try {
        const resp = await fetch(`${BUSCAR_URL}?query=${encodeURIComponent(query)}`, {
          signal: controller.signal,
          headers: { 'X-Requested-With': 'XMLHttpRequest' },
        });
        if (!resp.ok) return;
        const data = await resp.json();
        renderSugerencias(Array.isArray(data) ? data : []);
      } catch (err) {
        if (err.name !== 'AbortError') {
          cerrarSugerencias();
        }
      }
    }

    nombreInput.addEventListener('input', () => {
      const q = nombreInput.value.trim();
      clearTimeout(debounceTimer);

      if (q.length < 2) {
        cerrarSugerencias();
        return;
      }

      debounceTimer = setTimeout(() => buscarClientes(q), 260);
    });

    nombreInput.addEventListener('keydown', (e) => {
      if (sugerenciasBox.classList.contains('d-none') || items.length === 0) return;

      if (e.key === 'ArrowDown') {
        e.preventDefault();
        activeIndex = Math.min(activeIndex + 1, items.length - 1);
        marcarActivo();
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        activeIndex = Math.max(activeIndex - 1, 0);
        marcarActivo();
      } else if (e.key === 'Enter') {
        if (activeIndex >= 0 && items[activeIndex]) {
          e.preventDefault();
          seleccionarCliente(items[activeIndex]);
        }
      } else if (e.key === 'Escape') {
        cerrarSugerencias();
      }
    });

    document.addEventListener('click', (e) => {
      if (!e.target.closest('.autocomplete-wrap')) {
        cerrarSugerencias();
      }
    });
  });
})();
