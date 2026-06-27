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

    // Validación del formulario de contraseña
    const formSeguridad = document.getElementById('formSeguridad');
    if (formSeguridad) {
        formSeguridad.addEventListener('submit', function(e) {
            const currentPass = document.getElementById('currentPassword').value;
            const newPass = document.getElementById('newPassword').value;
            const confirmPass = document.getElementById('confirmPassword').value;

            if (newPass !== confirmPass) {
                e.preventDefault();
                alert('La nueva contraseña y la confirmación no coinciden.');
                document.getElementById('confirmPassword').focus();
                return;
            }

            if (newPass.length > 0 && newPass.length < 6) {
                e.preventDefault();
                alert('La nueva contraseña debe tener al menos 6 caracteres.');
                document.getElementById('newPassword').focus();
                return;
            }
            
            // En caso de que todo esté bien, se envía
            // Nota: Podrías hacer que el formulario de seguridad se envíe al mismo endpoint
            // copiando los valores de nombre y email actuales en campos ocultos,
            // ya que nuestro endpoint actualiza todo junto.
            
            document.getElementById('hiddenNombre').value = document.getElementById('nombre').value;
            document.getElementById('hiddenEmail').value = document.getElementById('email').value;
        });
    }
});