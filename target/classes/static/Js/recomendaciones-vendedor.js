(() => {
  const grid = document.getElementById('recommendationsGrid');
  const alertBox = document.getElementById('recommendationAlert');
  const btn = document.getElementById('btnRecommend');

  if (!grid || !alertBox || !btn) return;

  function escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, (s) => ({ '&': '&amp;', '<': '<', '>': '>', '"': '"', "'": '&#39;' }[s]));
  }

  function showAlert(msg) {
    alertBox.textContent = msg;
    alertBox.classList.remove('d-none');
  }

  function clearAlert() {
    alertBox.classList.add('d-none');
  }

  function render(items) {
    grid.innerHTML = '';

    if (!items || items.length === 0) {
      grid.innerHTML = `
        <div class="col-12">
          <div class="alert alert-warning mb-0">No hay productos para recomendar todavía.</div>
        </div>`;
      return;
    }

    for (const p of items) {
      const card = document.createElement('div');
      card.className = 'col-md-4';
      card.innerHTML = `
        <div class="card h-100">
          <div class="card-body">
            <div class="d-flex justify-content-between align-items-start">
              <div>
                <h5 class="mb-1">${escapeHtml(p.nombre)}</h5>
                <p class="text-muted mb-2">${escapeHtml(p.categoria || 'Sin categoría')}</p>
              </div>
              <div class="text-end">
                <div class="fw-bold">${escapeHtml(p.precioFormateado)}</div>
                ${p.razon ? `<div class="small text-primary">${escapeHtml(p.razon)}</div>` : ''}
              </div>
            </div>
          </div>
        </div>`;
      grid.appendChild(card);
    }
  }

  async function recommend() {
    clearAlert();
    grid.innerHTML = '';
    btn.disabled = true;

    try {
      const petQuery = document.getElementById('petQuery')?.value || '';
      const prefQuery = document.getElementById('prefQuery')?.value || '';

      showAlert('Generando recomendaciones...');

      // Nota: la URL final se construye en el template con un data-attr.
      const baseUrl = btn.getAttribute('data-api-url');
      const url = `${baseUrl}?pet=${encodeURIComponent(petQuery)}&pref=${encodeURIComponent(prefQuery)}`;

      const resp = await fetch(url, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
      });

      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt || `HTTP ${resp.status}`);
      }

      const data = await resp.json();
      render(data.items);
    } catch (e) {
      showAlert('Error generando recomendaciones: ' + e.message);
      grid.innerHTML = '';
    } finally {
      btn.disabled = false;
    }
  }

  btn.addEventListener('click', recommend);
})();

