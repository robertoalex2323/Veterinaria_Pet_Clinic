package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Consulta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ConsultaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    public ConsultaService(ConsultaRepository consultaRepository,
                           MascotaRepository mascotaRepository,
                           UsuarioRepository usuarioRepository) {
        this.consultaRepository = consultaRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ==================== GUARDAR / CREAR ====================

    @Transactional
    public Consulta guardar(Consulta consulta) {
        return consultaRepository.save(consulta);
    }

    @Transactional
    public Consulta crearConsulta(Long mascotaId, Long veterinarioId, String motivoConsulta,
                                  Double peso, Double temperatura, Integer frecuenciaCardiaca,
                                  Integer frecuenciaRespiratoria, String observaciones) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada con ID: " + mascotaId));

        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new EntityNotFoundException("Veterinario no encontrado con ID: " + veterinarioId));

        Consulta consulta = new Consulta();
        consulta.setMascota(mascota);
        consulta.setVeterinario(veterinario);
        consulta.setMotivoConsulta(motivoConsulta);
        consulta.setPeso(peso);
        consulta.setTemperatura(temperatura);
        consulta.setFrecuenciaCardiaca(frecuenciaCardiaca);
        consulta.setFrecuenciaRespiratoria(frecuenciaRespiratoria);
        consulta.setObservaciones(observaciones);
        consulta.setEstado("ACTIVA");
        consulta.setFechaConsulta(LocalDateTime.now());

        return consultaRepository.save(consulta);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public Consulta actualizar(Consulta consultaActualizada) {
        Consulta existente = buscarPorId(consultaActualizada.getId());

        existente.setMotivoConsulta(consultaActualizada.getMotivoConsulta());
        existente.setObservaciones(consultaActualizada.getObservaciones());
        existente.setEstado(consultaActualizada.getEstado());
        existente.setPeso(consultaActualizada.getPeso());
        existente.setTemperatura(consultaActualizada.getTemperatura());
        existente.setFrecuenciaCardiaca(consultaActualizada.getFrecuenciaCardiaca());
        existente.setFrecuenciaRespiratoria(consultaActualizada.getFrecuenciaRespiratoria());
        // No se actualizan mascota, veterinario, fechaConsulta

        return consultaRepository.save(existente);
    }

    @Transactional
    public Consulta actualizarEstado(Long consultaId, String nuevoEstado) {
        Consulta consulta = buscarPorId(consultaId);
        consulta.setEstado(nuevoEstado);
        return consultaRepository.save(consulta);
    }

    // ==================== CONSULTAS ====================

    public Consulta buscarPorId(Long id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada con ID: " + id));
    }

    public List<Consulta> listarTodas() {
        return consultaRepository.findAll();
    }

    public List<Consulta> buscarPorMascota(Long mascotaId) {
        return consultaRepository.findByMascotaId(mascotaId);
    }

    public List<Consulta> buscarPorVeterinario(Long veterinarioId) {
        return consultaRepository.findByVeterinarioId(veterinarioId);
    }

    public List<Consulta> buscarPorEstado(String estado) {
        return consultaRepository.findByEstado(estado);
    }

    public List<Consulta> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return consultaRepository.findByFechaConsultaBetween(inicio, fin);
    }

    public List<Consulta> ultimasConsultasDeMascota(Long mascotaId, int limite) {
        // Si quieres un límite variable, puedes usar el método fijo top5 o escribir un query dinámico.
        return consultaRepository.findTop5ByMascotaIdOrderByFechaConsultaDesc(mascotaId);
    }

    public long contarConsultasActivasPorMascota(Long mascotaId) {
        return consultaRepository.countByMascotaIdAndEstado(mascotaId, "ACTIVA");
    }

    // ==================== ELIMINAR ====================

    @Transactional
    public void eliminar(Long id) {
        if (!consultaRepository.existsById(id)) {
            throw new EntityNotFoundException("Consulta no encontrada con ID: " + id);
        }
        consultaRepository.deleteById(id);
    }
}

