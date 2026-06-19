package com.veterinariapetCcinic.veterinaria_pet_clinic.service;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Pago;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.PagoRepository;

@Service
public class PagoService {
    
    private final PagoRepository pagoRepository;
    
    public PagoService(PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
    }
    
    @Transactional
    public Pago guardar(Pago pago) {
        pago.setFechaPago(LocalDateTime.now());
        return pagoRepository.save(pago);
    }
    
    @Transactional
    public Pago registrarPago(Pago pago, Long citaId) {
        pago.setEstado("PAGADO");
        return guardar(pago);
    }
    
    public Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + id));
    }
    
    public List<Pago> listarTodos() {
        return pagoRepository.findAll();
    }
    
    public List<Pago> obtenerPagosPendientes() {
        return pagoRepository.findByEstado("PENDIENTE");
    }
    
    public List<Pago> obtenerPagosPorCliente(Long clienteId) {
        return pagoRepository.findByClienteId(clienteId);
    }
    
    public List<Pago> obtenerPagosPorMetodo(String metodoPago) {
        return pagoRepository.findByMetodoPago(metodoPago);
    }
    
    public List<Pago> obtenerPagosDelDia() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return pagoRepository.findPagosByFechaRange(inicio, fin);
    }
    
    public Double getTotalPagos() {
        return pagoRepository.sumTotalPagos();
    }
    
    public Double getTotalPagosDelDia() {
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fin = LocalDate.now().atTime(LocalTime.MAX);
        return pagoRepository.sumPagosByFechaRange(inicio, fin);
    }
    
    public long contarPagosPendientes() {
        return pagoRepository.countByEstado("PENDIENTE");
    }
    
    @Transactional
    public void actualizarEstado(Long id, String nuevoEstado) {
        Pago pago = buscarPorId(id);
        pago.setEstado(nuevoEstado);
        pagoRepository.save(pago);
    }
}