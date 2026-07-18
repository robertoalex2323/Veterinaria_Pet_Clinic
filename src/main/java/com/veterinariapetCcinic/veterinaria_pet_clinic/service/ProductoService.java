package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarTodos() {
        return productoRepository.findByActivoTrue();
    }


    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    @Transactional
    public void descontarStock(Long productoId, int cantidad) {
        Producto producto = buscarPorId(productoId);
        if (producto.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + producto.getStock());
        }
        producto.descontarStock(cantidad);
        productoRepository.save(producto);
    }

    @Transactional
    public Producto guardar(Producto producto) {
        return productoRepository.save(producto);
    }
    
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }
}
