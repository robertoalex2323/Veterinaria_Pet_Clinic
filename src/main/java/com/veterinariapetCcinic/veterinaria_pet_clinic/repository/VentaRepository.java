package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByOrderByFechaDesc();

    @Query("SELECT SUM(v.total) FROM Venta v WHERE v.fecha >= :inicio")
    BigDecimal sumVentasDesde(@Param("inicio") LocalDateTime inicio);

    Optional<Venta> findByRecetaMedicaId(Long recetaMedicaId);

    @Query("SELECT v FROM Venta v WHERE v.fecha >= :inicio ORDER BY v.fecha DESC")
    List<Venta> findVentasDesde(@Param("inicio") LocalDateTime inicio);
}
