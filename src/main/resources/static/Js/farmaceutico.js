// ===========================
// DATOS SIMULADOS
// ===========================
let prescriptions = [
    { id: "RX001", code: "VET-2024-001", patient: "Max (Perro)", vet: "Dr. Meza", date: "2024-01-15", status: "Pendiente", medicines: ["Amoxicilina", "Meloxicam"] },
    { id: "RX002", code: "VET-2024-002", patient: "Luna (Gato)", vet: "Dra. Llacsahuanga ", date: "2024-01-14", status: "Pendiente", medicines: ["Doxiciclina"] },
    { id: "RX003", code: "VET-2024-003", patient: "Rocky (Perro)", vet: "Dr. Anton", date: "2024-01-13", status: "Verificada", medicines: ["Omeprazol"] }
];

let inventory = [
    { id: 1, name: "Amoxicilina", presentation: "Tabletas 500mg", stock: 150, price: 15.50, description: "Antibiótico de amplio espectro. Indicado para infecciones bacterianas." },
    { id: 2, name: "Meloxicam", presentation: "Suspensión oral", stock: 45, price: 22.00, description: "Antiinflamatorio no esteroideo para dolor y fiebre." },
    { id: 3, name: "Doxiciclina", presentation: "Cápsulas 100mg", stock: 8, price: 18.75, description: "Antibiótico para infecciones respiratorias y dermatológicas." },
    { id: 4, name: "Omeprazol", presentation: "Tabletas 20mg", stock: 120, price: 12.30, description: "Protector gastrointestinal, reduce acidez estomacal." },
    { id: 5, name: "Fipronil", presentation: "Solución tópica", stock: 3, price: 35.00, description: "Antipulgas y garrapatas de acción prolongada." }
];

let sales = [
    { date: "2024-01-15", medicine: "Amoxicilina", quantity: 2, total: 31.00, client: "María González", payment: "Efectivo" }
];

let dispensations = [];

// ===========================
// FUNCIONES DE UTILIDAD
// ===========================
function showToast(message, isError = false) {
    const toast = document.getElementById('toast');
    const toastMessage = document.getElementById('toastMessage');
    toastMessage.textContent = message;
    toast.style.backgroundColor = isError ? '#DC2626' : '#1D9E75';
    toast.style.display = 'flex';
    setTimeout(() => {
        toast.style.display = 'none';
        toast.style.backgroundColor = '#1D9E75';
    }, 3000);
}

function updateStats() {
    document.getElementById('prescriptionsCount').textContent = prescriptions.filter(p => p.status === 'Pendiente').length;
    document.getElementById('dispensedCount').textContent = dispensations.length;
    
    const todaySales = sales.filter(s => s.date === new Date().toISOString().split('T')[0]);
    const totalSales = todaySales.reduce((sum, s) => sum + s.total, 0);
    document.getElementById('salesCount').textContent = `$${totalSales.toFixed(2)}`;
    
    const lowStock = inventory.filter(m => m.stock < 10).length;
    document.getElementById('lowStockCount').textContent = lowStock;
}

function renderPrescriptions() {
    const tbody = document.getElementById('prescriptionsList');
    tbody.innerHTML = prescriptions.map(p => `
        <tr>
            <td>${p.code}</td>
            <td>${p.patient}</td>
            <td>${p.vet}</td>
            <td>${p.date}</td>
            <td><span class="badge ${p.status === 'Verificada' ? 'badge-success' : 'badge-warning'}">${p.status}</span></td>
            <td>
                ${p.status === 'Pendiente' ? `
                    <button class="action-btn" onclick="verifyPrescriptionById('${p.id}')" title="Verificar">
                        <i class="fa-solid fa-check-circle"></i>
                    </button>
                ` : '<span class="badge badge-success">Verificada</span>'}
            </td>
        </tr>
    `).join('');
}

