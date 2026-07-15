
function confirmDelete(nombre, id) {
    Swal.fire({
        title: '¿Eliminar proveedor?',
        text: 'Se eliminará "' + nombre + '". Esta acción no se puede revertir.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            window.location.href = '/farmaceutico/proveedores/eliminar/' + id;
        }
    });
    return false;
}

/**
 * Inicializa el buscador en vivo. Filtra las tarjetas de proveedores ya cargadas
 * desde la BD (no hace ninguna petición nueva), comparando contra el atributo
 * data-search de cada tarjeta (nombre + RUC + contacto).
 */
function initBuscadorProveedores() {
    const searchInput = document.getElementById('searchProveedor');
    if (!searchInput) return;

    searchInput.addEventListener('keyup', function () {
        const filter = this.value.trim().toLowerCase();
        const cards = document.querySelectorAll('[data-search]');

        cards.forEach((card) => {
            const searchData = card.getAttribute('data-search').toLowerCase();
            card.style.display = searchData.includes(filter) ? '' : 'none';
        });
    });
}

document.addEventListener('DOMContentLoaded', function () {
    initBuscadorProveedores();
});