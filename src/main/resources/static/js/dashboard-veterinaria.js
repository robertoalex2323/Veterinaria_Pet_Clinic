const VET_THEME_STORAGE_KEY = "vetDashboardTheme";
const VET_CLINICAL_SETTINGS_STORAGE_KEY = "vetClinicalSettings";

(function initVeterinaryTheme() {
    const defaults = { mode: "light", color: "lavender", text: "medium" };

    function readTheme() {
        try {
            return { ...defaults, ...JSON.parse(localStorage.getItem(VET_THEME_STORAGE_KEY) || "{}") };
        } catch (error) {
            return { ...defaults };
        }
    }

    function resolveMode(mode) {
        if (mode !== "auto") return mode;
        return window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }

    function applyTheme(theme) {
        const next = { ...defaults, ...theme };
        const resolvedMode = resolveMode(next.mode);
        document.documentElement.dataset.vetThemeMode = resolvedMode;
        document.documentElement.dataset.vetThemeChoice = next.mode;
        document.documentElement.dataset.vetThemeColor = next.color;
        document.documentElement.dataset.vetTextSize = next.text;
    }

    window.vetThemeSettings = {
        get: readTheme,
        set(partial) {
            const next = { ...readTheme(), ...partial };
            localStorage.setItem(VET_THEME_STORAGE_KEY, JSON.stringify(next));
            applyTheme(next);
            return next;
        }
    };

    applyTheme(readTheme());

    const media = window.matchMedia("(prefers-color-scheme: dark)");
    if (media.addEventListener) {
        media.addEventListener("change", () => {
            const current = readTheme();
            if (current.mode === "auto") applyTheme(current);
        });
    }
})();

(function initClinicalPreferences() {
    const defaults = { weightUnit: "kg", temperatureUnit: "celsius", defaultPriority: "media", language: "es", region: "PE" };
    const translations = {
        "Configuracion": "Settings", "Preferencias Clinicas": "Clinical Preferences", "Unidad de peso": "Weight unit",
        "Temperatura": "Temperature", "Prioridad por defecto": "Default priority", "Idioma": "Language",
        "Idioma del sistema": "System language", "Region": "Region", "Kilogramos": "Kilograms", "Libras": "Pounds",
        "Media": "Medium", "Alta": "High", "Baja": "Low", "Historial Clinico": "Clinical History",
        "Reportes Clinicos": "Clinical Reports", "Peso": "Weight", "Signos Vitales Actuales": "Current Vital Signs",
        "Consultas Anteriores": "Previous Consultations", "Editar": "Edit", "Guardar signos": "Save vital signs",
        "Cancelar": "Cancel", "Seleccionar Paciente": "Select Patient", "Vista Previa": "Preview",
        "Generar Reporte": "Generate Report", "Datos del Paciente": "Patient Information", "Paciente": "Patient",
        "Dueno": "Owner", "Fecha": "Date", "No registrado": "Not registered", "Registrado": "Registered"
    };

    function read() {
        try { return { ...defaults, ...JSON.parse(localStorage.getItem(VET_CLINICAL_SETTINGS_STORAGE_KEY) || "{}") }; }
        catch (error) { return { ...defaults }; }
    }
    function formatNumber(value, digits) {
        return new Intl.NumberFormat(read().language === "en" ? "en-US" : "es-PE", { maximumFractionDigits: digits }).format(value);
    }
    function formatWeight(value) {
        const number = Number(value);
        if (!Number.isFinite(number)) return "-";
        const usePounds = read().weightUnit === "lb";
        return `${formatNumber(usePounds ? number * 2.2046226218 : number, 2)} ${usePounds ? "lb" : "kg"}`;
    }
    function formatTemperature(value) {
        const number = Number(value);
        if (!Number.isFinite(number)) return "-";
        const fahrenheit = read().temperatureUnit === "fahrenheit";
        return `${formatNumber(fahrenheit ? (number * 9 / 5) + 32 : number, 1)} °${fahrenheit ? "F" : "C"}`;
    }
    function translateText(text) {
        return read().language === "en" && translations[text.trim()] ? text.replace(text.trim(), translations[text.trim()]) : text;
    }
    function applyToPage() {
        const settings = read();
        document.documentElement.lang = settings.language;
        const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
            acceptNode(node) {
                const parent = node.parentElement;
                return parent && !["SCRIPT", "STYLE", "TEXTAREA", "OPTION"].includes(parent.tagName) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
            }
        });
        const nodes = [];
        while (walker.nextNode()) nodes.push(walker.currentNode);
        nodes.forEach(node => {
            let text = node.nodeValue;
            text = text.replace(/(-?\d+(?:[.,]\d+)?)\s*kg\b/gi, (_, value) => formatWeight(String(value).replace(",", ".")));
            text = text.replace(/(-?\d+(?:[.,]\d+)?)\s*°?C\b/gi, (_, value) => formatTemperature(String(value).replace(",", ".")));
            node.nodeValue = translateText(text);
        });
    }
    window.vetClinicalSettings = {
        get: read,
        set(partial) {
            const next = { ...read(), ...partial };
            localStorage.setItem(VET_CLINICAL_SETTINGS_STORAGE_KEY, JSON.stringify(next));
            window.dispatchEvent(new CustomEvent("vetClinicalSettingsChanged", { detail: next }));
            return next;
        },
        formatWeight,
        formatTemperature,
        applyToPage
    };
    document.addEventListener("DOMContentLoaded", applyToPage);
    window.addEventListener("vetClinicalSettingsChanged", () => window.location.reload());
})();

