package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Veterinario;

public interface VeterinarioRepository extends JpaRepository<Veterinario, Long> {
}