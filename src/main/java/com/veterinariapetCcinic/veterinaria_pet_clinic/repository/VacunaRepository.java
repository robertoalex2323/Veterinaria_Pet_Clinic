package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Vacuna;

@Repository
public interface VacunaRepository extends JpaRepository<Vacuna, Long> {
    
    // Buscar por nombre exacto 
    Optional<Vacuna> findByNombre(String nombre);
    
    // Buscar por nombre que contenga el texto
    List<Vacuna> findByNombreContainingIgnoreCase(String nombre);
    
    // Verificar si ya existe una vacuna con ese nombre
    boolean existsByNombre(String nombre);
}

