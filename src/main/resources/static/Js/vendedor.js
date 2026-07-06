let salesChart = null;
let productos = [];
let descuentoCalculado = 0;
let promocionesAplicadas = [];

let ventasCompletas = [];
let ventasFiltradas = [];
let paginaActual = 1;
let pageSize = 10;
let busquedaActiva = false;

document.addEventListener("DOMContentLoaded", function() {
    console.log("Panel de Ventas iniciado");
    
    precargarAudio();
    initDate();
    initTabs();
    loadProducts();
    loadPromotions();
    loadHistoryFromAPI();
    loadDashboardStats();
    loadChartData();
    setMaxDateFilter();
});

function precargarAudio() {
    const audio = document.getElementById("audioConfirmacion");
    if (audio) {
        audio.load();
        audio.volume = 1;
        audio.play().then(() => {
            audio.pause();
            audio.currentTime = 0;
        }).catch(() => {});
    }
}

function initTabs() {
    document.querySelectorAll(".nav-item").forEach(item => {
        item.addEventListener("click", function() {
            showPanel(this.getAttribute("data-tab"));
        });
    });
}

function showPanel(tabId) {
    document.querySelectorAll(".panel-container").forEach(p => {
        p.style.display = "none";
        p.classList.remove("active");
    });
    
    document.querySelectorAll(".nav-item").forEach(el => el.classList.remove("active"));
    const nav = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
    if (nav) nav.classList.add("active");
    
    const panel = document.getElementById(tabId + "Panel");
    if (panel) {
        panel.style.display = "block";
        panel.classList.add("active");
    }
}

// ===== FECHA =====
function initDate() {
    const dateEl = document.getElementById("currentDate");
    dateEl.textContent = new Date().toLocaleDateString('es-PE', { 
        weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' 
    });
}

function setMaxDateFilter() {
    const filterDate = document.getElementById("filterDate");
    if (filterDate) filterDate.setAttribute("max", new Date().toISOString().split('T')[0]);
}

// ===== PRODUCTOS =====
async function loadProducts() {
    try {
        const response = await fetch('/api/v1/vendedor/productos');
        if (!response.ok) throw new Error('Error al cargar productos');
        productos = await response.json();
        populateProductSelect(productos);
        console.log("Productos cargados:", productos.length);
    } catch (error) {
        showToast("Error al cargar productos", "error");
    }
}

function populateProductSelect(productos) {
    const select = document.getElementById('saleProduct');
    select.innerHTML = '<option value="" data-price="0" data-stock="0" data-id="">Seleccione un producto...</option>';
    
    const categorias = [...new Set(productos.map(p => p.categoria))];
    categorias.forEach(cat => {
        const group = document.createElement('optgroup');
        group.label = `-- ${cat} --`;
        productos.filter(p => p.categoria === cat).forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.id;
            opt.setAttribute('data-price', p.precio);
            opt.setAttribute('data-stock', p.stock);
            opt.setAttribute('data-id', p.id);
            opt.textContent = `${p.nombre} (S/ ${p.precio.toFixed(2)})`;
            group.appendChild(opt);
        });
        select.appendChild(group);
    });
    updateStockIndicator();
}

// ===== AUDIO =====
function reproducirSonidoConfirmacion() {
    const audio = document.getElementById("audioConfirmacion");
    if (!audio) return;
    audio.currentTime = 0;
    audio.volume = 1;
    audio.muted = false;
    audio.play().catch(() => {});
}

