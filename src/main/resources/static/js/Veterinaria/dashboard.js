document.addEventListener("DOMContentLoaded", () => {
    cargarDashboard();
    cargarPacientesDemo();
});

function cargarDashboard() {
    // Datos demo mientras conectas backend
    document.getElementById("atendidosHoy").textContent = 24;
    document.getElementById("pendientes").textContent = 8;
    document.getElementById("criticos").textContent = 3;
    document.getElementById("mejoria").textContent = 18;
}

function cargarPacientesDemo() {
    const pacientes = [
        {
            id: 1,
            nombre: "Luna",
            especie: "Perro",
            estado: "Observación",
            colorEstado: "warning",
            edad: "3 años",
            peso: "12.3 kg",
            sexo: "Hembra",
            emoji: "🐶"
        },
        {
            id: 2,
            nombre: "Max",
            especie: "Gato",
            estado: "Crítico",
            colorEstado: "danger",
            edad: "5 años",
            peso: "4.8 kg",
            sexo: "Macho",
            emoji: "🐱"
        },
        {
            id: 3,
            nombre: "Kiwi",
            especie: "Ave",
            estado: "Estable",
            colorEstado: "normal",
            edad: "1 año",
            peso: "0.4 kg",
            sexo: "Macho",
            emoji: "🐦"
        },
        {
            id: 4,
            nombre: "Rex",
            especie: "Reptil",
            estado: "Pendiente",
            colorEstado: "warning",
            edad: "2 años",
            peso: "2.1 kg",
            sexo: "Hembra",
            emoji: "🦎"
        }
    ];

    const lista = document.getElementById("listaPacientes");
    lista.innerHTML = "";

    pacientes.forEach((p, index) => {
        const item = document.createElement("div");
        item.className = "patient-item" + (index === 0 ? " active" : "");
        item.innerHTML = `
            <div class="patient-info">
                <strong>${p.emoji} ${p.nombre} <span class="dot">●</span></strong>
                <p>${p.estado}</p>
            </div>
            <span class="tag ${p.colorEstado}">${p.colorEstado === "danger" ? "Alta" : p.colorEstado === "warning" ? "Media" : "Baja"}</span>
        `;

        item.addEventListener("click", () => mostrarFichaPaciente(p));
        lista.appendChild(item);
    });

    mostrarFichaPaciente(pacientes[0]);

    const alertas = [
        { mascota: "Max - Gato", detalle: "Deshidratación severa" },
        { mascota: "Bella - Perro", detalle: "Control post-cirugía" }
    ];

    const alertasBox = document.getElementById("alertasCriticas");
    alertasBox.innerHTML = "";

    alertas.forEach(a => {
        const div = document.createElement("div");
        div.className = "small-item";
        div.innerHTML = `
            <div>
                <strong>${a.mascota}</strong>
                <p>${a.detalle}</p>
            </div>
        `;
        alertasBox.appendChild(div);
    });
}

function mostrarFichaPaciente(p) {
    document.getElementById("pacienteSeleccionadoTitulo").textContent = p.nombre;

    const ficha = document.getElementById("fichaPaciente");
    ficha.innerHTML = `
        <div class="profile-head">
            <div class="avatar">${p.emoji}</div>
            <div class="profile-meta">
                <h3>${p.nombre}</h3>
                <span>${p.especie}</span>
            </div>
        </div>

        <div class="profile-grid">
            <div class="profile-box">
                <label>Edad</label>
                <strong>${p.edad}</strong>
            </div>
            <div class="profile-box">
                <label>Peso</label>
                <strong>${p.peso}</strong>
            </div>
            <div class="profile-box">
                <label>Sexo</label>
                <strong>${p.sexo}</strong>
            </div>
        </div>
    `;
}