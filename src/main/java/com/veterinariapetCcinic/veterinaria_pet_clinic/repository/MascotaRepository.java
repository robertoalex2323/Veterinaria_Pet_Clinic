package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;

@Repository
public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    
    List<Mascota> findByClienteId(Long clienteId);
    
    List<Mascota> findByNombreContainingIgnoreCase(String nombre);
    
    List<Mascota> findByEspecie(String especie);

    @Query("SELECT m FROM Mascota m LEFT JOIN FETCH m.cliente ORDER BY m.nombre ASC")
    List<Mascota> findAllWithCliente();

    @Query("SELECT m FROM Mascota m LEFT JOIN FETCH m.cliente WHERE m.id = :id")
    Optional<Mascota> findByIdWithCliente(@Param("id") Long id);
    
    @Query("SELECT m FROM Mascota m LEFT JOIN FETCH m.citas WHERE m.id = :id")
    Optional<Mascota> findByIdWithCitas(@Param("id") Long id);
    
    long countByClienteId(Long clienteId);
}
