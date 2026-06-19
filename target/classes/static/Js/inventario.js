'use strict';

document.addEventListener('DOMContentLoaded', function () {
    // Lógica para el buscador dinámico
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keyup', function () {
            const filter = this.value.toLowerCase();
            const rows = document.querySelectorAll('#medicamentosTableBody tr');
            rows.forEach(row => {
                const name = row.cells[0].textContent.toLowerCase();
                row.style.display = name.includes(filter) ? '' : 'none';
            });
        });
    }
});

// Lógica para preparar el modal para un nuevo medicamento
function prepareNewForm() {
    const form = document.getElementById('medicamentoForm');
    if (form) {
        form.reset();
        document.getElementById('medicamentoId').value = '';
        document.getElementById('modalTitle').innerText = 'Registrar Nuevo Medicamento';
    }
}

// Lógica para rellenar el modal con datos para editar
function prepareEditForm(button) {
    document.getElementById('modalTitle').innerText = 'Editar Medicamento';
    document.getElementById('medicamentoId').value = button.dataset.id;
    document.getElementById('nombre').value = button.dataset.nombre;
    document.getElementById('presentacion').value = button.dataset.presentacion;
    document.getElementById('precio').value = button.dataset.precio;
    document.getElementById('stock').value = button.dataset.stock;
    document.getElementById('stockMinimo').value = button.dataset.stockminimo;
    document.getElementById('proveedor').value = button.dataset.proveedorid;
}

// Lógica para eliminar con confirmación (ahora funcional)
function confirmDelete(id) {
    Swal.fire({
        title: '¿Estás seguro?',
        text: "Esta acción eliminará el medicamento del inventario y no se puede revertir.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#d33',
        cancelButtonColor: '#3085d6',
        confirmButtonText: 'Sí, ¡eliminar!',
        cancelButtonText: 'Cancelar'
    }).then((result) => {
        if (result.isConfirmed) {
            // Creamos un formulario dinámico para enviar la petición de borrado
            const form = document.createElement('form');
            form.method = 'POST';
            form.action = `/farmaceutico/inventario/eliminar/${id}`;
            
            // Para protección CSRF si la tienes habilitada en el futuro
            // const csrfInput = document.querySelector('input[name="_csrf"]');
            // if(csrfInput) form.appendChild(csrfInput.cloneNode());
            
            document.body.appendChild(form);
            form.submit();
        }
    });
}