// ===== DESCUENTO EN TIEMPO REAL =====
async function calcularDescuentoEnTiempoReal() {
    const select = document.getElementById("saleProduct");
    const opt = select.options[select.selectedIndex];
    const productoId = parseInt(opt.getAttribute("data-id")) || 0;
    const quantity = parseInt(document.getElementById("saleQuantity").value) || 0;
    const price = parseFloat(opt.getAttribute("data-price")) || 0;
    const clientName = document.getElementById("clientName").value.trim();

    if (!productoId || quantity <= 0 || !clientName || clientName.length < 3) {
        document.getElementById("saleDescuento").textContent = "-S/ 0.00";
        document.getElementById("promocionesAplicables").style.display = "none";
        descuentoCalculado = 0;
        promocionesAplicadas = [];
        return;
    }

    try {
        const response = await fetch('/api/v1/vendedor/calcular-descuento', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productoId, cantidad: quantity, precioUnitario: price, clienteNombre: clientName
            })
        });

        if (response.ok) {
            const result = await response.json();
            descuentoCalculado = result.descuento || 0;
            promocionesAplicadas = result.promociones || [];

            document.getElementById("saleDescuento").textContent = `-S/ ${descuentoCalculado.toFixed(2)}`;
            
            const promoContainer = document.getElementById("promocionesAplicables");
            const promoList = document.getElementById("listaPromocionesAplicables");
            
            if (promocionesAplicadas.length > 0) {
                promoContainer.style.display = "block";
                promoList.innerHTML = promocionesAplicadas.map(p => 
                    `<span style="background:#d1fae5;padding:0.15rem 0.75rem;border-radius:12px;margin:0.15rem;font-size:0.75rem;">
                        <i class="fas fa-tag" style="color:#059669;"></i> ${p}
                    </span>`
                ).join(' ');
                document.getElementById("saleDescuento").style.color = "#dc2626";
                document.getElementById("saleDescuento").style.fontWeight = "700";
            } else {
                promoContainer.style.display = "none";
                document.getElementById("saleDescuento").style.color = "#64748b";
                document.getElementById("saleDescuento").style.fontWeight = "600";
            }
            
            const subtotal = price * quantity;
            const totalConDescuento = subtotal + (subtotal * 0.18) - descuentoCalculado;
            document.getElementById("saleTotalWithIgv").textContent = `S/ ${totalConDescuento.toFixed(2)}`;
        }
    } catch (error) {
        console.error('Error:', error);
    }
}

// ===== VENTAS =====
function updatePrice() {
    const opt = document.getElementById("saleProduct").options[document.getElementById("saleProduct").selectedIndex];
    document.getElementById("salePrice").value = parseFloat(opt.getAttribute("data-price") || "0").toFixed(2);
    updateStockIndicator();
    validateQuantity();
    calculateTotal();
}

function calculateTotal() {
    const price = parseFloat(document.getElementById("salePrice").value) || 0;
    const quantity = parseInt(document.getElementById("saleQuantity").value) || 1;
    const subtotal = price * quantity;
    const igv = subtotal * 0.18;
    const total = subtotal + igv;
    
    document.getElementById("saleSubtotal").textContent = `S/ ${subtotal.toFixed(2)}`;
    document.getElementById("saleIgv").textContent = `S/ ${igv.toFixed(2)}`;
    document.getElementById("saleTotalWithIgv").textContent = `S/ ${total.toFixed(2)}`;
    
    calcularDescuentoEnTiempoReal();
    return total;
}

function resetSaleForm() {
    document.getElementById("saleProduct").selectedIndex = 0;
    document.getElementById("saleQuantity").value = 1;
    document.getElementById("salePrice").value = "0.00";
    document.getElementById("clientName").value = "";
    document.getElementById("clientPhone").value = "";
    document.getElementById("paymentMethod").selectedIndex = 0;
    document.getElementById("saleSubtotal").textContent = "S/ 0.00";
    document.getElementById("saleIgv").textContent = "S/ 0.00";
    document.getElementById("saleDescuento").textContent = "-S/ 0.00";
    document.getElementById("saleTotalWithIgv").textContent = "S/ 0.00";
    document.getElementById("promocionesAplicables").style.display = "none";
    descuentoCalculado = 0;
    promocionesAplicadas = [];
    updateStockIndicator();
}

