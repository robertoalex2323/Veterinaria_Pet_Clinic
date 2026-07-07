package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promocion")
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal descuento = BigDecimal.ZERO; // porcentaje: ej 10.00

    @Column(nullable = false)
    private Boolean activa = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Promocion() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Precio oferta calculado para el UI.
     */
    @Transient
    public BigDecimal getPrecioOferta() {
        if (producto == null || producto.getPrecio() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal base = producto.getPrecio();
        BigDecimal pct = (descuento == null ? BigDecimal.ZERO : descuento);
        // oferta = precio * (1 - pct/100)
        return base.multiply(BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP)));
    }

    @Transient
    public String getProductoNombre() {
        return producto != null ? producto.getNombre() : null;
    }

    @Transient
    public String getProductoCategoria() {
        return producto != null ? producto.getCategoria() : null;
    }
}

