
(() => {
  document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('ventaForm');
    if (!form) return;

    function parseIds() {
      const checked = Array.from(
        form.querySelectorAll('input[name="agregarProducto[]"]:checked')
      );
      return checked.map(x => x.value);
    }

    function parseCantidades(ids) {
      const cantidades = [];
      ids.forEach(id => {
        const inp = form.querySelector(`input[name^="cantidadProducto[${id}]"]`);

        const inp2 = form.querySelector(`input[name="cantidadProducto[${id}]"]`);
        const el = inp2 || inp;

        const val = el ? parseInt(el.value, 10) : 0;
        cantidades.push(Number.isNaN(val) ? 0 : val);
      });
      return cantidades;
    }

    // (Opcional) Validar y/o calcular total en pantalla si existen elementos.
    // Esto asegura que la lógica sea: subtotal = precio * cantidad.
    function safeNumberFromText(text) {
      const raw = (text || '').toString().trim();
      const num = Number(raw.replace(',', '.'));
      return Number.isFinite(num) ? num : 0;
    }

    function obtenerPrecioPorProductoId(productoId) {
      // En registrar.html el precio está en el TD #5 (Cantidad es #4, Precio es #5)
      const qtyInput = form.querySelector(`input[name="cantidadProducto[__${productoId}__]"]`);
      const row = qtyInput ? qtyInput.closest('tr') : null;
      if (!row) return 0;

      const precioSpan = row.querySelector('td:nth-child(5) span');
      if (!precioSpan) return 0;
      return safeNumberFromText(precioSpan.textContent);
    }

    function updateTotalsIfPresent() {
      const totalEl = document.getElementById('totalVenta');
      const subtotalEl = document.getElementById('subtotalVenta');
      if (!totalEl && !subtotalEl) return;

      let subtotal = 0;
      const qtyInputs = form.querySelectorAll('input[name^="cantidadProducto"]');
      qtyInputs.forEach(inp => {
        // cantidadProducto[__ID__]
        const m = inp.name.match(/^cantidadProducto\[__([^\]]+)__\]$/);
        if (!m) return;
        const productoId = m[1];

        // Solo si está marcado el checkbox del producto
        const chk = form.querySelector(`input[name="agregarProducto[]"][value="${productoId}"]`);
        if (!chk || !chk.checked) return;

        const cantidad = parseInt(inp.value, 10);
        if (!Number.isFinite(cantidad) || cantidad <= 0) return;

        const precio = obtenerPrecioPorProductoId(productoId);
        subtotal += precio * cantidad;
      });

      if (subtotalEl) subtotalEl.textContent = subtotal.toFixed(2);
      if (totalEl) totalEl.textContent = subtotal.toFixed(2);
    }

    form.addEventListener('input', (e) => {
      if (e.target && e.target.matches('input[name^="cantidadProducto"]')) {
        updateTotalsIfPresent();
      }
    });
    form.addEventListener('change', (e) => {
      if (e.target && e.target.matches('input[name="agregarProducto[]"]')) {
        updateTotalsIfPresent();
      }
    });

    form.addEventListener('submit', (e) => {
      const ids = parseIds();
      if (ids.length === 0) {
        e.preventDefault();
        alert('Selecciona al menos un producto (marca “Sí”).');
        return;
      }

      const cantidades = parseCantidades(ids);
      const allValid = cantidades.every(c => c > 0);
      if (!allValid) {
        e.preventDefault();
        alert('Revisa cantidades: todas deben ser mayores a 0.');
        return;
      }

      const productoIdsInput = document.getElementById('productoIds');
      const cantidadesInput = document.getElementById('cantidades');
      if (!productoIdsInput || !cantidadesInput) return;

      productoIdsInput.value = ids.join(',');
      cantidadesInput.value = cantidades.join(',');
    });

    updateTotalsIfPresent();
  });
})();

