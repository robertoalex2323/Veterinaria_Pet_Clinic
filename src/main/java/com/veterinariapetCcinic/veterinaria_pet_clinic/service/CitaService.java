package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.config.AppProperties;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.CitaRepository;

import java.time.ZoneId;

@Service
public class CitaService {
    
    private static final Logger log = LoggerFactory.getLogger(CitaService.class);

    private final CitaRepository citaRepository;
    private final NotificacionService notificacionService;
    private final AgendaService agendaService;
    private final AppProperties appProperties;

    public CitaService(CitaRepository citaRepository, NotificacionService notificacionService, AgendaService agendaService, AppProperties appProperties) {
        this.citaRepository = citaRepository;
        this.notificacionService = notificacionService;
        this.agendaService = agendaService;
        this.appProperties = appProperties;
    }
    
    @Transactional
    public Cita guardar(Cita cita) {


        if (cita.getMascota() != null && cita.getMascota().getId() != null
        && citaRepository.existsByMascotaIdAndFechaHoraAndEstadoNot(
                cita.getMascota().getId(),
                cita.getFechaHora(),
                "CANCELADA")) {
    throw new RuntimeException("Ya existe una cita registrada para esta mascota en ese horario.");
}

        Objects.requireNonNull(cita, "La cita no puede ser nula");

        Long vetId = cita.getVeterinario() != null ? cita.getVeterinario().getId() : null;
        com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda agenda = validarDisponibilidad(cita.getFechaHora(), vetId);
        if (cita.getVeterinario() == null && agenda != null && agenda.getVeterinario() != null) {
            // Si no se eligió veterinario, se asigna el del turno de la agenda
            cita.setVeterinario(agenda.getVeterinario());
        }

        // Bloquear horario en la agenda automáticamente
        if (agenda != null) {
            agendaService.bloquearHorario(agenda.getId());
        }
        
        Cita citaGuardada = Objects.requireNonNull(citaRepository.save(cita));
        log.info("Cita agendada exitosamente: ID {} para mascota {}", citaGuardada.getId(), citaGuardada.getMascota().getNombre());
        notificacionService.enviarConfirmacionCita(citaGuardada);
        if (citaGuardada.getVeterinario() != null) {
            notificacionService.enviarNotificacionVeterinario(citaGuardada);
        }
        return citaGuardada;
    }
    
    @Transactional
    public Cita actualizar(Cita cita) {
        Objects.requireNonNull(cita, "La cita no puede ser nula");
        return Objects.requireNonNull(citaRepository.save(cita));
    }
    
    @Transactional
    public Cita confirmarCita(Long id) {
        Cita cita = buscarPorId(id);
        cita.setEstado("CONFIRMADA");
        log.info("Cita ID {} confirmada por el veterinario", id);
        notificacionService.enviarNotificacionVeterinario(cita);
        return Objects.requireNonNull(citaRepository.save(cita));
    }

    @Transactional
    public Cita marcarEnSalaDeEspera(Long id) {
        Cita cita = buscarPorId(id);
        if (!"AGENDADA".equalsIgnoreCase(cita.getEstado())
                && !"CONFIRMADA".equalsIgnoreCase(cita.getEstado())) {
            throw new IllegalStateException("Solo se pueden registrar en sala citas agendadas o confirmadas.");
        }
        cita.setEstado("EN_ESPERA");
        log.info("Cita ID {} registrada en sala de espera", id);
        return Objects.requireNonNull(citaRepository.save(cita));
    }
    
    @Transactional
    public void cancelarCita(Long id, String motivo) {
        Cita cita = buscarPorId(id);
        cita.setEstado("CANCELADA");
        cita.setObservaciones("Cancelada: " + motivo);
        Objects.requireNonNull(citaRepository.save(cita));
        log.warn("Cita ID {} cancelada. Motivo: {}", id, motivo);
        
        // Liberar horario en la agenda automáticamente
        com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda agenda = agendaService.buscarAgendaPorFechaYHora(cita.getFechaHora().toLocalDate(), cita.getFechaHora().toLocalTime(),
                cita.getVeterinario() != null ? cita.getVeterinario().getId() : null);
        if (agenda != null) {
            agendaService.liberarHorario(agenda.getId());
        }
        
        notificacionService.enviarCancelacionCita(cita);
    }
    
    public Cita buscarPorId(Long id) {
        return citaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cita no encontrada con ID: " + id));
    }
    
