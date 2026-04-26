package com.veterinariapetCcinic.veterinaria_pet_clinic.repository.Veterinaria;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Veterinaria.Seguimiento;

public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {
    List<Seguimiento> findTop5ByOrderByIdDesc();
}