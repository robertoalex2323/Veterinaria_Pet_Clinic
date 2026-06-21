package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Receta;

@Repository
public interface RecetaRepository extends JpaRepository<Receta, Long> {
    List<Receta> findByEstado(String estado);
    List<Receta> findByConsultaId(Long consultaId);
}