function processSale() {
    const opt = document.getElementById("saleProduct").options[document.getElementById("saleProduct").selectedIndex];
    const productoId = parseInt(opt.getAttribute("data-id")) || 0;
    const precio = parseFloat(opt.getAttribute("data-price")) || 0;
    const stock = parseInt(opt.getAttribute("data-stock")) || 0;
    const quantity = parseInt(document.getElementById("saleQuantity").value) || 0;
    const clientName = document.getElementById("clientName").value.trim();
    const clientPhone = document.getElementById("clientPhone").value.trim();
    const paymentMethod = document.getElementById("paymentMethod").value;

    if (!productoId) { showToast("Seleccione un producto", "error"); return; }
    if (quantity <= 0) { showToast("Cantidad invalida", "error"); return; }
    if (stock > 0 && stock < 999 && quantity > stock) { showToast(`Stock: ${stock}`, "error"); return; }
    if (!clientName || clientName.length < 3) { showToast("Nombre invalido", "error"); return; }
    if (!clientPhone || !/^[0-9]{9}$/.test(clientPhone)) { 
        showToast("Telefono invalido (9 digitos)", "error"); 
        return; 
    }

    showToast("Procesando...", "info");

    fetch('/api/v1/vendedor/ventas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            detalles: [{ producto: { id: productoId }, cantidad: quantity, precioUnitario: precio }],
            metodoPago: paymentMethod,
            cliente: { nombre: clientName, telefono: clientPhone },
            descuentoAplicado: descuentoCalculado
        })
    })
    .then(response => {
        if (!response.ok) throw new Error('Error al registrar');
        return response.json();
    })
    .then(result => {
        showToast(`Venta #${result.id} registrada`, "success");
        setTimeout(reproducirSonidoConfirmacion, 300);
        resetSaleForm();
        loadHistoryFromAPI();
        loadDashboardStats();
        refreshChart();
        loadProducts();
    })
    .catch(error => {
        showToast("Error al registrar: " + error.message, "error");
    });
}

// ===== HISTORIAL CON PAGINACION =====
function loadHistoryFromAPI() {
    fetch('/api/v1/vendedor/ventas')
        .then(r => r.json())
        .then(ventas => {
            ventasCompletas = ventas;
            if (!busquedaActiva) {
                ventasFiltradas = [...ventasCompletas];
            }
            paginaActual = 1;
            renderizarTabla();
        })
        .catch(() => {
            document.getElementById("salesTableBody").innerHTML = '<tr><td colspan="9" style="text-align:center;color:#64748b;">Error al cargar</td></tr>';
        });
}

