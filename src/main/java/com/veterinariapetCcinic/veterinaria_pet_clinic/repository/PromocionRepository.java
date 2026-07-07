package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PromocionRepository extends JpaRepository<Promocion, Long> {

    List<Promocion> findByActivaTrue();

    Optional<Promocion> findFirstByProducto_IdAndActivaTrue(Long productoId);

    List<Promocion> findByProducto_Id(Long productoId);
}

