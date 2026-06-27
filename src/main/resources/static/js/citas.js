document.addEventListener('DOMContentLoaded', function () {
    // Submit form automatically when date changes
    const fechaInput = document.getElementById('fechaFiltro');
    if (fechaInput) {
        fechaInput.addEventListener('change', function () {
            const form = document.getElementById('filtroCitasForm');
            if (form) form.submit();
        });
    }

    const fechaCita = document.getElementById('fechaCita');
    const horaCita = document.getElementById('horaCita');
    const veterinarioId = document.getElementById('veterinarioId');
    const veterinarioNombre = document.getElementById('veterinarioNombre');

    async function cargarHorariosDisponibles(fecha) {
        if (!horaCita) return;

        horaCita.innerHTML = '<option value="">Cargando...</option>';
        horaCita.disabled = true;
        if (veterinarioId) veterinarioId.value = '';
        if (veterinarioNombre) veterinarioNombre.value = 'Se asigna según el horario';

        try {
            const res = await fetch(`/recepcionista/agenda/api/disponibles?fecha=${encodeURIComponent(fecha)}`);
            if (!res.ok) throw new Error('No se pudieron cargar los horarios disponibles.');
            const data = await res.json();

            if (!Array.isArray(data) || data.length === 0) {
                horaCita.innerHTML = '<option value="">No hay horarios disponibles</option>';
                return;
            }

            horaCita.innerHTML = '<option value="">Seleccione un horario...</option>';
            for (const slot of data) {
                const opt = document.createElement('option');
                opt.value = slot.horaInicio;
                opt.textContent = `${slot.horaInicio} - ${slot.horaFin}`;
                if (slot.veterinarioId) opt.dataset.veterinarioId = slot.veterinarioId;
                if (slot.veterinarioNombre) opt.dataset.veterinarioNombre = slot.veterinarioNombre;
                horaCita.appendChild(opt);
            }

            horaCita.disabled = false;
        } catch (e) {
            horaCita.innerHTML = '<option value="">Error al cargar horarios</option>';
        }
    }

    if (fechaCita && horaCita) {
        fechaCita.addEventListener('change', function () {
            const fecha = fechaCita.value;
            if (fecha) {
                cargarHorariosDisponibles(fecha);
            } else {
                horaCita.innerHTML = '<option value="">Seleccione una fecha primero...</option>';
                horaCita.disabled = true;
                if (veterinarioId) veterinarioId.value = '';
                if (veterinarioNombre) veterinarioNombre.value = 'Se asigna según el horario';
            }
        });

        horaCita.addEventListener('change', function () {
            const selected = horaCita.options[horaCita.selectedIndex];
            const id = selected ? selected.dataset.veterinarioId : '';
            const nombre = selected ? selected.dataset.veterinarioNombre : '';
            if (veterinarioId) veterinarioId.value = id || '';
            if (veterinarioNombre) {
                veterinarioNombre.value = nombre || (horaCita.value ? 'No disponible' : 'Se asigna según el horario');
            }
        });
    }

    // Initialize Select2 if available for better pet searching in forms
    if (typeof jQuery !== 'undefined' && typeof jQuery.fn.select2 !== 'undefined') {
        const mascotaSelect = document.getElementById('mascotaId');
        if (mascotaSelect) {
            $('#mascotaId').select2({
                theme: 'bootstrap-5',
                placeholder: 'Busca una mascota o cliente...'
            });
        }
    }

    // Initialize modal actions (cancel & reprogram)
    initModalActions();
});

function initModalActions() {
    const btnConfirmarCancelarCita = document.getElementById('btnConfirmarCancelarCita');
    const motivoEl = document.getElementById('cancelarMotivo');
    const idElCancelar = document.getElementById('cancelarCitaId');

    const btnConfirmarReprogramar = document.getElementById('btnConfirmarReprogramar');
    const idElReprogramar = document.getElementById('reprogramarCitaId');
    const fechaEl = document.getElementById('reprogramarFecha');
    const horaEl = document.getElementById('reprogramarHora');
    const motivoReprogramarEl = document.getElementById('reprogramarMotivo');

    if (btnConfirmarCancelarCita && motivoEl && idElCancelar) {
        btnConfirmarCancelarCita.addEventListener('click', function () {
            const id = idElCancelar.value;
            const motivo = (motivoEl.value || '').trim();

            if (!id) {
                alert('ID de cita no encontrado.');
                return;
            }
            if (!motivo) {
                alert('Por favor ingresa el motivo de cancelación.');
                motivoEl.focus();
                return;
            }

            window.location.href = `/recepcionista/citas/cancelar/${id}?motivo=${encodeURIComponent(motivo)}`;
        });
    }

    if (btnConfirmarReprogramar && idElReprogramar && fechaEl && horaEl) {
        btnConfirmarReprogramar.addEventListener('click', function () {
            const id = idElReprogramar.value;
            const fecha = fechaEl.value;
            const hora = horaEl.value;
            const motivo = (motivoReprogramarEl && motivoReprogramarEl.value ? motivoReprogramarEl.value : '').trim();

            if (!id) {
                alert('ID de cita no encontrado.');
                return;
            }
            if (!fecha) {
                alert('Debe ingresar la nueva fecha.');
                return;
            }
            if (!hora) {
                alert('Debe ingresar la nueva hora.');
                return;
            }

            const params = new URLSearchParams();
            params.set('reprogramarDesdeId', String(id));
            params.set('fechaNueva', fecha);
            params.set('horaNueva', hora);
            if (motivo.length > 0) params.set('motivo', motivo);

            window.location.href = `/recepcionista/citas/nueva?${params.toString()}`;
        });
    }
}

function abrirModalCancelarCita(id) {
    const idEl = document.getElementById('cancelarCitaId');
    if (idEl) idEl.value = id;

    const modalEl = document.getElementById('modalCancelarCita');
    if (!modalEl) {
        const motivoCancel = prompt('Motivo de cancelación:');
        if (motivoCancel !== null && motivoCancel.trim().length > 0) {
            window.location.href = `/recepcionista/citas/cancelar/${id}?motivo=${encodeURIComponent(motivoCancel)}`;
        }
        return;
    }

    const modal = window.bootstrap?.Modal?.getOrCreateInstance(modalEl);
    modal?.show?.();
}

function abrirModalReprogramarCita(id) {
    const idEl = document.getElementById('reprogramarCitaId');
    if (idEl) idEl.value = id;

    const modalEl = document.getElementById('modalReprogramarCita');
    if (!modalEl) {
        console.error('No se encontró el modalReprogramarCita en el DOM. Verifica el fragmento/templates.');
        return;
    }


    const modal = window.bootstrap?.Modal?.getOrCreateInstance(modalEl);
    modal?.show?.();
}

function confirmarCancelacion(id) {
    // Usar SIEMPRE el modal (sin prompts) para capturar el motivo
    abrirModalCancelarCita(id);
}


function confirmarCancelacionOReprogramar(id) {
    // Menú simple: Aceptar => Reprogramar, Cancelar => Cancelar
    const quiereReprogramar = confirm('¿Desea REPROGRAMAR esta cita?\n\nAceptar: reprogramar\nCancelar: cancelar');

    if (!quiereReprogramar) {
        abrirModalCancelarCita(id);
        return;
    }

    abrirModalReprogramarCita(id);
}