function renderInventory() {
    const searchTerm = document.getElementById('inventorySearch')?.value.toLowerCase() || '';
    const filtered = inventory.filter(m => m.name.toLowerCase().includes(searchTerm));
    const tbody = document.getElementById('inventoryList');
    tbody.innerHTML = filtered.map(m => `
        <tr class="${m.stock < 10 ? 'inventory-low' : ''}">
            <td>${m.id}</td>
            <td><strong>${m.name}</strong></td>
            <td>${m.presentation}</td>
            <td><span class="badge ${m.stock < 10 ? 'badge-danger' : 'badge-success'}">${m.stock} unidades</span></td>
            <td>$${m.price.toFixed(2)}</td>
            <td>${m.stock < 10 ? '<span class="badge badge-danger">Stock Bajo</span>' : '<span class="badge badge-success">Disponible</span>'}</td>
            <td>
                <button class="action-btn" onclick="editStock(${m.id})" title="Editar stock">
                    <i class="fa-solid fa-pen"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function renderSalesHistory() {
    const tbody = document.getElementById('salesHistory');
    tbody.innerHTML = sales.slice().reverse().map(s => `
        <tr>
            <td>${s.date}</td>
            <td>${s.medicine}</td>
            <td>${s.quantity}</td>
            <td>$${s.total.toFixed(2)}</td>
            <td>${s.client}</td>
        </tr>
    `).join('');
}

function renderMedicinesInfo() {
    const searchTerm = document.getElementById('infoSearch')?.value.toLowerCase() || '';
    const filtered = inventory.filter(m => m.name.toLowerCase().includes(searchTerm));
    const container = document.getElementById('medicinesInfo');
    container.innerHTML = filtered.map(m => `
        <div style="padding: 1rem; border-bottom: 1px solid var(--border-default);">
            <h3 style="color: var(--green-main); margin-bottom: 0.5rem;">${m.name}</h3>
            <p><strong>Presentación:</strong> ${m.presentation}</p>
            <p><strong>Precio:</strong> $${m.price.toFixed(2)}</p>
            <p><strong>Stock disponible:</strong> ${m.stock} unidades</p>
            <p><strong>Descripción:</strong> ${m.description}</p>
        </div>
    `).join('');
    if (filtered.length === 0) container.innerHTML = '<p style="text-align: center; padding: 2rem;">No se encontraron medicamentos</p>';
}

function populateMedicineSelects() {
    const options = inventory.map(m => `<option value="${m.id}">${m.name} - $${m.price.toFixed(2)} (Stock: ${m.stock})</option>`).join('');
    document.getElementById('dispenseMedicine').innerHTML = '<option value="">Seleccione medicamento...</option>' + options;
    document.getElementById('saleMedicine').innerHTML = '<option value="">Seleccione medicamento...</option>' + options;
    
    const pendingPrescriptions = prescriptions.filter(p => p.status === 'Verificada');
    const prescOptions = pendingPrescriptions.map(p => `<option value="${p.id}">${p.code} - ${p.patient}</option>`).join('');
    document.getElementById('dispensePrescription').innerHTML = '<option value="">Seleccione una receta...</option>' + prescOptions;
}

// ===========================
// FUNCIONES PRINCIPALES
// ===========================
function verifyPrescription() {
    const code = document.getElementById('prescriptionCode').value;
    const resultDiv = document.getElementById('prescriptionResult');
    
    if (!code) {
        showToast('Ingrese un código de receta', true);
        return;
    }
    
    const prescription = prescriptions.find(p => p.code === code);
    if (prescription) {
        resultDiv.innerHTML = `
            <div style="background: var(--success-bg); padding: 1rem; border-radius: var(--radius-sm); border-left: 4px solid var(--green-main);">
                <strong>✓ Receta Encontrada</strong><br>
                Paciente: ${prescription.patient}<br>
                Veterinario: ${prescription.vet}<br>
                Fecha: ${prescription.date}<br>
                Medicamentos: ${prescription.medicines.join(', ')}<br>
                Estado actual: ${prescription.status}
            </div>
        `;
    } else {
        resultDiv.innerHTML = `
            <div style="background: var(--red-bg); padding: 1rem; border-radius: var(--radius-sm); border-left: 4px solid var(--red-text);">
                <strong>✗ Receta no encontrada</strong><br>
                Verifique el código ingresado.
            </div>
        `;
    }
}

function verifyPrescriptionById(id) {
    const prescription = prescriptions.find(p => p.id === id);
    if (prescription && prescription.status === 'Pendiente') {
        prescription.status = 'Verificada';
        renderPrescriptions();
        populateMedicineSelects();
        updateStats();
        showToast(`Receta ${prescription.code} verificada correctamente`);
    } else if (prescription.status === 'Verificada') {
        showToast('Esta receta ya fue verificada', true);
    }
}

function dispenseMedicine() {
    const prescriptionId = document.getElementById('dispensePrescription').value;
    const medicineId = document.getElementById('dispenseMedicine').value;
    const quantity = parseInt(document.getElementById('dispenseQuantity').value);
    const instructions = document.getElementById('dispenseInstructions').value;
    
    if (!prescriptionId || !medicineId || !quantity) {
        showToast('Complete todos los campos', true);
        return;
    }
    
    const prescription = prescriptions.find(p => p.id === prescriptionId);
    const medicine = inventory.find(m => m.id == medicineId);
    
    if (!prescription || !medicine) {
        showToast('Datos inválidos', true);
        return;
    }
    
    if (medicine.stock < quantity) {
        showToast(`Stock insuficiente de ${medicine.name}. Disponible: ${medicine.stock}`, true);
        return;
    }
    
    medicine.stock -= quantity;
    
    dispensations.push({
        id: Date.now(),
        prescriptionId,
        medicineId,
        medicineName: medicine.name,
        quantity,
        instructions,
        date: new Date().toISOString().split('T')[0]
    });
    
    showToast(`Dispensado: ${quantity} ${medicine.name} para ${prescription.patient}`);
    
    document.getElementById('dispenseQuantity').value = '';
    document.getElementById('dispenseInstructions').value = '';
    
    renderInventory();
    populateMedicineSelects();
    updateStats();
}

function updateTotalAmount() {
    const medicineId = document.getElementById('saleMedicine').value;
    const quantity = parseInt(document.getElementById('saleQuantity').value) || 0;
    const medicine = inventory.find(m => m.id == medicineId);
    if (medicine && quantity > 0) {
        document.getElementById('salePrice').value = `$${medicine.price.toFixed(2)}`;
        document.getElementById('totalAmount').textContent = `$${(medicine.price * quantity).toFixed(2)}`;
    } else {
        document.getElementById('salePrice').value = '';
        document.getElementById('totalAmount').textContent = '$0';
    }
}

function registerSale() {
    const medicineId = document.getElementById('saleMedicine').value;
    const quantity = parseInt(document.getElementById('saleQuantity').value);
    const client = document.getElementById('saleClient').value;
    const payment = document.getElementById('salePayment').value;
    
    if (!medicineId || !quantity || !client) {
        showToast('Complete todos los campos', true);
        return;
    }
    
    const medicine = inventory.find(m => m.id == medicineId);
    if (!medicine) {
        showToast('Medicamento no encontrado', true);
        return;
    }
    
    if (medicine.stock < quantity) {
        showToast(`Stock insuficiente. Disponible: ${medicine.stock}`, true);
        return;
    }
    
    const total = medicine.price * quantity;
    
    medicine.stock -= quantity;
    
    sales.push({
        date: new Date().toISOString().split('T')[0],
        medicine: medicine.name,
        quantity,
        total,
        client,
        payment
    });
    
    showToast(`Venta registrada: $${total.toFixed(2)}`);
    
    document.getElementById('saleQuantity').value = '';
    document.getElementById('saleClient').value = '';
    document.getElementById('saleMedicine').value = '';
    document.getElementById('salePrice').value = '';
    document.getElementById('totalAmount').textContent = '$0';
    
    renderInventory();
    renderSalesHistory();
    populateMedicineSelects();
    updateStats();
}

function editStock(id) {
    const newStock = prompt('Ingrese el nuevo stock:');
    if (newStock !== null && !isNaN(newStock) && parseInt(newStock) >= 0) {
        const medicine = inventory.find(m => m.id === id);
        if (medicine) {
            medicine.stock = parseInt(newStock);
            renderInventory();
            populateMedicineSelects();
            updateStats();
            showToast(`Stock de ${medicine.name} actualizado a ${newStock}`);
        }
    }
}

function openAddMedicineModal() {
    document.getElementById('addMedicineModal').classList.add('active');
}

function closeModal() {
    document.getElementById('addMedicineModal').classList.remove('active');
    document.getElementById('newMedName').value = '';
    document.getElementById('newMedPresentation').value = '';
    document.getElementById('newMedStock').value = '';
    document.getElementById('newMedPrice').value = '';
    document.getElementById('newMedDescription').value = '';
}

function addMedicine() {
    const name = document.getElementById('newMedName').value;
    const presentation = document.getElementById('newMedPresentation').value;
    const stock = parseInt(document.getElementById('newMedStock').value);
    const price = parseFloat(document.getElementById('newMedPrice').value);
    const description = document.getElementById('newMedDescription').value;
    
    if (!name || !presentation || isNaN(stock) || isNaN(price)) {
        showToast('Complete todos los campos correctamente', true);
        return;
    }
    
    const newId = inventory.length > 0 ? Math.max(...inventory.map(m => m.id)) + 1 : 1;
    inventory.push({
        id: newId,
        name,
        presentation,
        stock,
        price,
        description: description || 'Sin descripción'
    });
    
    closeModal();
    renderInventory();
    populateMedicineSelects();
    updateStats();
    renderMedicinesInfo();
    showToast(`Medicamento ${name} agregado correctamente`);
}

// ===========================
// NAVEGACIÓN ENTRE PANELES
// ===========================
function switchPanel(panelId) {
    const panels = ['verifyPanel', 'dispensePanel', 'salesPanel', 'inventoryPanel', 'infoPanel'];
    panels.forEach(panel => {
        const element = document.getElementById(panel);
        if (element) element.style.display = 'none';
    });
    document.getElementById(panelId).style.display = 'block';
    
    // Actualizar estado activo del menú
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
}

// Event listeners para navegación
document.addEventListener('DOMContentLoaded', function() {
    // Mostrar fecha actual
    const dateDisplay = document.getElementById('currentDate');
    if (dateDisplay) {
        const today = new Date();
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        dateDisplay.textContent = today.toLocaleDateString('es-ES', options);
    }
    
    // Configurar navegación
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function() {
            const tab = this.getAttribute('data-tab');
            let panelId = '';
            switch(tab) {
                case 'verify': panelId = 'verifyPanel'; break;
                case 'dispense': panelId = 'dispensePanel'; break;
                case 'sales': panelId = 'salesPanel'; break;
                case 'inventory': panelId = 'inventoryPanel'; break;
                case 'info': panelId = 'infoPanel'; break;
            }
            
            navItems.forEach(nav => nav.classList.remove('active'));
            this.classList.add('active');
            
            if (panelId) {
                switchPanel(panelId);
            }
        });
    });
    
    // Inicializar datos
    renderPrescriptions();
    renderInventory();
    renderSalesHistory();
    renderMedicinesInfo();
    populateMedicineSelects();
    updateStats();
    
    // Event listener para total en ventas
    const saleMedicine = document.getElementById('saleMedicine');
    if (saleMedicine) {
        saleMedicine.addEventListener('change', updateTotalAmount);
    }
    const saleQuantity = document.getElementById('saleQuantity');
    if (saleQuantity) {
        saleQuantity.addEventListener('input', updateTotalAmount);
    }
});