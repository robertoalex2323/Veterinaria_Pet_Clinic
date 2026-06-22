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
});