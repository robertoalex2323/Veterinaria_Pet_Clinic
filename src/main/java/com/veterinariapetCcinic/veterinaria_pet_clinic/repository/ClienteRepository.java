package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    Optional<Cliente> findByTelefono(String telefono);
    
    Optional<Cliente> findByEmail(String email);
    
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
    
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.mascotas WHERE c.id = :id")
    Optional<Cliente> findByIdWithMascotas(@Param("id") Long id);
    
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.pagos WHERE c.id = :id")
    Optional<Cliente> findByIdWithPagos(@Param("id") Long id);
    
    boolean existsByTelefono(String telefono);
    
    @Override
    long count();
}