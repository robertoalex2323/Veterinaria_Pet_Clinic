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
});

function confirmarCancelacion(id) {
    const motivo = prompt('Por favor, ingrese el motivo de cancelación:');
    if (motivo !== null && motivo.trim().length > 0) {
        window.location.href = `/recepcionista/citas/cancelar/${id}?motivo=${encodeURIComponent(motivo)}`;
    }
}

function confirmarCancelacionOReprogramar(id) {
    // Menú simple: Aceptar => Reprogramar, Cancelar => Cancelar
    const quiereReprogramar = confirm('¿Desea REPROGRAMAR esta cita?\n\nAceptar: reprogramar\nCancelar: cancelar');

    if (!quiereReprogramar) {
        const motivoCancel = prompt('Motivo de cancelación:');
        if (motivoCancel !== null && motivoCancel.trim().length > 0) {
            window.location.href = `/recepcionista/citas/cancelar/${id}?motivo=${encodeURIComponent(motivoCancel)}`;
        }
        return;
    }

    // Reprogramar: pide fecha/hora nueva
    const fechaNueva = prompt('Ingrese la NUEVA FECHA (YYYY-MM-DD):');
    if (!fechaNueva) return;
    const horaNueva = prompt('Ingrese la NUEVA HORA (HH:mm):');
    if (!horaNueva) return;

    const motivo = prompt('Motivo de la reprogramación (opcional):') || '';

    // Navega al formulario de nueva cita con parámetros para precargar
    const params = new URLSearchParams();
    params.set('reprogramarDesdeId', String(id));
    params.set('fechaNueva', fechaNueva);
    params.set('horaNueva', horaNueva);
    if (motivo.trim().length > 0) params.set('motivo', motivo.trim());

    window.location.href = `/recepcionista/citas/nueva?${params.toString()}`;
}

