package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Consulta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.SignosVitales;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ConsultaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.SignosVitalesRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class SignosVitalesService {

    private final SignosVitalesRepository signosVitalesRepository;
    private final MascotaRepository mascotaRepository;
    private final ConsultaRepository consultaRepository;

    public SignosVitalesService(SignosVitalesRepository signosVitalesRepository,
                                MascotaRepository mascotaRepository,
                                ConsultaRepository consultaRepository) {
        this.signosVitalesRepository = signosVitalesRepository;
        this.mascotaRepository = mascotaRepository;
        this.consultaRepository = consultaRepository;
    }

    // ==================== CREAR / GUARDAR ====================

    @Transactional
    public SignosVitales guardar(SignosVitales signosVitales) {
        return signosVitalesRepository.save(signosVitales);
    }

    @Transactional
    public SignosVitales crearSignosVitales(Long mascotaId, Long consultaId,
                                            Double peso, Double temperatura,
                                            Integer frecuenciaCardiaca, Integer frecuenciaRespiratoria,
                                            String observaciones) {
        Mascota mascota = null;
        if (mascotaId != null) {
            mascota = mascotaRepository.findById(mascotaId)
                    .orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada ID: " + mascotaId));
        }

        Consulta consulta = null;
        if (consultaId != null) {
            consulta = consultaRepository.findById(consultaId)
                    .orElseThrow(() -> new EntityNotFoundException("Consulta no encontrada ID: " + consultaId));
        }

        SignosVitales sv = new SignosVitales();
        sv.setMascota(mascota);
        sv.setConsulta(consulta);
        sv.setPeso(peso);
        sv.setTemperatura(temperatura);
        sv.setFrecuenciaCardiaca(frecuenciaCardiaca);
        sv.setFrecuenciaRespiratoria(frecuenciaRespiratoria);
        sv.setObservaciones(observaciones);
        sv.setFechaRegistro(LocalDateTime.now());

        return signosVitalesRepository.save(sv);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public SignosVitales actualizar(SignosVitales actualizado) {
        SignosVitales existente = buscarPorId(actualizado.getId());

        existente.setPeso(actualizado.getPeso());
        existente.setTemperatura(actualizado.getTemperatura());
        existente.setFrecuenciaCardiaca(actualizado.getFrecuenciaCardiaca());
        existente.setFrecuenciaRespiratoria(actualizado.getFrecuenciaRespiratoria());
        existente.setObservaciones(actualizado.getObservaciones());
        // No se actualizan mascota, consulta ni fechaRegistro (se mantiene la original)
        // Si se necesita cambiar fechaRegistro, se puede agregar manualmente

        return signosVitalesRepository.save(existente);
    }

    // ==================== CONSULTAS ====================

    public SignosVitales buscarPorId(Long id) {
        return signosVitalesRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Signos vitales no encontrados con ID: " + id));
    }

    public List<SignosVitales> listarTodos() {
        return signosVitalesRepository.findAll();
    }

    public List<SignosVitales> buscarPorMascota(Long mascotaId) {
        return signosVitalesRepository.findByMascotaId(mascotaId);
    }

    public List<SignosVitales> buscarPorConsulta(Long consultaId) {
        return signosVitalesRepository.findByConsultaId(consultaId);
    }

    public List<SignosVitales> buscarPorRangoFechas(LocalDateTime inicio, LocalDateTime fin) {
        return signosVitalesRepository.findByFechaRegistroBetween(inicio, fin);
    }

    public List<SignosVitales> ultimosRegistrosDeMascota(Long mascotaId, int limite) {
        // Por simplicidad usamos top5; si deseas límite variable, puedes escribir una consulta personalizada en el repositorio.
        return signosVitalesRepository.findTop5ByMascotaIdOrderByFechaRegistroDesc(mascotaId);
    }

    // ==================== ELIMINAR ====================

    @Transactional
    public void eliminar(Long id) {
        if (!signosVitalesRepository.existsById(id)) {
            throw new EntityNotFoundException("Signos vitales no encontrados con ID: " + id);
        }
        signosVitalesRepository.deleteById(id);
    }
}

