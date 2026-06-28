package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;

public interface RecetaMedicaRepository extends JpaRepository<RecetaMedica, Long> {
    List<RecetaMedica> findByEstado(RecetaEstado estado);
}