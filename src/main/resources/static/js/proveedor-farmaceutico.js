
function confirmDelete(nombre, id) {
    Swal.fire({
        title: '¿Eliminar proveedor?',
        text: 'Se eliminará "' + nombre + '". Esta acción no se puede revertir.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = '/farmaceutico/proveedores/eliminar/' + id;
            document.body.appendChild(form);
            form.submit();
        }
    });
    return false;
}

function initBuscadorProveedores() {
    const searchInput = document.getElementById('searchProveedor');
    if (!searchInput) return;

    const emptyState = document.getElementById('sinResultadosBusqueda');
    const contadorTotal = document.getElementById('totalProveedoresLabel');
    const cards = document.querySelectorAll('[data-search]');
    const totalCards = cards.length;

    searchInput.addEventListener('keyup', function () {
        const filter = this.value.trim().toLowerCase();
        let visibles = 0;

        cards.forEach((card) => {
            const searchData = card.getAttribute('data-search').toLowerCase();
            const match = searchData.includes(filter);
            card.style.display = match ? '' : 'none';
            if (match) visibles++;
        });

        if (emptyState) {
            emptyState.classList.toggle('d-none', visibles !== 0);
        }
        if (contadorTotal) {
            contadorTotal.textContent = filter
                ? visibles + ' de ' + totalCards
                : totalCards;
        }
    });
}

/**
 * Inicializa el formulario de nuevo/editar proveedor:
 *  - RUC: solo dígitos, máximo 11, con feedback visual válido/inválido.
 *  - Vista previa en vivo de cómo quedará la tarjeta del proveedor.
 *  - Bloquea el envío si el RUC no tiene 11 dígitos (el backend también
 *    valida esto, pero avisar antes evita el viaje al servidor).
 */
function initFormProveedor() {
    const form = document.getElementById('formProveedor');
    if (!form) return;

    const rucInput = document.getElementById('rucInput');
    const nombreInput = document.getElementById('nombreInput');
    const contactoInput = document.getElementById('contactoInput');
    const telefonoInput = document.getElementById('telefonoInput');
    const emailInput = document.getElementById('emailInput');
    const direccionInput = document.getElementById('direccionInput');

    const previewNombre = document.getElementById('previewNombre');
    const previewRuc = document.getElementById('previewRuc');
    const previewContacto = document.getElementById('previewContacto');
    const previewTelefono = document.getElementById('previewTelefono');
    const previewEmail = document.getElementById('previewEmail');
    const previewDireccion = document.getElementById('previewDireccion');

    function validarRuc() {
        if (!rucInput) return true;
        const valido = rucInput.value.length === 11;
        const vacio = rucInput.value.length === 0;
        rucInput.classList.toggle('is-invalid', !vacio && !valido);
        rucInput.classList.toggle('is-valid', valido);
        return valido;
    }

    if (rucInput) {
        rucInput.addEventListener('input', function () {
            // Solo dígitos, máximo 11 (formato RUC Perú)
            this.value = this.value.replace(/\D/g, '').slice(0, 11);
            validarRuc();
            actualizarPreview();
        });
        validarRuc();
    }

    function setPreview(el, valor, fallback) {
        if (!el) return;
        el.textContent = valor && valor.trim() !== '' ? valor : fallback;
    }

    function actualizarPreview() {
        setPreview(previewNombre, nombreInput ? nombreInput.value : '', 'Nombre del proveedor');
        setPreview(previewRuc, rucInput ? ('RUC: ' + (rucInput.value || '—')) : '', 'RUC: —');
        setPreview(previewContacto, contactoInput ? contactoInput.value : '', 'Sin contacto registrado');
        setPreview(previewTelefono, telefonoInput ? telefonoInput.value : '', 'Sin teléfono registrado');
        setPreview(previewEmail, emailInput ? emailInput.value : '', 'Sin email registrado');
        setPreview(previewDireccion, direccionInput ? direccionInput.value : '', 'Sin dirección registrada');
    }

    [nombreInput, contactoInput, telefonoInput, emailInput, direccionInput].forEach((el) => {
        if (el) el.addEventListener('input', actualizarPreview);
    });

    actualizarPreview();

    form.addEventListener('submit', function (e) {
        if (!validarRuc()) {
            e.preventDefault();
            if (rucInput) rucInput.focus();
            if (window.Swal) {
                Swal.fire({
                    icon: 'warning',
                    title: 'RUC inválido',
                    text: 'El RUC debe tener exactamente 11 dígitos.'
                });
            }
        }
    });
}

document.addEventListener('DOMContentLoaded', function () {
    initBuscadorProveedores();
    initFormProveedor();
});