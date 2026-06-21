'use strict';

document.addEventListener('DOMContentLoaded', function () {
    // Lógica para el buscador dinámico
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keyup', function () {
            const filter = this.value.toLowerCase();
            const rows = document.querySelectorAll('#medicamentosTableBody tr');
            rows.forEach(row => {
                const name = row.cells[1].textContent.toLowerCase();
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

    // Set imagenUrl and preview
    const imgUrl = button.dataset.imagenurl || '';
    document.getElementById('imagenUrl').value = imgUrl;
    const preview = document.getElementById('modalImagePreview');
    if (imgUrl) {
        preview.innerHTML = '<img src="' + imgUrl + '" alt="Preview" style="width:64px;height:64px;object-fit:cover;border-radius:6px;">';
    } else {
        preview.innerHTML = '<i class="fas fa-pills"></i>';
    }
}

// Preview de imagen desde URL manual
document.addEventListener('DOMContentLoaded', function () {
    const imagenUrlInput = document.getElementById('imagenUrl');
    if (imagenUrlInput) {
        imagenUrlInput.addEventListener('input', function () {
            const preview = document.getElementById('modalImagePreview');
            const val = this.value.trim();
            if (val) {
                preview.innerHTML = '<img src="' + val + '" alt="Preview" style="width:64px;height:64px;object-fit:cover;border-radius:6px;" onerror="this.parentElement.innerHTML=\'<i class=\\\'fas fa-pills\\\'></i>\'">';
            } else {
                preview.innerHTML = '<i class="fas fa-pills"></i>';
            }
        });
    }
});

// Preview de imagen desde archivo
function previewModalImage(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = function (e) {
            const preview = document.getElementById('modalImagePreview');
            preview.innerHTML = '<img src="' + e.target.result + '" alt="Preview" style="width:64px;height:64px;object-fit:cover;border-radius:6px;">';
        };
        reader.readAsDataURL(input.files[0]);
    }
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
            
            document.body.appendChild(form);
            form.submit();
        }
    });
}
