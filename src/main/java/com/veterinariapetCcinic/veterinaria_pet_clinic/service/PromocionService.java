package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.PromocionRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PromocionService {

    private final PromocionRepository promocionRepository;
    private final ProductoRepository productoRepository;

    public PromocionService(PromocionRepository promocionRepository, ProductoRepository productoRepository) {
        this.promocionRepository = promocionRepository;
        this.productoRepository = productoRepository;
    }

    public List<Promocion> listarActivas() {
        return promocionRepository.findByActivaTrue();
    }

    public Promocion buscarPorId(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promoción no encontrada"));
    }

    public Promocion guardar(Long id,
                               Long productoId,
                               BigDecimal descuento,
                               Boolean activa) {

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        Promocion promocion;
        if (id != null) {
            promocion = buscarPorId(id);
        } else {
            promocion = new Promocion();
        }

        promocion.setProducto(producto);
        promocion.setDescuento(descuento != null ? descuento : BigDecimal.ZERO);
        promocion.setActiva(activa != null ? activa : true);

        return promocionRepository.save(promocion);
    }

    @Transactional
    public void eliminar(Long id) {
        // Soft delete: marcamos como inactiva
        Promocion p = buscarPorId(id);
        p.setActiva(false);
        promocionRepository.save(p);
    }

    /**
     * Regla UI pedida: 2 = permitimos múltiples promociones por producto.
     * No hacemos validación de unicidad.
     */
}

