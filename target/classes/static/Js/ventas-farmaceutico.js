'use strict';

let ventasData = [];
let currentVentaId = null;

document.addEventListener('DOMContentLoaded', function () {
    cargarVentas();
    setupEventListeners();
});

// ==============================
// SETUP
// ==============================
function setupEventListeners() {
    // Botón refrescar
    const btnRefrescar = document.getElementById('btnRefrescar');
    if (btnRefrescar) {
        btnRefrescar.addEventListener('click', function() {
            cargarVentas();
            Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'Lista de ventas actualizada',
                timer: 1500,
                showConfirmButton: false,
                toast: true,
                position: 'top-end'
            });
        });
    }

    // Búsqueda
    const searchInput = document.getElementById('searchVenta');
    if (searchInput) {
        searchInput.addEventListener('keyup', function () {
            filtrarVentas();
        });
    }

    // Filtros
    const filtroMetodo = document.getElementById('filtroMetodoPago');
    const filtroFecha = document.getElementById('filtroFecha');
    if (filtroMetodo) filtroMetodo.addEventListener('change', filtrarVentas);
    if (filtroFecha) filtroFecha.addEventListener('change', filtrarVentas);
}

// ==============================
// CARGAR VENTAS DESDE API
// ==============================
async function cargarVentas() {
    const tbody = document.getElementById('ventasTableBody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="10" class="text-center py-5"><i class="fas fa-spinner fa-spin fa-2x text-primary mb-3"></i><br><span class="text-muted">Cargando ventas...</span></td></tr>';

    try {
        const response = await fetch('/farmaceutico/api/ventas');
        if (!response.ok) throw new Error('Error al cargar ventas');
        ventasData = await response.json();
        renderizarVentas(ventasData);
        actualizarStats(ventasData);
    } catch (error) {
        console.error('Error cargando ventas:', error);
        tbody.innerHTML = '<tr><td colspan="10" class="text-center py-5 text-danger"><i class="fas fa-exclamation-circle fa-2x mb-2"></i><br>Error al cargar ventas</td></tr>';
    }
}

// ==============================
// RENDERIZAR TABLA
// ==============================
function renderizarVentas(ventas) {
    const tbody = document.getElementById('ventasTableBody');
    if (!tbody) return;

    if (!ventas || ventas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" class="text-center py-5">' +
            '<i class="fas fa-cash-register fa-3x text-muted mb-3"></i>' +
            '<h5 class="text-muted">No hay ventas registradas</h5>' +
            '<p class="text-muted">Las ventas generadas aparecerán aquí automáticamente.</p>' +
            '</td></tr>';
        return;
    }

    tbody.innerHTML = ventas.map(v => {
        const fecha = v.fechaFormateada || v.fecha || '---';
        const cliente = v.cliente ? v.cliente.nombre : '---';
        const metodoPago = v.metodoPago || '---';
        const subtotal = v.subtotal ? 'S/ ' + parseFloat(v.subtotal).toFixed(2) : 'S/ 0.00';
        const igv = v.igv ? 'S/ ' + parseFloat(v.igv).toFixed(2) : 'S/ 0.00';
        const total = v.total ? 'S/ ' + parseFloat(v.total).toFixed(2) : 'S/ 0.00';
        const numVenta = 'VTA-' + String(v.id).padStart(5, '0');
        const cantMed = v.detalles ? v.detalles.length : 0;
        const comprobanteEnviado = v.comprobanteEnviado;

        let metodoBadge = '';
        switch(metodoPago) {
            case 'EFECTIVO': metodoBadge = '<span class="badge badge-metodo bg-soft-success text-success"><i class="fas fa-money-bill-wave me-1"></i>Efectivo</span>'; break;
            case 'TARJETA': metodoBadge = '<span class="badge badge-metodo bg-soft-primary text-primary"><i class="fas fa-credit-card me-1"></i>Tarjeta</span>'; break;
            case 'YAPE': metodoBadge = '<span class="badge badge-metodo bg-soft-info text-info"><i class="fas fa-mobile-alt me-1"></i>Yape</span>'; break;
            case 'PLIN': metodoBadge = '<span class="badge badge-metodo bg-soft-warning text-warning"><i class="fas fa-mobile-alt me-1"></i>Plin</span>'; break;
            case 'TRANSFERENCIA': metodoBadge = '<span class="badge badge-metodo bg-soft-secondary text-secondary"><i class="fas fa-exchange-alt me-1"></i>Transferencia</span>'; break;
            default: metodoBadge = '<span class="badge badge-metodo bg-light text-dark">' + metodoPago + '</span>';
        }

        let comprobanteBadge = comprobanteEnviado
            ? '<span class="text-success"><i class="fas fa-check-circle"></i> Enviado</span>'
            : '<span class="text-muted"><i class="fas fa-times-circle"></i> No enviado</span>';

        return `<tr>
            <td class="fw-bold text-primary">${numVenta}</td>
            <td><span class="text-nowrap">${fecha}</span></td>
            <td class="fw-bold">${cliente}</td>
            <td><span class="badge bg-primary-soft text-primary">${cantMed} producto(s)</span></td>
            <td class="text-end">${subtotal}</td>
            <td class="text-end">${igv}</td>
            <td class="text-end fw-bold text-success fs-6">${total}</td>
            <td>${metodoBadge}</td>
            <td>${comprobanteBadge}</td>
            <td class="text-center">
                <div class="d-flex gap-1 justify-content-center">
                    <button class="btn btn-sm btn-outline-info" onclick="verDetalleVenta(${v.id})" title="Ver detalle">
                        <i class="fas fa-eye"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-primary" onclick="descargarComprobante(${v.id})" title="Descargar comprobante">
                        <i class="fas fa-file-pdf"></i>
                    </button>
                </div>
            </td>
        </tr>`;
    }).join('');
}

// ==============================
// FILTROS
// ==============================
function filtrarVentas() {
    const texto = (document.getElementById('searchVenta').value || '').toLowerCase();
    const metodo = document.getElementById('filtroMetodoPago').value;
    const fecha = document.getElementById('filtroFecha').value;

    let filtradas = ventasData.filter(v => {
        // Filtro texto
        const numVenta = 'VTA-' + String(v.id).padStart(5, '0');
        const cliente = v.cliente ? v.cliente.nombre.toLowerCase() : '';
        const meds = v.detalles ? v.detalles.map(d => d.medicamento?.nombre?.toLowerCase() || '').join(' ') : '';
        const searchable = (numVenta + ' ' + cliente + ' ' + meds).toLowerCase();
        if (texto && !searchable.includes(texto)) return false;

        // Filtro método pago
        if (metodo && v.metodoPago !== metodo) return false;

        // Filtro fecha
        if (fecha && fecha !== 'todas') {
            const fechaVenta = new Date(v.fecha);
            const hoy = new Date();
            hoy.setHours(0, 0, 0, 0);

            if (fecha === 'hoy') {
                const hoyFin = new Date(hoy);
                hoyFin.setHours(23, 59, 59, 999);
                if (fechaVenta < hoy || fechaVenta > hoyFin) return false;
            } else if (fecha === 'semana') {
                const hace7 = new Date(hoy);
                hace7.setDate(hace7.getDate() - 7);
                if (fechaVenta < hace7) return false;
            } else if (fecha === 'mes') {
                const hace30 = new Date(hoy);
                hace30.setDate(hace30.getDate() - 30);
                if (fechaVenta < hace30) return false;
            }
        }

        return true;
    });

    renderizarVentas(filtradas);
}

// ==============================
// ACTUALIZAR STATS
// ==============================
function actualizarStats(ventas) {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const hoyFin = new Date(hoy);
    hoyFin.setHours(23, 59, 59, 999);

    const ventasHoy = ventas.filter(v => {
        const f = new Date(v.fecha);
        return f >= hoy && f <= hoyFin;
    });

    const countHoy = document.getElementById('ventasHoyCount');
    const totalHoy = document.getElementById('totalHoy');

    if (countHoy) countHoy.textContent = ventasHoy.length;

    if (totalHoy) {
        const total = ventasHoy.reduce((sum, v) => sum + parseFloat(v.total || 0), 0);
        totalHoy.textContent = 'S/ ' + total.toFixed(2);
    }
}

// ==============================
// MODAL NUEVA VENTA
// ==============================
function abrirModalNuevaVenta() {
    // Resetear
    document.getElementById('selectReceta').value = '';
    document.getElementById('recetaDetalle').classList.add('d-none');
    document.getElementById('recetaNoSeleccionada').classList.remove('d-none');
    document.getElementById('btnGenerarVenta').classList.add('d-none');
    document.getElementById('itemsVentaBody').innerHTML = '';
    document.getElementById('metodoPagoSeleccionado').value = '';
    document.getElementById('ventaSubtotal').textContent = 'S/ 0.00';
    document.getElementById('ventaIgv').textContent = 'S/ 0.00';
    document.getElementById('ventaTotal').textContent = 'S/ 0.00';

    // Resetear botones de método pago
    document.querySelectorAll('.metodo-pago-btn').forEach(btn => {
        btn.classList.remove('active', 'btn-success', 'btn-primary', 'btn-info', 'btn-warning', 'btn-secondary');
        btn.classList.add('btn-outline-success', 'btn-outline-primary', 'btn-outline-info', 'btn-outline-warning', 'btn-outline-secondary');
    });

    const modal = new bootstrap.Modal(document.getElementById('nuevaVentaModal'));
    modal.show();
}

// ==============================
// CARGAR DETALLE RECETA (para venta)
// ==============================
async function cargarDetalleRecetaVenta() {
    const select = document.getElementById('selectReceta');
    const recetaId = select.value;

    if (!recetaId) {
        document.getElementById('recetaDetalle').classList.add('d-none');
        document.getElementById('recetaNoSeleccionada').classList.remove('d-none');
        document.getElementById('btnGenerarVenta').classList.add('d-none');
        return;
    }

    document.getElementById('recetaNoSeleccionada').classList.add('d-none');
    document.getElementById('recetaDetalle').classList.remove('d-none');
    document.getElementById('btnGenerarVenta').classList.add('d-none');

    const tbody = document.getElementById('itemsVentaBody');
    tbody.innerHTML = '<tr><td colspan="4" class="text-center py-3"><i class="fas fa-spinner fa-spin me-2"></i>Cargando...</td></tr>';

    try {
        const response = await fetch('/farmaceutico/recetas/validar/' + recetaId, { method: 'POST' });
        const data = await response.json();

        const receta = data.receta || {};
        const items = receta.items || [];

        // Información del paciente
        document.getElementById('detPaciente').textContent = receta.paciente?.nombre || '---';
        document.getElementById('detEspecie').textContent = (receta.paciente?.especie || '---') + ' | ' + (receta.paciente?.raza || '');
        document.getElementById('detVeterinario').textContent = receta.veterinario?.nombre || '---';

        if (items.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No hay medicamentos en esta receta</td></tr>';
            return;
        }

        // Calcular totales
        let subtotal = 0;
        const TASA_IGV = 0.18;

        tbody.innerHTML = items.map(item => {
            const med = item.medicamento || {};
            const cantidad = item.cantidad || 0;
            const precio = parseFloat(med.precio || 0);
            const importe = cantidad * precio;
            subtotal += importe;

            return `<tr>
                <td class="fw-bold">${med.nombre || 'N/A'}</td>
                <td>${cantidad}</td>
                <td>S/ ${precio.toFixed(2)}</td>
                <td class="fw-bold">S/ ${importe.toFixed(2)}</td>
            </tr>`;
        }).join('');

        const igv = subtotal * TASA_IGV;
        const total = subtotal + igv;

        document.getElementById('ventaSubtotal').textContent = 'S/ ' + subtotal.toFixed(2);
        document.getElementById('ventaIgv').textContent = 'S/ ' + igv.toFixed(2);
        document.getElementById('ventaTotal').textContent = 'S/ ' + total.toFixed(2);

        // Si hay método de pago seleccionado, mostrar botón
        verificarGenerarVenta();

    } catch (error) {
        console.error('Error cargando detalle:', error);
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-danger">Error al cargar detalle de la receta</td></tr>';
    }
}

// ==============================
// SELECCIONAR MÉTODO DE PAGO
// ==============================
function seleccionarMetodoPago(btn) {
    // Desmarcar todos
    document.querySelectorAll('.metodo-pago-btn').forEach(b => {
        b.classList.remove('active');
        // Reset clases outline
        ['btn-success','btn-primary','btn-info','btn-warning','btn-secondary'].forEach(cls => b.classList.remove(cls));
        ['btn-outline-success','btn-outline-primary','btn-outline-info','btn-outline-warning','btn-outline-secondary'].forEach(cls => b.classList.add(cls));
    });

    // Marcar seleccionado
    btn.classList.remove('btn-outline-success', 'btn-outline-primary', 'btn-outline-info', 'btn-outline-warning', 'btn-outline-secondary');
    btn.classList.add('active');

    // Aplicar color sólido según el tipo
    const value = btn.getAttribute('data-value');
    switch(value) {
        case 'EFECTIVO': btn.classList.add('btn-success'); break;
        case 'TARJETA': btn.classList.add('btn-primary'); break;
        case 'YAPE': btn.classList.add('btn-info'); break;
        case 'PLIN': btn.classList.add('btn-warning'); break;
        case 'TRANSFERENCIA': btn.classList.add('btn-secondary'); break;
    }

    document.getElementById('metodoPagoSeleccionado').value = value;
    verificarGenerarVenta();
}

// ==============================
// VERIFICAR SI SE PUEDE GENERAR
// ==============================
function verificarGenerarVenta() {
    const recetaId = document.getElementById('selectReceta').value;
    const metodo = document.getElementById('metodoPagoSeleccionado').value;

    const btn = document.getElementById('btnGenerarVenta');
    if (recetaId && metodo) {
        btn.classList.remove('d-none');
    } else {
        btn.classList.add('d-none');
    }
}

// ==============================
// GENERAR VENTA DESDE RECETA
// ==============================
async function generarVentaDesdeReceta() {
    const recetaId = document.getElementById('selectReceta').value;
    const metodoPago = document.getElementById('metodoPagoSeleccionado').value;

    if (!recetaId || !metodoPago) {
        Swal.fire({
            icon: 'warning',
            title: 'Campos incompletos',
            text: 'Seleccione una receta y un método de pago.',
            confirmButtonColor: '#F59E0B'
        });
        return;
    }

    const btn = document.getElementById('btnGenerarVenta');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Generando venta...';

    try {
        const response = await fetch('/farmaceutico/api/ventas/crear-desde-receta?' + new URLSearchParams({
            recetaId: recetaId,
            metodoPago: metodoPago
        }), { method: 'POST' });

        const result = await response.json();

        if (result.success) {
            Swal.fire({
                icon: 'success',
                title: '¡Venta Generada!',
                html: '<strong>' + result.message + '</strong><br><br>' +
                      '<a href="/farmaceutico/ventas/comprobante/' + result.ventaId + '" class="btn btn-primary mt-2" target="_blank">' +
                      '<i class="fas fa-file-pdf me-2"></i>Ver Comprobante</a>',
                confirmButtonColor: '#1D9E75',
                confirmButtonText: 'OK'
            });

            // Cerrar modal y recargar
            const modal = bootstrap.Modal.getInstance(document.getElementById('nuevaVentaModal'));
            modal.hide();
            cargarVentas();
        } else {
            Swal.fire({
                icon: 'error',
                title: 'Error',
                text: result.message || 'No se pudo generar la venta.',
                confirmButtonColor: '#DC2626'
            });
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-check-circle me-2"></i>Generar Venta y Enviar Comprobante';
        }
    } catch (error) {
        console.error('Error generando venta:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error de conexión',
            text: 'No se pudo conectar con el servidor.',
            confirmButtonColor: '#DC2626'
        });
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-check-circle me-2"></i>Generar Venta y Enviar Comprobante';
    }
}

