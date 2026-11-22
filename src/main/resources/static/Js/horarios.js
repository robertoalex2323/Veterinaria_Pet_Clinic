// Horarios de veterinarios: cálculo automático de duración y resumen de turnos
document.addEventListener('DOMContentLoaded', function () {
    const inicio = document.getElementById('genInicio');
    const fin = document.getElementById('genFin');
    const duracion = document.getElementById('genDuracion');
    const presets = document.getElementById('duracionPresets');
    const desde = document.getElementById('genDesde');
    const hasta = document.getElementById('genHasta');
    const diasWrap = document.getElementById('diasWrap');
    const resTotal = document.getElementById('resTotal');
    const resTurnos = document.getElementById('resTurnos');
    const resDetalle = document.getElementById('resDetalle');
    const resumen = document.getElementById('resumenGen');
    const form = document.getElementById('formGenerar');

    if (!inicio || !fin || !duracion) return;

    // 'all' = un solo turno que ocupa todo el rango (se recalcula al cambiar las horas)
    let modo = 'all';

    const aMin = (hhmm) => {
        if (!hhmm) return null;
        const [h, m] = hhmm.split(':').map(Number);
        return h * 60 + m;
    };
    const aHora = (min) => String(Math.floor(min / 60)).padStart(2, '0') + ':' + String(min % 60).padStart(2, '0');
    const formatoDuracion = (min) => {
        const h = Math.floor(min / 60), m = min % 60;
        if (h && m) return `${min} min (${h} h ${m} min)`;
        if (h) return `${min} min (${h} h)`;
        return `${min} min`;
    };

    function marcarPreset() {
        if (!presets) return;
        presets.querySelectorAll('button').forEach(b => {
            const activo = (modo === 'all' && b.dataset.min === 'all') || (modo !== 'all' && b.dataset.min === String(duracion.value));
            b.classList.toggle('active', activo);
        });
    }

    function recalcular() {
        const ini = aMin(inicio.value);
        const fn = aMin(fin.value);
        const total = (ini !== null && fn !== null) ? fn - ini : 0;

        if (total <= 0) {
            resTotal.textContent = '—';
            resTurnos.textContent = '0';
            resDetalle.textContent = 'La hora fin debe ser mayor que la hora de inicio.';
            resumen.classList.add('is-invalid');
            return;
        }
        resumen.classList.remove('is-invalid');

        if (modo === 'all') {
            duracion.value = total;
        }
        const dur = parseInt(duracion.value, 10) || 0;
        resTotal.textContent = formatoDuracion(total);

        if (dur < 15) {
            resTurnos.textContent = '0';
            resDetalle.textContent = 'La duración mínima es 15 minutos.';
            resumen.classList.add('is-invalid');
            return;
        }
        if (dur > total) {
            resTurnos.textContent = '0';
            resDetalle.textContent = `La duración (${dur} min) es mayor que el rango (${total} min).`;
            resumen.classList.add('is-invalid');
            return;
        }

        const n = Math.floor(total / dur);
        const sobra = total % dur;
        resTurnos.textContent = n;
        const lista = [];
        for (let i = 0; i < Math.min(n, 4); i++) {
            lista.push(`${aHora(ini + i * dur)}–${aHora(ini + (i + 1) * dur)}`);
        }
        let detalle = lista.join(', ') + (n > 4 ? ` … ${aHora(ini + (n - 1) * dur)}–${aHora(ini + n * dur)}` : '');
        if (sobra) detalle += ` · Sobran ${sobra} min al final`;
        resDetalle.textContent = detalle;
        marcarPreset();
    }

    inicio.addEventListener('input', recalcular);
    fin.addEventListener('input', recalcular);
    inicio.addEventListener('change', recalcular);
    fin.addEventListener('change', recalcular);
    duracion.addEventListener('input', function () {
        modo = 'manual';
        recalcular();
    });

    if (presets) {
        presets.addEventListener('click', function (e) {
            const btn = e.target.closest('button[data-min]');
            if (!btn) return;
            if (btn.dataset.min === 'all') {
                modo = 'all';
            } else {
                modo = 'manual';
                duracion.value = btn.dataset.min;
            }
            recalcular();
            marcarPreset();
        });
    }

    // Rango de fechas: "hasta" no puede ser menor que "desde"; los días solo aplican a rangos
    function syncFechas() {
        if (!desde || !hasta) return;
        hasta.min = desde.value;
        if (!hasta.value || hasta.value < desde.value) hasta.value = desde.value;
        if (diasWrap) diasWrap.classList.toggle('dias-disabled', hasta.value === desde.value);
    }
    if (desde) desde.addEventListener('change', syncFechas);
    if (hasta) hasta.addEventListener('change', syncFechas);
    syncFechas();

    document.querySelectorAll('[data-dias]').forEach(a => {
        a.addEventListener('click', function (e) {
            e.preventDefault();
            const sel = this.dataset.dias.split(',');
            document.querySelectorAll('input[name="dias"]').forEach(chk => {
                chk.checked = sel.includes(chk.value);
            });
        });
    });

    if (form) {
        form.addEventListener('submit', function (e) {
            if (resumen.classList.contains('is-invalid')) {
                e.preventDefault();
                if (window.Swal) {
                    Swal.fire({ icon: 'warning', title: 'Revise el horario', text: resDetalle.textContent });
                } else {
                    alert(resDetalle.textContent);
                }
            }
        });
    }

    // Confirmación al eliminar turnos
    document.querySelectorAll('.form-eliminar-turno').forEach(f => {
        f.addEventListener('submit', function (e) {
            if (f.dataset.ok) return;
            e.preventDefault();
            const confirmar = () => { f.dataset.ok = '1'; f.submit(); };
            if (window.Swal) {
                Swal.fire({
                    icon: 'question', title: '¿Eliminar este turno?',
                    text: 'El turno dejará de estar disponible para agendar citas.',
                    showCancelButton: true, confirmButtonText: 'Sí, eliminar', cancelButtonText: 'Cancelar',
                    confirmButtonColor: '#dc3545'
                }).then(r => { if (r.isConfirmed) confirmar(); });
            } else if (confirm('¿Eliminar este turno?')) {
                confirmar();
            }
        });
    });

    recalcular();
});
