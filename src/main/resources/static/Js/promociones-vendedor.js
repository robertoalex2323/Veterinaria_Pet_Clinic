(() => {
  // ── Helpers ───────────────────────────────────────────────────
  function setText(id, value, fallback = '0') {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = value === null || value === undefined ? fallback : String(value);
  }

  function animateCounter(el, end, duration = 900) {
    if (!el || isNaN(end)) return;
    const start = 0;
    const range = end - start;
    const startTime = performance.now();
    function step(ts) {
      const progress = Math.min((ts - startTime) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      el.textContent = Math.round(start + range * eased);
      if (progress < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }

  function animateMoney(el, end, duration = 1100) {
    if (!el || isNaN(end)) return;
    const startTime = performance.now();
    function step(ts) {
      const progress = Math.min((ts - startTime) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = end * eased;
      el.textContent = current.toLocaleString('es-PE', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      });
      if (progress < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }

  function formatMoney(v) {
    const num = Number(v);
    if (isNaN(num)) return '0.00';
    return num.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  // ── Live clock ────────────────────────────────────────────────
  function startClock() {
    const clockEl = document.getElementById('posReloj');
    if (!clockEl) return;
    function tick() {
      const now = new Date();
      const h = String(now.getHours()).padStart(2, '0');
      const m = String(now.getMinutes()).padStart(2, '0');
      const s = String(now.getSeconds()).padStart(2, '0');
      clockEl.textContent = `${h}:${m}:${s}`;
    }
    tick();
    setInterval(tick, 1000);
  }

  // ── Fetch metrics ─────────────────────────────────────────────
  async function fetchMetrics() {
    try {
      const resp = await fetch('/vendedor/api/dashboard-metrics', {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });
      if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
      return await resp.json();
    } catch (e) {
      console.error('Error fetch caja metrics:', e);
      return null;
    }
  }

  // ── Fetch últimas ventas ──────────────────────────────────────
  async function fetchUltimasVentas() {
    try {
      const resp = await fetch('/vendedor/api/ventas/ultimas', {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });
      if (!resp.ok) return null;
      return await resp.json();
    } catch (e) {
      console.warn('No se pudieron cargar últimas ventas:', e);
      return null;
    }
  }

  // ── Render ticket list ────────────────────────────────────────
  function renderTickets(ventas) {
    const listEl = document.getElementById('ticketList');
    if (!listEl) return;

    if (!ventas || ventas.length === 0) {
      listEl.innerHTML = `
        <li class="pos-ticket-empty">
          <i class="fas fa-inbox"></i>
          No hay ventas registradas hoy
        </li>`;
      return;
    }

    // Show last 5
    const items = ventas.slice(0, 5);
    listEl.innerHTML = items.map(v => {
      const monto = formatMoney(v.total || v.monto || 0);
      const hora = v.fecha
        ? new Date(v.fecha).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit' })
        : '--:--';
      const desc = v.clienteNombre || v.cliente || 'Venta';
      return `
        <li class="pos-ticket-item">
          <div class="pos-ticket-left">
            <div class="pos-ticket-icon t-sale"><i class="fas fa-receipt"></i></div>
            <div>
              <div class="pos-ticket-desc">${desc}</div>
              <div class="pos-ticket-time">${hora}</div>
            </div>
          </div>
          <div class="pos-ticket-amount">+S/ ${monto}</div>
        </li>`;
    }).join('');
  }

  // ── Boot ──────────────────────────────────────────────────────
  async function boot() {
    startClock();

    const metrics = await fetchMetrics();

    if (!metrics) {
      setText('ingresosHoy', '0.00');
      setText('ventasHoy', '0');
      setText('boletasEmitidas', '0');
      setText('cfEntradas', '0');
      setText('promosActivas', '0');
      setText('recomendacionesGeneradas', '0');
      setText('shiftVentas', '0');
      setText('shiftBoletas', '0');
      setText('shiftPromos', '0');
      setText('shiftReco', '0');
      setText('shiftTotal', 'S/ 0.00');
      renderTickets(null);
      return;
    }

    const vHoy = Number(metrics.ventasHoy ?? 0);
    const bEm = Number(metrics.boletasEmitidas ?? 0);
    const pAct = Number(metrics.promosActivas ?? 0);
    const rGen = Number(metrics.recomendacionesGeneradas ?? 0);
    const ingresos = Number(metrics.ingresosHoy ?? 0);

    // Animate the LED display amount
    animateMoney(document.getElementById('ingresosHoy'), ingresos);

    // Animate mini counters on the display
    animateCounter(document.getElementById('ventasHoy'), vHoy);
    animateCounter(document.getElementById('boletasEmitidas'), bEm);

    // Cash flow cards
    animateCounter(document.getElementById('cfEntradas'), vHoy, 700);
    animateCounter(document.getElementById('promosActivas'), pAct, 700);
    animateCounter(document.getElementById('recomendacionesGeneradas'), rGen, 700);

    // Shift summary
    setText('shiftVentas', vHoy);
    setText('shiftBoletas', bEm);
    setText('shiftPromos', pAct);
    setText('shiftReco', rGen);
    setText('shiftTotal', 'S/ ' + formatMoney(ingresos));

    // Try to load last transactions
    const ventas = await fetchUltimasVentas();
    renderTickets(ventas);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