function renderizarTabla() {
    const datos = busquedaActiva ? ventasFiltradas : ventasCompletas;
    const total = datos.length;
    const totalPaginas = Math.ceil(total / pageSize) || 1;
    
    if (paginaActual > totalPaginas) paginaActual = totalPaginas;
    
    const inicio = (paginaActual - 1) * pageSize;
    const fin = Math.min(inicio + pageSize, total);
    const paginaDatos = datos.slice(inicio, fin);
    
    const tbody = document.getElementById("salesTableBody");
    tbody.innerHTML = "";
    
    if (paginaDatos.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-secondary); padding: 2rem;">
            <i class="fas fa-inbox" style="font-size: 2rem; opacity: 0.5;"></i>
            <p>No hay ventas registradas</p>
        </td></tr>`;
        return;
    }
    
    paginaDatos.forEach(venta => {
        const tr = document.createElement("tr");
        const fecha = new Date(venta.fecha);
        let productoNombre = 'Producto';
        if (venta.detalles?.length > 0) {
            productoNombre = venta.detalles[0].producto?.nombre || 
                            venta.detalles[0].medicamento?.nombre || 'Producto';
        }
        
        tr.innerHTML = `
            <td><strong>#${venta.id}</strong></td>
            <td>${fecha.toLocaleString('es-PE')}</td>
            <td>${venta.cliente?.nombre || 'Cliente'}</td>
            <td>${productoNombre}</td>
            <td>S/ ${(venta.subtotal || 0).toFixed(2)}</td>
            <td>S/ ${(venta.igv || 0).toFixed(2)}</td>
            <td style="font-weight:600;color:#059669;">S/ ${(venta.total || 0).toFixed(2)}</td>
            <td><span class="badge badge-success">Pagado</span></td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="descargarBoletaPDF(${venta.id})" title="PDF">
                    <i class="fas fa-file-pdf"></i>
                </button>
                <button class="btn btn-sm btn-danger" onclick="eliminarVenta(${venta.id})" title="Eliminar" style="background:#dc2626;color:white;border:none;border-radius:4px;padding:0.25rem 0.5rem;cursor:pointer;">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
    
    // Actualizar paginacion
    document.getElementById("paginaDesde").textContent = total > 0 ? inicio + 1 : 0;
    document.getElementById("paginaHasta").textContent = fin;
    document.getElementById("totalRegistros").textContent = total;
    
    document.getElementById("btnPrimera").disabled = paginaActual === 1;
    document.getElementById("btnAnterior").disabled = paginaActual === 1;
    document.getElementById("btnSiguiente").disabled = paginaActual === totalPaginas;
    document.getElementById("btnUltima").disabled = paginaActual === totalPaginas;
    
    const paginasContainer = document.getElementById("paginasNumeros");
    paginasContainer.innerHTML = "";
    
    let inicioPaginas = Math.max(1, paginaActual - 2);
    let finPaginas = Math.min(totalPaginas, paginaActual + 2);
    
    if (inicioPaginas > 1) {
        const btn = document.createElement("button");
        btn.className = "btn btn-sm";
        btn.style.cssText = "padding:0.3rem 0.7rem;font-size:0.8rem;background:#f1f5f9;border:1px solid #e2e8f0;border-radius:4px;cursor:pointer;";
        btn.textContent = "1";
        btn.onclick = () => irPagina(1);
        paginasContainer.appendChild(btn);
        if (inicioPaginas > 2) {
            const span = document.createElement("span");
            span.textContent = "...";
            span.style.cssText = "padding:0.3rem 0.3rem;color:#64748b;";
            paginasContainer.appendChild(span);
        }
    }
    
    for (let i = inicioPaginas; i <= finPaginas; i++) {
        const btn = document.createElement("button");
        btn.className = "btn btn-sm";
        btn.style.cssText = `padding:0.3rem 0.7rem;font-size:0.8rem;background:${i === paginaActual ? '#059669' : '#f1f5f9'};color:${i === paginaActual ? 'white' : '#1e293b'};border:1px solid ${i === paginaActual ? '#059669' : '#e2e8f0'};border-radius:4px;cursor:pointer;`;
        btn.textContent = i;
        btn.onclick = () => irPagina(i);
        paginasContainer.appendChild(btn);
    }
    
    if (finPaginas < totalPaginas) {
        if (finPaginas < totalPaginas - 1) {
            const span = document.createElement("span");
            span.textContent = "...";
            span.style.cssText = "padding:0.3rem 0.3rem;color:#64748b;";
            paginasContainer.appendChild(span);
        }
        const btn = document.createElement("button");
        btn.className = "btn btn-sm";
        btn.style.cssText = "padding:0.3rem 0.7rem;font-size:0.8rem;background:#f1f5f9;border:1px solid #e2e8f0;border-radius:4px;cursor:pointer;";
        btn.textContent = totalPaginas;
        btn.onclick = () => irPagina(totalPaginas);
        paginasContainer.appendChild(btn);
    }
}

function irPagina(pagina) {
    const total = Math.ceil((ventasFiltradas.length || ventasCompletas.length) / pageSize);
    if (pagina < 1 || pagina > total) return;
    paginaActual = pagina;
    renderizarTabla();
}

function cambiarPageSize() {
    pageSize = parseInt(document.getElementById("pageSize").value);
    paginaActual = 1;
    renderizarTabla();
}

function buscarVentas() {
    const searchTerm = document.getElementById("searchClient").value.toLowerCase().trim();
    const filterDate = document.getElementById("filterDate").value;
    
    if (!searchTerm && !filterDate) {
        showToast("Ingrese un criterio de búsqueda", "info");
        return;
    }
    
    busquedaActiva = true;
    ventasFiltradas = ventasCompletas.filter(venta => {
        let coincide = true;
        if (searchTerm) {
            coincide = coincide && (venta.cliente?.nombre || '').toLowerCase().includes(searchTerm);
        }
        if (filterDate) {
            const fechaStr = new Date(venta.fecha).toISOString().split('T')[0];
            coincide = coincide && fechaStr === filterDate;
        }
        return coincide;
    });
    
    paginaActual = 1;
    renderizarTabla();
    showToast(`Se encontraron ${ventasFiltradas.length} resultados`, "success");
}

