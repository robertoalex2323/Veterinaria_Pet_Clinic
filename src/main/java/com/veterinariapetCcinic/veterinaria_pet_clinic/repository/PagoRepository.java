package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Pago;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    
    List<Pago> findByEstado(String estado);
    
    List<Pago> findByClienteId(Long clienteId);
    
    List<Pago> findByMetodoPago(String metodoPago);
    
    @Query("SELECT p FROM Pago p WHERE p.fechaPago BETWEEN :inicio AND :fin")
    List<Pago> findPagosByFechaRange(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.estado = 'PAGADO'")
    Double sumTotalPagos();
    
    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.estado = 'PAGADO' AND p.fechaPago BETWEEN :inicio AND :fin")
    Double sumPagosByFechaRange(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    long countByEstado(String estado);
}