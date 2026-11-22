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

    @Query("SELECT a FROM Agenda a WHERE a.fecha = :fecha AND a.horaInicio <= :hora AND a.horaFin > :hora AND a.disponible = true AND a.veterinario.id = :vetId")
    List<Agenda> findAgendaDisponiblePorFechaHoraYVeterinario(@Param("fecha") LocalDate fecha, @Param("hora") java.time.LocalTime hora, @Param("vetId") Long vetId);

    @Query("SELECT a FROM Agenda a WHERE a.fecha = :fecha AND a.horaInicio <= :hora AND a.horaFin > :hora AND a.veterinario.id = :vetId")
    List<Agenda> findAgendaPorFechaHoraYVeterinario(@Param("fecha") LocalDate fecha, @Param("hora") java.time.LocalTime hora, @Param("vetId") Long vetId);

    /** Slots de un veterinario que se cruzan con el rango [inicio, fin). */
    @Query("SELECT a FROM Agenda a WHERE a.fecha = :fecha AND a.horaInicio < :fin AND a.horaFin > :inicio "
            + "AND a.veterinario.id = :vetId")
    List<Agenda> findSolapados(@Param("fecha") LocalDate fecha, @Param("inicio") java.time.LocalTime inicio,
            @Param("fin") java.time.LocalTime fin, @Param("vetId") Long vetId);

    /** Próximo turno libre con veterinario asignado a partir de una fecha (para sugerir fechas en el formulario de citas). */
    java.util.Optional<Agenda> findFirstByFechaGreaterThanEqualAndDisponibleTrueAndVeterinarioIsNotNullOrderByFechaAscHoraInicioAsc(LocalDate fecha);
}
