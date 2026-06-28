package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    // ===== BÚSQUEDAS POR CAMPOS =====
    Optional<Cliente> findByTelefono(String telefono);
    
    Optional<Cliente> findByEmail(String email);
    
    // ✅ NUEVO: Buscar cliente por nombre exacto
    Cliente findByNombre(String nombre);
    
    List<Cliente> findByNombreContainingIgnoreCase(String nombre);
    
    // ===== CONSULTAS CON FETCH =====
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.mascotas WHERE c.id = :id")
    Optional<Cliente> findByIdWithMascotas(@Param("id") Long id);
    
    @Query("SELECT c FROM Cliente c JOIN c.mascotas m WHERE LOWER(m.nombre) LIKE LOWER(CONCAT('%', :nombreMascota, '%'))")
    List<Cliente> findByMascotasNombre(@Param("nombreMascota") String nombreMascota);
    
    @Query("SELECT c FROM Cliente c LEFT JOIN FETCH c.pagos WHERE c.id = :id")
    Optional<Cliente> findByIdWithPagos(@Param("id") Long id);
    
    // ===== VALIDACIONES =====
    boolean existsByTelefono(String telefono);
    
    // ===== CONTADORES =====
    @Override
    long count();
}