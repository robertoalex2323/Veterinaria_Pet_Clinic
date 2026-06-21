package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {
    // Aquí podrías agregar consultas para reportes de productos más vendidos
}