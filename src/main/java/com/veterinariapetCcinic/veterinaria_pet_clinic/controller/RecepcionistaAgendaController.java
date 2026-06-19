package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Agenda;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.AgendaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;

@Controller
@RequestMapping("/recepcionista/agenda")
public class RecepcionistaAgendaController {

    private static final Logger log = LoggerFactory.getLogger(RecepcionistaAgendaController.class);

    private final AgendaService agendaService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public RecepcionistaAgendaController(AgendaService agendaService, UsuarioRepository usuarioRepository, NotificacionService notificacionService) {
        this.agendaService = agendaService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    @GetMapping("/horarios")
    public String horarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {
        if (fecha == null) {
            fecha = LocalDate.now();
        }

        List<Agenda> agendas = agendaService.obtenerHorariosDelDia(fecha);
        agendas.sort(Comparator.comparing(Agenda::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Usuario> veterinarios = usuarioRepository.findByRolIgnoreCase("VETERINARIO").stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .sorted(Comparator.comparing(Usuario::getNombre, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        model.addAttribute("fecha", fecha);
        model.addAttribute("agendas", agendas);
        model.addAttribute("veterinarios", veterinarios);
        model.addAttribute("currentPage", "horarios");
        return "Recepcionista/agenda-horarios";
    }

    @PostMapping("/horarios/generar")
    public String generarHorarios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam Integer duracionTurno,
            @RequestParam(required = false) Long veterinarioId,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (duracionTurno == null || duracionTurno <= 0) {
                throw new IllegalArgumentException("La duración del turno debe ser mayor a 0.");
            }
            // 1. Validaciones de Inteligencia de Negocio
            if (fecha.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("No se pueden generar horarios para una fecha pasada.");
            }
            if (fecha.getDayOfWeek() == DayOfWeek.SUNDAY) {
                throw new IllegalArgumentException("La veterinaria no atiende los días domingos.");
            }
            if (!horaFin.isAfter(horaInicio)) {
                throw new IllegalArgumentException("La hora fin debe ser mayor a la hora inicio.");
            }
            if (duracionTurno < 15) {
                throw new IllegalArgumentException("La duración mínima permitida es de 15 minutos.");
            }
            long minutosTotales = ChronoUnit.MINUTES.between(horaInicio, horaFin);
            if (minutosTotales < duracionTurno) {
                throw new IllegalArgumentException("El rango horario es menor a la duración del turno.");
            }
            // Inteligencia: Validamos que el bloque total encaje con la duración específica de esta cita
            if (minutosTotales % duracionTurno != 0) {
                throw new IllegalArgumentException("El tiempo total debe ser divisible exactamente por la duración de la cita (" + duracionTurno + " min).");
            }

            Usuario veterinario = null;
            if (veterinarioId != null) {
                veterinario = usuarioRepository.findById(veterinarioId)
                        .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado."));
                if (!"VETERINARIO".equalsIgnoreCase(veterinario.getRol())) {
                    throw new IllegalArgumentException("El usuario seleccionado no es veterinario.");
                }
            }

            log.info("Generando bloques para el día {} de {} a {} ({} min) - Vet ID: {}", 
                     fecha, horaInicio, horaFin, duracionTurno, veterinarioId);

            LocalTime cursor = horaInicio;
            int creados = 0;
            int actualizados = 0;

            while (!cursor.plusMinutes(duracionTurno).isAfter(horaFin)) {
                LocalTime slotFin = cursor.plusMinutes(duracionTurno);

                // Inteligencia: Evitar generar horarios que ya pasaron si la fecha es hoy
                if (fecha.isEqual(LocalDate.now()) && cursor.isBefore(LocalTime.now())) {
                    cursor = slotFin;
                    continue;
                }

                Agenda existente = agendaService.buscarAgendaPorFechaYHora(fecha, cursor);

                if (existente == null) {
                    Agenda nueva = new Agenda();
                    nueva.setFecha(fecha);
                    nueva.setHoraInicio(cursor);
                    nueva.setHoraFin(slotFin);
                    nueva.setDuracionTurno(duracionTurno);
                    nueva.setDisponible(true);
                    nueva.setVeterinario(veterinario);
                    agendaService.guardar(nueva);
                    creados++;
                } else if (Boolean.TRUE.equals(existente.getDisponible()) && existente.getVeterinario() == null && veterinario != null) {
                    // Si el bloque existe y está libre, asignamos al veterinario solicitado
                    existente.setVeterinario(veterinario);
                    agendaService.guardar(existente);
                    actualizados++;
                }

                cursor = slotFin; // El sistema salta automáticamente al siguiente bloque calculado
            }

            log.info("Proceso terminado. Creados: {}, Actualizados: {}", creados, actualizados);
            redirectAttributes.addFlashAttribute("success", 
                String.format("Agenda actualizada para el %s. Bloques nuevos: %d, Asignados: %d", fecha, creados, actualizados));

        } catch (Exception e) {
            log.error("Error al generar horarios: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/agenda/horarios?fecha=" + fecha;
    }

    @PostMapping("/horarios/bloquear/{id}")
    public String bloquear(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirectAttributes) {
        try {
            agendaService.bloquearHorario(id);
            redirectAttributes.addFlashAttribute("success", "Horario bloqueado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/agenda/horarios?fecha=" + fecha;
    }

    @PostMapping("/horarios/liberar/{id}")
    public String liberar(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirectAttributes) {
        try {
            agendaService.liberarHorario(id);
            redirectAttributes.addFlashAttribute("success", "Horario liberado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/agenda/horarios?fecha=" + fecha;
    }

    @GetMapping("/api/disponibles")
    public ResponseEntity<List<Map<String, String>>> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<Map<String, String>> payload = agendaService.obtenerHorariosDisponibles(fecha).stream()
                .filter(a -> a.getVeterinario() != null)
                .sorted(Comparator.comparing(Agenda::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> Map.of(
                        "id", String.valueOf(a.getId()),
                        "horaInicio", a.getHoraInicio() != null ? a.getHoraInicio().format(formatter) : "",
                        "horaFin", a.getHoraFin() != null ? a.getHoraFin().format(formatter) : "",
                        "veterinarioId", a.getVeterinario() != null && a.getVeterinario().getId() != null
                                ? String.valueOf(a.getVeterinario().getId())
                                : "",
                        "veterinarioNombre", a.getVeterinario() != null && a.getVeterinario().getNombre() != null
                                ? a.getVeterinario().getNombre()
                                : ""))
                .toList();
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/api/ui-notifications")
    @ResponseBody
    public List<NotificacionService.UINotification> getNotifications() {
        return notificacionService.getAndClearUINotifications();
    }
}