document.addEventListener("DOMContentLoaded", () => {
    const lista = document.getElementById("listaPacientes");
    const pacientes = Array.from(document.querySelectorAll(".patient-item"));
    const buscador = document.getElementById("buscarPacienteDashboard");
    const chips = Array.from(document.querySelectorAll(".chip[data-especie]"));

    if (!lista || pacientes.length === 0) {
        return;
    }

    pacientes.forEach(item => {
        item.addEventListener("click", () => seleccionarPaciente(item));
    });

    if (buscador) {
        buscador.addEventListener("input", aplicarFiltros);
    }

    chips.forEach(chip => {
        chip.addEventListener("click", () => {
            chips.forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            aplicarFiltros();
        });
    });

    seleccionarPaciente(pacientes[0]);
});

function aplicarFiltros() {
    const q = (document.getElementById("buscarPacienteDashboard")?.value || "").toLowerCase().trim();
    const especie = document.querySelector(".chip.active[data-especie]")?.dataset.especie || "todas";
    const pacientes = Array.from(document.querySelectorAll(".patient-item"));

    pacientes.forEach(item => {
        const nombre = (item.dataset.nombre || "").toLowerCase();
        const especiePaciente = (item.dataset.especie || "").toLowerCase();
        const coincideNombre = !q || nombre.includes(q);
        const coincideEspecie = especie === "todas" || especiePaciente.includes(especie);
        item.style.display = coincideNombre && coincideEspecie ? "" : "none";
    });

    const activoVisible = pacientes.some(item => item.classList.contains("active") && item.style.display !== "none");
    if (!activoVisible) {
        const primerVisible = pacientes.find(item => item.style.display !== "none");
        if (primerVisible) {
            seleccionarPaciente(primerVisible);
        }
    }
}

function seleccionarPaciente(item) {
    document.querySelectorAll(".patient-item").forEach(p => p.classList.remove("active"));
    item.classList.add("active");

    const data = item.dataset;
    document.getElementById("pacienteSeleccionadoTitulo").textContent = data.nombre || "-";

    const btnHistorial = document.getElementById("btnHistorialPaciente");
    if (btnHistorial) {
        btnHistorial.href = data.historialUrl || "/veterinaria/historial";
    }

    const inputMascota = document.getElementById("mascotaSolicitudId");
    if (inputMascota) {
        inputMascota.value = data.id || "";
    }

    const inputNombrePaciente = document.getElementById("pacienteSolicitudNombre");
    if (inputNombrePaciente) {
        inputNombrePaciente.value = data.nombre || "";
    }

    mostrarFichaPaciente(data);
}

