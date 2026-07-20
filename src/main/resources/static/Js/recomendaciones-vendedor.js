(() => {
  const form = document.getElementById('rcForm');
  const grid = document.getElementById('recommendationsGrid');
  const btn = document.getElementById('btnRecommend');
  const btnReset = document.getElementById('btnReset');
  const resultsInfo = document.getElementById('rcResultsInfo');
  const aiMsgContainer = document.getElementById('aiMessageContainer');
  const aiMsgText = document.getElementById('aiMessageText');
  const petSelect = document.getElementById('petQuery');
  const prefSelect = document.getElementById('prefQuery');

  if (!grid || !btn || !form) return;

  let currentController = null;

  function escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, (s) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[s]));
  }

  function showAiMessage(msg) {
    aiMsgContainer.classList.remove('d-none', 'alert-danger');
    aiMsgContainer.classList.add('alert-success');
    aiMsgText.textContent = msg;
  }

  function showError(msg) {
    aiMsgContainer.classList.remove('d-none', 'alert-success');
    aiMsgContainer.classList.add('alert-danger');
    aiMsgText.textContent = msg;
  }

  function hideAiMessage() {
    aiMsgContainer.classList.add('d-none');
  }

  function typeEffect(element, text, speed = 18) {
    element.textContent = '';
    let i = 0;
    return new Promise((resolve) => {
      function type() {
        if (i < text.length) {
          element.textContent += text.charAt(i);
          i++;
          setTimeout(type, speed);
        } else {
          resolve();
        }
      }
      type();
    });
  }

  function renderSkeleton(count = 3) {
    grid.innerHTML = '';
    for (let i = 0; i < count; i++) {
      const col = document.createElement('div');
      col.className = 'col-md-4 mb-3';
      col.innerHTML = '<div class="rc-skeleton"></div>';
      grid.appendChild(col);
    }
  }

  function renderEmptyState(message) {
    grid.innerHTML = `
      <div class="col-12">
        <div class="alert alert-warning mb-0">
          <i class="fas fa-exclamation-triangle me-2"></i> ${escapeHtml(message)}
        </div>
      </div>`;
  }

  function updateResultsInfo(count) {
    if (!resultsInfo) return;
    if (count <= 0) {
      resultsInfo.classList.add('d-none');
      return;
    }
    resultsInfo.classList.remove('d-none');
    resultsInfo.innerHTML = `<span class="rc-count-pill">${count}</span> producto${count === 1 ? '' : 's'} recomendado${count === 1 ? '' : 's'}`;
  }

  function render(items) {
    grid.innerHTML = '';

    if (!items || items.length === 0) {
      renderEmptyState('No hay productos para recomendar según los filtros seleccionados.');
      updateResultsInfo(0);
      return;
    }

    updateResultsInfo(items.length);

    const fragment = document.createDocumentFragment();
    for (const p of items) {
      const card = document.createElement('div');
      card.className = 'col-md-4 mb-3';
      const confianzaPct = typeof p.confianza === 'number' ? p.confianza : null;
      card.innerHTML = `
        <div class="card rc-product-card">
          <div class="card-body">
            <div class="rc-product-header">
              <div>
                <p class="rc-product-name">${escapeHtml(p.nombre)}</p>
                <span class="rc-category-badge">
                  <i class="fas fa-tag" style="font-size:0.6rem;"></i>
                  ${escapeHtml(p.categoria || 'Sin categoría')}
                </span>
              </div>
              <div class="rc-price-block">
                <span class="rc-price-label">Precio</span>
                <div class="rc-price">S/ ${escapeHtml(p.precioFormateado)}</div>
              </div>
            </div>

            ${confianzaPct !== null ? `
              <div class="rc-confianza-wrap">
                <div class="rc-confianza-label">
                  <span><i class="fas fa-brain me-1"></i> Coincidencia IA</span>
                  <span class="rc-confianza-pct">${confianzaPct}%</span>
                </div>
                <div class="rc-confianza-bar">
                  <div class="rc-confianza-fill" style="width:${confianzaPct}%"></div>
                </div>
              </div>` : ''}

            ${p.razon ? `
              <div class="rc-reason">
                <i class="fas fa-robot"></i>
                <span>${escapeHtml(p.razon)}</span>
              </div>` : ''}

            <div class="mt-3 d-grid">
              <button type="button" class="btn rc-mark-exitosa"
                data-producto-id="${escapeHtml(p.id)}"
                data-categoria="${escapeHtml(p.categoria || '')}"
                data-razon="${escapeHtml(p.razon || '')}">
                <i class="fas fa-check me-2"></i> Marcar como exitosa
              </button>
            </div>
          </div>
        </div>`;
      fragment.appendChild(card);
    }
    grid.appendChild(fragment);

    // listeners de los botones renderizados
    grid.querySelectorAll('.rc-mark-exitosa').forEach((btn) => {
      btn.addEventListener('click', async () => {
        const productoId = btn.getAttribute('data-producto-id');
        const categoria = btn.getAttribute('data-categoria');
        const razon = btn.getAttribute('data-razon');

        // Para relacionar con cliente: en este proyecto aún no se provee un selector en la pantalla,
        // así que por ahora enviamos null.
        // Si en `recomendaciones.html` agregas un select de cliente, aquí se toma su value.
        const clienteId = null;

        const baseUrlPost = '/vendedor/recomendaciones/registrar';
        btn.disabled = true;
        const originalHtml = btn.innerHTML;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Registrando...';

        try {
          const params = new URLSearchParams();
          params.append('productoId', productoId);
          if (categoria !== null) params.append('categoria', categoria);
          if (razon !== null) params.append('razon', razon);
          if (clienteId !== null) params.append('clienteId', clienteId);

          const resp = await fetch(baseUrlPost, {
            method: 'POST',
            headers: { 'Accept': 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
          });

          const data = await resp.json();
          if (!resp.ok || !data.success) {
            throw new Error(data.message || `HTTP ${resp.status}`);
          }

          showAiMessage('¡Guardado! La recomendación fue marcada como exitosa.');
          btn.innerHTML = '<i class="fas fa-check me-2"></i> Exitosa';
        } catch (e) {
          showError('No se pudo registrar: ' + e.message);
          btn.disabled = false;
          btn.innerHTML = originalHtml;
        }
      });
    });
  }

  async function recommend() {
    // Cancela una solicitud anterior si sigue en curso
    if (currentController) {
      currentController.abort();
    }
    currentController = new AbortController();

    hideAiMessage();
    renderSkeleton();
    updateResultsInfo(0);
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Generando...';

    aiMsgContainer.classList.remove('d-none', 'alert-danger');
    aiMsgContainer.classList.add('alert-success');
    aiMsgText.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> La IA está analizando el catálogo...';

    try {
      const petQuery = petSelect?.value || '';
      const prefQuery = prefSelect?.value || '';

      const baseUrl = btn.getAttribute('data-api-url');
      const url = `${baseUrl}?pet=${encodeURIComponent(petQuery)}&pref=${encodeURIComponent(prefQuery)}`;

      const resp = await fetch(url, {
        method: 'GET',
        headers: { 'Accept': 'application/json' },
        signal: currentController.signal,
      });

      if (!resp.ok) {
        throw new Error(`HTTP ${resp.status}`);
      }

      const data = await resp.json();

      await typeEffect(aiMsgText, data.message || 'Aquí tienes las recomendaciones.');
      render(data.items);
    } catch (e) {
      if (e.name === 'AbortError') return; // solicitud reemplazada, no es un error real
      showError('No se pudo generar recomendaciones: ' + e.message);
      renderEmptyState('Ocurrió un problema al consultar el catálogo. Intenta nuevamente.');
      updateResultsInfo(0);
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="fas fa-magic me-2"></i> Generar recomendaciones';
    }
  }

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    recommend();
  });

  if (btnReset) {
    btnReset.addEventListener('click', () => {
      if (currentController) currentController.abort();
      if (petSelect) petSelect.value = '';
      if (prefSelect) prefSelect.value = '';
      hideAiMessage();
      updateResultsInfo(0);
      grid.innerHTML = `
        <div class="col-12">
          <div class="rc-empty-state">
            <div class="rc-empty-icon">
              <i class="fas fa-wand-magic-sparkles"></i>
            </div>
            <p class="mb-1 fw-bold text-dark">¿Qué producto recomiendas hoy?</p>
            <p class="text-muted small mb-0">
              Elige una categoría o preferencia y presiona
              <strong>"Generar recomendaciones"</strong>
              para que la IA sugiera productos del catálogo.
            </p>
          </div>
        </div>`;
    });
    recommend();
  }
})();