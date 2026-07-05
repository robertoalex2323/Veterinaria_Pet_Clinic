package com.veterinariapetCcinic.veterinaria_pet_clinic.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "ventas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({"mascotas", "pagos", "hibernateLazyInitializer", "handler"})
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({"password", "email", "hibernateLazyInitializer", "handler"})
    private Usuario usuario; 

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "metodo_pago", length = 50)
    private String metodoPago;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_medica_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private RecetaMedica recetaMedica;

    @Column(name = "comprobante_enviado")
    private Boolean comprobanteEnviado = false;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"venta", "hibernateLazyInitializer", "handler"})
    private List<DetalleVenta> detalles = new ArrayList<>();

    public static final BigDecimal IGV_TASA = new BigDecimal("0.18");

    public void addDetalle(DetalleVenta detalle) {
        this.detalles.add(detalle);
        detalle.setVenta(this);
        recalcularTotales();
    }

    public void removeDetalle(DetalleVenta detalle) {
        detalles.remove(detalle);
        detalle.setVenta(null);
        recalcularTotales();
    }

    public void recalcularTotales() {
        this.subtotal = detalles.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        this.igv = this.subtotal.multiply(IGV_TASA).setScale(2, RoundingMode.HALF_UP);
        this.total = this.subtotal.add(this.igv).setScale(2, RoundingMode.HALF_UP);
    }

    public String getFechaFormateada() {
        return fecha != null ? fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }

    public Long getId() { return id; }
    public void setId(Long idVenta) { this.id = idVenta; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario user) { this.usuario = user; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal igv) { this.igv = igv; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal totalAmount) { this.total = totalAmount; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public RecetaMedica getRecetaMedica() { return recetaMedica; }
    public void setRecetaMedica(RecetaMedica recetaMedica) { this.recetaMedica = recetaMedica; }

    public Boolean getComprobanteEnviado() { return comprobanteEnviado; }
    public void setComprobanteEnviado(Boolean comprobanteEnviado) { this.comprobanteEnviado = comprobanteEnviado; }

    public List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVenta> items) { 
        this.detalles = items; 
        recalcularTotales(); 
    }
}