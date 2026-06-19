package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Receta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaRepository;

@Service
public class RecetaService {

    private final RecetaRepository recetaRepository;

    public RecetaService(RecetaRepository repo) {
        this.recetaRepository = repo;
    }

    public List<Receta> listarTodas() {
        return recetaRepository.findAll();
    }

    public List<Receta> listarPendientes() {
        return recetaRepository.findByEstado("PENDIENTE");
    }

    @Transactional
    public void marcarComoEntregada(Long id) {
        Receta receta = recetaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Receta no encontrada"));
        receta.setEstado("ENTREGADA");
        recetaRepository.save(receta);
    }
}