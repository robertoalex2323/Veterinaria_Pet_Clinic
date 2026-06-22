(() => {
  const input = document.getElementById('universalSearchInput');
  const dropdown = document.getElementById('universalSearchDropdown');
  if (!input || !dropdown) return;

  let debounceTimer = null;

  function closeDropdown() {
    dropdown.classList.add('d-none');
    dropdown.innerHTML = '';
  }

  function normalize(s) {
    return (s || '').toString().toLowerCase().trim();
  }

  function renderSuggestions(items) {
    dropdown.innerHTML = '';

    if (!items || items.length === 0) {
      dropdown.innerHTML = `
        <div class="dropdown-item text-muted" style="cursor: default;">
          Sin resultados
        </div>`;
      dropdown.classList.remove('d-none');
      return;
    }

    for (const item of items) {
      const label = item.label || '';
      const sub = item.sublabel || '';
      const type = item.type || '';

      const itemEl = document.createElement('div');
      itemEl.className = 'dropdown-item';
      itemEl.setAttribute('role','button');

      itemEl.style.cursor = 'pointer';
      itemEl.innerHTML = `
        <div class="d-flex align-items-start justify-content-between gap-2">
          <div>
            <div style="font-weight: 700;">${label}</div>
            <div class="text-muted" style="font-size: 0.85rem;">${sub}</div>
          </div>
          <div class="badge bg-light text-dark border" style="font-size: 0.7rem; height: fit-content;">
            ${type}
          </div>
        </div>
      `;

      itemEl.addEventListener('click', () => {
        closeDropdown();
        if (item.url) window.location.href = item.url;
      });

      dropdown.appendChild(itemEl);
    }

    dropdown.classList.remove('d-none');
  }

  async function fetchSuggestions(q) {
    const query = normalize(q);
    if (!query || query.length < 2) {
      closeDropdown();
      return;
    }

    try {
      const res = await fetch(`/recepcionista/api/busqueda-universal?query=${encodeURIComponent(query)}`);
      if (!res.ok) throw new Error('Error en búsqueda');
      const data = await res.json();
      renderSuggestions(data || []);
    } catch (e) {
      // silencioso: mostramos sin resultados
      renderSuggestions([]);
    }
  }

  input.addEventListener('input', () => {
    const q = input.value;
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => fetchSuggestions(q), 220);
  });

  document.addEventListener('click', (e) => {
    const clickedInside = e.target === input || input.contains(e.target) || dropdown.contains(e.target);
    if (!clickedInside) closeDropdown();
  });

  // Ocultar si presionan ESC
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeDropdown();
  });

  // cargar con valor inicial si existe
  if (input.value) {
    fetchSuggestions(input.value);
  }
})();

