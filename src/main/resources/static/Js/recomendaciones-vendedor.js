(() => {
  const form = document.getElementById('rcForm');
  const grid = document.getElementById('recommendationsGrid');
  const btn = document.getElementById('btnRecommend');
  const btnReset = document.getElementById('btnReset');
  const resultsInfo = document.getElementById('rcResultsInfo');
  const aiMsgContainer = document.getElementById('aiMessageContainer');
  const aiMsgText = document.getElementById('aiMessageText');

  const especieSelect = document.getElementById('especieQuery');
  const etapaSelect = document.getElementById('etapaQuery');
  const necesidadSelect = document.getElementById('necesidadQuery');
  const petSelect = document.getElementById('petQuery');
  const prefSelect = document.getElementById('prefQuery');

  if (!grid || !btn || !form) return;

  let currentController = null;

  function escapeHtml(str) {
    return String(str || '').replace(/[&<>"']/g, (s) => ({
      '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    }[s]));
  }

  function showAiMessage(msg) {
    if (!aiMsgContainer) return;
    aiMsgContainer.classList.remove('d-none', 'alert-danger');
    aiMsgContainer.classList.add('alert-success');
    aiMsgText.textContent = msg;
  }

  function showError(msg) {
    if (!aiMsgContainer) return;
    aiMsgContainer.classList.remove('d-none', 'alert-success');
    aiMsgContainer.classList.add('alert-danger');
    aiMsgText.textContent = msg;
  }

  function hideAiMessage() {
    if (aiMsgContainer) aiMsgContainer.classList.add('d-none');
  }

  function typeEffect(element, text, speed = 14) {
    if (!element) return Promise.resolve();
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
      col.innerHTML = `
        <div class="card p-4 shadow-sm border-0 h-100 placeholder-glow">
          <div class="placeholder col-7 mb-2 bg-secondary py-2 rounded"></div>
          <div class="placeholder col-4 mb-3 bg-secondary rounded"></div>
          <div class="placeholder col-12 mb-2 bg-secondary py-3 rounded"></div>
          <div class="placeholder col-8 bg-secondary rounded"></div>
        </div>`;
      grid.appendChild(col);
    }
  }

  function renderEmptyState(message) {
    grid.innerHTML = `
      <div class="col-12">
        <div class="alert alert-warning mb-0 text-center py-4 shadow-sm">
          <i class="fas fa-exclamation-triangle fa-2x mb-2 d-block text-warning"></i>
          <strong>${escapeHtml(message)}</strong>
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
    resultsInfo.innerHTML = `
      <div class="d-flex justify-content-between align-items-center mb-2">
        <span class="badge bg-primary px-3 py-2 fs-6">
          <i class="fas fa-list-check me-1"></i> ${count} producto${count === 1 ? '' : 's'} recomendado${count === 1 ? '' : 's'} por afinidad ML
        </span>
        <small class="text-muted"><i class="fas fa-filter me-1"></i>Ordenado por mayor índice de confianza</small>
      </div>`;
  }

  function render(items) {
    grid.innerHTML = '';

    if (!items || items.length === 0) {
      renderEmptyState('No se encontraron productos compatibles con los parámetros biológicos y comerciales ingresados.');
      updateResultsInfo(0);
      return;
    }

    updateResultsInfo(items.length);

    const fragment = document.createDocumentFragment();
    for (const p of items) {
      const card = document.createElement('div');
      card.className = 'col-md-6 col-lg-4 mb-3';
      const confianza = typeof p.confianza === 'number' ? p.confianza : 75;

      // Color de confianza según score ML
      let barColor = 'bg-success';
      let badgeClass = 'bg-success-subtle text-success border-success';
      if (confianza < 80) {
        barColor = 'bg-primary';
        badgeClass = 'bg-primary-subtle text-primary border-primary';
      }
      if (confianza < 70) {
        barColor = 'bg-warning';
        badgeClass = 'bg-warning-subtle text-warning border-warning';
      }

      // XAI Factores explicativos
      let factoresHtml = '';
      if (p.factores && Array.isArray(p.factores) && p.factores.length > 0) {
        factoresHtml = `
          <div class="mt-2 p-2 rounded bg-light border small">
            <div class="fw-bold text-muted mb-1" style="font-size:0.75rem;">
              <i class="fas fa-brain text-primary me-1"></i> Factores de Inferencia (XAI):
            </div>
            <ul class="list-unstyled mb-0 ps-1" style="font-size:0.78rem;">
              ${p.factores.map(f => `<li class="text-secondary"><i class="fas fa-check text-success me-1"></i>${escapeHtml(f)}</li>`).join('')}
            </ul>
          </div>`;
      }

      card.innerHTML = `
        <div class="card h-100 shadow-sm border-0 border-top border-3 ${confianza >= 85 ? 'border-success' : 'border-primary'} position-relative">
          <div class="card-body d-flex flex-column p-3">
            
            <!-- Encabezado del Producto -->
            <div class="d-flex align-items-center gap-3 mb-2">
              <div class="rounded border shadow-sm d-flex align-items-center justify-content-center bg-white"
                   style="width: 54px; height: 54px; min-width: 54px; overflow: hidden;">
                ${p.foto ? `<img src="${escapeHtml(p.foto)}" alt="${escapeHtml(p.nombre)}" style="width:100%;height:100%;object-fit:cover;" onerror="this.style.display='none';">` : ''}
                <i class="fas fa-paw text-muted fa-lg" style="${p.foto ? 'display:none;' : ''}"></i>
              </div>
              <div class="flex-grow-1">
                <h6 class="fw-bold text-dark mb-0 text-truncate" title="${escapeHtml(p.nombre)}">${escapeHtml(p.nombre)}</h6>
                <div class="d-flex align-items-center gap-2 mt-1">
                  <span class="badge bg-light text-secondary border px-2">${escapeHtml(p.categoria || 'Catálogo')}</span>
                  <span class="small text-muted"><i class="fas fa-boxes me-1"></i>Stock: ${p.stock || 0}</span>
                </div>
              </div>
            </div>

            <!-- Precio y Badge de Afinidad -->
            <div class="d-flex justify-content-between align-items-center my-2 p-2 bg-light rounded">
              <div>
                <small class="text-muted d-block" style="font-size:0.72rem;">Precio Tienda</small>
                <span class="fs-5 fw-bold text-dark">S/ ${escapeHtml(p.precioFormateado || '0.00')}</span>
              </div>
              <div class="text-end">
                <span class="badge border ${badgeClass} px-2 py-1 fs-6">
                  <i class="fas fa-bolt me-1"></i>${confianza}% Afinidad
                </span>
              </div>
            </div>

            <!-- Barra de Afinidad ML -->
            <div class="mb-2">
              <div class="d-flex justify-content-between small text-muted mb-1" style="font-size:0.72rem;">
                <span>Score de Coincidencia ML</span>
                <span class="fw-bold">${confianza}/100</span>
              </div>
              <div class="progress" style="height: 6px;">
                <div class="progress-bar ${barColor} progress-bar-striped progress-bar-animated" role="progressbar" style="width: ${confianza}%" aria-valuenow="${confianza}" aria-valuemin="0" aria-valuemax="100"></div>
              </div>
            </div>

            <!-- Factores Explicativos XAI -->
            ${factoresHtml}

            <!-- Botones de Acción -->
            <div class="mt-auto pt-3 d-flex gap-2">
              <a href="/vendedor/ventas/registrar" class="btn btn-outline-success btn-sm flex-fill" title="Ir a registrar venta">
                <i class="fas fa-cart-plus me-1"></i> Vender
              </a>
              <button type="button" class="btn btn-primary btn-sm flex-fill rc-mark-exitosa shadow-sm"
                      data-producto-id="${escapeHtml(p.id)}"
                      data-categoria="${escapeHtml(p.categoria || '')}"
                      data-razon="${escapeHtml(p.razon || '')}">
                <i class="fas fa-check me-1"></i> Marcar Exitosa
              </button>
            </div>

          </div>
        </div>`;
      fragment.appendChild(card);
    }
    grid.appendChild(fragment);

    // Listeners de los botones "Marcar como exitosa"
    grid.querySelectorAll('.rc-mark-exitosa').forEach((btnEl) => {
      btnEl.addEventListener('click', async () => {
        const productoId = btnEl.getAttribute('data-producto-id');
        const categoria = btnEl.getAttribute('data-categoria');
        const razon = btnEl.getAttribute('data-razon');

        const baseUrlPost = '/vendedor/recomendaciones/registrar';
        btnEl.disabled = true;
        const originalHtml = btnEl.innerHTML;
        btnEl.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span> Guardando...';

        try {
          const params = new URLSearchParams();
          params.append('productoId', productoId);
          if (categoria) params.append('categoria', categoria);
          if (razon) params.append('razon', razon);

          const resp = await fetch(baseUrlPost, {
            method: 'POST',
            headers: { 'Accept': 'application/json', 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
            body: params.toString()
          });

          const data = await resp.json();
          if (!resp.ok || !data.success) {
            throw new Error(data.message || `HTTP ${resp.status}`);
          }

          showAiMessage('¡Recomendación registrada con éxito en el histórico del sistema!');
          btnEl.classList.remove('btn-primary');
          btnEl.classList.add('btn-success');
          btnEl.innerHTML = '<i class="fas fa-check-double me-1"></i> Registrada';
        } catch (e) {
          showError('No se pudo registrar la recomendación: ' + e.message);
          btnEl.disabled = false;
          btnEl.innerHTML = originalHtml;
        }
      });
    });
  }

  async function recommend() {
    if (currentController) {
      currentController.abort();
    }
    currentController = new AbortController();

    hideAiMessage();
    renderSkeleton(3);
    updateResultsInfo(0);

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span> Calculando Afinidad ML...';

    if (aiMsgContainer) {
      aiMsgContainer.classList.remove('d-none', 'alert-danger');
      aiMsgContainer.classList.add('alert-success');
      aiMsgText.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span> Procesando características vectoriales del paciente...';
    }

    try {
      const especie = especieSelect ? especieSelect.value : '';
      const etapa = etapaSelect ? etapaSelect.value : '';
      const necesidad = necesidadSelect ? necesidadSelect.value : '';
      const petQuery = petSelect ? petSelect.value : '';
      const prefQuery = prefSelect ? prefSelect.value : '';

      const baseUrl = btn.getAttribute('data-api-url') || '/vendedor/recomendaciones/api';
      const params = new URLSearchParams();
      if (especie) params.append('especie', especie);
      if (etapa) params.append('etapa', etapa);
      if (necesidad) params.append('necesidad', necesidad);
      if (petQuery) params.append('pet', petQuery);
      if (prefQuery) params.append('pref', prefQuery);

      const url = `${baseUrl}?${params.toString()}`;

      const resp = await fetch(url, {
        method: 'GET',
        headers: { 'Accept': 'application/json' },
        signal: currentController.signal,
      });

      if (!resp.ok) {
        throw new Error(`HTTP ${resp.status}`);
      }

      const data = await resp.json();

      await typeEffect(aiMsgText, data.message || 'Se ejecutó la inferencia del modelo predictivo.');
      render(data.items);
    } catch (e) {
      if (e.name === 'AbortError') return;
      showError('Ocurrió un error al procesar las recomendaciones: ' + e.message);
      renderEmptyState('No se pudo completar el análisis predictivo. Por favor, intenta de nuevo.');
      updateResultsInfo(0);
    } finally {
      btn.disabled = false;
      btn.innerHTML = '<i class="fas fa-microchip me-2"></i> Ejecutar Inferencia de Machine Learning';
    }
  }

  form.addEventListener('submit', (e) => {
    e.preventDefault();
    recommend();
  });

  if (btnReset) {
    btnReset.addEventListener('click', () => {
      if (currentController) currentController.abort();
      if (especieSelect) especieSelect.value = '';
      if (etapaSelect) etapaSelect.value = '';
      if (necesidadSelect) necesidadSelect.value = '';
      if (petSelect) petSelect.value = '';
      if (prefSelect) prefSelect.value = '';
      hideAiMessage();
      updateResultsInfo(0);
      grid.innerHTML = `
        <div class="col-12">
          <div class="rc-empty-state text-center py-5 border rounded bg-light">
            <div class="rc-empty-icon text-primary mb-3">
              <i class="fas fa-wand-magic-sparkles fa-3x opacity-50"></i>
            </div>
            <h5 class="fw-bold text-dark">¿Listo para predecir las mejores recomendaciones?</h5>
            <p class="text-muted small mb-3" style="max-width: 500px; margin: 0 auto;">
              Selecciona la especie, etapa de vida y necesidad de la mascota del cliente y presiona 
              <strong>"Ejecutar Inferencia de Machine Learning"</strong>.
            </p>
          </div>
        </div>`;
    });
  }

  // Ejecución inicial automática al cargar
  setTimeout(recommend, 200);
})();
