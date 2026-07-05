'use strict';

// Datos en memoria
let ventasData = [];
let stockBajoData = [];
let ventasFiltradas = [];

document.addEventListener('DOMContentLoaded', function () {
    cargarTodo();
    setupEventListeners();
});

// ==============================
// SETUP
// ==============================
function setupEventListeners() {
    const btnRefrescar = document.getElementById('btnRefrescar');
    if (btnRefrescar) {
        btnRefrescar.addEventListener('click', function () {
            cargarTodo();
            Swal.fire({
                icon: 'success',
                title: 'Actualizado',
                text: 'Reportes actualizados',
                timer: 1500,
                showConfirmButton: false,
                toast: true,
                position: 'top-end'
            });
        });
    }
}

// ==============================
// CARGAR TODOS LOS DATOS
// ==============================
async function cargarTodo() {
    await Promise.all([
        cargarVentas(),
        cargarStockBajo()
    ]);
    calcularTopMedicamentos();
}

// ==============================
// CARGAR VENTAS
// ==============================
async function cargarVentas() {
    const tbody = document.getElementById('reporteVentasBody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5"><i class="fas fa-spinner fa-spin fa-2x text-primary mb-3"></i><br><span class="text-muted">Cargando ventas...</span></td></tr>';

    try {
        const response = await fetch('/farmaceutico/api/reportes/ventas');
        if (!response.ok) throw new Error('Error al cargar ventas');
        ventasData = await response.json();
        ventasFiltradas = [...ventasData];
        renderizarVentas(ventasFiltradas);
        actualizarStats();
    } catch (error) {
        console.error('Error cargando ventas:', error);
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-danger"><i class="fas fa-exclamation-circle fa-2x mb-2"></i><br>Error al cargar ventas</td></tr>';
    }
}

// ==============================
// CARGAR STOCK BAJO
// ==============================
async function cargarStockBajo() {
    const tbody = document.getElementById('stockBajoBody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5"><i class="fas fa-spinner fa-spin fa-2x text-primary mb-3"></i><br><span class="text-muted">Cargando stock crítico...</span></td></tr>';

    try {
        const response = await fetch('/farmaceutico/api/reportes/stock-bajo');
        if (!response.ok) throw new Error('Error al cargar stock');
        stockBajoData = await response.json();
        renderizarStockBajo(stockBajoData);
    } catch (error) {
        console.error('Error cargando stock bajo:', error);
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5 text-danger"><i class="fas fa-exclamation-circle fa-2x mb-2"></i><br>Error al cargar stock crítico</td></tr>';
    }
}

// ==============================
// RENDERIZAR VENTAS
// ==============================
function renderizarVentas(ventas) {
    const tbody = document.getElementById('reporteVentasBody');
    const tfoot = document.getElementById('reporteVentasFoot');
    if (!tbody) return;

    if (!ventas || ventas.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5">' +
            '<i class="fas fa-chart-bar fa-3x text-muted mb-3"></i>' +
            '<h5 class="text-muted">No hay ventas en el periodo seleccionado</h5>' +
            '<p class="text-muted">Ajusta los filtros de fecha para ver más resultados.</p>' +
            '</td></tr>';
        if (tfoot) tfoot.classList.add('d-none');
        return;
    }

    let totalSubtotal = 0;
    let totalIgv = 0;
    let totalTotal = 0;

    tbody.innerHTML = ventas.map(v => {
        const fecha = v.fechaFormateada || v.fecha || '---';
        const cliente = v.cliente ? v.cliente.nombre : '---';
        const metodoPago = v.metodoPago || '---';
        const subtotal = parseFloat(v.subtotal || 0);
        const igv = parseFloat(v.igv || 0);
        const total = parseFloat(v.total || 0);
        const numVenta = 'VTA-' + String(v.id).padStart(5, '0');

        totalSubtotal += subtotal;
        totalIgv += igv;
        totalTotal += total;

        return `<tr>
            <td class="fw-bold text-primary">${numVenta}</td>
            <td><span class="text-nowrap">${fecha}</span></td>
            <td class="fw-bold">${cliente}</td>
            <td>${renderMetodoPagoBadge(metodoPago)}</td>
            <td class="text-end">S/ ${subtotal.toFixed(2)}</td>
            <td class="text-end">S/ ${igv.toFixed(2)}</td>
            <td class="text-end fw-bold text-success">S/ ${total.toFixed(2)}</td>
        </tr>`;
    }).join('');

    // Mostrar totales
    if (tfoot) {
        tfoot.classList.remove('d-none');
        document.getElementById('footSubtotal').textContent = 'S/ ' + totalSubtotal.toFixed(2);
        document.getElementById('footIgv').textContent = 'S/ ' + totalIgv.toFixed(2);
        document.getElementById('footTotal').textContent = 'S/ ' + totalTotal.toFixed(2);
    }
}

// ==============================
// RENDERIZAR STOCK BAJO
// ==============================
function renderizarStockBajo(items) {
    const tbody = document.getElementById('stockBajoBody');
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-5">' +
            '<i class="fas fa-check-circle fa-3x text-success mb-3"></i>' +
            '<h5 class="text-muted">No hay medicamentos con stock crítico</h5>' +
            '<p class="text-muted">Todos los medicamentos tienen stock suficiente.</p>' +
            '</td></tr>';
        return;
    }

    tbody.innerHTML = items.map(m => {
        const stock = m.stock != null ? m.stock : 0;
        const min = m.stockMinimo != null ? m.stockMinimo : 0;
        let estado = 'BAJO';
        let estadoClass = 'bajo';

        if (stock <= 0) {
            estado = 'AGOTADO';
            estadoClass = 'agotado';
        } else if (stock <= min) {
            estado = 'CRÍTICO';
            estadoClass = 'critico';
        }

        return `<tr>
            <td class="fw-bold">${m.nombre || '---'}</td>
            <td>${m.presentacion || '---'}</td>
            <td class="fw-bold ${stock <= 0 ? 'text-danger' : 'text-warning'}">${stock}</td>
            <td>${min}</td>
            <td><span class="badge-stock ${estadoClass}"><i class="fas fa-exclamation-circle"></i>${estado}</span></td>
        </tr>`;
    }).join('');
}

// ==============================
// CALCULAR TOP MEDICAMENTOS
// ==============================
function calcularTopMedicamentos() {
    const tbody = document.getElementById('topMedicamentosBody');
    if (!tbody) return;

    if (!ventasData || ventasData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5">' +
            '<i class="fas fa-trophy fa-3x text-muted mb-3"></i>' +
            '<h5 class="text-muted">No hay datos de ventas</h5>' +
            '</td></tr>';
        return;
    }

    // Agregar por medicamento
    const mapa = {};
    ventasData.forEach(v => {
        if (!v.detalles) return;
        v.detalles.forEach(d => {
            const nombre = d.medicamento ? d.medicamento.nombre : 'Desconocido';
            const cantidad = d.cantidad || 0;
            const precio = parseFloat(d.precioUnitario || 0);
            const ingresos = cantidad * precio;

            if (!mapa[nombre]) {
                mapa[nombre] = { nombre, unidades: 0, ingresos: 0 };
            }
            mapa[nombre].unidades += cantidad;
            mapa[nombre].ingresos += ingresos;
        });
    });

    // Convertir a array y ordenar
    const top = Object.values(mapa)
        .sort((a, b) => b.unidades - a.unidades)
        .slice(0, 10);

    if (top.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">No hay medicamentos vendidos</td></tr>';
        return;
    }

    tbody.innerHTML = top.map((m, i) => {
        const rank = i + 1;
        let rankClass = 'rank-other';
        if (rank === 1) rankClass = 'rank-1';
        else if (rank === 2) rankClass = 'rank-2';
        else if (rank === 3) rankClass = 'rank-3';

        return `<tr>
            <td><span class="rank-badge ${rankClass}">${rank}</span></td>
            <td class="fw-bold">${m.nombre}</td>
            <td><span class="badge bg-primary-soft text-primary">${m.unidades} und.</span></td>
            <td class="fw-bold text-success">S/ ${m.ingresos.toFixed(2)}</td>
        </tr>`;
    }).join('');
}

// ==============================
// APLICAR FILTROS DE VENTAS
// ==============================
function aplicarFiltrosVentas() {
    const desde = document.getElementById('filtroFechaDesde').value;
    const hasta = document.getElementById('filtroFechaHasta').value;
    const metodo = document.getElementById('filtroMetodoPagoReporte').value;

    ventasFiltradas = ventasData.filter(v => {
        // Filtro fecha desde
        if (desde) {
            const fechaDesde = new Date(desde + 'T00:00:00');
            const fechaVenta = new Date(v.fecha);
            if (fechaVenta < fechaDesde) return false;
        }

        // Filtro fecha hasta
        if (hasta) {
            const fechaHasta = new Date(hasta + 'T23:59:59');
            const fechaVenta = new Date(v.fecha);
            if (fechaVenta > fechaHasta) return false;
        }

        // Filtro método pago
        if (metodo && v.metodoPago !== metodo) return false;

        return true;
    });

    renderizarVentas(ventasFiltradas);
}

// ==============================
// LIMPIAR FILTROS
// ==============================
function limpiarFiltrosVentas() {
    document.getElementById('filtroFechaDesde').value = '';
    document.getElementById('filtroFechaHasta').value = '';
    document.getElementById('filtroMetodoPagoReporte').value = '';

    ventasFiltradas = [...ventasData];
    renderizarVentas(ventasFiltradas);
}

// ==============================
// ACTUALIZAR STATS
// ==============================
function actualizarStats() {
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    const hoyFin = new Date(hoy);
    hoyFin.setHours(23, 59, 59, 999);

    // Ventas hoy
    const ventasHoy = ventasData.filter(v => {
        const f = new Date(v.fecha);
        return f >= hoy && f <= hoyFin;
    });

    const totalHoy = ventasHoy.reduce((sum, v) => sum + parseFloat(v.total || 0), 0);
    const totalIngresos = ventasData.reduce((sum, v) => sum + parseFloat(v.total || 0), 0);

    const statVentasHoy = document.getElementById('statVentasHoy');
    const statTotalVentas = document.getElementById('statTotalVentas');
    const statTotalIngresos = document.getElementById('statTotalIngresos');

    if (statVentasHoy) statVentasHoy.textContent = 'S/ ' + totalHoy.toFixed(2);
    if (statTotalVentas) statTotalVentas.textContent = ventasData.length;
    if (statTotalIngresos) statTotalIngresos.textContent = 'S/ ' + totalIngresos.toFixed(2);
}

// ==============================
// HELPER: BADGE MÉTODO PAGO
// ==============================
function renderMetodoPagoBadge(metodo) {
    switch (metodo) {
        case 'EFECTIVO':
            return '<span class="badge-metodo efectivo"><i class="fas fa-money-bill-wave"></i>Efectivo</span>';
        case 'TARJETA':
            return '<span class="badge-metodo tarjeta"><i class="fas fa-credit-card"></i>Tarjeta</span>';
        case 'YAPE':
            return '<span class="badge-metodo yape"><i class="fas fa-mobile-alt"></i>Yape</span>';
        case 'PLIN':
            return '<span class="badge-metodo plin"><i class="fas fa-mobile-alt"></i>Plin</span>';
        case 'TRANSFERENCIA':
            return '<span class="badge-metodo transferencia"><i class="fas fa-exchange-alt"></i>Transferencia</span>';
        default:
            return '<span class="badge-metodo otro">' + (metodo || '---') + '</span>';
    }
}

// ==============================
// DESCARGAR PDF: REPORTE DE VENTAS
// ==============================
async function descargarReporteVentasPDF() {
    const desde = document.getElementById('filtroFechaDesde').value;
    const hasta = document.getElementById('filtroFechaHasta').value;

    Swal.fire({
        title: 'Generando reporte...',
        text: 'El PDF incluirá el logo de la veterinaria',
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading()
    });

    try {
        const params = new URLSearchParams();
        if (desde) params.append('desde', desde);
        if (hasta) params.append('hasta', hasta);

        const url = '/farmaceutico/reportes/ventas/pdf' + (params.toString() ? '?' + params.toString() : '');
        const response = await fetch(url);

        if (!response.ok) throw new Error('Error al descargar');

        const blob = await response.blob();
        const blobUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;

        let filename = 'Reporte_Ventas';
        if (desde) filename += '_' + desde;
        if (hasta) filename += '_a_' + hasta;
        filename += '.pdf';

        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(blobUrl);

        Swal.fire({
            icon: 'success',
            title: 'Reporte descargado',
            text: 'El PDF se ha generado con el logo incluido.',
            timer: 2000,
            showConfirmButton: false,
            toast: true,
            position: 'top-end'
        });
    } catch (error) {
        console.error('Error descargando reporte de ventas:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo descargar el reporte de ventas',
            confirmButtonColor: '#DC2626'
        });
    }
}

// ==============================
// DESCARGAR PDF: REPORTE DE STOCK BAJO
// ==============================
async function descargarReporteStockPDF() {
    Swal.fire({
        title: 'Generando reporte...',
        text: 'El PDF incluirá el logo de la veterinaria',
        allowOutsideClick: false,
        didOpen: () => Swal.showLoading()
    });

    try {
        const response = await fetch('/farmaceutico/reportes/stock-bajo');
        if (!response.ok) throw new Error('Error al descargar');

        const blob = await response.blob();
        const blobUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = 'Reporte_Stock_Critico.pdf';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(blobUrl);

        Swal.fire({
            icon: 'success',
            title: 'Reporte descargado',
            text: 'El PDF se ha generado con el logo incluido.',
            timer: 2000,
            showConfirmButton: false,
            toast: true,
            position: 'top-end'
        });
    } catch (error) {
        console.error('Error descargando reporte de stock:', error);
        Swal.fire({
            icon: 'error',
            title: 'Error',
            text: 'No se pudo descargar el reporte de stock',
            confirmButtonColor: '#DC2626'
        });
    }
}