package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda;

@Repository
public interface AgendaRepository extends JpaRepository<Agenda, Long> {
    
    List<Agenda> findByFecha(LocalDate fecha);
    
    List<Agenda> findByFechaBetween(LocalDate inicio, LocalDate fin);
    
    List<Agenda> findByDisponibleTrue();
    
    @Query("SELECT a FROM Agenda a WHERE a.fecha = :fecha AND a.disponible = true")
    List<Agenda> findHorariosDisponiblesByFecha(@Param("fecha") LocalDate fecha);
    
    @Query("SELECT a FROM Agenda a WHERE a.veterinario.id = :veterinarioId AND a.fecha = :fecha")
    List<Agenda> findByVeterinarioAndFecha(@Param("veterinarioId") Long veterinarioId, @Param("fecha") LocalDate fecha);
    

    @Query("SELECT a FROM Agenda a WHERE a.fecha = :fecha AND a.horaInicio <= :hora AND a.horaFin > :hora AND a.disponible = true")
    List<Agenda> findAgendaDisponiblePorFechaYHora(@Param("fecha") LocalDate fecha, @Param("hora") java.time.LocalTime hora);

    @Query("SELECT a FROM Agenda a WHERE a.fecha = :fecha AND a.horaInicio <= :hora AND a.horaFin > :hora")
    List<Agenda> findAgendaPorFechaYHora(@Param("fecha") LocalDate fecha, @Param("hora") java.time.LocalTime hora);

}