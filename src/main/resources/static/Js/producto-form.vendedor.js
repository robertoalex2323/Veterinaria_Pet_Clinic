(() => {
  const form = document.getElementById('productoForm');
  if (!form) return;

  const nombreInput = form.querySelector('input[name="nombre"]');
  const categoriaInput = form.querySelector('input[name="categoria"]');
  const precioInput = form.querySelector('input[name="precio"]');
  const stockInput = form.querySelector('input[name="stock"]');
  const descripcionInput = document.getElementById('descripcionInput');
  const descripcionCounter = document.getElementById('descripcionCounter');
  const fotoInput = document.getElementById('fotoInput');
  const fotoError = document.getElementById('fotoError');
  let fotoPreviewImg = document.getElementById('fotoPreviewImg');
  const fotoPreviewPlaceholder = document.getElementById('fotoPreviewPlaceholder');
  const eliminarFotoCheck = document.getElementById('eliminarFotoCheck');
  const btnGuardar = document.getElementById('btnGuardarProducto');

  const MAX_FOTO_BYTES = 2 * 1024 * 1024; // 2MB
  const TIPOS_VALIDOS = ['image/png', 'image/jpeg', 'image/webp', 'image/gif'];

  function showFieldError(input, show) {
    const wrapper = input.closest('.col-md-6, .col-md-12');
    const feedback = wrapper ? wrapper.querySelector(`[data-error-for="${input.name}"]`) : null;
    if (!feedback) return;
    feedback.classList.toggle('d-none', !show);
    input.classList.toggle('is-invalid', show);
  }

  function ensurePreviewImg() {
    if (fotoPreviewImg) return fotoPreviewImg;
    const wrap = document.getElementById('fotoPreviewWrap');
    if (!wrap) return null;
    const img = document.createElement('img');
    img.id = 'fotoPreviewImg';
    img.className = 'pf-foto-img';
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
      fotoError.classList.add('d-none');
      fotoError.textContent = '';

      const file = fotoInput.files && fotoInput.files[0];
      if (!file) return;

      if (!TIPOS_VALIDOS.includes(file.type)) {
        fotoError.textContent = 'Formato no permitido. Usa JPG, PNG, WEBP o GIF.';
        fotoError.classList.remove('d-none');
        fotoInput.value = '';
        return;
      }

      if (file.size > MAX_FOTO_BYTES) {
        fotoError.textContent = 'La imagen supera el máximo de 2MB.';
        fotoError.classList.remove('d-none');
        fotoInput.value = '';
        return;
      }

      // Si elige una foto nueva, ya no tiene sentido "eliminar la actual"
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

  // Si marca "eliminar foto actual", limpiamos cualquier archivo seleccionado
  if (eliminarFotoCheck) {
    eliminarFotoCheck.addEventListener('change', () => {
      if (eliminarFotoCheck.checked) {
        if (fotoInput) fotoInput.value = '';
        if (fotoPreviewImg) fotoPreviewImg.classList.add('d-none');
        if (fotoPreviewPlaceholder) fotoPreviewPlaceholder.classList.remove('d-none');
      }
    });
  }

  // --- Validación en el envío (además de la del backend) ---
  form.addEventListener('submit', (e) => {
    let valido = true;

    const nombreOk = nombreInput.value.trim().length > 0;
    showFieldError(nombreInput, !nombreOk);
    valido = valido && nombreOk;

    const categoriaOk = categoriaInput.value.trim().length > 0;
    showFieldError(categoriaInput, !categoriaOk);
    valido = valido && categoriaOk;

    const precioNum = parseFloat(precioInput.value);
    const precioOk = precioInput.value !== '' && !Number.isNaN(precioNum) && precioNum > 0;
    showFieldError(precioInput, !precioOk);
    valido = valido && precioOk;

    const stockNum = parseInt(stockInput.value, 10);
    const stockOk = stockInput.value === '' || (!Number.isNaN(stockNum) && stockNum >= 0);
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
      btnGuardar.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span> Guardando...';
    }
  });

  [nombreInput, categoriaInput, precioInput, stockInput].forEach((input) => {
    if (!input) return;
    input.addEventListener('input', () => showFieldError(input, false));
  });
})();