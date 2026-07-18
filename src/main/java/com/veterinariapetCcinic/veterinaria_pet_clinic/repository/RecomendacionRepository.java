package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Recomendacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface RecomendacionRepository extends JpaRepository<Recomendacion, Long> {

    long countByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
}

