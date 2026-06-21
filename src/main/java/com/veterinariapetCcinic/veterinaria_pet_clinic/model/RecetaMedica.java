package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;


@Entity
public class RecetaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Paciente paciente;

    @ManyToOne
    private Veterinario veterinario;

    private LocalDate fecha = LocalDate.now();

    @Enumerated(EnumType.STRING)
    private RecetaEstado estado = RecetaEstado.PENDIENTE;

    private String observaciones;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<RecetaItem> items = new ArrayList<>();

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }
    public Veterinario getVeterinario() { return veterinario; }
    public void setVeterinario(Veterinario veterinario) { this.veterinario = veterinario; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public RecetaEstado getEstado() { return estado; }
    public void setEstado(RecetaEstado estado) { this.estado = estado; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public List<RecetaItem> getItems() { return items; }
    public void setItems(List<RecetaItem> items) { this.items = items; }
}