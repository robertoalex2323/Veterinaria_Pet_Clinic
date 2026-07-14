(() => {
  document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('ventaForm');
    if (!form) return;

    const tabla = document.getElementById('tablaProductos');
    const buscarInput = document.getElementById('buscarProducto');
    const sinResultados = document.getElementById('sinResultados');
    const formAlert = document.getElementById('formAlert');
    const formAlertText = document.getElementById('formAlertText');
    const btnGuardar = document.getElementById('btnGuardarVenta');

    const subtotalTextoEl = document.getElementById('subtotalTexto');
    const igvTextoEl = document.getElementById('igvTexto');
    const totalEl = document.getElementById('totalVenta');
    const subtotalHiddenEl = document.getElementById('subtotalVenta');
    const itemsEl = document.getElementById('totalItems');

    const CURRENCY = 'S/';
    const IGV_TASA = 0.18;

    // ---------- Utilidades ----------
    function safeNumberFromText(text) {
      const raw = (text || '').toString().trim();
      const num = Number(raw.replace(/[^\d.,-]/g, '').replace(',', '.'));
      return Number.isFinite(num) ? num : 0;
    }

    function formatMoney(n) {
      return (Number.isFinite(n) ? n : 0).toFixed(2);
    }

    function mostrarAlerta(mensaje) {
      if (!formAlert || !formAlertText) {
        alert(mensaje);
        return;
      }
      formAlertText.textContent = mensaje;
      formAlert.classList.remove('d-none');
      formAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    function ocultarAlerta() {
      if (formAlert) formAlert.classList.add('d-none');
    }

    // ---------- Selección de fila / checkbox ----------
    function getFila(el) {
      return el ? el.closest('tr.producto-row') : null;
    }

    function todasLasFilas() {
      return tabla ? Array.from(tabla.querySelectorAll('tbody tr.producto-row')) : [];
    }

    function toggleFilaSeleccionada(fila) {
      if (!fila) return;
      const chk = fila.querySelector('.producto-check');
      fila.classList.toggle('is-selected', !!(chk && chk.checked));
    }

    function actualizarSubtotalFila(fila) {
      if (!fila) return;
      const chk = fila.querySelector('.producto-check');
      const qtyInput = fila.querySelector('.cantidad-input');
      const precioEl = fila.querySelector('.precio-valor');
      const subtotalEl = fila.querySelector('.row-subtotal');
      if (!qtyInput || !precioEl || !subtotalEl) return;

      const precio = safeNumberFromText(precioEl.textContent);
      const cantidad = chk && chk.checked ? parseInt(qtyInput.value, 10) : 0;
      const subtotal = Number.isFinite(cantidad) && cantidad > 0 ? precio * cantidad : 0;
      subtotalEl.textContent = `${CURRENCY} ${formatMoney(subtotal)}`;
    }

    // ---------- Stepper +/- ----------
    function clampCantidad(qtyInput) {
      const min = parseInt(qtyInput.min || '1', 10) || 1;
      const max = parseInt(qtyInput.max || '999', 10) || 999;
      let val = parseInt(qtyInput.value, 10);
      if (!Number.isFinite(val) || val < min) val = min;
      if (val > max) val = max;
      qtyInput.value = val;
    }

    // ---------- Resumen general: SIEMPRE recorre el DOM en tiempo real ----------
    function actualizarResumen() {
      let subtotal = 0;
      let items = 0;

      todasLasFilas().forEach((fila) => {
        const chk = fila.querySelector('.producto-check');
        const qtyInput = fila.querySelector('.cantidad-input');
        const precioEl = fila.querySelector('.precio-valor');
        if (!chk || !chk.checked || !qtyInput || !precioEl) return;

        const cantidad = parseInt(qtyInput.value, 10);
        if (!Number.isFinite(cantidad) || cantidad <= 0) return;

        const precio = safeNumberFromText(precioEl.textContent);
        subtotal += precio * cantidad;
        items += 1;
      });

      const igv = subtotal * IGV_TASA;
      const total = subtotal + igv;

      if (subtotalTextoEl) subtotalTextoEl.textContent = formatMoney(subtotal);
      if (igvTextoEl) igvTextoEl.textContent = formatMoney(igv);
      if (totalEl) totalEl.textContent = formatMoney(total);
      if (subtotalHiddenEl) subtotalHiddenEl.value = formatMoney(subtotal);
      if (itemsEl) itemsEl.textContent = String(items);

      if (btnGuardar) {
        btnGuardar.disabled = items === 0;
      }
    }

    function recalcularTodo() {
      todasLasFilas().forEach((fila) => {
        toggleFilaSeleccionada(fila);
        actualizarSubtotalFila(fila);
      });
      actualizarResumen();
    }

    // ---------- Delegación de eventos: botones +/- ----------
    tabla?.addEventListener('click', (e) => {
      const btn = e.target.closest('.qty-btn');
      if (!btn) return;
      e.preventDefault();

      const fila = getFila(btn);
      const qtyInput = fila?.querySelector('.cantidad-input');
      if (!qtyInput || qtyInput.disabled) return;

      const step = btn.classList.contains('qty-plus') ? 1 : -1;
      const current = parseInt(qtyInput.value, 10) || 1;
      qtyInput.value = current + step;
      clampCantidad(qtyInput);

      const chk = fila.querySelector('.producto-check');
      if (chk && !chk.disabled) {
        chk.checked = true;
      }

      recalcularTodo();
      ocultarAlerta();
    });

    // ---------- Click en la fila selecciona/deselecciona (excepto en controles) ----------
    tabla?.addEventListener('click', (e) => {
      if (e.target.closest('.qty-stepper') || e.target.closest('.producto-check') || e.target.closest('label')) {
        return;
      }
      const fila = e.target.closest('tr.producto-row');
      if (!fila) return;
      const chk = fila.querySelector('.producto-check');
      if (!chk || chk.disabled) return;
      chk.checked = !chk.checked;
      recalcularTodo();
      ocultarAlerta();
    });

    // ---------- Cambios directos en checkbox ----------
    tabla?.addEventListener('change', (e) => {
      if (e.target.matches('.producto-check')) {
        recalcularTodo();
        ocultarAlerta();
      }
    });

    // ---------- Cantidad escrita/modificada directamente ----------
    tabla?.addEventListener('input', (e) => {
      if (e.target.matches('.cantidad-input')) {
        clampCantidad(e.target);
        const fila = getFila(e.target);

        const chk = fila?.querySelector('.producto-check');
        if (chk && !chk.disabled && !chk.checked) {
          chk.checked = true;
        }

        recalcularTodo();
        ocultarAlerta();
      }
    });

    tabla?.addEventListener('change', (e) => {
      if (e.target.matches('.cantidad-input')) {
        clampCantidad(e.target);
        recalcularTodo();
      }
    });

    // ---------- Búsqueda / filtro de productos ----------
    function filtrarProductos() {
      if (!tabla || !buscarInput) return;
      const q = buscarInput.value.trim().toLowerCase();
      const filas = todasLasFilas();
      let visibles = 0;

      filas.forEach((fila) => {
        const nombre = fila.getAttribute('data-nombre') || '';
        const categoria = fila.getAttribute('data-categoria') || '';
        const coincide = !q || nombre.includes(q) || categoria.includes(q);
        fila.classList.toggle('d-none', !coincide);
        if (coincide) visibles++;
      });

      if (sinResultados) {
        sinResultados.classList.toggle('d-none', visibles !== 0 || filas.length === 0);
      }
    }

    let buscarTimeout;
    buscarInput?.addEventListener('input', () => {
      clearTimeout(buscarTimeout);
      buscarTimeout = setTimeout(filtrarProductos, 120);
    });

    // ---------- Construcción de datos ocultos + validación al enviar ----------
    function parseSeleccionados() {
      const ids = [];
      const cantidades = [];

      todasLasFilas().forEach((fila) => {
        const chk = fila.querySelector('.producto-check');
        const qtyInput = fila.querySelector('.cantidad-input');
        if (!chk || !chk.checked || !qtyInput) return;

        const cantidad = parseInt(qtyInput.value, 10);
        if (Number.isFinite(cantidad) && cantidad > 0) {
          ids.push(chk.value);
          cantidades.push(cantidad);
        }
      });

      return { ids, cantidades };
    }

    form.addEventListener('submit', (e) => {
      ocultarAlerta();

      const { ids, cantidades } = parseSeleccionados();

      if (ids.length === 0) {
        e.preventDefault();
        mostrarAlerta('Selecciona al menos un producto para continuar.');
        return;
      }

      const nombre = document.getElementById('clienteNombre');
      const telefono = document.getElementById('clienteTelefono');
      if (nombre && !nombre.value.trim()) {
        e.preventDefault();
        mostrarAlerta('Ingresa el nombre del cliente.');
        nombre.focus();
        return;
      }
      if (telefono && !/^\d{7,15}$/.test(telefono.value.trim())) {
        e.preventDefault();
        mostrarAlerta('Ingresa un teléfono válido (7 a 15 dígitos).');
        telefono.focus();
        return;
      }

      const metodoPago = document.getElementById('metodoPago');
      const codigoOperacion = document.getElementById('codigoOperacion');
      if (metodoPago && (metodoPago.value === 'Yape' || metodoPago.value === 'Plin')) {
        if (!codigoOperacion || !codigoOperacion.value.trim()) {
          e.preventDefault();
          mostrarAlerta(`Ingresa el código de operación de ${metodoPago.value} para continuar.`);
          codigoOperacion?.focus();
          return;
        }
      }

      const productoIdsInput = document.getElementById('productoIds');
      const cantidadesInput = document.getElementById('cantidades');
      if (productoIdsInput) productoIdsInput.value = ids.join(',');
      if (cantidadesInput) cantidadesInput.value = cantidades.join(',');

      if (btnGuardar) {
        btnGuardar.disabled = true;
        btnGuardar.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Guardando...';
      }
    });

    // Inicialización
    recalcularTodo();
  });
})();