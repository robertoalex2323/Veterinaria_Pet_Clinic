document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keyup', filtrarClientes);
    }
});

function filtrarClientes() {
    const filter = this.value.toLowerCase();
    const rows = document.querySelectorAll('#clientesTable tbody tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = text.includes(filter) ? '' : 'none';
    });
}

function eliminarCliente(id, nombre) {
    if (confirm(`¿Eliminar al cliente "${nombre}"?`)) {
        window.location.href = `/recepcionista/clientes/eliminar/${id}`;
    }
}