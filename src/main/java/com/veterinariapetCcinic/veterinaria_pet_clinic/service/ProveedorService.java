package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Proveedor;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ProveedorRepository;

@Service
public class ProveedorService {
    
    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository repo) {
        this.proveedorRepository = repo;
    }

    public List<Proveedor> listarTodos() {
        return this.proveedorRepository.findAll();
    }

    @Transactional
    public Proveedor guardar(Proveedor p) {
        return this.proveedorRepository.save(p);
    }

    public Proveedor buscarPorId(Long idProveedor) {
        return this.proveedorRepository.findById(idProveedor)
                .orElseThrow(() -> new RuntimeException("No se encontró el proveedor con el identificador: " + idProveedor));
    }

    @Transactional
    public void eliminar(Long idParaEliminar) {
        if (!this.proveedorRepository.existsById(idParaEliminar)) {
            throw new RuntimeException("No es posible eliminar. Proveedor no encontrado.");
        }
        this.proveedorRepository.deleteById(idParaEliminar);
    }

    public Proveedor buscarPorRuc(String rucIdentificador) {
        return this.proveedorRepository.findByRuc(rucIdentificador).orElse(null);
    }
}
