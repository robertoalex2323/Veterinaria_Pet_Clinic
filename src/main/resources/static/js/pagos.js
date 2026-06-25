document.addEventListener('DOMContentLoaded', function() {
    // Inicializar Select2 al abrir el modal (para que funcione correctamente dentro del modal)
    const modalPagoElement = document.getElementById('modalPago');
    
    if (modalPagoElement) {
        modalPagoElement.addEventListener('shown.bs.modal', function () {
            if (typeof jQuery !== 'undefined' && typeof jQuery.fn.select2 !== 'undefined') {
                $('#clienteId').select2({
                    theme: 'bootstrap-5',
                    dropdownParent: $('#modalPago'), // Crucial para modals de Bootstrap
                    placeholder: 'Busca y selecciona un cliente...',
                    width: '100%'
                });
            }
            // Focus en monto
            document.getElementById('monto').focus();
        });
    }

    // Validación de formulario
    const pagoForm = document.getElementById('pagoForm');
    if (pagoForm) {
        pagoForm.addEventListener('submit', function(e) {
            const montoInput = document.getElementById('monto');
            if (montoInput && parseFloat(montoInput.value) <= 0) {
                e.preventDefault();
                alert('El monto debe ser mayor a 0.');
                montoInput.focus();
                return;
            }
            
            // Validaciones por método de pago
            const metodoPago = document.getElementById('metodoPago').value;
            if (metodoPago === 'Tarjeta') {
                const numTarjeta = document.getElementById('numeroTarjeta').value.replace(/\s+/g, '');
                if (numTarjeta.length < 13) {
                    e.preventDefault();
                    alert('Ingrese un número de tarjeta válido.');
                    document.getElementById('numeroTarjeta').focus();
                }
            } else if (metodoPago === 'Transferencia') {
                const fileInput = document.getElementById('comprobante');
                if (fileInput.files.length === 0) {
                    e.preventDefault();
                    alert('Por favor, suba el voucher de la transferencia.');
                }
            } else if (metodoPago === 'Yape/Plin') {
                // En Yape/Plin el backend exige numeroComprobanteYape
                const numeroComprobanteYapeInput = document.getElementById('numeroComprobanteYape');
                if (!numeroComprobanteYapeInput) {
                    e.preventDefault();
                    alert('No se encontró el campo de comprobante Yape/Plin.');
                    return;
                }

                // Si no se llenó, pedir al usuario (fallback porque el QR real no retorna valor automáticamente)
                if (!numeroComprobanteYapeInput.value || numeroComprobanteYapeInput.value.trim().length === 0) {
                    const val = window.prompt('Ingrese el número de comprobante (Yape/Plin):');
                    if (!val || val.trim().length === 0) {
                        e.preventDefault();
                        alert('Debe ingresar el número de comprobante Yape/Plin.');
                        numeroComprobanteYapeInput.focus();
                        return;
                    }
                    numeroComprobanteYapeInput.value = val.trim();
                }

                // Validación final
                if (numeroComprobanteYapeInput.value.trim().length === 0) {
                    e.preventDefault();
                    alert('Debe ingresar el número de comprobante Yape/Plin.');
                }
            }
        });
    }

    // Si abres el formulario desde una cita, validamos que no esté pagada (protección extra frontend)
    // (se usa también para el input manual de citaId)
function mostrarModalYaPagada(mensaje) {
        // Si existe modal bootstrap usamos el primero con id=modalInfo (si tu layout lo tiene), si no usamos alert
        const modal = document.getElementById('modalInfo');
        if (modal && window.bootstrap) {
            const body = modal.querySelector('.modal-body');
            if (body) body.textContent = mensaje;
            const instance = window.bootstrap.Modal.getOrCreateInstance(modal);
            instance.show();
            return;
        }
        alert(mensaje);
    }


    function validarCitaPagadaYDesbloquearUI(citaId) {
        if (!citaId || parseInt(citaId) <= 0) return Promise.resolve(false);
        return fetch(`/recepcionista/pagos/estado-cita/${citaId}`)
            .then(r => r.json())
            .then(data => {
                if (data && data.ok && data.pagada) {
                    mostrarModalYaPagada('Usted ya ha pagado esta cita.');
                    return true;
                }
                return false;
            })
            .catch(() => false);
    }

    // Si existe modalPago en esta página, dejamos la lógica anterior intacta
    const modalPago = document.getElementById('modalPago');
    if (modalPago) {
        modalPago.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            if (!button) return;
            const citaId = button.getAttribute('data-cita-id');
            const citaFecha = button.getAttribute('data-cita-fecha');
            const citaMascota = button.getAttribute('data-cita-mascota');

            const citaIdInput = document.querySelector('input[name="citaId"]');
            const citaIdLabel = document.getElementById('citaIdLabel');
            const citaFechaHora = document.getElementById('citaFechaHora');
            const citaMascotaEl = document.getElementById('citaMascota');
            const citaEstadoBadge = document.getElementById('citaEstadoBadge');

            if (citaIdInput && citaId) citaIdInput.value = citaId;
            if (citaIdLabel) citaIdLabel.textContent = citaId ? citaId : '-';
            if (citaFechaHora) citaFechaHora.textContent = citaFecha ? citaFecha : '-';
            if (citaMascotaEl) citaMascotaEl.textContent = citaMascota ? citaMascota : '-';
            if (citaEstadoBadge) citaEstadoBadge.textContent = citaId ? 'Cita seleccionada' : 'Sin asignar';

            // Bloquear abrir si está pagada
            if (citaId) {
                validarCitaPagadaYDesbloquearUI(citaId).then(pagada => {
                    if (pagada && window.bootstrap) {
                        // Cierra el modal si se llegó a abrir
                        const instance = window.bootstrap.Modal.getOrCreateInstance(modalPago);
                        instance.hide();
                    }
                });
            }
        });
    }


    // Lógica para campos dinámicos de Método de Pago
    const metodoPagoSelect = document.getElementById('metodoPago');
    const tarjetaFields = document.getElementById('tarjetaFields');
    const transferenciaFields = document.getElementById('transferenciaFields');
    const yapeFields = document.getElementById('yapeFields');


    function hideAllDynamicFields() {
        if(tarjetaFields) tarjetaFields.classList.add('d-none');
        if(transferenciaFields) transferenciaFields.classList.add('d-none');
        if(yapeFields) yapeFields.classList.add('d-none');
    }

    if (metodoPagoSelect) {
        metodoPagoSelect.addEventListener('change', function() {
            hideAllDynamicFields();
            
            if (this.value === 'Tarjeta') {
                tarjetaFields.classList.remove('d-none');
            } else if (this.value === 'Transferencia') {
                transferenciaFields.classList.remove('d-none');
            } else if (this.value === 'Yape/Plin') {
                yapeFields.classList.remove('d-none');
            }
        });
        
        // Trigger inicial por si recarga la página
        metodoPagoSelect.dispatchEvent(new Event('change'));
    }

    // Detección automática de tipo de Tarjeta
    const numeroTarjetaInput = document.getElementById('numeroTarjeta');
    const cardIcon = document.getElementById('cardIcon');

    if (numeroTarjetaInput) {
        numeroTarjetaInput.addEventListener('input', function(e) {
            // Formatear agrupando de a 4
            let value = e.target.value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
            let formattedValue = '';
            for (let i = 0; i < value.length; i++) {
                if (i > 0 && i % 4 === 0) {
                    formattedValue += ' ';
                }
                formattedValue += value[i];
            }
            e.target.value = formattedValue;

            // Detectar marca
            const firstDigit = value.charAt(0);
            const firstTwoDigits = parseInt(value.substring(0, 2));

            if (firstDigit === '4') {
                cardIcon.innerHTML = '<i class="fab fa-cc-visa text-primary fa-lg"></i>';
            } else if (firstTwoDigits >= 51 && firstTwoDigits <= 55) {
                cardIcon.innerHTML = '<i class="fab fa-cc-mastercard text-danger fa-lg"></i>';
            } else if (firstDigit === '3') {
                cardIcon.innerHTML = '<i class="fab fa-cc-amex text-info fa-lg"></i>';
            } else {
                cardIcon.innerHTML = '<i class="fas fa-credit-card text-muted"></i>';
            }
        });
    }
    // Autocompletar info de cita al escribir el ID manualmente
    const citaIdInput = document.getElementById('citaIdInput');
    if (citaIdInput) {
        let citaTimer = null;
        citaIdInput.addEventListener('input', function () {
            clearTimeout(citaTimer);
            const id = this.value.trim();

            const citaMascotaEl   = document.getElementById('citaMascota');
            const citaFechaHoraEl = document.getElementById('citaFechaHora');
            const citaIdLabelEl   = document.getElementById('citaIdLabel');
            const citaEstadoBadge = document.getElementById('citaEstadoBadge');
            const montoInput      = document.getElementById('monto');

            if (!id || parseInt(id) <= 0) {
                if (citaMascotaEl)   citaMascotaEl.textContent   = '-';
                if (citaFechaHoraEl) citaFechaHoraEl.textContent = '-';
                if (citaIdLabelEl)   citaIdLabelEl.textContent   = '-';
                if (citaEstadoBadge) { citaEstadoBadge.textContent = 'Sin asignar'; citaEstadoBadge.className = 'badge bg-light text-muted border'; }
                return;
            }

            citaTimer = setTimeout(() => {
                fetch(`/recepcionista/pagos/info-cita/${id}`)
                    .then(r => r.json())
                    .then(data => {
                        if (data.ok) {
                            if (citaMascotaEl)   citaMascotaEl.textContent   = data.mascota + ' (Dueño: ' + data.cliente + ')';
                            if (citaFechaHoraEl) citaFechaHoraEl.textContent = data.fechaHora;
                            if (citaIdLabelEl)   citaIdLabelEl.textContent   = id;
                            if (citaEstadoBadge) {
                                citaEstadoBadge.textContent = data.estado;
                                citaEstadoBadge.className = 'badge border ' +
                                    (data.estado === 'AGENDADA' ? 'bg-primary text-white' :
                                    data.estado === 'COMPLETADA' ? 'bg-success text-white' : 'bg-secondary text-white');
                            }
                            // Si la cita ya tiene un pago previo, sugerir ese monto
                            if (data.montoPrevio && montoInput && !montoInput.value) {
                                montoInput.value = data.montoPrevio;
                            }
                        } else {
                            if (citaMascotaEl)   citaMascotaEl.textContent   = '⚠ ' + data.error;
                            if (citaFechaHoraEl) citaFechaHoraEl.textContent = '-';
                            if (citaIdLabelEl)   citaIdLabelEl.textContent   = '-';
                            if (citaEstadoBadge) { citaEstadoBadge.textContent = 'No encontrada'; citaEstadoBadge.className = 'badge bg-danger text-white'; }
                        }
                    })
                    .catch(() => {
                        if (citaEstadoBadge) { citaEstadoBadge.textContent = 'Error de conexión'; citaEstadoBadge.className = 'badge bg-warning text-dark'; }
                    });
            }, 500); // espera 500ms después de que el usuario deja de escribir
        });
    }
});