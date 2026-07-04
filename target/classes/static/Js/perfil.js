document.addEventListener('DOMContentLoaded', function() {
    
    // Funcionalidad para alternar la visibilidad de las contraseñas
    const togglePasswordButtons = document.querySelectorAll('.password-toggle');
    
    togglePasswordButtons.forEach(button => {
        button.addEventListener('click', function() {
            // El input siempre es el elemento anterior al group-text
            const input = this.parentElement.querySelector('input');
            const icon = this.querySelector('i');
            
            if (input.type === 'password') {
                input.type = 'text';
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            } else {
                input.type = 'password';
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            }
        });
    });

    function setValidityIndicator(indicatorEl, ok) {
        if (!indicatorEl) return;
        indicatorEl.textContent = ok ? '✔' : '✖';
        indicatorEl.classList.toggle('text-success', ok);
        indicatorEl.classList.toggle('text-danger', !ok);
        indicatorEl.setAttribute('aria-label', ok ? 'Cumple' : 'No cumple');
    }

    function ensureRequirementsUI() {
        // Si no existe, creamos el bloque de chequeo debajo de newPassword.
        const newPasswordInput = document.getElementById('newPassword');
        if (!newPasswordInput) return;

        let requirementsBox = document.getElementById('passwordRequirements');
        if (requirementsBox) return;

        requirementsBox = document.createElement('div');
        requirementsBox.id = 'passwordRequirements';
        requirementsBox.className = 'mt-2 small';

        requirementsBox.innerHTML = `
            <div class="fw-bold text-dark mb-1">Fortaleza mínima (para cambiar clave)</div>
            <div class="d-flex gap-2 align-items-center" style="line-height: 1.2;">
                <span id="reqMinLen" class="me-1 text-muted">✖</span>
                <span>Mínimo 6 caracteres</span>
            </div>
            <div class="d-flex gap-2 align-items-center" style="line-height: 1.2;">
                <span id="reqUpper" class="me-1 text-muted">✖</span>
                <span>Al menos una mayúscula</span>
            </div>
            <div class="d-flex gap-2 align-items-center" style="line-height: 1.2;">
                <span id="reqNumber" class="me-1 text-muted">✖</span>
                <span>Al menos un número</span>
            </div>
            <div class="d-flex gap-2 align-items-center" style="line-height: 1.2;">
                <span id="reqSymbol" class="me-1 text-muted">✖</span>
                <span>Al menos un símbolo</span>
            </div>
        `;

        // Inserta justo después del group del newPassword
        // newPasswordInput está dentro del group; lo colocamos después de su contenedor .input-group
        const inputGroup = newPasswordInput.closest('.input-group');
        if (inputGroup && inputGroup.parentElement) {
            inputGroup.parentElement.appendChild(requirementsBox);
        } else {
            newPasswordInput.parentElement.appendChild(requirementsBox);
        }
    }

    function getPasswordChecks(password) {
        const value = password || '';
        const trimmed = value.trim();

        const minLen = trimmed.length >= 6;
        const hasUpper = /[A-Z]/.test(trimmed);
        const hasNumber = /[0-9]/.test(trimmed);
        const hasSymbol = /[^A-Za-z0-9\s]/.test(trimmed);

        return { trimmed, minLen, hasUpper, hasNumber, hasSymbol };
    }

    // UI requirements
    ensureRequirementsUI();

    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    const formSeguridad = document.getElementById('formSeguridad');

    const reqMinLenEl = document.getElementById('reqMinLen');
    const reqUpperEl = document.getElementById('reqUpper');
    const reqNumberEl = document.getElementById('reqNumber');
    const reqSymbolEl = document.getElementById('reqSymbol');

    function updateRequirementsUI(password) {
        const { trimmed, minLen, hasUpper, hasNumber, hasSymbol } = getPasswordChecks(password);

        // Solo mostramos/actualizamos cuando el usuario intenta cambiar (hay contenido real)
        const isChanging = trimmed.length > 0;
        if (!isChanging) {
            setValidityIndicator(reqMinLenEl, false);
            setValidityIndicator(reqUpperEl, false);
            setValidityIndicator(reqNumberEl, false);
            setValidityIndicator(reqSymbolEl, false);
            return;
        }

        setValidityIndicator(reqMinLenEl, minLen);
        setValidityIndicator(reqUpperEl, hasUpper);
        setValidityIndicator(reqNumberEl, hasNumber);
        setValidityIndicator(reqSymbolEl, hasSymbol);
    }

    if (newPasswordInput) {
        newPasswordInput.addEventListener('input', function() {
            updateRequirementsUI(this.value);
        });
    }

    if (confirmPasswordInput) {
        confirmPasswordInput.addEventListener('input', function() {
            // No validamos aquí; el submit maneja.
        });
    }

    // Validación del formulario de contraseña
    if (formSeguridad) {
        formSeguridad.addEventListener('submit', function(e) {
            const currentPass = (document.getElementById('currentPassword')?.value || '');
            const newPass = (newPasswordInput?.value || '');
            const confirmPass = (confirmPasswordInput?.value || '');

            const { trimmed: newTrim, minLen, hasUpper, hasNumber, hasSymbol } = getPasswordChecks(newPass);

            // Si NO se intenta cambiar (vacío o solo espacios), no validamos fortaleza.
            const isChanging = newTrim.length > 0;

            if (isChanging) {
                // Confirmación
                const confirmTrim = (confirmPass || '').trim();
                if (confirmTrim.length === 0) {
                    e.preventDefault();
                    alert('Debes confirmar la nueva contraseña.');
                    if (confirmPasswordInput) confirmPasswordInput.focus();
                    return;
                }

                if (newTrim !== confirmTrim) {
                    e.preventDefault();
                    alert('La nueva contraseña y la confirmación no coinciden.');
                    if (confirmPasswordInput) confirmPasswordInput.focus();
                    return;
                }

                // Fortalezas: mínimas
                if (!minLen) {
                    e.preventDefault();
                    alert('La nueva contraseña debe tener al menos 6 caracteres.');
                    if (newPasswordInput) newPasswordInput.focus();
                    return;
                }

                if (!hasUpper || !hasNumber || !hasSymbol) {
                    e.preventDefault();
                    alert('La nueva contraseña debe incluir al menos una mayúscula, un número y un símbolo.');
                    if (newPasswordInput) newPasswordInput.focus();
                    return;
                }
            } else {
                // Si no se cambia contraseña: validamos confirm igual no usada.
                // No impedimos el submit; el backend decidirá si el currentPassword es requerido.
                // (No cambiamos comportamiento actual del backend).
            }

            // En caso de que todo esté bien, se envía
            // Nota: Copiamos nombre/email en campos ocultos para el endpoint actual.
            const nombreInput = document.getElementById('nombre');
            const emailInput = document.getElementById('email');
            const hiddenNombre = document.getElementById('hiddenNombre');
            const hiddenEmail = document.getElementById('hiddenEmail');

            if (hiddenNombre && nombreInput) hiddenNombre.value = nombreInput.value;
            if (hiddenEmail && emailInput) hiddenEmail.value = emailInput.value;
        });
    }
});

