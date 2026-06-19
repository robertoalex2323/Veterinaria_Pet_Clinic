package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Consulta;

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    // Buscar consultas por mascota
    List<Consulta> findByMascotaId(Long mascotaId);

    // Buscar consultas por veterinario (usuario)
    List<Consulta> findByVeterinarioId(Long veterinarioId);

    // Buscar consultas por estado (ej. "ACTIVA", "FINALIZADA", "CANCELADA")
    List<Consulta> findByEstado(String estado);

    // Buscar consultas en un rango de fechas
    List<Consulta> findByFechaConsultaBetween(LocalDateTime inicio, LocalDateTime fin);

    // Consultas de una mascota después de cierta fecha
    List<Consulta> findByMascotaIdAndFechaConsultaAfter(Long mascotaId, LocalDateTime fecha);

    // Contar consultas activas por mascota
    long countByMascotaIdAndEstado(Long mascotaId, String estado);

    // Últimas 5 consultas de una mascota (ordenadas por fecha descendente)
    @Query("SELECT c FROM Consulta c WHERE c.mascota.id = :mascotaId ORDER BY c.fechaConsulta DESC")
    List<Consulta> findTop5ByMascotaIdOrderByFechaConsultaDesc(@Param("mascotaId") Long mascotaId);
}

