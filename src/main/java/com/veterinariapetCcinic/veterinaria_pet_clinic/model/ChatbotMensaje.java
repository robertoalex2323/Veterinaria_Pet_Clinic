package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_mensajes", indexes = {
    @Index(name = "idx_chatbot_session_id", columnList = "sessionId"),
    @Index(name = "idx_chatbot_fecha", columnList = "fecha")
})
public class ChatbotMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String sessionId;

    @Column(nullable = false, length = 30)
    private String rol;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    @Column(length = 60)
    private String intencion;

    @Column(length = 60)
    private String proveedor;

    private Integer confianza;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public ChatbotMensaje() {
        this.fecha = LocalDateTime.now();
    }

    public ChatbotMensaje(String sessionId, String rol, String contenido, String intencion, String proveedor, Integer confianza) {
        this.sessionId = sessionId;
        this.rol = rol;
        this.contenido = contenido;
        this.intencion = intencion;
        this.proveedor = proveedor;
        this.confianza = confianza;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getIntencion() {
        return intencion;
    }

    public void setIntencion(String intencion) {
        this.intencion = intencion;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public Integer getConfianza() {
        return confianza;
    }

    public void setConfianza(Integer confianza) {
        this.confianza = confianza;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
