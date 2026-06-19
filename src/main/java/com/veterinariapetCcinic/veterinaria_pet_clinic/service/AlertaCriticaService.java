package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.AlertaCritica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Consulta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.AlertaCriticaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ConsultaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AlertaCriticaService {

    private final AlertaCriticaRepository alertaRepository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaRepository consultaRepository;
    private final UsuarioRepository usuarioRepository;

    public AlertaCriticaService(AlertaCriticaRepository alertaRepository,
                                MascotaRepository mascotaRepository,
                                ConsultaRepository consultaRepository,
                                UsuarioRepository usuarioRepository) {
        this.alertaRepository = alertaRepository;
        this.mascotaRepository = mascotaRepository;
        this.consultaRepository = consultaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ==================== CREAR / GUARDAR ====================

    @Transactional
    public AlertaCritica guardar(AlertaCritica alerta) {
        return alertaRepository.save(alerta);
    }

    @Transactional
    public AlertaCritica crearAlerta(Long mascotaId, Long consultaId, String tipoAlerta,
                                     String descripcion, String prioridad) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada ID: " + mascotaId));

        Consulta consulta = null;
        if (consultaId != null) {
            consulta = consultaRepository.findById(consultaId)
                    .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada ID: " + consultaId));
        }

        AlertaCritica alerta = new AlertaCritica();
        alerta.setMascota(mascota);
        alerta.setConsulta(consulta);
        alerta.setTipoAlerta(tipoAlerta);
        alerta.setDescripcion(descripcion);
        alerta.setPrioridad(prioridad);
        alerta.setEstado("PENDIENTE");
        alerta.setFechaCreacion(LocalDateTime.now());

        return alertaRepository.save(alerta);
    }

    // ==================== RESOLVER ALERTA ====================

    @Transactional
    public AlertaCritica resolverAlerta(Long alertaId, Long usuarioId, String comentarioResolucion) {
        AlertaCritica alerta = alertaRepository.findById(alertaId)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada ID: " + alertaId));

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado ID: " + usuarioId));

        alerta.setEstado("RESUELTA");
        alerta.setResueltaPor(usuario);
        // Si deseas agregar un campo "comentarioResolucion", deberías añadirlo a la entidad.
        // Por ahora solo marcamos como resuelta.

        return alertaRepository.save(alerta);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public AlertaCritica actualizar(AlertaCritica alertaActualizada) {
        AlertaCritica existente = buscarPorId(alertaActualizada.getId());

        existente.setTipoAlerta(alertaActualizada.getTipoAlerta());
        existente.setDescripcion(alertaActualizada.getDescripcion());
        existente.setPrioridad(alertaActualizada.getPrioridad());
        existente.setEstado(alertaActualizada.getEstado());

        // No se actualizan mascota, consulta, fechaCreacion ni resueltaPor directamente aquí
        // Para cambiar la resolución se usa resolverAlerta

        return alertaRepository.save(existente);
    }

    // ==================== CONSULTAS ====================

    public AlertaCritica buscarPorId(Long id) {
        return alertaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada con ID: " + id));
    }

    public List<AlertaCritica> listarTodos() {
        return alertaRepository.findAll();
    }

    public List<AlertaCritica> buscarPorMascota(Long mascotaId) {
        return alertaRepository.findByMascotaId(mascotaId);
    }

    public List<AlertaCritica> buscarPorConsulta(Long consultaId) {
        return alertaRepository.findByConsultaId(consultaId);
    }

    public List<AlertaCritica> buscarPorEstado(String estado) {
        return alertaRepository.findByEstado(estado);
    }

    public List<AlertaCritica> buscarPendientes() {
        return alertaRepository.findAlertasPendientes();
    }

    public List<AlertaCritica> buscarPorPrioridad(String prioridad) {
        return alertaRepository.findByPrioridad(prioridad);
    }

    public long contarAlertasActivasPorMascota(Long mascotaId) {
        return alertaRepository.countByMascotaIdAndEstadoNot(mascotaId, "RESUELTA");
    }

    // ==================== ELIMINAR ====================

    @Transactional
    public void eliminar(Long id) {
        if (!alertaRepository.existsById(id)) {
            throw new EntityNotFoundException("Alerta no encontrada con ID: " + id);
        }
        alertaRepository.deleteById(id);
    }
}

