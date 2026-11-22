// Formulario de cita: horarios libres por veterinario
document.addEventListener('DOMContentLoaded', function () {
    const fechaCita = document.getElementById('fechaCita');
    const horaCita = document.getElementById('horaCita');
    const veterinarioId = document.getElementById('veterinarioId');
    const veterinarioNombre = document.getElementById('veterinarioNombre');
    const filtroVet = document.getElementById('filtroVeterinario');
    const vetAsignado = document.getElementById('veterinarioAsignado');
    const aviso = document.getElementById('sinHorariosAviso');
    const sugerencia = document.getElementById('sinHorariosSugerencia');
    const linkHorarios = document.getElementById('linkGestionarHorarios');

    if (!fechaCita || !horaCita) return;

    let slotsDelDia = [];

    function mostrarAviso(mostrar) {
        if (!aviso) return;
        aviso.classList.toggle('d-none', !mostrar);
        aviso.classList.toggle('d-flex', mostrar);
    }

    function setVeterinario(id, nombre) {
        if (veterinarioId) veterinarioId.value = id || '';
        if (veterinarioNombre) veterinarioNombre.value = nombre || 'Se asigna según el horario';
        if (vetAsignado) {
            vetAsignado.innerHTML = nombre
                ? '<i class="fas fa-check-circle text-success"></i> La cita se asignará a <b></b>.'
                : '<i class="fas fa-info-circle"></i> Elija un veterinario para ver solo sus horarios, o deje «Cualquiera».';
            const b = vetAsignado.querySelector('b');
            if (b) b.textContent = nombre;
        }
    }

    function pintarHoras() {
        const vetSel = filtroVet ? filtroVet.value : '';
        const lista = slotsDelDia.filter(s => !vetSel || s.veterinarioId === vetSel);
        setVeterinario('', '');

        if (lista.length === 0) {
            horaCita.innerHTML = '<option value="">No hay horarios disponibles</option>';
            horaCita.disabled = true;
            return;
        }
        horaCita.innerHTML = '<option value="">Seleccione un horario (' + lista.length + ' libres)...</option>';
        for (const slot of lista) {
            const opt = document.createElement('option');
            opt.value = slot.horaInicio;
            opt.textContent = vetSel
                ? slot.horaInicio + ' - ' + slot.horaFin
                : slot.horaInicio + ' - ' + slot.horaFin + '  ·  ' + slot.veterinarioNombre;
            opt.dataset.veterinarioId = slot.veterinarioId;
            opt.dataset.veterinarioNombre = slot.veterinarioNombre;
            horaCita.appendChild(opt);
        }
        horaCita.disabled = false;
    }

    async function sugerirProximaFecha(fecha) {
        if (!sugerencia) return;
        sugerencia.textContent = 'Buscando la próxima fecha con horarios...';
        if (linkHorarios) linkHorarios.href = '/recepcionista/agenda/horarios?fecha=' + encodeURIComponent(fecha);
        try {
            const res = await fetch('/recepcionista/agenda/api/proxima-disponible?desde=' + encodeURIComponent(fecha));
            const data = await res.json();
            if (data && data.fecha && data.fecha !== fecha) {
                sugerencia.innerHTML = 'Próxima fecha con turnos libres: <a href="#" id="usarProximaFecha" class="fw-semibold"></a>';
                const a = document.getElementById('usarProximaFecha');
                a.textContent = data.fechaTexto;
                a.addEventListener('click', function (e) {
                    e.preventDefault();
                    fechaCita.value = data.fecha;
                    cargarHorariosDisponibles(data.fecha);
                });
            } else {
                sugerencia.textContent = 'Ningún veterinario tiene turnos libres próximamente. Asigne un horario primero.';
            }
        } catch (e) {
            sugerencia.textContent = '';
        }
    }

    async function cargarHorariosDisponibles(fecha) {
        horaCita.innerHTML = '<option value="">Cargando...</option>';
        horaCita.disabled = true;
        setVeterinario('', '');
        mostrarAviso(false);

        try {
            const res = await fetch('/recepcionista/agenda/api/disponibles?fecha=' + encodeURIComponent(fecha));
            if (!res.ok) throw new Error('No se pudieron cargar los horarios disponibles.');
            const data = await res.json();
            slotsDelDia = Array.isArray(data) ? data : [];

            // Llenar el filtro con los veterinarios que tienen turnos libres ese día
            if (filtroVet) {
                const previo = filtroVet.value;
                const vets = new Map();
                slotsDelDia.forEach(s => vets.set(s.veterinarioId, s.veterinarioNombre));
                filtroVet.innerHTML = '<option value="">Cualquier veterinario disponible</option>';
                vets.forEach((nombre, id) => {
                    const cuantos = slotsDelDia.filter(s => s.veterinarioId === id).length;
                    const opt = document.createElement('option');
                    opt.value = id;
                    opt.textContent = nombre + ' (' + cuantos + ' libres)';
                    filtroVet.appendChild(opt);
                });
                filtroVet.value = vets.has(previo) ? previo : '';
                filtroVet.disabled = vets.size === 0;
            }

            pintarHoras();
            if (slotsDelDia.length === 0) {
                mostrarAviso(true);
                sugerirProximaFecha(fecha);
            }
        } catch (e) {
            horaCita.innerHTML = '<option value="">Error al cargar horarios</option>';
        }
    }

    if (filtroVet) filtroVet.addEventListener('change', pintarHoras);

    fechaCita.addEventListener('change', function () {
        if (fechaCita.value) {
            cargarHorariosDisponibles(fechaCita.value);
        } else {
            horaCita.innerHTML = '<option value="">Seleccione una fecha primero...</option>';
            horaCita.disabled = true;
            setVeterinario('', '');
            mostrarAviso(false);
        }
    });

    horaCita.addEventListener('change', function () {
        const selected = horaCita.options[horaCita.selectedIndex];
        setVeterinario(selected ? selected.dataset.veterinarioId : '', selected ? selected.dataset.veterinarioNombre : '');
    });

    // Si la fecha ya viene cargada (reprogramación o tras un error), cargar sus horarios
    if (fechaCita.value && filtroVet) {
        cargarHorariosDisponibles(fechaCita.value);
    }

    // No permitir enviar sin un turno con veterinario
    const form = fechaCita.closest('form');
    if (form && filtroVet) {
        form.addEventListener('submit', function (e) {
            if (!veterinarioId || !veterinarioId.value) {
                e.preventDefault();
                const msg = 'Seleccione un horario disponible para asignar el veterinario.';
                if (window.Swal) Swal.fire({ icon: 'warning', title: 'Falta el horario', text: msg });
                else alert(msg);
            }
        });
    }
});
