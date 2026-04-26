package com.veterinariapetCcinic.veterinaria_pet_clinic.repository.Veterinaria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Veterinaria.AlertaCritica;

public interface AlertaCriticaRepository extends JpaRepository<AlertaCritica, Long> {
    List<AlertaCritica> findTop5ByOrderByFechaCreacionDesc();
}