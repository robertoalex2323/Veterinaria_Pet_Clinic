/**
 * Registrar Venta - Vendedor
 * Reemplaza el script inline de registrar.html.
 *
 * Construye listas formales solo con productos marcados:
 * - Back usa parámetros: productoIds (comma) y cantidades (comma)
 */
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
        // input cantidadProducto[ID]
        const inp = form.querySelector(`input[name^="cantidadProducto[${id}]"]`);
        // fallback (por si el nombre viniera exactamente)
        const inp2 = form.querySelector(`input[name="cantidadProducto[${id}]"]`);
        const el = inp2 || inp;

        const val = el ? parseInt(el.value, 10) : 0;
        cantidades.push(Number.isNaN(val) ? 0 : val);
      });
      return cantidades;
    }

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
  });
})();
