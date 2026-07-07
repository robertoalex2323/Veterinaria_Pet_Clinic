(function () {
  async function fetchMetrics() {
    try {
      const response = await fetch('/vendedor/api/dashboard-metrics');
      if (!response.ok) throw new Error('Network response was not ok');
      const data = await response.json();
      return data;
    } catch (error) {
      console.error('Error fetching dashboard metrics:', error);
      return null;
    }
  }

  function updateStats(data) {
    if (!data) return;

    const setText = (id, v) => {
      const el = document.getElementById(id);
      if (el) {
        // Formatear si es necesario
        el.textContent = String(v);
      }
    };

    setText('ventasHoy', data.ventasHoy ?? 0);
    setText('promosActivas', data.promosActivas ?? 0);
    setText('boletasEmitidas', data.boletasEmitidas ?? 0);
    setText('recomendacionesGeneradas', data.recomendacionesGeneradas ?? 0);
  }

  function drawLineChart(data7) {
    const canvas = document.getElementById('ventas7diasChart');

    if (!canvas) return;

    // Si no hay datos, no dibujamos con valores inventados.
    if (!data7 || !Array.isArray(data7) || data7.length === 0) return;


    const labels = data7 && data7.length ? data7.map(d => d.label) : [];
    const values = data7 && data7.length ? data7.map(d => d.value) : [];


    const ctx = canvas.getContext('2d');

    // Gradiente premium
    const gradient = ctx.createLinearGradient(0, 0, 0, 200);
    gradient.addColorStop(0, 'rgba(16, 185, 129, 0.28)');
    gradient.addColorStop(1, 'rgba(16, 185, 129, 0.01)');

    new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [{
          label: 'Ventas',
          data: values,
          borderColor: '#10B981',
          borderWidth: 3,
          backgroundColor: gradient,
          tension: 0.35,
          fill: true,
          pointBackgroundColor: '#FFFFFF',
          pointBorderColor: '#10B981',
          pointBorderWidth: 2,
          pointRadius: 4,
          pointHoverRadius: 6,
          pointHoverBackgroundColor: '#10B981',
          pointHoverBorderColor: '#FFFFFF',
          pointHoverBorderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1E293B',
            titleColor: '#FFFFFF',
            bodyColor: '#E2E8F0',
            titleFont: { size: 12, weight: 'bold', family: "'Inter', sans-serif" },
            bodyFont: { size: 12, family: "'Inter', sans-serif" },
            padding: 10,
            borderRadius: 8,
            displayColors: false,
            callbacks: {
              label: function(context) {
                return ` Ventas: ${context.raw}`;
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#64748B', font: { size: 11, family: "'Inter', sans-serif" } }
          },
          y: {
            grid: { color: 'rgba(226, 232, 240, 0.6)', borderDash: [5, 5] },
            ticks: { color: '#64748B', font: { size: 11, family: "'Inter', sans-serif" }, precision: 0 }
          }
        }
      }
    });
  }

  function drawBarChart(categorias) {
    const canvas = document.getElementById('ventasPorCategoriaChart');
    if (!canvas) return;

    // Si no hay datos, no dibujamos con valores inventados.
    if (!categorias || !Array.isArray(categorias) || categorias.length === 0) return;


    const labels = categorias && categorias.length ? categorias.map(c => c.label) : [];
    const values = categorias && categorias.length ? categorias.map(c => c.value) : [];


    const ctx = canvas.getContext('2d');

    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          data: values,
          backgroundColor: [
            'rgba(59, 130, 246, 0.82)', 
            'rgba(16, 185, 129, 0.82)', 
            'rgba(245, 158, 11, 0.82)', 
            'rgba(139, 92, 246, 0.82)'  
          ],
          hoverBackgroundColor: [
            '#3B82F6',
            '#10B981',
            '#F59E0B',
            '#8B5CF6'
          ],
          borderRadius: 8,
          borderSkipped: false,
          maxBarThickness: 28
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1E293B',
            titleColor: '#FFFFFF',
            bodyColor: '#E2E8F0',
            titleFont: { size: 12, weight: 'bold', family: "'Inter', sans-serif" },
            bodyFont: { size: 12, family: "'Inter', sans-serif" },
            padding: 10,
            borderRadius: 8,
            displayColors: false,
            callbacks: {
              label: function(context) {
                return ` Ventas: ${context.raw}`;
              }
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: '#64748B', font: { size: 11, family: "'Inter', sans-serif" } }
          },
          y: {
            grid: { color: 'rgba(226, 232, 240, 0.6)', borderDash: [5, 5] },
            ticks: { color: '#64748B', font: { size: 11, family: "'Inter', sans-serif" }, precision: 0 }
          }
        }
      }
    });
  }

  async function boot() {
    const data = await fetchMetrics();
    if (data) {
      updateStats(data);
      drawLineChart(data.ventas7dias);
      drawBarChart(data.categorias);

    } else {
      drawLineChart([]);
      drawBarChart([]);
    }
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