// ==============================
// VER DETALLE DE VENTA
// ==============================
function verDetalleVenta(ventaId) {
    currentVentaId = ventaId;
    const venta = ventasData.find(v => v.id === ventaId);
    if (!venta) return;

    document.getElementById('detVentaNumero').textContent = '#VTA-' + String(venta.id).padStart(5, '0');
    document.getElementById('detFecha').textContent = venta.fechaFormateada || venta.fecha || '---';
    document.getElementById('detCliente').textContent = venta.cliente ? venta.cliente.nombre : '---';
    document.getElementById('detUsuario').textContent = venta.usuario ? venta.usuario.nombre : '---';
    document.getElementById('detMetodoPago').textContent = venta.metodoPago || '---';

    // Items
    const tbody = document.getElementById('detalleItemsBody');
    if (venta.detalles && venta.detalles.length > 0) {
        tbody.innerHTML = venta.detalles.map(d => {
            const nombre = d.medicamento ? d.medicamento.nombre : '---';
            const cantidad = d.cantidad || 0;
            const precio = parseFloat(d.precioUnitario || 0).toFixed(2);
            const importe = (cantidad * parseFloat(d.precioUnitario || 0)).toFixed(2);
            return `<tr>
                <td class="fw-bold">${nombre}</td>
                <td>${cantidad}</td>
                <td>S/ ${precio}</td>
                <td class="fw-bold">S/ ${importe}</td>
            </tr>`;
        }).join('');
    } else {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">Sin productos</td></tr>';
    }

    // Totales
    document.getElementById('detSubtotal').textContent = 'S/ ' + parseFloat(venta.subtotal || 0).toFixed(2);
    document.getElementById('detIgv').textContent = 'S/ ' + parseFloat(venta.igv || 0).toFixed(2);
    document.getElementById('detTotal').textContent = 'S/ ' + parseFloat(venta.total || 0).toFixed(2);

    const modal = new bootstrap.Modal(document.getElementById('detalleVentaModal'));
    modal.show();
}

// ==============================
// DESCARGAR COMPROBANTE
// ==============================
async function descargarComprobante(ventaId) {
    const id = ventaId || currentVentaId;
    if (!id) return;

    // Mostrar loading
    Swal.fire({
        title: 'Generando comprobante...',
        text: 'Por favor espere',
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading()
    });

    try {
        const response = await fetch('/farmaceutico/ventas/comprobante/' + id);
        if (!response.ok) throw new Error('Error al descargar');

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'Comprobante_VTA' + String(id).padStart(5, '0') + '.pdf';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);

        Swal.fire({
            icon: 'success',
            title: 'Comprobante descargado',
            timer: 1500,
            showConfirmButton: false,
            toast: true,
            position: 'top-end'
        });
    } catch (error) {
        console.error('Error descargando comprobante:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo descargar el comprobante',
            confirmButtonColor: '#DC2626'
        });
    }
}
