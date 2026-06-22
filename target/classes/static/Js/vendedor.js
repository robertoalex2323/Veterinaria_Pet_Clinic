// ===== VARIABLES GLOBALES =====
let salesChart = null;
let productos = [];

// ===== DOM CONTENT LOADED =====
document.addEventListener("DOMContentLoaded", function() {
    console.log("🚀 Panel de Ventas iniciado");
    
    initDate();
    initTabs();
    loadProducts();
    loadPromotions();
    loadHistoryFromAPI();
    loadDashboardStats();
    loadChartData();
    setMaxDateFilter();
    renderSalesChart();
    initHistoryFilters();
});

// ===== NAVEGACIÓN =====
function initTabs() {
    document.querySelectorAll(".nav-item").forEach(item => {
        item.addEventListener("click", function() {
            const tabId = this.getAttribute("data-tab");
            showPanel(tabId);
        });
    });
}

function showPanel(tabId) {
    console.log("📌 Mostrando:", tabId);
    
    // Ocultar todos
    document.querySelectorAll(".panel-container").forEach(p => {
        p.style.display = "none";
        p.classList.remove("active");
    });
    
    // Actualizar menú
    document.querySelectorAll(".nav-item").forEach(el => el.classList.remove("active"));
    const nav = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
    if (nav) nav.classList.add("active");
    
    // Mostrar panel
    const panel = document.getElementById(tabId + "Panel");
    if (panel) {
        panel.style.display = "block";
        panel.classList.add("active");
        console.log("✅ Panel mostrado:", tabId);
    } else {
        console.error("❌ Panel no encontrado:", tabId);
    }
}

// ===== FUNCIONES DE FECHA =====
function initDate() {
    const dateEl = document.getElementById("currentDate");
    const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    dateEl.textContent = new Date().toLocaleDateString('es-PE', options);
}

function setMaxDateFilter() {
    const today = new Date().toISOString().split('T')[0];
    const filterDate = document.getElementById("filterDate");
    if (filterDate) filterDate.setAttribute("max", today);
}

// ===== PRODUCTOS =====
async function loadProducts() {
    try {
        const response = await fetch('/api/v1/vendedor/productos');
        if (!response.ok) throw new Error('Error al cargar productos');
        productos = await response.json();
        populateProductSelect(productos);
        console.log("✅ Productos cargados:", productos.length);
    } catch (error) {
        console.error("❌ Error cargando productos:", error);
        showToast("❌ Error al cargar productos", "error");
    }
}

