// ===================== PAGOS (Recepcionista) =====================
document.addEventListener('DOMContentLoaded', function () {
    const $ = (id) => document.getElementById(id);
    const soles = (n) => 'S/ ' + (Number.isFinite(n) ? n : 0).toFixed(2);

    const avisar = (titulo, texto, icono = 'warning') => {
        if (window.Swal) Swal.fire({ icon: icono, title: titulo, text: texto, confirmButtonColor: '#198754' });
        else alert(texto);
    };

    // ---------------- Filtros del historial ----------------
    const buscar = $('buscarPago');
    const filtroMetodo = $('filtroMetodo');
    const filtroEstado = $('filtroEstado');
    const filas = Array.from(document.querySelectorAll('#tablaPagos tbody tr[data-metodo]'));

    function filtrar() {
        const q = (buscar ? buscar.value : '').toLowerCase().trim();
        const m = filtroMetodo ? filtroMetodo.value : '';
        const e = filtroEstado ? filtroEstado.value : '';
        let visibles = 0;
        filas.forEach(tr => {
            const ok = (!q || tr.textContent.toLowerCase().includes(q))
                && (!m || tr.dataset.metodo === m)
                && (!e || tr.dataset.estado === e);
            tr.classList.toggle('d-none', !ok);
            if (ok) visibles++;
        });
        if ($('contadorPagos')) $('contadorPagos').textContent = visibles;
        if ($('sinResultados')) $('sinResultados').classList.toggle('d-none', visibles > 0 || filas.length === 0);
    }
    [buscar, filtroMetodo, filtroEstado].forEach(el => el && el.addEventListener('input', filtrar));

    // ---------------- Modal de pago ----------------
    const modalEl = $('modalPago');
    const form = $('pagoForm');
    if (!modalEl || !form) return;

    const monto = $('monto');
    const clienteSel = $('clienteId');
    const citaInput = $('citaIdInput');
    const metodos = form.querySelectorAll('input[name="metodoPago"]');
    const paneles = {
        'Efectivo': $('efectivoFields'),
        'Tarjeta': $('tarjetaFields'),
        'Yape/Plin': $('yapeFields'),
        'Transferencia': $('transferenciaFields')
    };

    const metodoActual = () => {
        const sel = form.querySelector('input[name="metodoPago"]:checked');
        return sel ? sel.value : 'Efectivo';
    };

    function actualizarResumen() {
        const total = parseFloat(monto.value) || 0;
        const sub = total / 1.18;
        $('resSubtotal').textContent = soles(sub);
        $('resIgv').textContent = soles(total - sub);
        $('resTotal').textContent = soles(total);
        $('btnMonto').textContent = soles(total);
        $('resMetodo').textContent = metodoActual();

        const opt = clienteSel.options[clienteSel.selectedIndex];
        $('resCliente').textContent = opt && opt.value ? opt.textContent.split(' · ')[0] : '—';
        const email = opt && opt.value ? (opt.dataset.email || '') : '';
        $('resEmail').textContent = !opt || !opt.value
            ? 'El comprobante PDF se enviará al correo del cliente si está registrado.'
            : (email ? 'El comprobante PDF se enviará a ' + email : 'El cliente no tiene correo: entregue el comprobante impreso.');
        if ($('clienteEmailInfo')) {
            $('clienteEmailInfo').innerHTML = opt && opt.value
                ? (email ? '<i class="fas fa-envelope text-success"></i> ' : '<i class="fas fa-exclamation-triangle text-warning"></i> Sin correo registrado')
                : '';
            if (email) $('clienteEmailInfo').append(email);
        }
        $('resCita').textContent = citaInput.value ? '#' + citaInput.value : '—';

        // Vuelto (solo efectivo)
        const recibido = parseFloat(($('montoRecibido') || {}).value) || 0;
        const vuelto = recibido - total;
        const vEl = $('vuelto');
        if (vEl) {
            vEl.textContent = recibido ? soles(Math.max(vuelto, 0)) : 'S/ 0.00';
            vEl.classList.toggle('text-danger', recibido > 0 && vuelto < 0);
            if (recibido > 0 && vuelto < 0) vEl.textContent = 'Falta ' + soles(-vuelto);
        }
    }

    function mostrarPanel() {
        const m = metodoActual();
        Object.entries(paneles).forEach(([k, el]) => el && el.classList.toggle('d-none', k !== m));
        actualizarResumen();
    }

    metodos.forEach(r => r.addEventListener('change', mostrarPanel));
    [monto, $('montoRecibido')].forEach(el => el && el.addEventListener('input', actualizarResumen));
    clienteSel.addEventListener('change', actualizarResumen);

    document.querySelectorAll('#montosRapidos [data-monto]').forEach(b => {
        b.addEventListener('click', () => {
            monto.value = parseFloat(b.dataset.monto).toFixed(2);
            actualizarResumen();
        });
    });

    if ($('resumenFecha')) {
        $('resumenFecha').textContent = new Date().toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });
    }

    // ---------------- Tarjeta: formato y marca ----------------
    const numTarjeta = $('numeroTarjeta');
    const cardIcon = $('cardIcon');
    if (numTarjeta) {
        numTarjeta.addEventListener('input', function (e) {
            const v = e.target.value.replace(/\D/g, '').slice(0, 16);
            e.target.value = v.replace(/(.{4})/g, '$1 ').trim();
            const d2 = parseInt(v.substring(0, 2), 10);
            if (v.startsWith('4')) cardIcon.innerHTML = '<i class="fab fa-cc-visa text-primary fa-lg"></i>';
            else if (d2 >= 51 && d2 <= 55) cardIcon.innerHTML = '<i class="fab fa-cc-mastercard text-danger fa-lg"></i>';
            else if (v.startsWith('34') || v.startsWith('37')) cardIcon.innerHTML = '<i class="fab fa-cc-amex text-info fa-lg"></i>';
            else cardIcon.innerHTML = '<i class="fas fa-credit-card"></i>';
        });
    }

    // ---------------- Cita de referencia ----------------
    let citaPagada = false;
    let citaTimer = null;
    function limpiarCita() {
        citaPagada = false;
        $('citaPreview').classList.add('d-none');
    }
    function consultarCita(id) {
        fetch('/recepcionista/pagos/info-cita/' + encodeURIComponent(id))
            .then(r => r.json())
            .then(data => {
                const prev = $('citaPreview');
                const badge = $('citaEstadoBadge');
                prev.classList.remove('d-none');
                if (!data.ok) {
                    $('citaMascota').textContent = 'Cita no encontrada';
                    $('citaFechaHora').textContent = 'Verifique el número de la cita';
                    badge.className = 'badge bg-danger-subtle text-danger-emphasis';
                    badge.textContent = 'No existe';
                    return;
                }
                $('citaMascota').textContent = data.mascota + ' · Dueño: ' + data.cliente;
                $('citaFechaHora').textContent = data.fechaHora + (data.motivo && data.motivo !== '-' ? ' · ' + data.motivo : '');
                badge.className = 'badge bg-primary-subtle text-primary-emphasis';
                badge.textContent = data.estado;
                if (data.montoPrevio && !monto.value) monto.value = data.montoPrevio;
                return fetch('/recepcionista/pagos/estado-cita/' + encodeURIComponent(id))
                    .then(r => r.json())
                    .then(est => {
                        citaPagada = !!(est && est.ok && est.pagada);
                        if (citaPagada) {
                            badge.className = 'badge bg-success';
                            badge.textContent = 'Ya pagada';
                        }
                    });
            })
            .catch(() => limpiarCita())
            .finally(actualizarResumen);
    }
    citaInput.addEventListener('input', function () {
        clearTimeout(citaTimer);
        const id = this.value.trim();
        actualizarResumen();
        if (!id || parseInt(id, 10) <= 0) { limpiarCita(); return; }
        citaTimer = setTimeout(() => consultarCita(id), 450);
    });
    if (citaInput.value) consultarCita(citaInput.value);

    // ---------------- Envío ----------------
    form.addEventListener('submit', function (e) {
        const total = parseFloat(monto.value);
        const m = metodoActual();
        let error = null;

        if (!clienteSel.value) error = ['Falta el cliente', 'Seleccione el cliente que realiza el pago.'];
        else if (!total || total < 0.1) error = ['Monto inválido', 'Ingrese un monto mayor a S/ 0.10.'];
        else if (citaPagada) error = ['Cita ya pagada', 'Esta cita ya tiene un pago registrado.'];
        else if (m === 'Tarjeta' && numTarjeta.value.replace(/\s/g, '').length < 13) {
            error = ['Tarjeta inválida', 'Ingrese un número de tarjeta válido.'];
        } else if (m === 'Transferencia' && (!$('comprobante').files || $('comprobante').files.length === 0)) {
            error = ['Falta el voucher', 'Adjunte el voucher de la transferencia.'];
        } else if (m === 'Yape/Plin') {
            const op = ($('numeroComprobanteYapeVisible').value || '').trim();
            $('numeroComprobanteYape').value = op;
            if (!op) error = ['Falta el N° de operación', 'Ingrese el número de operación de Yape/Plin.'];
        } else if (m === 'Efectivo') {
            const recibido = parseFloat($('montoRecibido').value) || 0;
            if (recibido > 0 && recibido < total) error = ['Monto recibido insuficiente', 'El monto recibido es menor que el total a cobrar.'];
        }

        if (error) {
            e.preventDefault();
            avisar(error[0], error[1]);
            return;
        }
        const btn = $('btnProcesarPago');
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Procesando...';
    });

    modalEl.addEventListener('shown.bs.modal', () => {
        if (!clienteSel.value) clienteSel.focus(); else monto.focus();
    });

    mostrarPanel();

    // Abrir automáticamente si se viene a cobrar una cita (o desde "Nuevo pago")
    if (modalEl.dataset.autoOpen === 'true' && window.bootstrap) {
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    }
});
