package com.veterinariapetCcinic.veterinaria_pet_clinic.repository.Veterinaria;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Veterinaria.Consulta;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    List<Consulta> findByFechaConsultaBetween(LocalDateTime inicio, LocalDateTime fin);
}