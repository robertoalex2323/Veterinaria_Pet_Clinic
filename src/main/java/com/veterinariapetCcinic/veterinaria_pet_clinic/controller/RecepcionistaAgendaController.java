package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.AgendaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;

@Controller
@RequestMapping("/recepcionista/agenda")
public class RecepcionistaAgendaController {

    private static final Logger log = LoggerFactory.getLogger(RecepcionistaAgendaController.class);
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DD_MM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private final AgendaService agendaService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public RecepcionistaAgendaController(AgendaService agendaService, UsuarioRepository usuarioRepository, NotificacionService notificacionService) {
        this.agendaService = agendaService;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    private List<Usuario> veterinariosActivos() {
        return usuarioRepository.findByRolIgnoreCase("VETERINARIO").stream()
                .filter(u -> Boolean.TRUE.equals(u.getActivo()))
                .sorted(Comparator.comparing(Usuario::getNombre, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void notificar(RedirectAttributes ra, String tipo, String mensaje) {
        ra.addFlashAttribute("error".equals(tipo) ? "error" : "success", mensaje);
        if (!"error".equals(tipo)) {
            try {
                notificacionService.enviarNotificacionUI(tipo, mensaje);
            } catch (Exception e) {
                log.warn("No se pudo difundir la notificación: {}", e.getMessage());
            }
        }
    }

    private static String redirectHorarios(LocalDate fecha) {
        return "redirect:/recepcionista/agenda/horarios" + (fecha != null ? "?fecha=" + fecha : "");
    }

    @GetMapping("/horarios")
    public String horarios(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {
        if (fecha == null) {
            fecha = LocalDate.now();
        }

        List<Usuario> veterinarios = veterinariosActivos();

        // ---- Turnos del día, agrupados por veterinario ----
        List<Agenda> agendasDia = new ArrayList<>(agendaService.obtenerHorariosDelDia(fecha));
        agendasDia.sort(Comparator.comparing(Agenda::getHoraInicio, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<String, List<Agenda>> turnosPorVet = new LinkedHashMap<>();
        for (Usuario v : veterinarios) {
            turnosPorVet.put(v.getNombre(), new ArrayList<>());
        }
        for (Agenda a : agendasDia) {
            String nombre = a.getVeterinario() != null ? a.getVeterinario().getNombre() : "Sin veterinario asignado";
            turnosPorVet.computeIfAbsent(nombre, k -> new ArrayList<>()).add(a);
        }
        turnosPorVet.entrySet().removeIf(e -> e.getValue().isEmpty());

        long libresDia = agendasDia.stream().filter(a -> Boolean.TRUE.equals(a.getDisponible())).count();

        // ---- Vista semanal: horario de cada veterinario (lunes a domingo) ----
        LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LocalDate> diasSemana = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            diasSemana.add(lunes.plusDays(i));
        }
        List<Agenda> agendasSemana = agendaService.obtenerAgendaSemanal(lunes);

        List<Map<String, Object>> semana = new ArrayList<>();
        for (Usuario v : veterinarios) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("veterinario", v);
            List<Map<String, Object>> celdas = new ArrayList<>();
            int totalSemana = 0;
            for (LocalDate d : diasSemana) {
                List<Agenda> delDia = agendasSemana.stream()
                        .filter(a -> d.equals(a.getFecha()) && a.getVeterinario() != null
                                && Objects.equals(a.getVeterinario().getId(), v.getId()))
                        .sorted(Comparator.comparing(Agenda::getHoraInicio))
                        .toList();
                Map<String, Object> celda = new LinkedHashMap<>();
                celda.put("fecha", d);
                celda.put("total", delDia.size());
                celda.put("libres", delDia.stream().filter(a -> Boolean.TRUE.equals(a.getDisponible())).count());
                celda.put("rango", delDia.isEmpty() ? "" : resumirRangos(delDia));
                celdas.add(celda);
                totalSemana += delDia.size();
            }
            fila.put("celdas", celdas);
            fila.put("totalSemana", totalSemana);
            semana.add(fila);
        }

        model.addAttribute("fecha", fecha);
        model.addAttribute("hoy", LocalDate.now());
        model.addAttribute("agendas", agendasDia);
        model.addAttribute("turnosPorVet", turnosPorVet);
        model.addAttribute("libresDia", libresDia);
        model.addAttribute("ocupadosDia", agendasDia.size() - libresDia);
        model.addAttribute("veterinarios", veterinarios);
        model.addAttribute("diasSemana", diasSemana);
        model.addAttribute("nombresDias", diasSemana.stream()
                .map(d -> capitalizar(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES)) + " " + d.format(DD_MM))
                .toList());
        model.addAttribute("semana", semana);
        model.addAttribute("semanaAnterior", lunes.minusWeeks(1));
        model.addAttribute("semanaSiguiente", lunes.plusWeeks(1));
        model.addAttribute("currentPage", "horarios");
        return "Recepcionista/agenda-horarios";
    }

    /** Une turnos consecutivos en rangos legibles: "08:00–12:00, 15:00–18:00". */
    private static String resumirRangos(List<Agenda> turnos) {
        List<String> rangos = new ArrayList<>();
        LocalTime ini = null;
        LocalTime fin = null;
        for (Agenda a : turnos) {
            if (ini == null) {
                ini = a.getHoraInicio();
                fin = a.getHoraFin();
            } else if (!a.getHoraInicio().isAfter(fin)) {
                if (a.getHoraFin().isAfter(fin)) fin = a.getHoraFin();
            } else {
                rangos.add(ini.format(HH_MM) + "–" + fin.format(HH_MM));
                ini = a.getHoraInicio();
                fin = a.getHoraFin();
            }
        }
        if (ini != null) rangos.add(ini.format(HH_MM) + "–" + fin.format(HH_MM));
        return String.join(", ", rangos);
    }

    private static String capitalizar(String s) {
        if (s == null || s.isEmpty()) return s;
        s = s.replace(".", "");
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @PostMapping("/horarios/generar")
    public String generarHorarios(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) List<Integer> dias,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin,
            @RequestParam Integer duracionTurno,
            @RequestParam(required = false) String veterinarioId,
            RedirectAttributes redirectAttributes) {

        try {
            if (fechaHasta == null) {
                fechaHasta = fecha;
            }
            if (fecha.isBefore(LocalDate.now())) {
                throw new IllegalArgumentException("No se pueden generar horarios para una fecha pasada.");
            }
            if (fechaHasta.isBefore(fecha)) {
                throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
            }
            if (ChronoUnit.DAYS.between(fecha, fechaHasta) > 92) {
                throw new IllegalArgumentException("El rango máximo permitido es de 3 meses.");
            }
            if (!horaFin.isAfter(horaInicio)) {
                throw new IllegalArgumentException("La hora fin debe ser mayor a la hora inicio.");
            }
            long minutosTotales = ChronoUnit.MINUTES.between(horaInicio, horaFin);
            if (duracionTurno == null || duracionTurno < 15) {
                throw new IllegalArgumentException("La duración mínima permitida es de 15 minutos.");
            }
            if (minutosTotales < duracionTurno) {
                throw new IllegalArgumentException("El rango horario (" + minutosTotales
                        + " min) es menor a la duración del turno (" + duracionTurno + " min).");
            }

            Set<DayOfWeek> diasSet = EnumSet.noneOf(DayOfWeek.class);
            if (dias == null || dias.isEmpty()) {
                // Si es un solo día se usa ese día; si es un rango, de lunes a sábado
                if (fecha.equals(fechaHasta)) {
                    diasSet.add(fecha.getDayOfWeek());
                } else {
                    diasSet.addAll(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.SATURDAY));
                }
            } else {
                for (Integer d : dias) {
                    if (d != null && d >= 1 && d <= 7) diasSet.add(DayOfWeek.of(d));
                }
            }

            List<Usuario> destino = new ArrayList<>();
            if (veterinarioId == null || veterinarioId.isBlank()) {
                throw new IllegalArgumentException("Seleccione un veterinario.");
            } else if ("TODOS".equalsIgnoreCase(veterinarioId)) {
                destino.addAll(veterinariosActivos());
                if (destino.isEmpty()) throw new IllegalArgumentException("No hay veterinarios activos.");
            } else {
                Usuario vet = usuarioRepository.findById(Long.valueOf(veterinarioId))
                        .orElseThrow(() -> new IllegalArgumentException("Veterinario no encontrado."));
                if (!"VETERINARIO".equalsIgnoreCase(vet.getRol())) {
                    throw new IllegalArgumentException("El usuario seleccionado no es veterinario.");
                }
                destino.add(vet);
            }

            int creados = 0;
            int omitidos = 0;
            int diasProc = 0;
            for (Usuario vet : destino) {
                AgendaService.ResultadoGeneracion r = agendaService.generarTurnos(vet, fecha, fechaHasta, diasSet,
                        horaInicio, horaFin, duracionTurno);
                creados += r.creados();
                omitidos += r.omitidos();
                diasProc = Math.max(diasProc, r.dias());
            }

            long sobrante = minutosTotales % duracionTurno;
            String nombres = destino.stream().map(Usuario::getNombre).collect(Collectors.joining(", "));
            String periodo = fecha.equals(fechaHasta) ? "el " + fecha.format(DD_MM_YYYY)
                    : "del " + fecha.format(DD_MM_YYYY) + " al " + fechaHasta.format(DD_MM_YYYY);
            StringBuilder msg = new StringBuilder()
                    .append("Horario de ").append(nombres).append(" ").append(periodo).append(" (")
                    .append(horaInicio.format(HH_MM)).append("–").append(horaFin.format(HH_MM)).append("): ")
                    .append(creados).append(" turno(s) de ").append(duracionTurno).append(" min creados");
            if (omitidos > 0) msg.append(", ").append(omitidos).append(" ya existían u horas pasadas");
            if (sobrante > 0) msg.append(". Sobran ").append(sobrante).append(" min al final del rango");

            if (diasProc == 0) {
                notificar(redirectAttributes, "error", "Ningún día del rango coincide con los días seleccionados.");
            } else if (creados == 0) {
                notificar(redirectAttributes, "error", "No se crearon turnos: ya existen en ese horario o la hora ya pasó.");
            } else {
                notificar(redirectAttributes, "success", msg.toString());
            }
            log.info("Generación de horarios: {}", msg);
        } catch (Exception e) {
            log.error("Error al generar horarios: {}", e.getMessage());
            notificar(redirectAttributes, "error", e.getMessage());
        }
        return redirectHorarios(fecha);
    }

    @PostMapping("/horarios/bloquear/{id}")
    public String bloquear(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirectAttributes) {
        try {
            agendaService.bloquearHorario(id);
            notificar(redirectAttributes, "warning", "Turno bloqueado (" + fecha.format(DD_MM_YYYY) + ")");
        } catch (Exception e) {
            notificar(redirectAttributes, "error", e.getMessage());
        }
        return redirectHorarios(fecha);
    }

    @PostMapping("/horarios/liberar/{id}")
    public String liberar(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirectAttributes) {
        try {
            agendaService.liberarHorario(id);
            notificar(redirectAttributes, "success", "Turno liberado (" + fecha.format(DD_MM_YYYY) + ")");
        } catch (Exception e) {
            notificar(redirectAttributes, "error", e.getMessage());
        }
        return redirectHorarios(fecha);
    }

    @PostMapping("/horarios/eliminar/{id}")
    public String eliminar(@PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            RedirectAttributes redirectAttributes) {
        try {
            agendaService.eliminarHorario(id);
            notificar(redirectAttributes, "warning", "Turno eliminado (" + fecha.format(DD_MM_YYYY) + ")");
        } catch (Exception e) {
            notificar(redirectAttributes, "error", e.getMessage());
        }
        return redirectHorarios(fecha);
    }

    @GetMapping("/api/disponibles")
    public ResponseEntity<List<Map<String, String>>> disponibles(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long veterinarioId) {
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        List<Map<String, String>> payload = agendaService.obtenerHorariosDisponibles(fecha).stream()
                .filter(a -> a.getVeterinario() != null && a.getHoraInicio() != null)
                .filter(a -> veterinarioId == null || Objects.equals(a.getVeterinario().getId(), veterinarioId))
                // No ofrecer turnos que ya pasaron hoy
                .filter(a -> !fecha.equals(hoy) || a.getHoraInicio().isAfter(ahora))
                .sorted(Comparator.comparing(Agenda::getHoraInicio)
                        .thenComparing(a -> a.getVeterinario().getNombre(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> Map.of(
                        "id", String.valueOf(a.getId()),
                        "horaInicio", a.getHoraInicio().format(HH_MM),
                        "horaFin", a.getHoraFin() != null ? a.getHoraFin().format(HH_MM) : "",
                        "veterinarioId", String.valueOf(a.getVeterinario().getId()),
                        "veterinarioNombre", a.getVeterinario().getNombre() != null ? a.getVeterinario().getNombre() : ""))
                .toList();
        return ResponseEntity.ok(payload);
    }

    /** Próxima fecha con turnos libres, para sugerirla cuando el día elegido no tiene horarios. */
    @GetMapping("/api/proxima-disponible")
    @ResponseBody
    public Map<String, String> proximaDisponible(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = (desde == null || desde.isBefore(hoy)) ? hoy : desde;
        LocalDate encontrada = agendaService.proximaFechaDisponible(inicio);
        // Si la encontrada es hoy pero ya no quedan turnos futuros, buscar desde mañana
        if (encontrada != null && encontrada.equals(hoy)
                && disponibles(hoy, null).getBody().isEmpty()) {
            encontrada = agendaService.proximaFechaDisponible(hoy.plusDays(1));
        }
        return encontrada == null ? Map.of() : Map.of(
                "fecha", encontrada.toString(),
                "fechaTexto", capitalizar(encontrada.getDayOfWeek().getDisplayName(TextStyle.FULL, ES)) + " "
                        + encontrada.format(DD_MM_YYYY));
    }

    @GetMapping("/api/ui-notifications")
    @ResponseBody
    public List<NotificacionService.UINotification> getNotifications() {
        return notificacionService.getAndClearUINotifications();
    }
}
