(() => {
  async function fetchMetrics() {
    try {
      const response = await fetch('/vendedor/api/dashboard-metrics', {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });
      if (!response.ok) {
        const txt = await response.text();
        throw new Error(txt || `HTTP ${response.status}`);
      }
      return await response.json();
    } catch (e) {
      console.error('Error fetch caja metrics:', e);
      return null;
    }
  }

  function setText(id, value, fallback = '0') {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = value === null || value === undefined ? fallback : String(value);
  }

  function formatMoney(v) {
    if (v === null || v === undefined) return '0';
    const num = Number(v);
    if (Number.isNaN(num)) return String(v);
    return num.toLocaleString('es-ES', { style: 'currency', currency: 'EUR' });
  }

  async function boot() {
    const metrics = await fetchMetrics();
    if (!metrics) {
      // valores por defecto
      setText('ventasHoy', '0');
      setText('boletasEmitidas', '0');
      setText('promosActivas', '0');
      setText('recomendacionesGeneradas', '0');
      setText('ingresosHoy', '0');
      return;
    }

    // dashboard-metrics ya devuelve varios campos
    setText('ventasHoy', metrics.ventasHoy ?? 0);
    setText('boletasEmitidas', metrics.boletasEmitidas ?? 0);
    setText('promosActivas', metrics.promosActivas ?? 0);
    setText('recomendacionesGeneradas', metrics.recomendacionesGeneradas ?? 0);

    // Este endpoint aún no devuelve ingresosHoy; lo dejamos a 0 salvo que tu backend lo agregue.
    // Si más adelante agregas ingresosHoy desde backend, aquí se mostrará.
    setText('ingresosHoy', metrics.ingresosHoy ?? 0);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();

