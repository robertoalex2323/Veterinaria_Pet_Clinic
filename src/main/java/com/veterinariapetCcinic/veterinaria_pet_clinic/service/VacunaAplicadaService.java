package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Vacuna;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.VacunaAplicada;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VacunaAplicadaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VacunaRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class VacunaAplicadaService {

    private final VacunaAplicadaRepository vacunaAplicadaRepository;
    private final MascotaRepository mascotaRepository;
    private final VacunaRepository vacunaRepository;
    private final UsuarioRepository usuarioRepository;

    public VacunaAplicadaService(VacunaAplicadaRepository vacunaAplicadaRepository,
                                 MascotaRepository mascotaRepository,
                                 VacunaRepository vacunaRepository,
                                 UsuarioRepository usuarioRepository) {
        this.vacunaAplicadaRepository = vacunaAplicadaRepository;
        this.mascotaRepository = mascotaRepository;
        this.vacunaRepository = vacunaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ==================== CREAR / GUARDAR ====================

    @Transactional
    public VacunaAplicada guardar(VacunaAplicada vacunaAplicada) {
        validarFechas(vacunaAplicada.getFechaAplicacion(), vacunaAplicada.getProximaDosis());
        return vacunaAplicadaRepository.save(vacunaAplicada);
    }

    @Transactional
    public VacunaAplicada registrarAplicacion(Long mascotaId, Long vacunaId, Long veterinarioId,
                                              LocalDate fechaAplicacion, LocalDate proximaDosis,
                                              String observaciones) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new EntityNotFoundException("Mascota no encontrada ID: " + mascotaId));
        Vacuna vacuna = vacunaRepository.findById(vacunaId)
                .orElseThrow(() -> new EntityNotFoundException("Vacuna no encontrada ID: " + vacunaId));
        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .orElseThrow(() -> new EntityNotFoundException("Veterinario no encontrado ID: " + veterinarioId));

        validarFechas(fechaAplicacion, proximaDosis);
        // Opcional: evitar duplicados recientes (misma vacuna en los últimos 30 días)
        if (vacunaAplicadaRepository.existsByMascotaIdAndVacunaIdAndFechaAplicacionAfter(mascotaId, vacunaId, fechaAplicacion.minusDays(30))) {
            throw new IllegalStateException("La mascota ya recibió esta vacuna en los últimos 30 días");
        }

        VacunaAplicada va = new VacunaAplicada();
        va.setMascota(mascota);
        va.setVacuna(vacuna);
        va.setVeterinario(veterinario);
        va.setFechaAplicacion(fechaAplicacion);
        va.setProximaDosis(proximaDosis);
        va.setObservaciones(observaciones);
        return vacunaAplicadaRepository.save(va);
    }

    // ==================== ACTUALIZAR ====================

    @Transactional
    public VacunaAplicada actualizar(VacunaAplicada actualizada) {
        VacunaAplicada existente = buscarPorId(actualizada.getId());
        validarFechas(actualizada.getFechaAplicacion(), actualizada.getProximaDosis());

        existente.setFechaAplicacion(actualizada.getFechaAplicacion());
        existente.setProximaDosis(actualizada.getProximaDosis());
        existente.setObservaciones(actualizada.getObservaciones());
        // No se actualizan mascota, vacuna ni veterinario (se asume que no cambian)
        return vacunaAplicadaRepository.save(existente);
    }

    // ==================== CONSULTAS ====================

    public VacunaAplicada buscarPorId(Long id) {
        return vacunaAplicadaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Vacuna aplicada no encontrada ID: " + id));
    }

    public List<VacunaAplicada> listarTodas() {
        return vacunaAplicadaRepository.findAll();
    }

    public List<VacunaAplicada> buscarPorMascota(Long mascotaId) {
        return vacunaAplicadaRepository.findByMascotaId(mascotaId);
    }

    public List<VacunaAplicada> buscarPorVacuna(Long vacunaId) {
        return vacunaAplicadaRepository.findByVacunaId(vacunaId);
    }

    public List<VacunaAplicada> buscarPorVeterinario(Long veterinarioId) {
        return vacunaAplicadaRepository.findByVeterinarioId(veterinarioId);
    }

    public List<VacunaAplicada> buscarPorRangoFechasAplicacion(LocalDate inicio, LocalDate fin) {
        return vacunaAplicadaRepository.findByFechaAplicacionBetween(inicio, fin);
    }

    public List<VacunaAplicada> buscarProximasDosisVencidas(LocalDate fechaReferencia) {
        return vacunaAplicadaRepository.findByProximaDosisBefore(fechaReferencia);
    }

    public List<VacunaAplicada> historialCompletoMascota(Long mascotaId) {
        return vacunaAplicadaRepository.findByMascotaIdOrderByFechaAplicacionDesc(mascotaId);
    }

    // ==================== ELIMINAR ====================

    @Transactional
    public void eliminar(Long id) {
        if (!vacunaAplicadaRepository.existsById(id)) {
            throw new EntityNotFoundException("Vacuna aplicada no encontrada ID: " + id);
        }
        vacunaAplicadaRepository.deleteById(id);
    }

    // ==================== VALIDACIONES PRIVADAS ====================

    private void validarFechas(LocalDate fechaAplicacion, LocalDate proximaDosis) {
        if (fechaAplicacion == null) {
            throw new IllegalArgumentException("La fecha de aplicación es obligatoria");
        }
        if (fechaAplicacion.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de aplicación no puede ser futura");
        }
        if (proximaDosis != null && proximaDosis.isBefore(fechaAplicacion)) {
            throw new IllegalArgumentException("La próxima dosis no puede ser anterior a la fecha de aplicación");
        }
    }
}
