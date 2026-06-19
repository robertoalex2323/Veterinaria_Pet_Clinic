package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.VacunaAplicada;

@Repository
public interface VacunaAplicadaRepository extends JpaRepository<VacunaAplicada, Long> {

    List<VacunaAplicada> findByMascotaId(Long mascotaId);

    List<VacunaAplicada> findByVacunaId(Long vacunaId);

    List<VacunaAplicada> findByVeterinarioId(Long veterinarioId);

    List<VacunaAplicada> findByFechaAplicacionBetween(LocalDate inicio, LocalDate fin);

    List<VacunaAplicada> findByProximaDosisBefore(LocalDate fecha);

    List<VacunaAplicada> findByMascotaIdOrderByFechaAplicacionDesc(Long mascotaId);

    boolean existsByMascotaIdAndVacunaIdAndFechaAplicacionAfter(Long mascotaId, Long vacunaId, LocalDate fecha);
}
