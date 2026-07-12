'use strict';

let recetasData = [];
let recetaIdActual = null;

document.addEventListener('DOMContentLoaded', function () {
    // Cargar datos de la tabla en un array para filtrado
    cargarRecetasData();

    // Buscador
    const searchInput = document.getElementById('searchReceta');
    if (searchInput) {
        searchInput.addEventListener('keyup', function () {
            filtrarTabla(this.value);
        });
    }

    // Contar pendientes
    actualizarContadorPendientes();
});

function cargarRecetasData() {
    recetasData = [];
    const rows = document.querySelectorAll('#recetasTableBody tr');
    rows.forEach(row => {
        if (row.cells.length >= 8) {
            const estadoElem = row.cells[6].querySelector('.badge');
            const estado = estadoElem ? estadoElem.textContent.trim().toLowerCase() : '';
            recetasData.push({
                element: row,
                texto: row.textContent.toLowerCase(),
                estado: estado
            });
        }
    });
}

function actualizarContadorPendientes() {
    const pendientes = document.querySelectorAll('.estado-pendiente').length;
    const badge = document.getElementById('pendientesCount');
    if (badge) badge.textContent = pendientes;
}

function filtrarRecetas(filtro) {
    const rows = document.querySelectorAll('#recetasTableBody tr');
    rows.forEach(row => {
        if (filtro === 'todas') {
            row.style.display = '';
        } else if (filtro === 'pendientes') {
            const estadoBadge = row.querySelector('.estado-pendiente');
            row.style.display = estadoBadge ? '' : 'none';
        }
    });
}

function filtrarTabla(texto) {
    const filter = texto.toLowerCase();
    recetasData.forEach(item => {
        item.element.style.display = item.texto.includes(filter) ? '' : 'none';
    });
}

function verDetalleReceta(btn) {
    const id = btn.getAttribute('data-id');
    recetaIdActual = id;

    // Buscar la fila para extraer datos
    const row = btn.closest('tr');
    const celdas = row.querySelectorAll('td');

    document.getElementById('detalleRecetaId').textContent = id;
    document.getElementById('detallePaciente').textContent = celdas[1]?.textContent.trim() || '-';
    document.getElementById('detalleEspecie').textContent = celdas[2]?.textContent.trim() || '-';
    document.getElementById('detalleVeterinario').textContent = celdas[3]?.textContent.trim() || '-';
    document.getElementById('detalleFecha').textContent = celdas[4]?.textContent.trim() || '-';

    // Ocultar resultado de validación previo
    document.getElementById('validacionResultado').classList.add('d-none');
    document.getElementById('btnDispensarDesdeModal').classList.add('d-none');

    // Cargar items mediante API
    cargarDetalleReceta(id);

    const modal = new bootstrap.Modal(document.getElementById('detalleRecetaModal'));
    modal.show();
}

async function cargarDetalleReceta(id) {
    const tbody = document.getElementById('detalleItemsBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3"><i class="fas fa-spinner fa-spin me-2"></i>Cargando...</td></tr>';

    try {
        const response = await fetch(`/farmaceutico/recetas/validar/${id}`, { method: 'POST' });
        const data = await response.json();

        const receta = data.receta || {};
        const items = receta.items || [];
        const errores = data.errores || [];
        const advertencias = data.advertencias || [];

        document.getElementById('detalleObservaciones').textContent = receta.observaciones || 'Sin observaciones';

        if (items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No hay medicamentos en esta receta</td></tr>';
        } else {
            tbody.innerHTML = items.map(item => {
                const med = item.medicamento || {};
                const stockOk = med.stock != null && item.cantidad != null && med.stock >= item.cantidad;
                return `<tr>
                    <td class="fw-bold">${med.nombre || 'N/A'}</td>
                    <td>${item.cantidad || 0}</td>
                    <td>${item.dosis || 'N/A'}</td>
                    <td>${item.frecuencia || 'N/A'}</td>
                    <td class="text-center">
                        <span class="badge ${stockOk ? 'bg-success-soft text-success' : 'bg-danger-soft text-danger'}">
                            ${med.stock != null ? med.stock : 'N/A'}
                        </span>
                    </td>
                    <td class="text-center">
                        ${stockOk
                            ? '<span class="text-success"><i class="fas fa-check-circle"></i> Disponible</span>'
                            : '<span class="text-danger"><i class="fas fa-exclamation-circle"></i> Stock Insuficiente</span>'
                        }
                    </td>
                </tr>`;
            }).join('');
        }

        // Mostrar resultado de validación
        const validacionDiv = document.getElementById('validacionResultado');
        const mensajesDiv = document.getElementById('validacionMensajes');

        if (errores.length > 0 || advertencias.length > 0) {
            validacionDiv.classList.remove('d-none');
            let html = '';
            if (errores.length > 0) {
                html += '<div class="alert alert-danger mb-2">';
                html += '<strong><i class="fas fa-times-circle me-1"></i> Errores:</strong>';
                html += '<ul class="mb-0 mt-1">' + errores.map(e => `<li>${e}</li>`).join('') + '</ul>';
                html += '</div>';
            }
            if (advertencias.length > 0) {
                html += '<div class="alert alert-warning mb-0">';
                html += '<strong><i class="fas fa-exclamation-triangle me-1"></i> Advertencias:</strong>';
                html += '<ul class="mb-0 mt-1">' + advertencias.map(a => `<li>${a}</li>`).join('') + '</ul>';
                html += '</div>';
            }
            mensajesDiv.innerHTML = html;
        } else {
            validacionDiv.classList.remove('d-none');
            mensajesDiv.innerHTML = '<div class="alert alert-success"><i class="fas fa-check-circle me-2"></i>Receta válida, sin errores ni advertencias.</div>';
        }

        // Mostrar botón dispensar solo si es válida y está pendiente o validada
        const estadoReceta = receta.estado || '';
        if (data.valida && (estadoReceta === 'PENDIENTE' || estadoReceta === 'VALIDADA')) {
            document.getElementById('btnDispensarDesdeModal').classList.remove('d-none');
        }

    } catch (error) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Error al cargar detalle</td></tr>';
        console.error('Error cargando detalle:', error);
    }
}

