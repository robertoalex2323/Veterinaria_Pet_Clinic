// ===========================
// API BASE URL
// ===========================
const API_BASE = '/api/farmaceutico';

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

async function fetchAPI(endpoint, options = {}) {

    try {

        console.log("URL:", `${API_BASE}${endpoint}`);

        const response = await fetch(`${API_BASE}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json'
            },
            ...options
        });

        console.log("STATUS:", response.status);

        const text = await response.text();

        console.log("RESPUESTA:", text);

        if (!response.ok) {
            throw new Error(text);
        }

        return text ? JSON.parse(text) : {};

    } catch (error) {

        console.error("ERROR FETCH:", error);

        showToast(error.message, true);

        throw error;
    }
}

// ===========================
// CARGAR ESTADÍSTICAS
// ===========================
async function loadStats() {
    try {
        const stats = await fetchAPI('/stats');
        document.getElementById('prescriptionsCount').textContent = stats.recetasPendientes;
        document.getElementById('dispensedCount').textContent = stats.dispensados;
        document.getElementById('salesCount').textContent = `$${stats.ventasHoy.toFixed(2)}`;
        document.getElementById('lowStockCount').textContent = stats.stockBajo;
    } catch (error) {
        console.error('Error cargando estadísticas:', error);
    }
}

// ===========================
// RECETAS PENDIENTES
// ===========================
async function loadPrescriptions() {
    try {
        const recetas = await fetchAPI('/recetas/pendientes');
        const tbody = document.getElementById('prescriptionsList');
        
        if (recetas.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">No hay recetas pendientes</td></tr>';
            return;
        }
        
        tbody.innerHTML = recetas.map(receta => `
            <tr>
                <td>${receta.id}</td>
                <td>${receta.paciente?.nombre || 'N/A'}</td>
                <td>${receta.veterinario?.nombre || 'N/A'}</td>
                <td>${receta.fecha || 'N/A'}</td>
                <td><span class="badge badge-warning">${receta.estado}</span></td>
                <td>
                    <button class="action-btn" onclick="verificarReceta(${receta.id})" title="Verificar">
                        <i class="fa-solid fa-check-circle"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error cargando recetas:', error);
    }
}

async function verificarReceta(id) {
    try {
        const resultado = await fetchAPI(`/recetas/verificar/${id}`);
        
        let mensaje = '';
        if (resultado.valida) {
            mensaje = '✅ Receta válida';
            // Opcional: dispensar automáticamente
            await dispensarReceta(id);
        } else {
            mensaje = '❌ Receta no válida:\n' + resultado.errores.join('\n');
        }
        
        if (resultado.advertencias.length > 0) {
            mensaje += '\n⚠️ Advertencias:\n' + resultado.advertencias.join('\n');
        }
        
        showToast(mensaje, !resultado.valida);
        if (resultado.valida) {
            loadPrescriptions();
            loadStats();
        }
    } catch (error) {
        showToast('Error al verificar receta', true);
    }
}

async function dispensarReceta(id) {
    try {
        const resultado = await fetchAPI(`/recetas/dispensar/${id}`, { method: 'POST' });
        if (resultado.dispensado) {
            showToast('Receta dispensada con éxito');
            loadPrescriptions();
            loadStats();
            loadInventory();
        } else {
            showToast('Error al dispensar: ' + resultado.errores.join(', '), true);
        }
    } catch (error) {
        showToast('Error al dispensar receta', true);
    }
}

// ===========================
// INVENTARIO
// ===========================
async function loadInventory() {
    try {
        const searchTerm = document.getElementById('inventorySearch')?.value || '';
        const medicamentos = await fetchAPI(`/medicamentos?search=${encodeURIComponent(searchTerm)}`);
        const tbody = document.getElementById('inventoryList');
        
        if (medicamentos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">No hay medicamentos</td></tr>';
            return;
        }
        
        tbody.innerHTML = medicamentos.map(m => `
            <tr class="${m.stock < 10 ? 'inventory-low' : ''}">
                <td>${m.id}</td>
                <td><strong>${m.nombre}</strong></td>
                <td>${m.presentacion || 'N/A'}</td>
                <td><span class="badge ${m.stock < 5 ? 'badge-danger' : m.stock < 10 ? 'badge-warning' : 'badge-success'}">${m.stock} unidades</span></td>
                <td>$${m.precio?.toFixed(2) || '0'}</td>
                <td>${m.stock < 5 ? '<span class="badge badge-danger">Stock Crítico</span>' : m.stock < 10 ? '<span class="badge badge-warning">Stock Bajo</span>' : '<span class="badge badge-success">Disponible</span>'}</td>
                <td>
                    <button class="action-btn" onclick="editStock(${m.id})" title="Editar stock">
                        <i class="fa-solid fa-pen"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    } catch (error) {
        console.error('Error cargando inventario:', error);
    }
}

async function editStock(id) {

    const newStock = prompt('Ingrese el nuevo stock:');

    if (newStock === null) return;

    if (isNaN(newStock) || parseInt(newStock) < 0) {

        showToast('Ingrese un stock válido', true);

        return;
    }

    try {

        const response = await fetchAPI(
            `/medicamentos/${id}/stock?stock=${parseInt(newStock)}`,
            {
                method: 'PUT'
            }
        );

        console.log(response);

        if (response.success) {

            showToast(response.message);

            await loadInventory();
            await loadStats();
            await loadMedicineSelects();

        } else {

            showToast(response.message, true);

        }

    } catch (error) {

        console.error('ERROR COMPLETO:', error);

        showToast('Error al actualizar stock', true);

    }
}
async function addMedicine() {
    const nombre = document.getElementById('newMedName').value;
    const presentacion = document.getElementById('newMedPresentation').value;
    const stock = parseInt(document.getElementById('newMedStock').value);
    const precio = parseFloat(document.getElementById('newMedPrice').value);
    const descripcion = document.getElementById('newMedDescription').value;
    
    if (!nombre || !presentacion || isNaN(stock) || isNaN(precio)) {
        showToast('Complete todos los campos correctamente', true);
        return;
    }
    
    try {
        await fetchAPI('/medicamentos', {
            method: 'POST',
            body: JSON.stringify({
                nombre,
                presentacion,
                stock,
                precio,
                descripcion: descripcion || 'Sin descripción',
                contraindicaciones: '',
                interacciones: ''
            })
        });
        
        closeModal();
        loadInventory();
        loadStats();
        renderMedicinesInfo();
        showToast(`Medicamento ${nombre} agregado correctamente`);
    } catch (error) {
        showToast('Error al agregar medicamento', true);
    }
}

// ===========================
// INFORMACIÓN DE MEDICAMENTOS
// ===========================
async function renderMedicinesInfo() {
    try {
        const searchTerm = document.getElementById('infoSearch')?.value || '';
        const medicamentos = await fetchAPI(`/medicamentos?search=${encodeURIComponent(searchTerm)}`);
        const container = document.getElementById('medicinesInfo');
        
        if (medicamentos.length === 0) {
            container.innerHTML = '<p style="text-align: center; padding: 2rem;">No se encontraron medicamentos</p>';
            return;
        }
        
        container.innerHTML = medicamentos.map(m => `
            <div style="padding: 1rem; border-bottom: 1px solid var(--border-default);">
                <h3 style="color: var(--green-main); margin-bottom: 0.5rem;">${m.nombre}</h3>
                <p><strong>Presentación:</strong> ${m.presentacion || 'N/A'}</p>
                <p><strong>Precio:</strong> $${m.precio?.toFixed(2) || '0'}</p>
                <p><strong>Stock disponible:</strong> ${m.stock} unidades</p>
                <p><strong>Descripción:</strong> ${m.descripcion || 'Sin descripción'}</p>
                ${m.contraindicaciones ? `<p><strong>Contraindicaciones:</strong> ${m.contraindicaciones}</p>` : ''}
                ${m.interacciones ? `<p><strong>Interacciones:</strong> ${m.interacciones}</p>` : ''}
            </div>
        `).join('');
    } catch (error) {
        console.error('Error cargando información:', error);
    }
}

// ===========================
// VENTAS
// ===========================
async function loadMedicineSelects() {
    try {
        const medicamentos = await fetchAPI('/medicamentos');
        const options = medicamentos.map(m => `<option value="${m.id}">${m.nombre} - $${m.precio?.toFixed(2)} (Stock: ${m.stock})</option>`).join('');
        document.getElementById('dispenseMedicine').innerHTML = '<option value="">Seleccione medicamento...</option>' + options;
        document.getElementById('saleMedicine').innerHTML = '<option value="">Seleccione medicamento...</option>' + options;
    } catch (error) {
        console.error('Error cargando selects:', error);
    }
}

async function updateTotalAmount() {
    const medicineId = document.getElementById('saleMedicine').value;
    const quantity = parseInt(document.getElementById('saleQuantity').value) || 0;
    
    if (medicineId && quantity > 0) {
        try {
            const medicamentos = await fetchAPI('/medicamentos');
            const medicine = medicamentos.find(m => m.id == medicineId);
            if (medicine) {
                document.getElementById('salePrice').value = `$${medicine.precio.toFixed(2)}`;
                document.getElementById('totalAmount').textContent = `$${(medicine.precio * quantity).toFixed(2)}`;
            }
        } catch (error) {
            console.error('Error calculando total:', error);
        }
    } else {
        document.getElementById('salePrice').value = '';
        document.getElementById('totalAmount').textContent = '$0';
    }
}

async function registerSale() {
    const medicineId = document.getElementById('saleMedicine').value;
    const quantity = parseInt(document.getElementById('saleQuantity').value);
    const cliente = document.getElementById('saleClient').value;
    const pago = document.getElementById('salePayment').value;
    
    if (!medicineId || !quantity || !cliente) {
        showToast('Complete todos los campos', true);
        return;
    }
    
    try {
        const resultado = await fetchAPI('/ventas', {
            method: 'POST',
            body: JSON.stringify({ medicamentoId: parseInt(medicineId), cantidad: quantity, cliente, pago })
        });
        
        showToast(`Venta registrada: $${resultado.total.toFixed(2)}`);
        
        document.getElementById('saleQuantity').value = '';
        document.getElementById('saleClient').value = '';
        document.getElementById('saleMedicine').value = '';
        document.getElementById('salePrice').value = '';
        document.getElementById('totalAmount').textContent = '$0';
        
        loadInventory();
        loadMedicineSelects();
        loadStats();
    } catch (error) {
        showToast('Error al registrar venta', true);
    }
}

// ===========================
// NAVEGACIÓN
// ===========================
function switchPanel(panelId) {
    const panels = ['verifyPanel', 'dispensePanel', 'salesPanel', 'inventoryPanel', 'infoPanel'];
    panels.forEach(panel => {
        const element = document.getElementById(panel);
        if (element) element.style.display = 'none';
    });
    document.getElementById(panelId).style.display = 'block';
    
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
    });
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

document.addEventListener('DOMContentLoaded', function() {
    // Mostrar fecha
    const dateDisplay = document.getElementById('currentDate');
    if (dateDisplay) {
        const today = new Date();
        const options = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        dateDisplay.textContent = today.toLocaleDateString('es-ES', options);
    }
    
    // Cargar datos
    loadStats();
    loadPrescriptions();
    loadInventory();
    loadMedicineSelects();
    renderMedicinesInfo();
    
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
            
            if (panelId) switchPanel(panelId);
        });
    });
    
    // Event listeners
    document.getElementById('saleMedicine')?.addEventListener('change', updateTotalAmount);
    document.getElementById('saleQuantity')?.addEventListener('input', updateTotalAmount);
    document.getElementById('inventorySearch')?.addEventListener('input', () => loadInventory());
    document.getElementById('infoSearch')?.addEventListener('input', () => renderMedicinesInfo());
    
    // Exponer funciones globales
    window.verificarReceta = verificarReceta;
    window.dispensarReceta = dispensarReceta;
    window.registerSale = registerSale;
    window.editStock = editStock;
    window.addMedicine = addMedicine;
    window.openAddMedicineModal = openAddMedicineModal;
    window.closeModal = closeModal;
    window.verifyPrescription = () => showToast('Use el botón "Verificar" en la lista de recetas', true);
    window.dispenseMedicine = () => showToast('Seleccione una receta de la lista para dispensar', true);
});