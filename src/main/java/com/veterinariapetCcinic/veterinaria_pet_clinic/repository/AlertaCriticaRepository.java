package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.AlertaCritica;

@Repository
public interface AlertaCriticaRepository extends JpaRepository<AlertaCritica, Long> {

    // Alertas por mascota
    List<AlertaCritica> findByMascotaId(Long mascotaId);

    // Alertas por consulta
    List<AlertaCritica> findByConsultaId(Long consultaId);

    // Alertas por estado (PENDIENTE, RESUELTA, etc.)
    List<AlertaCritica> findByEstado(String estado);

    // Alertas por prioridad (ALTA, MEDIA, BAJA)
    List<AlertaCritica> findByPrioridad(String prioridad);

    // Alertas creadas después de una fecha
    List<AlertaCritica> findByFechaCreacionAfter(LocalDateTime fecha);

    // Alertas no resueltas (donde resueltaPor es null y estado != 'RESUELTA')
    @Query("SELECT a FROM AlertaCritica a WHERE a.resueltaPor IS NULL AND a.estado != 'RESUELTA'")
    List<AlertaCritica> findAlertasPendientes();

    // Contar alertas activas por mascota
    long countByMascotaIdAndEstadoNot(Long mascotaId, String estadoExcluido);

    // Buscar alertas por tipo (ej. "PESO_CRITICO", "TEMPERATURA_ALTA")
    List<AlertaCritica> findByTipoAlerta(String tipoAlerta);
}