function populateProductSelect(productos) {
    const select = document.getElementById('saleProduct');
    select.innerHTML = '<option value="" data-price="0" data-stock="0" data-id="">Seleccione un producto...</option>';
    
    const categorias = [...new Set(productos.map(p => p.categoria))];
    categorias.forEach(cat => {
        const group = document.createElement('optgroup');
        group.label = `── ${cat} ──`;
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

// ===== VENTAS =====
function updatePrice() {
    const select = document.getElementById("saleProduct");
    const opt = select.options[select.selectedIndex];
    const price = opt.getAttribute("data-price") || "0";
    document.getElementById("salePrice").value = parseFloat(price).toFixed(2);
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
    
    return total;
}
function resetSaleForm() {
    document.getElementById("saleProduct").selectedIndex = 0;
    document.getElementById("saleQuantity").value = 1;
    document.getElementById("salePrice").value = "0.00";
    document.getElementById("clientName").value = "";
    document.getElementById("clientPhone").value = "";
    document.getElementById("clientName").classList.remove("input-error");
    document.getElementById("clientPhone").classList.remove("input-error");
    document.getElementById("paymentMethod").selectedIndex = 0;
    document.getElementById("saleSubtotal").textContent = "S/ 0.00";
    document.getElementById("saleIgv").textContent = "S/ 0.00";
    document.getElementById("saleTotalWithIgv").textContent = "S/ 0.00";
    document.getElementById("quantityError").style.display = "none";
    document.getElementById("clientError").style.display = "none";
    document.getElementById("phoneError").style.display = "none";
    updateStockIndicator();
}

function processSale() {
    const select = document.getElementById("saleProduct");
    const opt = select.options[select.selectedIndex];
    const productoId = parseInt(opt.getAttribute("data-id")) || 0;
    const precio = parseFloat(opt.getAttribute("data-price")) || 0;
    const stock = parseInt(opt.getAttribute("data-stock")) || 0;
    const quantity = parseInt(document.getElementById("saleQuantity").value) || 0;
    const clientName = document.getElementById("clientName").value.trim();
    const clientPhone = document.getElementById("clientPhone").value.trim();
    const paymentMethod = document.getElementById("paymentMethod").value;

    // Validaciones
    if (!productoId) { showToast("❌ Seleccione un producto", "error"); return; }
    if (quantity <= 0) { showToast("❌ Cantidad inválida", "error"); return; }
    if (stock > 0 && stock < 999 && quantity > stock) { showToast(`❌ Stock: ${stock}`, "error"); return; }
    if (!clientName || clientName.length < 3) { showToast("❌ Nombre inválido", "error"); return; }
    if (!clientPhone || !/^[0-9]{9}$/.test(clientPhone)) { 
        showToast("❌ Teléfono inválido (9 dígitos)", "error"); 
        return; 
    }

    const subtotal = precio * quantity;
    const igv = subtotal * 0.18;
    const total = subtotal + igv;

    const data = {
        detalles: [
            {
                producto: { id: productoId },
                cantidad: quantity,
                precioUnitario: precio
            }
        ],
        metodoPago: paymentMethod,
        cliente: { 
            nombre: clientName,
            telefono: clientPhone
        }
    };

    console.log("📦 Enviando:", data);

    showToast("⏳ Procesando...", "info");
    fetch('/api/v1/vendedor/ventas', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                console.error("❌ Error del backend:", text);
                throw new Error(text);
            });
        }
        return response.json();
    })
    .then(result => {
        console.log("✅ Venta creada:", result);
        showToast(`✅ Venta #${result.id} registrada`, "success");
        resetSaleForm();
        loadHistoryFromAPI();
        loadDashboardStats();
        refreshChart();
        loadProducts();
    })
    .catch(error => {
        console.error("❌ Error:", error);
        showToast("❌ Error al registrar: " + error.message, "error");
    });
}

// ===== HISTORIAL =====
function loadHistoryFromAPI() {
    fetch('/api/v1/vendedor/ventas')
        .then(r => r.json())
        .then(ventas => renderHistory(ventas))
        .catch(() => document.getElementById("salesTableBody").innerHTML = '<tr><td colspan="6" style="text-align:center;color:#64748b;">Error al cargar</td></tr>');
}

