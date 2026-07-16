

class DashboardFarmaceutico {
    constructor() {
        // Gráficos
        this.ventasChart = null;
        this.stockChart = null;

        // Datos que se leen del HTML (Thymeleaf) o de la API
        this.ventasHoy = 0;
        this.bajoStockCount = 0;
        this.stockNormalCount = 0;
        this.recetasPendientes = 0;
        this.totalMedicamentos = 0;
        this.recetasCompletadas = 0;
        this.proveedoresActivos = 0;
        this.eficienciaPorc = 0;

        // Ventas reales para gráfico y tabla
        this.ventas = [];
        this.datosVentas7Dias = { labels: [], datos: [] };

        // Configuración
        this.intervaloRefresco = 60000; // 60s

        // Endpoints
        this.API_VENTAS = '/farmaceutico/api/ventas';
        this.API_STATS  = '/farmaceutico/api/dashboard/estadisticas';
    }

    // ---------- Arranque ----------
    init() {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => this.iniciar());
        } else {
            this.iniciar();
        }
    }

    async iniciar() {
        try {
            this.leerDatosDelHTML();
            this.actualizarHora();
            setInterval(() => this.actualizarHora(), 1000);

            await this.cargarVentas();
            this.calcularVentas7Dias();

            this.crearGraficoVentas();
            this.crearGraficoStock();
            this.renderUltimasVentas();

            this.verificarStockCritico();
            this.animarTarjetas();

            this.configurarRefrescoAutomatico();

            console.log('Dashboard farmaceutico cargado');
        } catch (e) {
            console.error('Error iniciando dashboard:', e);
        }
    }

    // ---------- Lectura de datos renderizados por Thymeleaf ----------
    leerDatosDelHTML() {
        const num = (sel) => {
            const el = document.querySelector(sel);
            if (!el) return 0;
            const limpio = (el.textContent || '').toString().replace(/[^\d.-]/g, '');
            return parseFloat(limpio) || 0;
        };

        this.ventasHoy = num('[data-ventas-hoy]');
        this.bajoStockCount = num('[data-bajo-stock]');
        this.recetasPendientes = num('[data-recetas-pendientes]');

        const cajas = document.querySelectorAll('.stat-box-value');
        if (cajas.length >= 1) this.totalMedicamentos = parseInt(cajas[0].textContent) || 0;
        if (cajas.length >= 2) this.recetasCompletadas = parseInt(cajas[1].textContent) || 0;
        if (cajas.length >= 3) this.proveedoresActivos = parseInt(cajas[2].textContent) || 0;
        if (cajas.length >= 4) this.eficienciaPorc = parseInt(cajas[3].textContent) || 0;

        this.stockNormalCount = Math.max(this.totalMedicamentos - this.bajoStockCount, 0);
    }

    // ---------- Ventas reales desde la API ----------
    async cargarVentas() {
        try {
            const resp = await fetch(this.API_VENTAS, { headers: { 'Accept': 'application/json' } });
            if (!resp.ok) throw new Error('HTTP ' + resp.status);
            const data = await resp.json();
            this.ventas = Array.isArray(data) ? data : [];
        } catch (e) {
            console.warn('No se pudieron cargar las ventas desde la API:', e.message);
            this.ventas = [];
        }
    }

    _fechaVenta(v) {
        const raw = v.fecha || v.fechaVenta || v.createdAt || v.fecha_creacion || null;
        if (!raw) return null;
        const d = new Date(raw);
        return isNaN(d.getTime()) ? null : d;
    }

    _totalVenta(v) {
        const cand = v.total != null ? v.total
            : (v.montoTotal != null ? v.montoTotal
            : (v.monto != null ? v.monto
            : (v.importe != null ? v.importe
            : (v.precioTotal != null ? v.precioTotal : 0))));
        const n = parseFloat(cand);
        return isNaN(n) ? 0 : n;
    }

    _metodoPago(v) {
        return v.metodoPago || v.metodo_pago || v.metodo || 'Efectivo';
    }

    // ---------- Serie de 7 dias a partir de ventas reales ----------
    calcularVentas7Dias() {
        const dias = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];
        const hoy = new Date();
        const labels = [];
        const buckets = [];
        const claves = [];

        for (let i = 6; i >= 0; i--) {
            const f = new Date(hoy);
            f.setDate(hoy.getDate() - i);
            labels.push(dias[f.getDay()]);
            claves.push(f.toISOString().slice(0, 10));
            buckets.push(0);
        }

        this.ventas.forEach(v => {
            const f = this._fechaVenta(v);
            if (!f) return;
            const clave = f.toISOString().slice(0, 10);
            const idx = claves.indexOf(clave);
            if (idx >= 0) buckets[idx] += this._totalVenta(v);
        });

        this.datosVentas7Dias = {
            labels,
            datos: buckets.map(n => Math.round(n * 100) / 100)
        };
    }

    // ---------- Grafico de ventas (linea) ----------
    crearGraficoVentas() {
        const canvas = document.getElementById('ventasChart');
        if (!canvas || typeof Chart === 'undefined') return;
        if (this.ventasChart) this.ventasChart.destroy();

        this.ventasChart = new Chart(canvas.getContext('2d'), {
            type: 'line',
            data: {
                labels: this.datosVentas7Dias.labels,
                datasets: [{
                    label: 'Ventas (S/)',
                    data: this.datosVentas7Dias.datos,
                    borderColor: '#10B981',
                    backgroundColor: 'rgba(16,185,129,.15)',
                    borderWidth: 3,
                    fill: true,
                    tension: .4,
                    pointRadius: 5,
                    pointBackgroundColor: '#10B981',
                    pointBorderColor: '#fff',
                    pointBorderWidth: 2,
                    pointHoverRadius: 7
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { labels: { font: { size: 13, weight: '600' }, usePointStyle: true, pointStyle: 'circle' } },
                    tooltip: {
                        backgroundColor: 'rgba(0,0,0,.8)',
                        padding: 12,
                        callbacks: { label: c => 'Ventas: S/ ' + c.parsed.y.toFixed(2) }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { callback: v => 'S/ ' + v, color: '#6b7280', font: { size: 11 } },
                        grid: { color: '#f3f4f6' }
                    },
                    x: { ticks: { color: '#6b7280', font: { size: 11 } }, grid: { display: false } }
                }
            }
        });
    }

    // ---------- Grafico de stock (doughnut) ----------
    crearGraficoStock() {
        const canvas = document.getElementById('stockChart');
        if (!canvas || typeof Chart === 'undefined') return;
        if (this.stockChart) this.stockChart.destroy();

        const normal = Math.max(this.stockNormalCount, 0);
        const bajo = Math.max(this.bajoStockCount, 0);

        this.stockChart = new Chart(canvas.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Stock Normal', 'Stock Bajo'],
                datasets: [{
                    data: [normal, bajo],
                    backgroundColor: ['#10B981', '#EF4444'],
                    borderColor: '#fff',
                    borderWidth: 2,
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { font: { size: 12, weight: '600' }, usePointStyle: true, pointStyle: 'circle', padding: 15 }
                    },
                    tooltip: {
                        backgroundColor: 'rgba(0,0,0,.8)',
                        padding: 12,
                        callbacks: {
                            label: c => {
                                const total = c.dataset.data.reduce((a, b) => a + b, 0) || 1;
                                const pct = ((c.parsed / total) * 100).toFixed(1);
                                return c.label + ': ' + c.parsed + ' (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    // ---------- Tabla de ultimas ventas (reales) ----------
    renderUltimasVentas() {
        const tbody = document.getElementById('tablaUltimasVentas');
        if (!tbody) return;

        if (!this.ventas.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="tabla-vacia">No hay ventas registradas todavia.</td></tr>';
            return;
        }

        const ordenadas = [...this.ventas].sort((a, b) => {
            const fa = this._fechaVenta(a), fb = this._fechaVenta(b);
            return (fb ? fb.getTime() : 0) - (fa ? fa.getTime() : 0);
        }).slice(0, 5);

        tbody.innerHTML = ordenadas.map(v => {
            const id = v.id != null ? '#VTA' + String(v.id).padStart(5, '0') : '-';
            const f = this._fechaVenta(v);
            const fechaTxt = f ? f.toLocaleString('es-PE', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-';
            const total = this._totalVenta(v);
            return '<tr>' +
                '<td>' + id + '</td>' +
                '<td>' + fechaTxt + '</td>' +
                '<td><span class="pill">' + this._metodoPago(v) + '</span></td>' +
                '<td class="text-right">S/ ' + total.toFixed(2) + '</td>' +
                '</tr>';
        }).join('');
    }

    // ---------- Alerta de stock critico ----------
    verificarStockCritico() {
        const alerta = document.getElementById('alertaStockCritico');
        const texto = document.getElementById('medicamentosAlerta');
        if (!alerta || !texto) return;

        if (this.bajoStockCount > 0) {
            const n = this.bajoStockCount;
            texto.textContent = n === 1
                ? 'Tienes 1 medicamento con stock bajo. Actualiza el inventario.'
                : 'Tienes ' + n + ' medicamentos con stock bajo. Actualiza el inventario.';
            alerta.classList.remove('d-none');
            alerta.style.animation = 'pulse 2s infinite';
        } else {
            alerta.classList.add('d-none');
        }
    }

    // ---------- Animaciones ----------
    animarTarjetas() {
        document.querySelectorAll('.stat-card').forEach((c, i) => {
            c.style.animation = 'slideInUp .5s ease-out ' + (i * 0.1) + 's both';
        });
    }

    // ---------- Hora ----------
    actualizarHora() {
        const el = document.getElementById('horaActualizacion');
        if (!el) return;
        el.textContent = new Intl.DateTimeFormat('es-PE', {
            hour: '2-digit', minute: '2-digit', second: '2-digit'
        }).format(new Date());
    }

    // ---------- Refresco automatico (estadisticas + ventas) ----------
    configurarRefrescoAutomatico() {
        setInterval(async () => {
            await this.refrescarEstadisticas();
            await this.cargarVentas();
            this.calcularVentas7Dias();
            if (this.ventasChart) {
                this.ventasChart.data.labels = this.datosVentas7Dias.labels;
                this.ventasChart.data.datasets[0].data = this.datosVentas7Dias.datos;
                this.ventasChart.update('none');
            }
            this.renderUltimasVentas();
        }, this.intervaloRefresco);
    }

    async refrescarEstadisticas() {
        try {
            const resp = await fetch(this.API_STATS, { headers: { 'Accept': 'application/json' } });
            if (!resp.ok) return;
            const d = await resp.json();
            if (!d.success) return;

            this.ventasHoy = d.ventasHoy != null ? d.ventasHoy : this.ventasHoy;
            this.bajoStockCount = d.bajoStockCount != null ? d.bajoStockCount : this.bajoStockCount;
            this.stockNormalCount = d.stockNormalCount != null ? d.stockNormalCount : this.stockNormalCount;
            this.recetasPendientes = d.recetasPendientes != null ? d.recetasPendientes : this.recetasPendientes;
            this.totalMedicamentos = d.totalMedicamentos != null ? d.totalMedicamentos : this.totalMedicamentos;
            this.recetasCompletadas = d.recetasCompletadas != null ? d.recetasCompletadas : this.recetasCompletadas;
            this.proveedoresActivos = d.proveedoresActivos != null ? d.proveedoresActivos : this.proveedoresActivos;
            this.eficienciaPorc = d.eficienciaPorc != null ? d.eficienciaPorc : this.eficienciaPorc;

            this.pintarEstadisticas();

            if (this.stockChart) {
                this.stockChart.data.datasets[0].data = [
                    Math.max(this.stockNormalCount, 0),
                    Math.max(this.bajoStockCount, 0)
                ];
                this.stockChart.update('none');
            }
            this.verificarStockCritico();
        } catch (e) {
            console.warn('No se pudieron refrescar las estadisticas:', e.message);
        }
    }

    pintarEstadisticas() {
        const setTxt = (sel, val) => { const el = document.querySelector(sel); if (el) el.textContent = val; };

        setTxt('[data-ventas-hoy]', 'S/ ' + this.ventasHoy);
        setTxt('[data-bajo-stock]', this.bajoStockCount);
        setTxt('[data-recetas-pendientes]', this.recetasPendientes);

        const cajas = document.querySelectorAll('.stat-box-value');
        if (cajas[0]) { cajas[0].textContent = this.totalMedicamentos; this._flash(cajas[0]); }
        if (cajas[1]) { cajas[1].textContent = this.recetasCompletadas; this._flash(cajas[1]); }
        if (cajas[2]) { cajas[2].textContent = this.proveedoresActivos; this._flash(cajas[2]); }
        if (cajas[3]) { cajas[3].textContent = this.eficienciaPorc + '%'; this._flash(cajas[3]); }
    }

    _flash(el) {
        el.style.animation = 'none';
        void el.offsetWidth;
        el.style.animation = 'scaleIn .3s ease-out';
    }

    // ---------- Notificaciones toast ----------
    mostrarNotificacion(titulo, mensaje, tipo) {
        tipo = tipo || 'info';
        const toast = document.createElement('div');
        toast.className = 'toast-notif toast-' + tipo;
        toast.innerHTML = '<strong>' + titulo + '</strong><p>' + mensaje + '</p>';
        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.animation = 'slideOutRight .3s ease-out forwards';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    // ---------- Exportar CSV ----------
    exportarDatos() {
        try {
            const filas = [
                ['Metrica', 'Valor'],
                ['Fecha', new Date().toLocaleString('es-PE')],
                ['Ventas de hoy (S/)', this.ventasHoy],
                ['Stock bajo', this.bajoStockCount],
                ['Recetas pendientes', this.recetasPendientes],
                ['Total medicamentos', this.totalMedicamentos],
                ['Recetas completadas', this.recetasCompletadas],
                ['Proveedores activos', this.proveedoresActivos],
                ['Eficiencia (%)', this.eficienciaPorc]
            ];
            const csv = filas.map(f => f.join(',')).join('\n');
            const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'dashboard-farmaceutico-' + Date.now() + '.csv';
            a.click();
            URL.revokeObjectURL(url);
            this.mostrarNotificacion('Exportado', 'Los datos se descargaron en CSV.', 'success');
        } catch (e) {
            this.mostrarNotificacion('Error', 'No se pudo exportar: ' + e.message, 'error');
        }
    }

    // ---------- Imprimir ----------
    imprimirDashboard() {
        window.print();
    }
}

// Inicializacion global
const dashboard = new DashboardFarmaceutico();
dashboard.init();
window.dashboard = dashboard;