    public List<Cita> listarTodas() {
        return citaRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Cita> obtenerCitasDelDia(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.atTime(LocalTime.MAX);
        
        List<Cita> citas = citaRepository.findByFechaHoraBetween(inicio, fin);
        
        for (Cita cita : citas) {
            // Inicializar mascota y sus relaciones
            if (cita.getMascota() != null) {
                cita.getMascota().getNombre();
                cita.getMascota().getEspecie();
                cita.getMascota().getRaza();
                
                // Inicializar cliente
                if (cita.getMascota().getCliente() != null) {
                    cita.getMascota().getCliente().getNombre();
                    cita.getMascota().getCliente().getTelefono();
                    cita.getMascota().getCliente().getEmail();
                }
            }
            
            // Inicializar veterinario
            if (cita.getVeterinario() != null) {
                cita.getVeterinario().getNombre();
                cita.getVeterinario().getEmail();
            }
            
            // Inicializar otros campos
            cita.getMotivo();
            cita.getEstado();
            if (cita.getObservaciones() == null) {
                cita.setObservaciones("");
            }
            if (cita.getMotivo() == null) {
                cita.setMotivo("");
            }
        }
        
        return citas;
    }

    
    public List<Cita> obtenerCitasPorEstado(String estado) {
        return citaRepository.findByEstado(estado);
    }
    
    public List<Cita> obtenerCitasPorCliente(Long clienteId) {
        return citaRepository.findCitasByClienteId(clienteId);
    }
    
    public List<Cita> obtenerCitasPendientes() {
        return citaRepository.findCitasPendientes(LocalDateTime.now());
    }
    
    public List<Cita> listarCitasPendientesPago() {
        return citaRepository.findByEstado("ATENDIDA");
    }
    
    private com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda validarDisponibilidad(LocalDateTime fechaHora, Long vetId) {
        // 1. Validar horarios lógicos (no pasado)
        if (fechaHora.isBefore(LocalDateTime.now(ZoneId.of("America/Lima")))) {
            log.error("Intento de agendamiento fallido: Fecha pasada {}", fechaHora);
            throw new RuntimeException("No se pueden agendar citas en fechas u horas del pasado.");
        }
        
        // 2. Validar el horario de atención 
        LocalTime start = LocalTime.parse(appProperties.getBusiness().getStartTime());
        LocalTime end = LocalTime.parse(appProperties.getBusiness().getEndTime());
        LocalTime hora = fechaHora.toLocalTime();
        if (hora.isBefore(start) || hora.isAfter(end)) {
            throw new RuntimeException(String.format("La cita debe estar dentro del horario de atención (%s - %s).",
                appProperties.getBusiness().getStartTime(), 
                appProperties.getBusiness().getEndTime()));
        }
        
        // 3. Vincular con Agenda 
        com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda agenda = agendaService.buscarAgendaDisponible(fechaHora.toLocalDate(), hora, vetId);
        if (agenda == null) {
            log.warn("Horario no disponible en agenda para: {} (vet {})", fechaHora, vetId);
            throw new RuntimeException("El horario seleccionado no existe en la agenda del veterinario o ya no está disponible.");
        }
        Long vetAgenda = agenda.getVeterinario() != null ? agenda.getVeterinario().getId() : null;

        // Inteligencia: Validar cruces usando la duración REAL del turno definido en la agenda
        long duracion = ChronoUnit.MINUTES.between(agenda.getHoraInicio(), agenda.getHoraFin());
        LocalDateTime inicio = fechaHora.minusMinutes(duracion - 1);
        LocalDateTime fin = fechaHora.plusMinutes(duracion - 1);
        
        // Mejorar: Validar contra AGENDADA y CONFIRMADA
        List<String> estadosOcupados = List.of("AGENDADA", "CONFIRMADA");
        // El cruce se valida por veterinario: dos veterinarios pueden atender a la misma hora
        long cantidad = vetAgenda != null
                ? citaRepository.countByVeterinarioIdAndFechaHoraBetweenAndEstadoIn(vetAgenda, inicio, fin, estadosOcupados)
                : citaRepository.countByFechaHoraBetweenAndEstadoIn(inicio, fin, estadosOcupados);

        if (cantidad > 0) {
            log.warn("Intento de agendamiento fallido: Cruce en {} para una consulta de {} min", fechaHora, duracion);
            throw new RuntimeException("Horario ocupado por otra cita confirmada o agendada (" + duracion + " min).");
        }
        return agenda;
    }
    
    public long contarCitasHoy() {
        return obtenerCitasDelDia(LocalDate.now()).size();
    }
    
    public long contarCitasConfirmadas() {
        return citaRepository.findByEstado("CONFIRMADA").size();
    }
    
    @Transactional
    public void eliminar(Long id) {
        citaRepository.deleteById(id);
    }

    @Transactional
    public Cita reprogramarCita(Long citaId, LocalDateTime nuevaFechaHora, String motivoReprogramacion) {
        Cita citaOriginal = buscarPorId(citaId);

        // 1. Cancelar la cita original (libera la agenda automáticamente)
        cancelarCita(citaId, "Reprogramada: " + motivoReprogramacion);

        // 2. Crear la nueva cita con los mismos datos básicos
        Cita nuevaCita = new Cita();
        nuevaCita.setMascota(citaOriginal.getMascota());
        nuevaCita.setVeterinario(citaOriginal.getVeterinario());
        nuevaCita.setMotivo(citaOriginal.getMotivo());
        nuevaCita.setFechaHora(nuevaFechaHora);
        nuevaCita.setObservaciones("Reprogramada desde cita ID " + citaId + ". Motivo: " + motivoReprogramacion);

        // 3. Guardar (pasa por validación de disponibilidad, bloqueo de agenda, notificaciones)
        Cita nuevaGuardada = guardar(nuevaCita);

        // Correo adicional específico de reprogramación (sin tocar los ya existentes)
        notificacionService.enviarReprogramacionCita(citaOriginal, nuevaGuardada, motivoReprogramacion);

        return nuevaGuardada;
    }

}