function mostrarFichaPaciente(p) {
    const ficha = document.getElementById("fichaPaciente");
    if (!ficha) return;

    const iconClass = iconoPorEspecie(p.especie);
    const tieneTriaje = Boolean(p.temperatura || p.fc || p.fr || p.fechaTriaje || p.observaciones);
    const motivoCita = p.citaMotivo ? `
        <div class="triage-note">
            <label>Motivo de cita</label>
            <p>${escapeHtml(p.citaMotivo)}</p>
        </div>
    ` : "";
    const observaciones = p.observaciones ? `
        <div class="triage-note">
            <label>Observaciones</label>
            <p>${escapeHtml(p.observaciones)}</p>
        </div>
    ` : "";

    ficha.innerHTML = `
        <div class="profile-head">
            <div class="avatar"><i class="${iconClass}"></i></div>
            <div class="profile-meta">
                <h3>${escapeHtml(p.nombre || "Paciente")}</h3>
                <span>${escapeHtml(p.especie || "Especie no registrada")}</span>
            </div>
        </div>

        <div class="profile-grid">
            <div class="profile-box">
                <label>Edad</label>
                <strong>${escapeHtml(p.edad || "No registrada")}</strong>
            </div>
            <div class="profile-box">
                <label>Peso</label>
                <strong>${escapeHtml(formatWeightFromText(p.peso) || "No registrado")}</strong>
            </div>
            <div class="profile-box">
                <label>Dueño</label>
                <strong>${escapeHtml(p.dueno || "Sin dueño")}</strong>
            </div>
            <div class="profile-box">
                <label>Raza</label>
                <strong>${escapeHtml(p.raza || "No registrada")}</strong>
            </div>
            <div class="profile-box">
                <label>Color</label>
                <strong>${escapeHtml(p.color || "No registrado")}</strong>
            </div>
            <div class="profile-box">
                <label>Estado</label>
                <strong>${escapeHtml(p.estado || "Registrado")}</strong>
            </div>
        </div>

        ${tieneTriaje ? `
            <div class="triage-summary">
                <div class="triage-title">
                    <i class="fas fa-stethoscope"></i>
                    <span>Informacion del triaje</span>
                </div>
                <div class="profile-grid">
                    <div class="profile-box">
                        <label>Temperatura</label>
                        <strong>${escapeHtml(formatTemperatureFromText(p.temperatura) || "-")}</strong>
                    </div>
                    <div class="profile-box">
                        <label>FC</label>
                        <strong>${escapeHtml(p.fc || "-")}</strong>
                    </div>
                    <div class="profile-box">
                        <label>FR</label>
                        <strong>${escapeHtml(p.fr || "-")}</strong>
                    </div>
                </div>
                <div class="triage-meta">${escapeHtml(p.fechaTriaje ? "Registrado: " + p.fechaTriaje : "Triaje registrado")}</div>
                ${motivoCita}
                ${observaciones}
            </div>
        ` : `
            <div class="triage-summary triage-empty">
                <div class="triage-title">
                    <i class="fas fa-clock"></i>
                    <span>Sin triaje registrado</span>
                </div>
                <p>Esta mascota ya fue registrada desde recepcion. La informacion de triaje aparecera aqui cuando se inicie desde Pacientes.</p>
            </div>
        `}
    `;
}

function formatWeightFromText(value) {
    const number = parseFloat(String(value || "").replace(",", "."));
    return Number.isFinite(number) ? window.vetClinicalSettings.formatWeight(number) : value;
}

function formatTemperatureFromText(value) {
    const number = parseFloat(String(value || "").replace(",", "."));
    return Number.isFinite(number) ? window.vetClinicalSettings.formatTemperature(number) : value;
}

function iconoPorEspecie(especie) {
    const valor = (especie || "").toLowerCase();
    if (valor.includes("perro")) return "fas fa-dog";
    if (valor.includes("gato")) return "fas fa-cat";
    if (valor.includes("ave")) return "fas fa-dove";
    if (valor.includes("reptil")) return "fas fa-dragon";
    return "fas fa-paw";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
