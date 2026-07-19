document.addEventListener('DOMContentLoaded', function () {

    var DIAGNOSTIC_ENDPOINT = 'http://localhost:5000/diagnostico';

    const form = document.getElementById('iaDiagnosticoForm');
    if (!form) return;


    const mascotaSelect = document.getElementById('mascotaRegistrada');
    if (mascotaSelect) {
        mascotaSelect.addEventListener('change', function () {
            const option = mascotaSelect.options[mascotaSelect.selectedIndex];
            const especie = option ? option.getAttribute('data-especie') : '';
            const edad = option ? option.getAttribute('data-edad') : '';

            const especieSelect = document.getElementById('especie');
            const edadInput = document.getElementById('edad');

            if (especie && especieSelect) {
                especieSelect.value = especie;
            }
            if (edad && edadInput) {
                edadInput.value = edad;
            }
        });
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();

        const especie = document.getElementById('especie').value;
        const edad = document.getElementById('edad').value;
        const temperatura = document.getElementById('temperatura').value;
        const sintomas = document.getElementById('sintomas').value.trim();

        if (!sintomas || !especie) return;

        mostrarEstado('cargando');

        const btn = document.getElementById('btnAnalizar');
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Procesando...';

        fetch(DIAGNOSTIC_ENDPOINT, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                especie: especie,
                edad: edad,
                temperatura: temperatura,
                sintomas: sintomas
            })
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('El servicio de IA respondio con error.');
                }
                return response.json();
            })
            .then(function (data) {
                pintarResultado(data);
                mostrarEstado('resultados');
            })
            .catch(function () {
                mostrarError();
            })
            .finally(function () {
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-microchip me-2"></i> Analizar con IA';
            });
    });
});

// Funcion para reiniciar el formulario
window.resetIA = function () {
    document.getElementById('iaDiagnosticoForm').reset();
    mostrarEstado('inicial');
}

function mostrarEstado(estado) {
    const inicial = document.getElementById('estadoInicial');
    const cargando = document.getElementById('estadoCargando');
    const resultados = document.getElementById('estadoResultados');
    const error = document.getElementById('estadoError');

    [inicial, cargando, resultados, error].forEach(function (el) {
        if (el) el.classList.add('d-none');
    });
    [cargando, error].forEach(function (el) {
        if (el) el.classList.remove('d-flex');
    });

    if (estado === 'inicial' && inicial) {
        inicial.classList.remove('d-none');
        inicial.classList.add('d-flex');
    } else if (estado === 'cargando' && cargando) {
        cargando.classList.remove('d-none');
        cargando.classList.add('d-flex');
    } else if (estado === 'resultados' && resultados) {
        resultados.classList.remove('d-none');
    } else if (estado === 'error' && error) {
        error.classList.remove('d-none');
        error.classList.add('d-flex');
    }
}

function mostrarError() {
    // No inventamos un diagnostico: avisamos que el servicio Python no respondio.
    const error = document.getElementById('estadoError');
    if (error) {
        mostrarEstado('error');
        return;
    }

    // Respaldo si la plantilla aun no tiene el bloque #estadoError:
    // reutilizamos el panel de resultados solo para mostrar el aviso.
    document.getElementById('diagnosticoTitulo').textContent = 'No se pudo conectar con el servicio de IA';
    document.getElementById('confianzaPorcentaje').textContent = '--';
    const circulo = document.getElementById('confianzaCirculo');
    circulo.classList.remove('p-high', 'p-medium', 'p-low');
    document.getElementById('justificacionTexto').textContent =
        'No se logro contactar el microservicio Python (chatbot_service). Verifica que este ' +
        'encendido en http://localhost:5000 e intenta nuevamente.';
    document.getElementById('recomendacionesLista').innerHTML =
        '<li class="list-group-item bg-transparent px-0 text-muted">' +
        '<i class="fas fa-angle-right text-danger me-2"></i> Ejecuta "py -3 app.py" en la carpeta chatbot_service y vuelve a intentar.</li>';
    mostrarEstado('resultados');
}

// Pinta en el DOM la respuesta del endpoint /diagnostico
function pintarResultado(data) {
    data = data || {};

    const titulo = data.titulo || 'Patologia indeterminada';
    const confianza = Math.max(0, Math.min(100, Math.round(Number(data.confianza) || 60)));
    const justificacion = data.justificacion ||
        'El sistema requiere una evaluacion clinica presencial para descartar opciones.';
    const recomendaciones = (Array.isArray(data.recomendaciones) && data.recomendaciones.length)
        ? data.recomendaciones
        : ['Agendar cita de revision general.'];

    document.getElementById('diagnosticoTitulo').textContent = titulo;
    document.getElementById('confianzaPorcentaje').textContent = confianza + '%';

    const circulo = document.getElementById('confianzaCirculo');
    circulo.classList.remove('p-high', 'p-medium', 'p-low');
    if (confianza >= 85) circulo.classList.add('p-high');
    else if (confianza >= 70) circulo.classList.add('p-medium');
    else circulo.classList.add('p-low');

    document.getElementById('justificacionTexto').textContent = justificacion;

    const ul = document.getElementById('recomendacionesLista');
    ul.innerHTML = '';
    recomendaciones.forEach(function (rec) {
        const li = document.createElement('li');
        li.className = 'list-group-item bg-transparent px-0 border-bottom-dashed text-muted';
        li.innerHTML = '<i class="fas fa-angle-right text-info me-2"></i> ' + rec;
        ul.appendChild(li);
    });
}