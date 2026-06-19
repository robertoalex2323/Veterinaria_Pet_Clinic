package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.SignosVitales;

@Repository
public interface SignosVitalesRepository extends JpaRepository<SignosVitales, Long> {

    // Buscar por mascota
    List<SignosVitales> findByMascotaId(Long mascotaId);

    // Buscar por consulta
    List<SignosVitales> findByConsultaId(Long consultaId);

    // Buscar por rango de fechas
    List<SignosVitales> findByFechaRegistroBetween(LocalDateTime inicio, LocalDateTime fin);

    // Últimos registros de una mascota (orden descendente)
    List<SignosVitales> findByMascotaIdOrderByFechaRegistroDesc(Long mascotaId);

    // Últimos 5 registros de una mascota
    List<SignosVitales> findTop5ByMascotaIdOrderByFechaRegistroDesc(Long mascotaId);
}


