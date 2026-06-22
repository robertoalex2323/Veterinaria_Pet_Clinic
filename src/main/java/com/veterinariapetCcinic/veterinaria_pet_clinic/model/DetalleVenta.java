package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "detalle_ventas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id",nullable = false)
    @JsonIgnoreProperties({"detalles", "hibernateLazyInitializer", "handler"})
    private Venta venta;

    // ===== PARA VENDEDOR: Producto =====
    @ManyToOne
    @JoinColumn(name = "producto_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Producto producto;

    // ===== PARA FARMACEUTICO: Medicamento =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Medicamento medicamento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    public DetalleVenta() {
    }

    public DetalleVenta(Venta v, Medicamento m, Integer cant, BigDecimal prec) {
        this.venta = v;
        this.medicamento = m;
        this.cantidad = cant;
        this.precioUnitario = prec;
        calcularSubtotal();
    }

    public DetalleVenta(Venta v, Producto p, Integer cant, BigDecimal prec) {
        this.venta = v;
        this.producto = p;
        this.cantidad = cant;
        this.precioUnitario = prec;
        calcularSubtotal();
    }

    public BigDecimal calcularImporteTotal() {
        if (cantidad == null || precioUnitario == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public void calcularSubtotal() {
        if (this.precioUnitario != null && this.cantidad != null) {
            this.subtotal = this.precioUnitario.multiply(BigDecimal.valueOf(this.cantidad));
        }
    }

    // ===== GETTERS Y SETTERS =====
    public Long getId() {
        return id;
    }

    public void setId(Long identifier) {
        this.id = identifier;
    }

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta v) {
        this.venta = v;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
        if (producto != null && producto.getPrecio() != null) {
            this.precioUnitario = producto.getPrecio();
            calcularSubtotal();
        }
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(Medicamento med) {
        this.medicamento = med;
        if (med != null && med.getPrecio() != null) {
            this.precioUnitario = med.getPrecio();
            calcularSubtotal();
        }
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cant) {
        this.cantidad = cant;
        calcularSubtotal();
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal p) {
        this.precioUnitario = p;
        calcularSubtotal();
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}