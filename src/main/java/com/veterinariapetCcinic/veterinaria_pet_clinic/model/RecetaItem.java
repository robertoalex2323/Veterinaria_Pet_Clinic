package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class RecetaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonBackReference
    private RecetaMedica receta;

    @ManyToOne
    private Medicamento medicamento;

    private Integer cantidad;
    private String dosis;
    private String frecuencia;

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public RecetaMedica getReceta() { return receta; }
    public void setReceta(RecetaMedica receta) { this.receta = receta; }
    public Medicamento getMedicamento() { return medicamento; }
    public void setMedicamento(Medicamento medicamento) { this.medicamento = medicamento; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    public String getDosis() { return dosis; }
    public void setDosis(String dosis) { this.dosis = dosis; }
    public String getFrecuencia() { return frecuencia; }
    public void setFrecuencia(String frecuencia) { this.frecuencia = frecuencia; }
}