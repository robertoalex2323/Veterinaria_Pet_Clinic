package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cita;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);

    List<Cita> findByEstado(String estado);

    List<Cita> findByMascotaId(Long mascotaId);

    @Query("SELECT c FROM Cita c WHERE DATE(c.fechaHora) = :fecha")
    List<Cita> findCitasByFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Cita c WHERE c.mascota.cliente.id = :clienteId")
    List<Cita> findCitasByClienteId(@Param("clienteId") Long clienteId);

    @Query("SELECT c FROM Cita c WHERE c.mascota.cliente.telefono = :telefono")
    List<Cita> findCitasByClienteTelefono(@Param("telefono") String telefono);

    long countByFechaHoraBetweenAndEstadoIn(LocalDateTime inicio, LocalDateTime fin, List<String> estados);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'AGENDADA' AND c.fechaHora > :ahora")
    List<Cita> findCitasPendientes(@Param("ahora") LocalDateTime ahora);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'AGENDADA' AND DATE(c.fechaHora) = :fecha")
    List<Cita> findCitasPendientesByFecha(@Param("fecha") LocalDate fecha);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'AGENDADA' AND DATE(c.fechaHora) = :fecha AND c.recordatorioEnviado = false")
    List<Cita> findCitasPendientesParaRecordatorio(@Param("fecha") LocalDate fecha);

    // ── NUEVO: trae mascota + cliente en una sola query para evitar LazyInitializationException ──
    @Query("SELECT DISTINCT c FROM Cita c " +
           "JOIN FETCH c.mascota m " +
           "JOIN FETCH m.cliente " +
           "LEFT JOIN FETCH c.veterinario " +
           "WHERE c.fechaHora BETWEEN :inicio AND :fin")
    List<Cita> findCitasDelDiaConDatos(@Param("inicio") LocalDateTime inicio,
                                        @Param("fin") LocalDateTime fin);

       boolean existsByMascotaIdAndFechaHoraAndEstadoNot(Long mascotaId, LocalDateTime fechaHora, String estado);
}