
package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Consulta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Seguimiento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ConsultaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.SeguimientoRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SeguimientoService {

    private final SeguimientoRepository seguimientoRepository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaRepository consultaRepository;

    public SeguimientoService(SeguimientoRepository seguimientoRepository,
                              MascotaRepository mascotaRepository,
                              ConsultaRepository consultaRepository) {
        this.seguimientoRepository = seguimientoRepository;
        this.mascotaRepository = mascotaRepository;
        this.consultaRepository = consultaRepository;
    }

    // ==================== CREAR / GUARDAR ====================

    @Transactional
    public Seguimiento guardar(Seguimiento seguimiento) {
        return seguimientoRepository.save(seguimiento);
    }

    @Transactional
    public Seguimiento crearSeguimiento(Long mascotaId, Long consultaId,
                                        String descripcion, String estado,
                                        LocalDate fechaProgramada, String observaciones) {
        Mascota mascota = null;
        if (mascotaId != null) {
            mascota = mascotaRepository.findById(mascotaId)
                    .orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada ID: " + mascotaId));
        }

        Consulta consulta = null;
        if (consultaId != null) {
            consulta = consultaRepository.findById(consultaId)
                    .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada ID: " + consultaId));
        }

        Seguimiento seguimiento = new Seguimiento();
        seguimiento.setMascota(mascota);
        seguimiento.setConsulta(consulta);
        seguimiento.setDescripcion(descripcion);
        seguimiento.setEstado(estado != null ? estado : "PENDIENTE");
        seguimiento.setFechaProgramada(fechaProgramada);
        seguimiento.setObservaciones(observaciones);
        // fechaRealizada queda nula inicialmente

        return seguimientoRepository.save(seguimiento);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public Seguimiento actualizar(Seguimiento actualizado) {
        Seguimiento existente = buscarPorId(actualizado.getId());

        existente.setDescripcion(actualizado.getDescripcion());
        existente.setEstado(actualizado.getEstado());
        existente.setFechaProgramada(actualizado.getFechaProgramada());
        existente.setObservaciones(actualizado.getObservaciones());
        // No se actualiza fechaRealizada a menos que se pase explícitamente
        if (actualizado.getFechaRealizada() != null) {
            existente.setFechaRealizada(actualizado.getFechaRealizada());
        }

        return seguimientoRepository.save(existente);
    }

    @Transactional
    public Seguimiento marcarComoRealizado(Long id, LocalDate fechaRealizada) {
        Seguimiento seguimiento = buscarPorId(id);
        seguimiento.setEstado("REALIZADO");
        seguimiento.setFechaRealizada(fechaRealizada != null ? fechaRealizada : LocalDate.now());
        return seguimientoRepository.save(seguimiento);
    }

    // ==================== CONSULTAS ====================

    public Seguimiento buscarPorId(Long id) {
        return seguimientoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seguimiento no encontrado con ID: " + id));
    }

    public List<Seguimiento> listarTodos() {
        return seguimientoRepository.findAll();
    }

    public List<Seguimiento> buscarPorMascota(Long mascotaId) {
        return seguimientoRepository.findByMascotaId(mascotaId);
    }

    public List<Seguimiento> buscarPorConsulta(Long consultaId) {
        return seguimientoRepository.findByConsultaId(consultaId);
    }

    public List<Seguimiento> buscarPorEstado(String estado) {
        return seguimientoRepository.findByEstado(estado);
    }

    public List<Seguimiento> buscarPorRangoFechasProgramadas(LocalDate inicio, LocalDate fin) {
        return seguimientoRepository.findByFechaProgramadaBetween(inicio, fin);
    }

    public List<Seguimiento> buscarSeguimientosPendientesHoy() {
        return seguimientoRepository.findByFechaProgramadaAndEstado(LocalDate.now(), "PENDIENTE");
    }

    public List<Seguimiento> buscarSeguimientosAtrasados() {
        return seguimientoRepository.findByEstadoAndFechaProgramadaBefore("PENDIENTE", LocalDate.now());
    }

    public List<Seguimiento> proximosSeguimientosPorMascota(Long mascotaId) {
        return seguimientoRepository.findByMascotaIdOrderByFechaProgramadaAsc(mascotaId);
    }

    // ==================== ELIMINAR ====================

    @Transactional
    public void eliminar(Long id) {
        if (!seguimientoRepository.existsById(id)) {
            throw new EntityNotFoundException("Seguimiento no encontrado con ID: " + id);
        }
        seguimientoRepository.deleteById(id);
    }
}
