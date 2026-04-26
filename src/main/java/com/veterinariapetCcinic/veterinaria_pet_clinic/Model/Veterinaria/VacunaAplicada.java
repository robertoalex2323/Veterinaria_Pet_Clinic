package com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Veterinaria;


import java.time.LocalDate;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vacunas_aplicadas")
public class VacunaAplicada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "mascota_id")
    private Mascota mascota;

    @ManyToOne
    @JoinColumn(name = "vacuna_id")
    private Vacuna vacuna;

    @ManyToOne
    @JoinColumn(name = "veterinario_id")
    private Usuario veterinario;

    private LocalDate fechaAplicacion;
    private LocalDate proximaDosis;

    @Column(columnDefinition = "TEXT")
    private String observaciones;
    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Mascota getMascota() { return mascota; }
    public void setMascota(Mascota mascota) { this.mascota = mascota; }

    public Vacuna getVacuna() { return vacuna; }
    public void setVacuna(Vacuna vacuna) { this.vacuna = vacuna; }

    public Usuario getVeterinario() { return veterinario; }
    public void setVeterinario(Usuario veterinario) { this.veterinario = veterinario; }

    public LocalDate getFechaAplicacion() { return fechaAplicacion; }
    public void setFechaAplicacion(LocalDate fechaAplicacion) { this.fechaAplicacion = fechaAplicacion; }

    public LocalDate getProximaDosis() { return proximaDosis; }
    public void setProximaDosis(LocalDate proximaDosis) { this.proximaDosis = proximaDosis; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}