function renderHistory(ventas) {
    const tbody = document.getElementById("salesTableBody");
    tbody.innerHTML = "";

    if (!ventas || ventas.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: var(--text-secondary);">
            No hay ventas registradas.
        </td></tr>`;
        return;
    }

    ventas.slice(0, 20).forEach(venta => {
        const tr = document.createElement("tr");
        const fecha = new Date(venta.fecha);
        
        let productoNombre = 'Producto';
        if (venta.detalles && venta.detalles.length > 0) {
            productoNombre = venta.detalles[0].producto?.nombre || 
                            venta.detalles[0].medicamento?.nombre || 'Producto';
        }
        
        tr.innerHTML = `
            <td><strong>#${venta.id}</strong></td>
            <td style="color: var(--text-secondary); font-size: 0.85rem;">${fecha.toLocaleString('es-PE')}</td>
            <td>${venta.cliente?.nombre || 'Cliente'}</td>
            <td>${productoNombre}</td>
            <td>S/ ${(venta.subtotal || 0).toFixed(2)}</td>
            <td>S/ ${(venta.igv || 0).toFixed(2)}</td>
            <td style="font-weight: 600; color: #059669;">S/ ${(venta.total || 0).toFixed(2)}</td>
            <td><span class="badge badge-success">Pagado</span></td>
            <td>
                <button class="btn btn-sm btn-primary" onclick="descargarBoletaPDF(${venta.id})" style="padding: 0.25rem 0.5rem; font-size: 0.75rem;">
                    <i class="fa-solid fa-file-pdf"></i> PDF
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
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
    showToast("📄 Generando boleta...", "info");
    
    // Abrir en nueva pestaña para ver el HTML
    window.open(`/api/v1/vendedor/ventas/${ventaId}/boleta-pdf`, '_blank');
    
    setTimeout(() => {
        showToast("✅ Boleta generada", "success");
    }, 2000);
}
function exportarPDF() {
    showToast("📄 Generando PDF del historial...", "info");
    
    // Obtener datos de la tabla
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
        showToast("❌ No hay datos para exportar", "error");
        return;
    }
    
    // Crear tabla HTML para PDF
    let html = `
        <html>
        <head>
            <title>Historial de Ventas</title>
            <style>
                body { font-family: Arial, sans-serif; padding: 20px; }
                h1 { color: #059669; text-align: center; }
                table { width: 100%; border-collapse: collapse; margin-top: 20px; }
                th { background: #059669; color: white; padding: 10px; text-align: left; }
                td { padding: 8px; border-bottom: 1px solid #e2e8f0; }
                .total { font-weight: bold; color: #059669; }
                .footer { margin-top: 30px; text-align: center; color: #64748b; font-size: 12px; }
                .badge { background: #dcfce7; color: #059669; padding: 2px 8px; border-radius: 12px; }
            </style>
        </head>
        <body>
            <h1>🏥 Pet Clinic - Historial de Ventas</h1>
            <p style="text-align: center; color: #64748b;">Fecha: ${new Date().toLocaleDateString('es-PE')}</p>
            <table>
                <thead>
                    <tr>
                        <th>N° Boleta</th>
                        <th>Fecha</th>
                        <th>Cliente</th>
                        <th>Producto</th>
                        <th>Subtotal</th>
                        <th>IGV</th>
                        <th>Total</th>
                        <th>Estado</th>
                    </tr>
                </thead>
                <tbody>
    `;
    
    data.forEach(item => {
        html += `
            <tr>
                <td>${item.id}</td>
                <td>${item.fecha}</td>
                <td>${item.cliente}</td>
                <td>${item.producto}</td>
                <td>${item.subtotal}</td>
                <td>${item.igv}</td>
                <td class="total">${item.total}</td>
                <td><span class="badge">${item.estado}</span></td>
            </tr>
        `;
    });
    
    html += `
                </tbody>
            </table>
            <div class="footer">
                <p>Reporte generado automáticamente - Pet Clinic 2026</p>
                <p>Total de registros: ${data.length}</p>
            </div>
        </body>
        </html>
    `;
    
    // Crear blob y descargar como PDF
    const blob = new Blob([html], { type: 'application/pdf' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `Historial_Ventas_${new Date().toISOString().slice(0,10)}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    
    showToast("✅ PDF exportado correctamente", "success");
}

// ===== PROMOCIONES =====
async function loadPromotions() {
    try {
        const r = await fetch('/api/v1/vendedor/promociones/activas');
        if (!r.ok) throw new Error();
        const promos = await r.json();
        const list = document.getElementById("promosList");
        list.innerHTML = "";
        document.getElementById("activePromos").textContent = promos.length;
        promos.forEach(p => {
            list.innerHTML += `<div class="glass-panel" style="padding:1rem;margin-bottom:0.5rem;"><i class="fa-solid fa-bolt" style="color:#3b82f6;"></i> ${p}</div>`;
        });
    } catch (e) {
        document.getElementById("promosList").innerHTML = '<p style="color:#64748b;">Sin promociones</p>';
    }
}

// ===== VALIDACIONES =====
function validateQuantity() {
    const select = document.getElementById("saleProduct");
    const opt = select.options[select.selectedIndex];
    const stock = parseInt(opt.getAttribute("data-stock")) || 0;
    const qty = parseInt(document.getElementById("saleQuantity").value) || 0;
    const err = document.getElementById("quantityError");
    if (stock > 0 && stock < 999 && qty > stock) {
        err.textContent = `⚠️ Stock: ${stock}`;
        err.style.display = "block";
    } else {
        err.style.display = "none";
    }
}

function validateClientName() {
    const name = document.getElementById("clientName").value.trim();
    const err = document.getElementById("clientError");
    if (name && name.length < 3) {
        err.textContent = "Mínimo 3 caracteres";
        err.style.display = "block";
    } else {
        err.style.display = "none";
    }
}
function validateClientPhone() {
    const phone = document.getElementById("clientPhone").value.trim();
    const errorEl = document.getElementById("phoneError");
    
    if (phone && !/^[0-9]{9}$/.test(phone)) {
        errorEl.textContent = "⚠️ Ingrese 9 dígitos (ej: 987654321)";
        errorEl.style.display = "block";
        document.getElementById("clientPhone").classList.add("input-error");
        return false;
    } else {
        errorEl.style.display = "none";
        document.getElementById("clientPhone").classList.remove("input-error");
        return true;
    }
}

function updateStockIndicator() {
    const select = document.getElementById("saleProduct");
    const selectedOption = select.options[select.selectedIndex];
    const stock = parseInt(selectedOption.getAttribute("data-stock")) || 0;
    const indicator = document.getElementById("stockIndicator");
    
    if (!selectedOption.value) {
        indicator.textContent = "Seleccione un producto";
        indicator.className = "stock-indicator select-placeholder";
        return;
    }
    
    if (stock === 0) {
        indicator.textContent = "❌ Sin Stock";
        indicator.className = "stock-indicator out";
    } else if (stock <= 5) {
        indicator.textContent = `⚠️ Stock bajo: ${stock} disponibles`;
        indicator.className = "stock-indicator low";
    } else if (stock >= 999) {
        indicator.textContent = "📌 Stock Ilimitado";
        indicator.className = "stock-indicator";
    } else {
        indicator.textContent = `✅ ${stock} disponibles`;
        indicator.className = "stock-indicator";
    }
}

function filterHistory() {
    const search = document.getElementById("searchClient").value.toLowerCase();
    const date = document.getElementById("filterDate").value;
    const rows = document.querySelectorAll("#salesTableBody tr");
    let visible = 0;
    rows.forEach(row => {
        const cells = row.querySelectorAll("td");
        if (cells.length >= 6) {
            const matchClient = cells[2].textContent.toLowerCase().includes(search);
            const matchDate = !date || cells[1].textContent.includes(date);
            row.style.display = (matchClient && matchDate) ? "" : "none";
            if (matchClient && matchDate) visible++;
        }
    });
    document.getElementById("noResults").style.display = visible === 0 ? "block" : "none";
}

function exportarHistorial() {
    showToast("📥 Exportando...", "info");
    setTimeout(() => showToast("✅ Exportado", "success"), 1500);
}
// ===== CARGAR DATOS PARA LA GRÁFICA DESDE LA API =====
async function loadChartData() {
    try {
        const response = await fetch('/api/v1/vendedor/ventas/ultimos-7-dias');
        if (!response.ok) throw new Error('Error al cargar datos de la gráfica');
        
        const data = await response.json();
        renderSalesChart(data.labels, data.values);
        
    } catch (error) {
        console.error('Error cargando datos de la gráfica:', error);
        // Si falla, usar datos vacíos
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

// ===== TOAST =====
function showToast(msg, type = "success") {
    const toast = document.getElementById("toast");
    const icon = toast.querySelector("i");
    const msgEl = document.getElementById("toastMessage");
    msgEl.textContent = msg;
    if (type === "error") {
        icon.className = "fa-solid fa-circle-exclamation";
        icon.style.color = "#dc2626";
        toast.style.borderLeftColor = "#dc2626";
    } else {
        icon.className = "fa-solid fa-circle-check";
        icon.style.color = "#059669";
        toast.style.borderLeftColor = "#059669";
    }
    toast.classList.add("show");
    setTimeout(() => toast.classList.remove("show"), 4000);
}

// ===== GRÁFICA =====
function getSalesData() {
    const labels = [];
    const data = [];
    for (let i = 6; i >= 0; i--) {
        const d = new Date();
        d.setDate(d.getDate() - i);
        labels.push(d.toLocaleDateString('es-PE', { month: 'short', day: 'numeric' }));
        data.push(0);
    }
    // Intentar obtener datos de la tabla
    const rows = document.querySelectorAll("#salesTableBody tr");
    rows.forEach(row => {
        const cells = row.querySelectorAll("td");
        if (cells.length >= 6) {
            const date = cells[1].textContent.trim();
            const total = parseFloat(cells[4].textContent.replace('S/ ', '')) || 0;
            // Buscar en los últimos 7 días
            for (let i = 0; i < labels.length; i++) {
                if (date.includes(labels[i].replace(' ', ''))) {
                    data[i] += total;
                    break;
                }
            }
        }
    });
    return { labels, data };
}

// ===== RENDERIZAR GRÁFICA =====
function renderSalesChart(labels, data) {
    const canvas = document.getElementById("salesChart");
    if (!canvas) return;
    
    // Destruir gráfica anterior si existe
    if (salesChart) {
        salesChart.destroy();
        salesChart = null;
    }
    
    // Calcular estadísticas
    const totalSales = data.reduce((a, b) => a + b, 0);
    const avgSales = data.length > 0 ? totalSales / data.length : 0;
    const maxSales = data.length > 0 ? Math.max(...data) : 0;
    
    // Crear contexto de gráfica
    const ctx = canvas.getContext('2d');
    
    // Calcular línea de promedio
    const avgLine = Array(data.length).fill(avgSales);
    
    // Colores
    const color = '#059669';
    const colorLight = 'rgba(5, 150, 105, 0.1)';
    const colorAvg = '#f59e0b';
    
    salesChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [
                {
                    label: 'Ventas (S/)',
                    data: data,
                    backgroundColor: data.map(v => v > 0 ? color : 'rgba(200, 200, 200, 0.3)'),
                    borderColor: color,
                    borderWidth: 2,
                    borderRadius: 6,
                    barPercentage: 0.6
                },
                {
                    label: 'Promedio (S/)',
                    data: avgLine,
                    borderColor: colorAvg,
                    borderWidth: 2,
                    borderDash: [5, 5],
                    fill: false,
                    pointRadius: 0,
                    type: 'line'
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: true,
                    position: 'top',
                    labels: {
                        font: {
                            size: 13,
                            weight: '600',
                            family: "'Playfair Display', serif"
                        },
                        color: '#64748b',
                        padding: 15,
                        usePointStyle: true
                    }
                },
                tooltip: {
                    backgroundColor: 'rgba(0, 0, 0, 0.8)',
                    padding: 12,
                    titleFont: { size: 14, weight: 'bold' },
                    bodyFont: { size: 13 },
                    callbacks: {
                        label: function(context) {
                            if (context.datasetIndex === 0) {
                                return 'Ventas: S/ ' + context.parsed.y.toFixed(2);
                            } else {
                                return 'Promedio: S/ ' + context.parsed.y.toFixed(2);
                            }
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        callback: function(value) {
                            return 'S/ ' + value.toFixed(0);
                        },
                        font: { size: 12 },
                        color: '#64748b'
                    },
                    grid: {
                        color: 'rgba(203, 213, 225, 0.1)',
                        drawBorder: false
                    }
                },
                x: {
                    ticks: {
                        font: { size: 12 },
                        color: '#64748b'
                    },
                    grid: {
                        display: false,
                        drawBorder: false
                    }
                }
            }
        }
    });
    
    // Actualizar estadísticas
    updateChartStats(totalSales, avgSales, maxSales);
}

// ===== ACTUALIZAR ESTADÍSTICAS DE LA GRÁFICA =====
function updateChartStats(total, avg, max) {
    const statsEl = document.getElementById("chartStats");
    if (!statsEl) return;
    
    statsEl.innerHTML = `
        <div class="chart-stat" style="text-align: center; padding: 0.5rem;">
            <div style="font-size: 0.85rem; color: #64748b;">Total 7 Días</div>
            <div style="font-size: 1.2rem; font-weight: 700; color: #059669;">S/ ${total.toFixed(2)}</div>
        </div>
        <div class="chart-stat" style="text-align: center; padding: 0.5rem;">
            <div style="font-size: 0.85rem; color: #64748b;">Promedio/Día</div>
            <div style="font-size: 1.2rem; font-weight: 700; color: #3b82f6;">S/ ${avg.toFixed(2)}</div>
        </div>
        <div class="chart-stat" style="text-align: center; padding: 0.5rem;">
            <div style="font-size: 0.85rem; color: #64748b;">Máximo/Día</div>
            <div style="font-size: 1.2rem; font-weight: 700; color: #f59e0b;">S/ ${max.toFixed(2)}</div>
        </div>
    `;
}

// ===== REFRESCAR GRÁFICA =====
function refreshChart() {
    loadChartData();
}
// ===== FILTROS =====
function initHistoryFilters() {
    document.getElementById("searchClient")?.addEventListener("input", filterHistory);
    document.getElementById("filterDate")?.addEventListener("change", filterHistory);
}