package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final MedicamentoRepository medicamentoRepository;

    public VentaService(VentaRepository ventaRepository, MedicamentoRepository medicamentoRepository) {
        this.ventaRepository = ventaRepository;
        this.medicamentoRepository = medicamentoRepository;
    }

    @Transactional
    public Venta procesarVenta(Venta venta) {
        // 1. Validar y actualizar stock para cada detalle
        for (DetalleVenta detalle : venta.getDetalles()) {
            Medicamento med = medicamentoRepository.findById(detalle.getMedicamento().getId())
                    .orElseThrow(() -> new RuntimeException("Medicamento no encontrado: " + detalle.getMedicamento().getNombre()));

            if (med.getStock() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + med.getNombre());
            }

            med.setStock(med.getStock() - detalle.getCantidad());
            medicamentoRepository.save(med);
        }

        // 2. Guardar la venta (el cascade guardará los detalles)
        return ventaRepository.save(venta);
    }

    public List<Venta> listarVentas() {
        return ventaRepository.findByOrderByFechaDesc();
    }

    public BigDecimal calcularVentasHoy() {
        BigDecimal total = ventaRepository.sumVentasDesde(LocalDate.now().atStartOfDay());
        return total != null ? total : BigDecimal.ZERO;
    }
}