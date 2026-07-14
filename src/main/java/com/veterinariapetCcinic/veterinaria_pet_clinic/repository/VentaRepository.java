package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    
    List<Venta> findByOrderByFechaDesc();

    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fecha >= :inicio")
    BigDecimal sumVentasDesde(@Param("inicio") LocalDateTime inicio);

    Optional<Venta> findByRecetaMedicaId(Long recetaMedicaId);

    @Query("SELECT v FROM Venta v WHERE v.fecha >= :inicio ORDER BY v.fecha DESC")
    List<Venta> findVentasDesde(@Param("inicio") LocalDateTime inicio);
   
    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin")
    BigDecimal sumVentasEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
    
    @Query("SELECT v FROM Venta v " +
           "LEFT JOIN FETCH v.detalles d " +
           "LEFT JOIN FETCH d.producto p " +
           "LEFT JOIN FETCH d.medicamento m " +
           "LEFT JOIN FETCH v.cliente c " +
           "WHERE v.id = :id")

    Optional<Venta> findByIdWithDetalles(@Param("id") Long id);

    @Query("SELECT v FROM Venta v WHERE v.fecha BETWEEN :inicio AND :fin ORDER BY v.fecha DESC")
    List<Venta> findByFechaBetween(
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin);

    @Query("SELECT v FROM Venta v LEFT JOIN v.cliente c WHERE " +
       "(cast(:inicio as timestamp) IS NULL OR v.fecha >= :inicio) AND " +
       "(cast(:fin as timestamp) IS NULL OR v.fecha <= :fin) AND " +
       "(cast(:q as string) IS NULL OR :q = '' OR " +
       "(LOWER(c.nombre) LIKE LOWER(CONCAT('%', :q, '%')) " +
       "OR c.telefono LIKE CONCAT('%', :q, '%')) )" +
       "ORDER BY v.fecha DESC")
        Page<Venta> buscarHistorial(@Param("q") String q,
                                    @Param("inicio") LocalDateTime inicio,
                                    @Param("fin") LocalDateTime fin,
                                    Pageable pageable);    
}

