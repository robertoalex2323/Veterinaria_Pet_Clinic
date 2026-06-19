package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Vacuna;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VacunaRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class VacunaService {

    private final VacunaRepository vacunaRepository;

    public VacunaService(VacunaRepository vacunaRepository) {
        this.vacunaRepository = vacunaRepository;
    }

    // ==================== CREAR / GUARDAR ====================

    @Transactional
    public Vacuna guardar(Vacuna vacuna) {
        // Opcional: evitar nombres duplicados
        if (vacunaRepository.existsByNombre(vacuna.getNombre())) {
            throw new IllegalArgumentException("Ya existe una vacuna con el nombre: " + vacuna.getNombre());
        }
        return vacunaRepository.save(vacuna);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public Vacuna actualizar(Vacuna vacunaActualizada) {
        Vacuna existente = buscarPorId(vacunaActualizada.getId());
        
        // Si el nombre cambió, verificar que no haya duplicado
        if (!existente.getNombre().equalsIgnoreCase(vacunaActualizada.getNombre()) &&
            vacunaRepository.existsByNombre(vacunaActualizada.getNombre())) {
            throw new IllegalArgumentException("Ya existe otra vacuna con el nombre: " + vacunaActualizada.getNombre());
        }
        
        existente.setNombre(vacunaActualizada.getNombre());
        return vacunaRepository.save(existente);
    }

    // ==================== CONSULTAS ====================

    public Vacuna buscarPorId(Long id) {
        return vacunaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vacuna no encontrada con ID: " + id));
    }

    public Vacuna buscarPorNombreExacto(String nombre) {
        return vacunaRepository.findByNombre(nombre)
                .orElseThrow(() -> new EntityNotFoundException("Vacuna no encontrada con nombre: " + nombre));
    }

    public List<Vacuna> buscarPorNombreConteniendo(String texto) {
        return vacunaRepository.findByNombreContainingIgnoreCase(texto);
    }

    public List<Vacuna> listarTodas() {
        return vacunaRepository.findAll();
    }

    public boolean existeVacunaConNombre(String nombre) {
        return vacunaRepository.existsByNombre(nombre);
    }

    // ==================== ELIMINAR ====================

    @Transactional
    public void eliminar(Long id) {
        if (!vacunaRepository.existsById(id)) {
            throw new EntityNotFoundException("Vacuna no encontrada con ID: " + id);
        }
        vacunaRepository.deleteById(id);
    }
}

