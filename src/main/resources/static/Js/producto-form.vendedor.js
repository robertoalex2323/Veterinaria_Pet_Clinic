(() => {
  const form = document.getElementById('productoForm');
  if (!form) return;

  const nombreInput = document.getElementById('nombreInput') || form.querySelector('input[name="nombre"]');
  const categoriaInput = document.getElementById('categoriaInput') || form.querySelector('input[name="categoria"]');
  const precioInput = document.getElementById('precioInput') || form.querySelector('input[name="precio"]');
  const stockInput = document.getElementById('stockInput') || form.querySelector('input[name="stock"]');
  const descripcionInput = document.getElementById('descripcionInput');
  const descripcionCounter = document.getElementById('descripcionCounter');
  const fotoInput = document.getElementById('fotoInput');
  const fotoError = document.getElementById('fotoError');
  let fotoPreviewImg = document.getElementById('fotoPreviewImg');
  const fotoPreviewPlaceholder = document.getElementById('fotoPreviewPlaceholder');
  const eliminarFotoCheck = document.getElementById('eliminarFotoCheck');
  const btnGuardar = document.getElementById('btnGuardarProducto');

  // Elementos de la previsualización en vivo
  const previewNombre = document.getElementById('previewNombre');
  const previewCategoria = document.getElementById('previewCategoria');
  const previewPrecio = document.getElementById('previewPrecio');
  const previewStockBadge = document.getElementById('previewStockBadge');

  const MAX_FOTO_BYTES = 2 * 1024 * 1024; // 2MB
  const TIPOS_VALIDOS = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];

  function updateLivePreview() {
    if (previewNombre && nombreInput) {
      previewNombre.textContent = nombreInput.value.trim() || 'Nombre del Producto';
    }
    if (previewCategoria && categoriaInput) {
      previewCategoria.textContent = categoriaInput.value.trim() || 'Categoría';
    }
    if (previewPrecio && precioInput) {
      const p = parseFloat(precioInput.value);
      previewPrecio.textContent = !isNaN(p) && p > 0 ? `S/ ${p.toFixed(2)}` : 'S/ 0.00';
    }
    if (previewStockBadge && stockInput) {
      const s = parseInt(stockInput.value, 10);
      if (isNaN(s) || s <= 0) {
        previewStockBadge.className = 'badge bg-danger-subtle text-danger rounded-pill px-2 py-1 small';
        previewStockBadge.textContent = 'Agotado (0)';
      } else if (s <= 5) {
        previewStockBadge.className = 'badge bg-warning-subtle text-warning rounded-pill px-2 py-1 small';
        previewStockBadge.textContent = `Stock Bajo (${s})`;
      } else {
        previewStockBadge.className = 'badge bg-success-subtle text-success rounded-pill px-2 py-1 small';
        previewStockBadge.textContent = `En Stock (${s})`;
      }
    }
  }

  // Chips de sugerencia rápida para categoría
  document.querySelectorAll('.category-chip').forEach(btn => {
    btn.addEventListener('click', () => {
      if (categoriaInput) {
        categoriaInput.value = btn.dataset.cat;
        showFieldError(categoriaInput, false);
        updateLivePreview();
      }
    });
  });

  function showFieldError(input, show) {
    if (!input) return;
    const feedback = form.querySelector(`[data-error-for="${input.name}"]`);
    if (feedback) {
      feedback.classList.toggle('d-none', !show);
    }
    input.classList.toggle('is-invalid', show);
  }

  function ensurePreviewImg() {
    if (fotoPreviewImg) return fotoPreviewImg;
    const wrap = document.getElementById('fotoPreviewWrap');
    if (!wrap) return null;
    const img = document.createElement('img');
    img.id = 'fotoPreviewImg';
    img.className = 'img-fluid rounded-3 pf-foto-img';
    img.alt = 'Foto del producto';
    wrap.prepend(img);
    fotoPreviewImg = img;
    return img;
  }

  // --- Contador de descripción ---
  function updateDescripcionCounter() {
    if (!descripcionInput || !descripcionCounter) return;
    const len = descripcionInput.value.length;
    descripcionCounter.textContent = `${len} / 500`;
    descripcionCounter.classList.toggle('text-danger', len >= 500);
  }
  if (descripcionInput) {
    updateDescripcionCounter();
    descripcionInput.addEventListener('input', updateDescripcionCounter);
  }

  // --- Preview de la foto seleccionada ---
  if (fotoInput) {
    fotoInput.addEventListener('change', () => {
      if (fotoError) {
        fotoError.classList.add('d-none');
        fotoError.textContent = '';
      }

      const file = fotoInput.files && fotoInput.files[0];
      if (!file) return;

      if (!TIPOS_VALIDOS.includes(file.type)) {
        if (fotoError) {
          fotoError.textContent = 'Formato no permitido. Usa JPG, PNG, WEBP o GIF.';
          fotoError.classList.remove('d-none');
        }
        fotoInput.value = '';
        return;
      }

      if (file.size > MAX_FOTO_BYTES) {
        if (fotoError) {
          fotoError.textContent = 'La imagen supera el límite de 2MB.';
          fotoError.classList.remove('d-none');
        }
        fotoInput.value = '';
        return;
      }

      if (eliminarFotoCheck) eliminarFotoCheck.checked = false;

      const reader = new FileReader();
      reader.onload = (e) => {
        const img = ensurePreviewImg();
        if (!img) return;
        img.src = e.target.result;
        img.classList.remove('d-none');
        if (fotoPreviewPlaceholder) fotoPreviewPlaceholder.classList.add('d-none');
      };
      reader.readAsDataURL(file);
    });
  }

  // Si marca "eliminar foto actual"
  if (eliminarFotoCheck) {
    eliminarFotoCheck.addEventListener('change', () => {
      if (eliminarFotoCheck.checked) {
        if (fotoInput) fotoInput.value = '';
        if (fotoPreviewImg) fotoPreviewImg.classList.add('d-none');
        if (fotoPreviewPlaceholder) fotoPreviewPlaceholder.classList.remove('d-none');
      }
    });
  }

  // Validación y envío del formulario
  form.addEventListener('submit', (e) => {
    let valido = true;

    const nombreOk = nombreInput && nombreInput.value.trim().length > 0;
    showFieldError(nombreInput, !nombreOk);
    valido = valido && nombreOk;

    const categoriaOk = categoriaInput && categoriaInput.value.trim().length > 0;
    showFieldError(categoriaInput, !categoriaOk);
    valido = valido && categoriaOk;

    const precioNum = precioInput ? parseFloat(precioInput.value) : 0;
    const precioOk = precioInput && precioInput.value !== '' && !isNaN(precioNum) && precioNum > 0;
    showFieldError(precioInput, !precioOk);
    valido = valido && precioOk;

    const stockNum = stockInput ? parseInt(stockInput.value, 10) : -1;
    const stockOk = stockInput && stockInput.value !== '' && !isNaN(stockNum) && stockNum >= 0;
    showFieldError(stockInput, !stockOk);
    valido = valido && stockOk;

    if (!valido) {
      e.preventDefault();
      const primerInvalido = form.querySelector('.is-invalid');
      if (primerInvalido) primerInvalido.focus();
      return;
    }

    if (btnGuardar) {
      btnGuardar.disabled = true;
      btnGuardar.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Guardando producto...';
    }
  });

  [nombreInput, categoriaInput, precioInput, stockInput].forEach((input) => {
    if (!input) return;
    input.addEventListener('input', () => {
      showFieldError(input, false);
      updateLivePreview();
    });
  });

  // Inicializar preview al cargar
  updateLivePreview();
})();