function limpiarFiltros() {
    document.getElementById("searchClient").value = "";
    document.getElementById("filterDate").value = "";
    busquedaActiva = false;
    ventasFiltradas = [...ventasCompletas];
    paginaActual = 1;
    renderizarTabla();
    showToast("Filtros limpiados", "info");
}

// ===== ELIMINAR VENTA =====
function eliminarVenta(id) {
    if (!confirm(`¿Eliminar venta #${id}?`)) return;
    
    fetch(`/api/v1/vendedor/ventas/${id}`, { method: 'DELETE' })
        .then(response => {
            if (!response.ok) throw new Error('Error al eliminar');
            showToast(`Venta #${id} eliminada`, "success");
            loadHistoryFromAPI();
        })
        .catch(error => showToast("Error al eliminar: " + error.message, "error"));
}

// ===== DASHBOARD =====
function loadDashboardStats() {
    fetch('/api/v1/vendedor/ventas/hoy')
        .then(r => r.json())
        .then(data => {
            document.getElementById('todaySales').textContent = `S/ ${(data.total||0).toFixed(2)}`;
            document.getElementById('todayOperations').textContent = data.cantidad || 0;
            document.getElementById('todayClients').textContent = data.cantidad || 0;
        })
        .catch(() => {});
}

function descargarBoletaPDF(ventaId) {
    showToast("Generando boleta...", "info");
    window.open(`/api/v1/vendedor/ventas/${ventaId}/boleta-pdf`, '_blank');
    setTimeout(() => showToast("Boleta generada", "success"), 2000);
}

