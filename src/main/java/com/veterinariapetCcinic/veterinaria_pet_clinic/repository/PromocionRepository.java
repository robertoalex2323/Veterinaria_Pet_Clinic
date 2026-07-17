package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    List<Promocion> findByActivoTrue();

    @Query("SELECT p FROM Promocion p WHERE p.activo = true " +
           "AND (p.fechaInicio IS NULL OR p.fechaInicio <= :fecha) " +
           "AND (p.fechaFin IS NULL OR p.fechaFin >= :fecha)")
    List<Promocion> findPromocionesActivas(@Param("fecha") LocalDate fecha);

    @Query("SELECT p FROM Promocion p WHERE p.activo = true " +
           "AND p.categoriaAplicable = :categoria " +
           "AND (p.fechaInicio IS NULL OR p.fechaInicio <= :fecha) " +
           "AND (p.fechaFin IS NULL OR p.fechaFin >= :fecha)")
    List<Promocion> findByCategoriaAndFecha(@Param("categoria") String categoria, 
                                            @Param("fecha") LocalDate fecha);
}