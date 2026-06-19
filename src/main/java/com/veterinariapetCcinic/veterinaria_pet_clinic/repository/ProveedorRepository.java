package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByRuc(String ruc);
}
