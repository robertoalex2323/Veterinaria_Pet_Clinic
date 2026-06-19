package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Seguimiento;

@Repository
public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {

    // Buscar por mascota
    List<Seguimiento> findByMascotaId(Long mascotaId);

    // Buscar por consulta
    List<Seguimiento> findByConsultaId(Long consultaId);

    // Buscar por estado (PENDIENTE, REALIZADO, CANCELADO, etc.)
    List<Seguimiento> findByEstado(String estado);

    // Buscar por rango de fechas programadas
    List<Seguimiento> findByFechaProgramadaBetween(LocalDate inicio, LocalDate fin);

    // Buscar seguimientos programados para hoy (pendientes)
    List<Seguimiento> findByFechaProgramadaAndEstado(LocalDate fecha, String estado);

    // Buscar seguimientos pendientes con fecha programada anterior a hoy (atrasados)
    List<Seguimiento> findByEstadoAndFechaProgramadaBefore(String estado, LocalDate fecha);

    // Ordenar por fecha programada ascendente (próximos)
    List<Seguimiento> findByMascotaIdOrderByFechaProgramadaAsc(Long mascotaId);
}