async function validarReceta(btn) {
    const id = btn.getAttribute('data-id');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Validando...';

    try {
        const response = await fetch(`/farmaceutico/recetas/validar/${id}`, { method: 'POST' });
        const data = await response.json();

        if (data.valida) {
            Swal.fire({
                icon: 'success',
                title: 'Receta Válida',
                text: 'La receta ha sido validada correctamente. Puede proceder a dispensar.',
                confirmButtonColor: '#1D9E75'
            });
            // Recargar la página para ver el cambio de estado
            setTimeout(() => location.reload(), 1500);
        } else {
            let mensaje = '<strong>Errores encontrados:</strong><ul>';
            (data.errores || []).forEach(e => { mensaje += `<li>${e}</li>`; });
            mensaje += '</ul>';
            if ((data.advertencias || []).length > 0) {
                mensaje += '<strong>Advertencias:</strong><ul>';
                data.advertencias.forEach(a => { mensaje += `<li>${a}</li>`; });
                mensaje += '</ul>';
            }
            Swal.fire({
                icon: 'error',
                title: 'Receta No Válida',
                html: mensaje,
                confirmButtonColor: '#DC2626'
            });
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-check-circle"></i> Validar';
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error al validar la receta. Intente nuevamente.',
            confirmButtonColor: '#DC2626'
        });
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-check-circle"></i> Validar';
    }
}

async function dispensarReceta(btn) {
    const id = btn.getAttribute('data-id');
    
    const result = await Swal.fire({
        title: '¿Dispensar Receta?',
        text: 'Se descontarán los medicamentos del stock y la receta se marcará como dispensada.',
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#1D9E75',
        cancelButtonColor: '#6B7280',
        confirmButtonText: 'Sí, dispensar',
        cancelButtonText: 'Cancelar'
    });

    if (!result.isConfirmed) return;

    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Dispensando...';

    try {
        const response = await fetch(`/farmaceutico/recetas/dispensar/${id}`, { method: 'POST' });
        const data = await response.json();

        if (data.dispensado) {
            Swal.fire({
                icon: 'success',
                title: '¡Receta Dispensada!',
                text: 'Los medicamentos han sido descontados del stock.',
                confirmButtonColor: '#1D9E75'
            });
            setTimeout(() => location.reload(), 1500);
        } else {
            let mensaje = '<strong>Errores:</strong><ul>';
            (data.errores || []).forEach(e => { mensaje += `<li>${e}</li>`; });
            mensaje += '</ul>';
            Swal.fire({
                icon: 'error',
                title: 'No se pudo dispensar',
                html: mensaje,
                confirmButtonColor: '#DC2626'
            });
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-prescription-bottle"></i> Dispensar';
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error al dispensar la receta.',
            confirmButtonColor: '#DC2626'
        });
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-prescription-bottle"></i> Dispensar';
    }
}

function dispensarDesdeModal() {
    if (recetaIdActual) {
        const modal = bootstrap.Modal.getInstance(document.getElementById('detalleRecetaModal'));
        modal.hide();
        // Buscar el botón de dispensar en la tabla
        const btn = document.querySelector(`button[data-id="${recetaIdActual}"][onclick*="dispensarReceta"]`);
        if (btn) {
            dispensarReceta(btn);
        } else {
            // Si no hay botón, hacer la llamada directa
            dispensarRecetaDirecta(recetaIdActual);
        }
    }
}

async function dispensarRecetaDirecta(id) {
    try {
        const response = await fetch(`/farmaceutico/recetas/dispensar/${id}`, { method: 'POST' });
        const data = await response.json();

        if (data.dispensado) {
            Swal.fire({
                icon: 'success',
                title: '¡Receta Dispensada!',
                text: 'Los medicamentos han sido descontados del stock.',
                confirmButtonColor: '#1D9E75'
            });
            setTimeout(() => location.reload(), 1500);
        } else {
            let mensaje = '<strong>Errores:</strong><ul>';
            (data.errores || []).forEach(e => { mensaje += `<li>${e}</li>`; });
            mensaje += '</ul>';
            Swal.fire({
                icon: 'error',
                title: 'No se pudo dispensar',
                html: mensaje,
                confirmButtonColor: '#DC2626'
            });
        }
    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'Error al dispensar la receta.',
            confirmButtonColor: '#DC2626'
        });
    }
}
