package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String presentacion;
    private Integer stock;
    private BigDecimal precio;
    private String descripcion;
    private String contraindicaciones;
    private String interacciones;

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String presentacion) { this.presentacion = presentacion; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getContraindicaciones() { return contraindicaciones; }
    public void setContraindicaciones(String contraindicaciones) { this.contraindicaciones = contraindicaciones; }
    public String getInteracciones() { return interacciones; }
    public void setInteracciones(String interacciones) { this.interacciones = interacciones; }
}