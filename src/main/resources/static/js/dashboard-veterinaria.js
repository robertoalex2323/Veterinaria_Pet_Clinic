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
                <strong>${escapeHtml(p.peso || "No registrado")}</strong>
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
                        <strong>${escapeHtml(p.temperatura || "-")}</strong>
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
