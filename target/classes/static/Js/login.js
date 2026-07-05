'use strict';

document.addEventListener('DOMContentLoaded', function () {

    // ===========================
    // REFERENCIAS AL DOM
    // ===========================
    const loginForm      = document.getElementById('loginForm');
    const btnLogin       = document.getElementById('btnLogin');
    const btnText        = btnLogin?.querySelector('.btn-text');
    const togglePassword = document.getElementById('togglePassword');
    const passwordInput  = document.getElementById('password');
    const usernameInput  = document.getElementById('username');
    const usernameError  = document.getElementById('usernameError');
    const passwordError  = document.getElementById('passwordError');

    // ===========================
    // TOGGLE MOSTRAR CONTRASEÑA
    // ===========================
    if (togglePassword && passwordInput) {
        togglePassword.addEventListener('click', function () {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';

            const icon = this.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-eye',       !isPassword);
                icon.classList.toggle('fa-eye-slash',  isPassword);
            }

            this.setAttribute('aria-label', isPassword ? 'Ocultar contraseña' : 'Mostrar contraseña');
            passwordInput.focus();
        });
    }

    // ===========================
    // VALIDACIÓN EN TIEMPO REAL
    // ===========================
    function clearError(input, errorEl) {
        if (!errorEl) return;
        errorEl.textContent = '';
        input.style.borderColor = '';
    }

    function showError(input, errorEl, message) {
        if (!errorEl) return;
        errorEl.textContent = message;
        input.style.borderColor = '#DC2626';
        input.style.boxShadow  = '0 0 0 3px rgba(220,38,38,0.12)';
    }

    function validateField(input, errorEl, rules) {
        const value = input.value.trim();
        for (const { test, message } of rules) {
            if (!test(value)) {
                showError(input, errorEl, message);
                return false;
            }
        }
        clearError(input, errorEl);
        input.style.boxShadow = '';
        return true;
    }

    if (usernameInput) {
        usernameInput.addEventListener('input', () =>
            validateField(usernameInput, usernameError, [
                { test: v => v.length > 0, message: 'El usuario es obligatorio.' },
                { test: v => v.length >= 3, message: 'Mínimo 3 caracteres.' },
            ])
        );
    }

    if (passwordInput) {
        passwordInput.addEventListener('input', () =>
            validateField(passwordInput, passwordError, [
                { test: v => v.length > 0, message: 'La contraseña es obligatoria.' },
                { test: v => v.length >= 6, message: 'Mínimo 6 caracteres.' },
            ])
        );
    }

    // ===========================
    // ENVÍO DEL FORMULARIO
    // ===========================
    if (loginForm && btnLogin) {
        loginForm.addEventListener('submit', function (e) {
            const usernameOk = validateField(usernameInput, usernameError, [
                { test: v => v.length > 0, message: 'El usuario es obligatorio.' },
                { test: v => v.length >= 3, message: 'Mínimo 3 caracteres.' },
            ]);

            const passwordOk = validateField(passwordInput, passwordError, [
                { test: v => v.length > 0, message: 'La contraseña es obligatoria.' },
                { test: v => v.length >= 6, message: 'Mínimo 6 caracteres.' },
            ]);

            if (!usernameOk || !passwordOk) {
                e.preventDefault();
                // Vibración suave en el botón
                btnLogin.classList.add('shake');
                btnLogin.addEventListener('animationend', () => btnLogin.classList.remove('shake'), { once: true });
                return;
            }

            // Estado de carga
            setLoading(true);
        });
    }

    function setLoading(state) {
        if (!btnLogin || !btnText) return;
        btnLogin.disabled = state;
        btnLogin.classList.toggle('loading', state);
        btnText.textContent = state ? 'Ingresando...' : 'Ingresar';
    }

    // ===========================
    // ANIMACIÓN DE ENTRADA
    // ===========================
    const animatedEls = document.querySelectorAll('.input-group, .form-footer, .btn-login');
    animatedEls.forEach((el, i) => {
        el.style.opacity   = '0';
        el.style.transform = 'translateY(12px)';
        el.style.transition = `opacity 0.35s ease ${i * 80}ms, transform 0.35s ease ${i * 80}ms`;

        requestAnimationFrame(() => {
            el.style.opacity   = '1';
            el.style.transform = 'translateY(0)';
        });
    });

    // ===========================
    // ATAJO: ENTER EN USERNAME
    // ===========================
    if (usernameInput && passwordInput) {
        usernameInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                passwordInput.focus();
            }
        });
    }

    // ===========================
    // FOCO AUTOMÁTICO
    // ===========================
    if (usernameInput) {
        usernameInput.focus();
    }

});