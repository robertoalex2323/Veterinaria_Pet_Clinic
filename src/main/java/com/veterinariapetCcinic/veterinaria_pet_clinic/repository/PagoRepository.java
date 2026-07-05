package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    
    List<Pago> findByEstado(String estado);
    
    List<Pago> findByClienteId(Long clienteId);
    
    List<Pago> findByMetodoPago(String metodoPago);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Pago p WHERE p.cita.id = :citaId AND p.estado = 'PAGADO'")
    boolean existsPagoPagadoPorCita(@Param("citaId") Long citaId);


    
    @Query("SELECT p FROM Pago p WHERE p.fechaPago BETWEEN :inicio AND :fin")
    List<Pago> findPagosByFechaRange(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.estado = 'PAGADO'")
    Double sumTotalPagos();
    
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.estado = 'PAGADO' AND p.fechaPago BETWEEN :inicio AND :fin")
    Double sumPagosByFechaRange(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    long countByEstado(String estado);

    /**
     * Obtiene el máximo comprobante para un prefijo de año (ej: PET2027-00000).
     * Debe pasarse el prefijo completo tipo: "PET{YYYY}-".
     */
    @Query("SELECT COALESCE(MAX(p.comprobante), :prefijo) FROM Pago p WHERE p.comprobante LIKE CONCAT(:prefijo, '%')")
    String findMaxComprobantePorPrefijo(@Param("prefijo") String prefijo);
}

