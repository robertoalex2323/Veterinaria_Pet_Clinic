package com.veterinariapetCcinic.veterinaria_pet_clinic.repository.Veterinaria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Veterinaria.VacunaAplicada;

public interface VacunaAplicadaRepository extends JpaRepository<VacunaAplicada, Long> {
    List<VacunaAplicada> findTop5ByOrderByFechaAplicacionDesc();
}