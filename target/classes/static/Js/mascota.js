// Mascotas específico
function filtrarPorEspecie() {
    const especie = document.getElementById('filtroEspecie').value.toLowerCase();
    const rows = document.querySelectorAll('#mascotasTable tbody tr');

    rows.forEach(row => {
        if (!especie || row.dataset.especie.toLowerCase() === especie) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

function filtrarMascotas() {
    const busqueda = document.getElementById('buscarMascota').value.toLowerCase();
    const rows = document.querySelectorAll('#mascotasTable tbody tr');

    rows.forEach(row => {
        const nombre = row.querySelector('.fw-bold').textContent.toLowerCase();
        const dueño = row.querySelector('.dueño-nombre') ? row.querySelector('.dueño-nombre').textContent.toLowerCase() : '';

        if (nombre.includes(busqueda) || dueño.includes(busqueda)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

function calcularEdad() {
    const fechaNac = document.getElementById('fechaNacimiento');
    const edadInput = document.getElementById('edad');

    if (fechaNac && fechaNac.value) {
        const nacimiento = new Date(fechaNac.value);
        const hoy = new Date();
        let edad = hoy.getFullYear() - nacimiento.getFullYear();
        const mes = hoy.getMonth() - nacimiento.getMonth();
        if (mes < 0 || (mes === 0 && hoy.getDate() < nacimiento.getDate())) {
            edad--;
        }
        edadInput.value = edad > 0 ? edad : 0;
    }
}

// Nueva función de vista previa de imagen
function actualizarVistaPrevia() {
    const urlInput = document.getElementById('fotoUrl');
    const previewImg = document.getElementById('previewFoto');
    const placeholder = document.getElementById('placeholderFoto');

    if (urlInput && urlInput.value.trim() !== '') {
        previewImg.src = urlInput.value;
        previewImg.style.display = 'block';
        if (placeholder) placeholder.style.display = 'none';

        // Manejo de error si la imagen no carga
        previewImg.onerror = function () {
            previewImg.style.display = 'none';
            if (placeholder) {
                placeholder.style.display = 'flex';
                placeholder.innerHTML = '<i class="fas fa-image-slash text-danger"></i>';
            }
        };
    } else {
        previewImg.style.display = 'none';
        previewImg.src = '';
        if (placeholder) {
            placeholder.style.display = 'flex';
            placeholder.innerHTML = '<i class="fas fa-paw"></i>';
        }
    }
}

document.addEventListener('DOMContentLoaded', function () {
    const fotoUrlInput = document.getElementById('fotoUrl');
    if (fotoUrlInput) {
        fotoUrlInput.addEventListener('input', actualizarVistaPrevia);
        // Inicializar si ya tiene valor (modo edición)
        actualizarVistaPrevia();
    }

    // Listeners para filtros
    const searchInput = document.getElementById('buscarMascota');
    if (searchInput) {
        searchInput.addEventListener('input', filtrarMascotas);
    }
});