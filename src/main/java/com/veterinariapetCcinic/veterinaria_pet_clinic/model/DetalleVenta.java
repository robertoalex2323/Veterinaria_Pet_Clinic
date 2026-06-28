package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import java.math.BigDecimal;

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
public class DetalleVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id")
    private Medicamento medicamento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    public DetalleVenta() {
    }

    public DetalleVenta(Venta v, Medicamento m, Integer cant, BigDecimal prec) {
        this.venta = v;
        this.medicamento = m;
        this.cantidad = cant;
        this.precioUnitario = prec;
    }

    public BigDecimal calcularImporteTotal() {
        if (cantidad == null || precioUnitario == null) {
            return BigDecimal.ZERO;
        }
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

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

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public void setMedicamento(Medicamento med) {
        this.medicamento = med;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cant) {
        this.cantidad = cant;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal p) {
        this.precioUnitario = p;
    }
}