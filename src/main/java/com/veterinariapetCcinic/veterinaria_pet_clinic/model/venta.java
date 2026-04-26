package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class venta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productoNombre;
    private Integer cantidad;
    private Double precioUnitario;
    private Double total;
    private String categoria; 
    
    private LocalDateTime fechaVenta = LocalDateTime.now();
}