document.addEventListener('DOMContentLoaded', function () {

    const form = document.getElementById('iaDiagnosticoForm');

    if (form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();

            const sintomas = document.getElementById('sintomas').value.toLowerCase();
            const especie = document.getElementById('especie').value;

            if (!sintomas || !especie) return;

            // Ocultar estado inicial, mostrar cargando
            document.getElementById('estadoInicial').classList.add('d-none');
            document.getElementById('estadoResultados').classList.add('d-none');

            const estadoCargando = document.getElementById('estadoCargando');
            estadoCargando.classList.remove('d-none');
            estadoCargando.classList.add('d-flex');

            // Deshabilitar botón
            const btn = document.getElementById('btnAnalizar');
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i> Procesando...';

            // Simulación de respuesta de un modelo de Machine Learning (Retraso de 2.5 segundos)
            setTimeout(() => {
                generarDiagnosticoMock(especie, sintomas);

                // Ocultar cargando, mostrar resultados
                estadoCargando.classList.remove('d-flex');
                estadoCargando.classList.add('d-none');

                document.getElementById('estadoResultados').classList.remove('d-none');

                // Restaurar botón
                btn.disabled = false;
                btn.innerHTML = '<i class="fas fa-microchip me-2"></i> Analizar con IA';
            }, 2500);
        });
    }
});

// Función para reiniciar el formulario
window.resetIA = function () {
    document.getElementById('iaDiagnosticoForm').reset();
    document.getElementById('estadoResultados').classList.add('d-none');

    const estadoInicial = document.getElementById('estadoInicial');
    estadoInicial.classList.remove('d-none');
    estadoInicial.classList.add('d-flex');
}

// Simulador básico de reglas de Machine Learning
function generarDiagnosticoMock(especie, sintomasText) {
    let titulo = "Patología Indeterminada";
    let porcentaje = 65;
    let justificacion = "El algoritmo requiere una evaluación clínica presencial para descartar opciones, ya que los síntomas son genéricos.";
    let recomendaciones = [
        "Agendar cita general para revisión física.",
        "Monitorear evolución en las próximas 24 horas."
    ];

    // Reglas simuladas (Keywords extraídas con "NLP")
    if (sintomasText.includes('vómito') || sintomasText.includes('diarrea') || sintomasText.includes('vomito')) {
        if (especie === 'Perro') {
            titulo = "Parvovirosis o Gastroenteritis";
            porcentaje = 88;
            justificacion = "Los síntomas gastrointestinales agudos en caninos sugieren una fuerte posibilidad de infección viral o inflamación del tracto digestivo severa.";
            recomendaciones = [
                "Agendar consulta de EMERGENCIAS inmediatamente.",
                "Solicitar prueba rápida de Parvovirus.",
                "Iniciar fluidoterapia (solo bajo indicación médica)."
            ];
        } else {
            titulo = "Gastroenteritis Felina / Obstrucción";
            porcentaje = 82;
            justificacion = "Los vómitos en gatos pueden indicar desde bolas de pelo hasta obstrucciones intestinales graves o infecciones.";
            recomendaciones = [
                "Agendar cita lo antes posible.",
                "Evitar dar comida sólida temporalmente.",
                "Posible requerimiento de ecografía abdominal."
            ];
        }
    }
    else if (sintomasText.includes('cojea') || sintomasText.includes('pata') || sintomasText.includes('hueso')) {
        titulo = "Traumatismo o Displasia";
        porcentaje = 92;
        justificacion = "Problemas de movilidad focalizados apuntan a alteraciones musculoesqueléticas articulares o fracturas fisuradas.";
        recomendaciones = [
            "Derivar a Traumatología Veterinaria.",
            "Requiere toma de Rayos X (Radiografía).",
            "Mantener al paciente en reposo absoluto."
        ];
    }
    else if (sintomasText.includes('rasca') || sintomasText.includes('piel') || sintomasText.includes('pelo') || sintomasText.includes('pulga')) {
        titulo = "Dermatitis Alérgica / Parasitaria";
        porcentaje = 95;
        justificacion = "El patrón de rascado intenso y alteraciones en la capa dérmica están estrechamente correlacionados con ectoparásitos o alergias atópicas.";
        recomendaciones = [
            "Agendar consulta con Dermatología.",
            "Revisar historial de desparasitación.",
            "Evitar baños hasta la evaluación médica."
        ];
    }
    else if (sintomasText.includes('fiebre') || sintomasText.includes('caliente') || sintomasText.includes('decaimiento')) {
        titulo = "Infección Sistémica";
        porcentaje = 75;
        justificacion = "La hipertermia acompañada de letargo indica una respuesta inmunológica activa, posiblemente originada por cuadros bacterianos o virales.";
        recomendaciones = [
            "Consulta general urgente.",
            "Realizar Hemograma Completo (análisis de sangre).",
            "Controlar temperatura cada 4 horas."
        ];
    }

    // Actualizar DOM
    document.getElementById('diagnosticoTitulo').textContent = titulo;
    document.getElementById('confianzaPorcentaje').textContent = porcentaje + "%";

    const circulo = document.getElementById('confianzaCirculo');
    circulo.className = "progress-circle p-85"; // Reset
    if (porcentaje >= 85) circulo.classList.add('p-high');
    else if (porcentaje >= 70) circulo.classList.add('p-medium');
    else circulo.classList.add('p-low');

    document.getElementById('justificacionTexto').textContent = justificacion;

    const ul = document.getElementById('recomendacionesLista');
    ul.innerHTML = '';
    recomendaciones.forEach(rec => {
        const li = document.createElement('li');
        li.className = "list-group-item bg-transparent px-0 border-bottom-dashed text-muted";
        li.innerHTML = `<i class="fas fa-angle-right text-info me-2"></i> ${rec}`;
        ul.appendChild(li);
    });
}
