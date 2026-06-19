package com.veterinariapetCcinic.veterinaria_pet_clinic.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "proveedores")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String ruc;
    private String contacto;
    private String telefono;
    private String email;
    private String direccion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String valorNombre) {
        this.nombre = valorNombre;
    }

    public String getRuc() {
        return this.ruc;
    }

    public void setRuc(String identificacionRuc) {
        this.ruc = identificacionRuc;
    }

    public String getContacto() {
        return this.contacto;
    }

    public void setContacto(String personaContacto) {
        this.contacto = personaContacto;
    }

    public String getTelefono() {
        return this.telefono;
    }

    public void setTelefono(String numeroTel) {
        this.telefono = numeroTel;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }
}