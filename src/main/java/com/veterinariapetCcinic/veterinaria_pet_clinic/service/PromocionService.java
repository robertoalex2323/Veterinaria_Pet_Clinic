package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.PromocionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class PromocionService {

    private final PromocionRepository promocionRepository;

    public PromocionService(PromocionRepository promocionRepository) {
        this.promocionRepository = promocionRepository;
    }

    public List<Promocion> getPromocionesActivas() {
        LocalDate hoy = LocalDate.now();
        List<Promocion> promociones = promocionRepository.findPromocionesActivas(hoy);
        return promociones.stream()
                .filter(this::aplicaDiaSemana)
                .toList();
    }

    // Compatibilidad para controladores existentes
    public List<Promocion> listarActivas() {
        return getPromocionesActivas();
    }


    private boolean aplicaDiaSemana(Promocion promo) {
        if (promo.getDiasSemana() == null || promo.getDiasSemana().isEmpty()) {
            return true;
        }

        int diaActual = LocalDate.now().getDayOfWeek().getValue();
        for (String dia : promo.getDiasSemana().split(",")) {
            try {
                if (Integer.parseInt(dia.trim()) == diaActual) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // ignorar formato inválido
            }
        }
        return false;
    }

    public BigDecimal aplicarPromociones(Venta venta) {
        LocalDate hoy = LocalDate.now();
        DayOfWeek diaSemana = hoy.getDayOfWeek();
        List<Promocion> promociones = promocionRepository.findPromocionesActivas(hoy);

        BigDecimal descuentoTotal = BigDecimal.ZERO;

        for (Promocion promo : promociones) {
            if (promo.getDiasSemana() != null && !promo.getDiasSemana().isEmpty()) {
                String[] dias = promo.getDiasSemana().split(",");
                boolean diaValido = Arrays.stream(dias)
                        .map(Integer::parseInt)
                        .anyMatch(d -> d == diaSemana.getValue());
                if (!diaValido) continue;
            }

            if (promo.getMontoMinimo() != null && 
                venta.getTotal().compareTo(promo.getMontoMinimo()) < 0) {
                continue;
            }

            switch (promo.getTipo().toUpperCase()) {
                case "PORCENTAJE":
                    if (promo.getCategoriaAplicable() != null) {
                        BigDecimal montoCategoria = calcularMontoPorCategoria(venta, promo.getCategoriaAplicable());
                        if (montoCategoria.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal descuento = montoCategoria.multiply(
                                promo.getDescuento().divide(new BigDecimal("100"))
                            );
                            descuentoTotal = descuentoTotal.add(descuento);
                        }
                    } else {
                        BigDecimal descuento = venta.getTotal().multiply(
                            promo.getDescuento().divide(new BigDecimal("100"))
                        );
                        descuentoTotal = descuentoTotal.add(descuento);
                    }
                    break;

                case "2X1":
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        if (detalle.getProducto() != null && detalle.getCantidad() >= 2) {
                            String categoria = detalle.getProducto().getCategoria();
                            if (promo.getCategoriaAplicable() == null || 
                                promo.getCategoriaAplicable().equals(categoria)) {
                                int pares = detalle.getCantidad() / 2;
                                BigDecimal descuento = detalle.getPrecioUnitario().multiply(
                                    BigDecimal.valueOf(pares)
                                );
                                descuentoTotal = descuentoTotal.add(descuento);
                            }
                        }
                    }
                    break;

                case "FIJO":
                    if (promo.getProductoId() != null) {
                        for (DetalleVenta detalle : venta.getDetalles()) {
                            if (detalle.getProducto() != null && 
                                detalle.getProducto().getId().equals(promo.getProductoId())) {
                                descuentoTotal = descuentoTotal.add(promo.getDescuento());
                            }
                        }
                    }
                    break;
            }
        }

        return descuentoTotal;
    }

    private BigDecimal calcularMontoPorCategoria(Venta venta, String categoria) {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle.getProducto() != null && 
                categoria.equals(detalle.getProducto().getCategoria())) {
                total = total.add(detalle.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(detalle.getCantidad())));
            }
        }
        return total;
    }

    public Map<String, Object> getPromocionesAplicadas(Long clienteId) {
        return new HashMap<>();
    }

    public List<Promocion> getTodasPromociones() {
        return promocionRepository.findAll();
    }

    public Promocion getPromocionPorId(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Promoción no encontrada"));
    }

    public Promocion guardarPromocion(Promocion promocion) {
        if (promocion.getActivo() == null) {
            promocion.setActivo(true);
        }
        return promocionRepository.save(promocion);
    }

    public Promocion actualizarPromocion(Long id, Promocion datos) {
        Promocion existente = getPromocionPorId(id);
        existente.setNombre(datos.getNombre());
        existente.setDescripcion(datos.getDescripcion());
        existente.setTipo(datos.getTipo());
        existente.setDescuento(datos.getDescuento());
        existente.setCategoriaAplicable(datos.getCategoriaAplicable());
        existente.setProductoId(datos.getProductoId());
        existente.setMontoMinimo(datos.getMontoMinimo());
        existente.setDiasSemana(datos.getDiasSemana());
        existente.setFechaInicio(datos.getFechaInicio());
        existente.setFechaFin(datos.getFechaFin());
        existente.setActivo(datos.getActivo() != null ? datos.getActivo() : existente.getActivo());
        return promocionRepository.save(existente);
    }

    public void eliminarPromocion(Long id) {
        Promocion promocion = getPromocionPorId(id);
        promocionRepository.delete(promocion);
    }
}