function exportarPDF() {
    showToast("Generando PDF del historial...", "info");
    const rows = document.querySelectorAll("#salesTableBody tr");
    const data = [];
    
    rows.forEach(row => {
        const cells = row.querySelectorAll("td");
        if (cells.length >= 9) {
            data.push({
                id: cells[0].textContent.trim(),
                fecha: cells[1].textContent.trim(),
                cliente: cells[2].textContent.trim(),
                producto: cells[3].textContent.trim(),
                subtotal: cells[4].textContent.trim(),
                igv: cells[5].textContent.trim(),
                total: cells[6].textContent.trim(),
                estado: cells[7].textContent.trim()
            });
        }
    });
    
    if (data.length === 0) {
        showToast("No hay datos para exportar", "error");
        return;
    }
    
    let html = `<html><head><title>Historial de Ventas</title>
        <style>body{font-family:Arial;padding:20px}h1{color:#059669;text-align:center}
        table{width:100%;border-collapse:collapse;margin-top:20px}
        th{background:#059669;color:white;padding:10px;text-align:left}
        td{padding:8px;border-bottom:1px solid #e2e8f0}
        .total{font-weight:bold;color:#059669}
        .badge{background:#dcfce7;color:#059669;padding:2px 8px;border-radius:12px}
        .footer{margin-top:30px;text-align:center;color:#64748b;font-size:12px}
    </style></head><body>
        <h1>Pet Clinic - Historial de Ventas</h1>
        <p style="text-align:center;color:#64748b;">Fecha: ${new Date().toLocaleDateString('es-PE')}</p>
        <table><thead><tr><th>N Boleta</th><th>Fecha</th><th>Cliente</th><th>Producto</th>
        <th>Subtotal</th><th>IGV</th><th>Total</th><th>Estado</th></tr></thead><tbody>`;
    
    data.forEach(item => {
        html += `<tr><td>${item.id}</td><td>${item.fecha}</td><td>${item.cliente}</td>
        <td>${item.producto}</td><td>${item.subtotal}</td><td>${item.igv}</td>
        <td class="total">${item.total}</td><td><span class="badge">${item.estado}</span></td></tr>`;
    });
    
    html += `</tbody></table><div class="footer"><p>Reporte generado automaticamente - Pet Clinic 2026</p>
        <p>Total de registros: ${data.length}</p></div></body></html>`;
    
    const blob = new Blob([html], { type: 'application/pdf' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `Historial_Ventas_${new Date().toISOString().slice(0,10)}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    showToast("PDF exportado correctamente", "success");
}

async function loadPromotions() {
    try {
        const r = await fetch('/api/v1/vendedor/promociones/activas');
        if (!r.ok) throw new Error();
        const promos = await r.json();
        const list = document.getElementById("promosList");
        list.innerHTML = "";
        document.getElementById("activePromos").textContent = promos.length;
        promos.forEach(p => {
            list.innerHTML += `<div class="glass-panel" style="padding:1rem;margin-bottom:0.5rem;"><i class="fas fa-bolt" style="color:#3b82f6;"></i> ${p}</div>`;
        });
    } catch (e) {
        document.getElementById("promosList").innerHTML = '<p style="color:#64748b;">Sin promociones</p>';
    }
}

function validateQuantity() {
    const opt = document.getElementById("saleProduct").options[document.getElementById("saleProduct").selectedIndex];
    const stock = parseInt(opt.getAttribute("data-stock")) || 0;
    const qty = parseInt(document.getElementById("saleQuantity").value) || 0;
    const err = document.getElementById("quantityError");
    err.style.display = (stock > 0 && stock < 999 && qty > stock) ? "block" : "none";
    if (stock > 0 && stock < 999 && qty > stock) {
        err.textContent = `Stock: ${stock}`;
    }
}

function validateClientName() {
    const name = document.getElementById("clientName").value.trim();
    const err = document.getElementById("clientError");
    if (name && name.length < 3) {
        err.textContent = "Minimo 3 caracteres";
        err.style.display = "block";
    } else {
        err.style.display = "none";
        if (name.length >= 3) calcularDescuentoEnTiempoReal();
    }
}

function validateClientPhone() {
    const phone = document.getElementById("clientPhone").value.trim();
    const errorEl = document.getElementById("phoneError");
    if (phone && !/^[0-9]{9}$/.test(phone)) {
        errorEl.textContent = "Ingrese 9 digitos";
        errorEl.style.display = "block";
        document.getElementById("clientPhone").classList.add("input-error");
        return false;
    }
    errorEl.style.display = "none";
    document.getElementById("clientPhone").classList.remove("input-error");
    return true;
}

function updateStockIndicator() {
    const opt = document.getElementById("saleProduct").options[document.getElementById("saleProduct").selectedIndex];
    const stock = parseInt(opt.getAttribute("data-stock")) || 0;
    const indicator = document.getElementById("stockIndicator");
    
    if (!opt.value) {
        indicator.textContent = "Seleccione un producto";
        indicator.className = "stock-indicator select-placeholder";
        return;
    }
    
    if (stock === 0) {
        indicator.textContent = "Sin Stock";
        indicator.className = "stock-indicator out";
    } else if (stock <= 5) {
        indicator.textContent = `Stock bajo: ${stock} disponibles`;
        indicator.className = "stock-indicator low";
    } else if (stock >= 999) {
        indicator.textContent = "Stock Ilimitado";
        indicator.className = "stock-indicator";
    } else {
        indicator.textContent = `${stock} disponibles`;
        indicator.className = "stock-indicator";
    }
}

// ===== GRAFICA =====
async function loadChartData() {
    try {
        const response = await fetch('/api/v1/vendedor/ventas/ultimos-7-dias');
        if (!response.ok) throw new Error('Error al cargar datos de la grafica');
        const data = await response.json();
        renderSalesChart(data.labels, data.values);
    } catch (error) {
        const today = new Date();
        const labels = [];
        for (let i = 6; i >= 0; i--) {
            const d = new Date(today);
            d.setDate(d.getDate() - i);
            labels.push(d.toLocaleDateString('es-PE', { month: 'short', day: 'numeric' }));
        }
        renderSalesChart(labels, Array(7).fill(0));
    }
}

function renderSalesChart(labels, data) {
    const canvas = document.getElementById("salesChart");
    if (!canvas) return;
    
    if (!data || !Array.isArray(data) || data.length === 0) {
        const today = new Date();
        const defaultLabels = [];
        for (let i = 6; i >= 0; i--) {
            const d = new Date(today);
            d.setDate(d.getDate() - i);
            defaultLabels.push(d.toLocaleDateString('es-PE', { month: 'short', day: 'numeric' }));
        }
        labels = defaultLabels;
        data = Array(7).fill(0);
    }
    
    if (!labels || !Array.isArray(labels) || labels.length === 0) {
        const today = new Date();
        labels = [];
        for (let i = 6; i >= 0; i--) {
            const d = new Date(today);
            d.setDate(d.getDate() - i);
            labels.push(d.toLocaleDateString('es-PE', { month: 'short', day: 'numeric' }));
        }
    }
    
    while (data.length < labels.length) data.push(0);
    while (data.length > labels.length) data.pop();
    
    if (salesChart) {
        salesChart.destroy();
        salesChart = null;
    }
    
    const totalSales = data.reduce((a, b) => a + b, 0);
    const avgSales = data.length > 0 ? totalSales / data.length : 0;
    const maxSales = data.length > 0 ? Math.max(...data) : 0;
    const ctx = canvas.getContext('2d');
    const avgLine = Array(data.length).fill(avgSales);
    const color = '#059669';
    const colorAvg = '#f59e0b';
    
    salesChart = new Chart(ctx, {
        type: 'bar',
        data: { labels, datasets: [
            { label: 'Ventas (S/)', data, backgroundColor: data.map(v => v > 0 ? color : 'rgba(200,200,200,0.3)'), borderColor: color, borderWidth: 2, borderRadius: 6, barPercentage: 0.6 },
            { label: 'Promedio (S/)', data: avgLine, borderColor: colorAvg, borderWidth: 2, borderDash: [5,5], fill: false, pointRadius: 0, type: 'line' }
        ] },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: true, position: 'top', labels: { font: { size: 13, weight: '600', family: "'Playfair Display', serif" }, color: '#64748b', padding: 15, usePointStyle: true } },
                tooltip: { backgroundColor: 'rgba(0,0,0,0.8)', padding: 12, callbacks: { label: function(context) { return context.dataset.label + ': S/ ' + context.parsed.y.toFixed(2); } } }
            },
            scales: {
                y: { beginAtZero: true, ticks: { callback: value => 'S/ ' + value.toFixed(0), color: '#64748b' }, grid: { color: 'rgba(203,213,225,0.1)', drawBorder: false } },
                x: { ticks: { color: '#64748b' }, grid: { display: false } }
            }
        }
    });
    
    document.getElementById("chartStats").innerHTML = `
        <div style="text-align:center;padding:0.5rem;"><div style="font-size:0.85rem;color:#64748b;">Total 7 Dias</div>
        <div style="font-size:1.2rem;font-weight:700;color:#059669;">S/ ${totalSales.toFixed(2)}</div></div>
        <div style="text-align:center;padding:0.5rem;"><div style="font-size:0.85rem;color:#64748b;">Promedio/Dia</div>
        <div style="font-size:1.2rem;font-weight:700;color:#3b82f6;">S/ ${avgSales.toFixed(2)}</div></div>
        <div style="text-align:center;padding:0.5rem;"><div style="font-size:0.85rem;color:#64748b;">Maximo/Dia</div>
        <div style="font-size:1.2rem;font-weight:700;color:#f59e0b;">S/ ${maxSales.toFixed(2)}</div></div>
    `;
}

function refreshChart() {
    loadChartData();
}

function showToast(msg, type = "success") {
    const toast = document.getElementById("toast");
    const icon = toast.querySelector("i");
    const msgEl = document.getElementById("toastMessage");
    msgEl.textContent = msg;
    
    if (type === "error") {
        icon.className = "fas fa-circle-exclamation";
        icon.style.color = "#dc2626";
        toast.style.borderLeftColor = "#dc2626";
    } else {
        icon.className = "fas fa-circle-check";
        icon.style.color = "#059669";
        toast.style.borderLeftColor = "#059669";
    }
    
    toast.classList.add("show");
    setTimeout(() => toast.classList.remove("show"), 4000);
}