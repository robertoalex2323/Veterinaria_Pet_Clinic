package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;

@Service
public class MedicamentoService {

    private final MedicamentoRepository medicamentoRepository;

    public MedicamentoService(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }

    public List<Medicamento> listarTodos() {
        return medicamentoRepository.findAll();
    }

    public List<Medicamento> listarBajoStock() {
        return medicamentoRepository.findBajoStock();
    }

    @Transactional
    public Medicamento guardar(Medicamento med) {
        return medicamentoRepository.save(med);
    }

    public Medicamento buscarPorId(Long id) {
        return medicamentoRepository.findById(id).orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));
    }

    public void eliminar(Long id) {
        medicamentoRepository.deleteById(id);
    }
}