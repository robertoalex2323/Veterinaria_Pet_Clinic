package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaItem;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaMedicaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final RecetaMedicaRepository recetaMedicaRepository;
    private final ClienteRepository clienteRepository;

    public VentaService(VentaRepository ventaRepository, 
                       MedicamentoRepository medicamentoRepository,
                       RecetaMedicaRepository recetaMedicaRepository,
                       ClienteRepository clienteRepository) {
        this.ventaRepository = ventaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.recetaMedicaRepository = recetaMedicaRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Venta procesarVenta(Venta venta) {
        for (DetalleVenta detalle : venta.getDetalles()) {
            Medicamento med = medicamentoRepository.findById(detalle.getMedicamento().getId())
                    .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + detalle.getMedicamento().getNombre()));

            if (med.getStock() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + med.getNombre() + " (disponible: " + med.getStock() + ")");
            }

            // Asignar el nombre y precio real desde la BD
            detalle.setMedicamento(med);
            if (detalle.getPrecioUnitario() == null || detalle.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                detalle.setPrecioUnitario(med.getPrecio());
            }

            med.setStock(med.getStock() - detalle.getCantidad());
            medicamentoRepository.save(med);
        }

        venta.recalcularTotales();
        return ventaRepository.save(venta);
    }

    @Transactional
    public Venta crearVentaDesdeReceta(Long recetaId, String metodoPago, Usuario usuario) {
        RecetaMedica receta = recetaMedicaRepository.findById(recetaId)
                .orElseThrow(() -> new RuntimeException("Receta no encontrada"));

        if (receta.getEstado() != RecetaEstado.DISPENSADA) {
            throw new RuntimeException("La receta debe estar dispensada antes de generar la venta. Estado actual: " + receta.getEstado());
        }

        // Verificar si ya existe una venta para esta receta
        if (ventaRepository.findByRecetaMedicaId(recetaId).isPresent()) {
            throw new RuntimeException("Ya existe una venta registrada para esta receta");
        }

        Venta venta = new Venta();
        venta.setRecetaMedica(receta);
        venta.setMetodoPago(metodoPago);
        venta.setUsuario(usuario);

        // Determinar cliente desde el paciente de la receta
        // Buscamos en clientes asociados a mascotas con el nombre del paciente
        if (receta.getPaciente() != null) {
            List<Cliente> clientes = clienteRepository.findByMascotasNombre(receta.getPaciente().getNombre());
            if (!clientes.isEmpty()) {
                venta.setCliente(clientes.get(0));
            }
        }

        // Crear detalles desde items de la receta
        List<DetalleVenta> detalles = new ArrayList<>();
        for (RecetaItem item : receta.getItems()) {
            Medicamento med = item.getMedicamento();
            if (med == null) continue;

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setMedicamento(med);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(med.getPrecio() != null ? med.getPrecio() : BigDecimal.ZERO);
            detalles.add(detalle);
        }

        venta.setDetalles(detalles);
        venta.recalcularTotales();

        return ventaRepository.save(venta);
    }

    public List<Venta> listarVentas() {
        return ventaRepository.findByOrderByFechaDesc();
    }

    public List<Venta> listarVentasHoy() {
        return ventaRepository.findVentasDesde(LocalDate.now().atStartOfDay());
    }

    public BigDecimal calcularVentasHoy() {
        BigDecimal total = ventaRepository.sumVentasDesde(LocalDate.now().atStartOfDay());
        return total != null ? total : BigDecimal.ZERO;
    }

    public Venta buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
    }
}
