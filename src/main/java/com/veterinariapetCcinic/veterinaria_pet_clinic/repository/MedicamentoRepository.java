package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;

@Repository
public interface MedicamentoRepository extends JpaRepository<Medicamento, Long> {
    
    @Query("SELECT m FROM Medicamento m WHERE m.stock <= m.stockMinimo")
    List<Medicamento> findBajoStock();
    
    List<Medicamento> findByNombreContainingIgnoreCase(String nombre);
    
    List<Medicamento> findByStockLessThan(Integer stock);
    
    List<Medicamento> findByPresentacion(String presentacion);
}
