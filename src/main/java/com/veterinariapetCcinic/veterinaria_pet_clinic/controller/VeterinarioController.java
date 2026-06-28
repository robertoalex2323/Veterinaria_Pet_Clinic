package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.CitaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MascotaService;

@Controller
@RequestMapping("/veterinario")
public class VeterinarioController {

    private final CitaService citaService;
    private final MascotaService mascotaService;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;

    public VeterinarioController(CitaService citaService,
            MascotaService mascotaService,
            CitaRepository citaRepository,
            UsuarioRepository usuarioRepository) {
        this.citaService = citaService;
        this.mascotaService = mascotaService;
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping({"", "/"})
    public String inicio() {
        return "redirect:/veterinario/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate hoy = LocalDate.now();
        List<Cita> citasHoy = citaService.obtenerCitasDelDia(hoy);
        List<Cita> citasPendientes = citasHoy.stream()
                .filter(cita -> "AGENDADA".equalsIgnoreCase(cita.getEstado())
                        || "CONFIRMADA".equalsIgnoreCase(cita.getEstado()))
                .toList();
        List<Cita> citasAtendidas = citasHoy.stream()
                .filter(cita -> "ATENDIDA".equalsIgnoreCase(cita.getEstado())
                        || "EN_CONSULTA".equalsIgnoreCase(cita.getEstado()))
                .toList();

        addCommonModel(model, "dashboard");
        model.addAttribute("fecha", hoy);
        model.addAttribute("totalMascotas", mascotaService.contarMascotas());
        model.addAttribute("mascotasAtendidas", citasAtendidas.stream()
                .map(Cita::getMascota)
                .filter(mascota -> mascota != null && mascota.getId() != null)
                .map(Mascota::getId)
                .distinct()
                .count());
        model.addAttribute("consultasRegistradas", citaService.listarTodas().stream()
                .filter(cita -> "ATENDIDA".equalsIgnoreCase(cita.getEstado())
                        || "EN_CONSULTA".equalsIgnoreCase(cita.getEstado()))
                .count());
        model.addAttribute("diagnosticosRealizados", citaService.listarTodas().stream()
                .filter(cita -> cita.getObservaciones() != null && !cita.getObservaciones().isBlank())
                .count());
        model.addAttribute("citasPendientes", citasPendientes.size());
        model.addAttribute("pacientesDelDia", citasHoy.size());
        model.addAttribute("proximasCitas", ordenarCitas(citasPendientes));
        model.addAttribute("ultimosPacientes", ordenarMascotas(mascotaService.listarTodos()).stream().limit(6).toList());
        return "veterinario/dashboard";
    }

    @GetMapping("/consultas")
    public String consultas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now();
        addCommonModel(model, "consultas");
        model.addAttribute("fecha", fechaConsulta);
        model.addAttribute("citas", ordenarCitas(citaService.obtenerCitasDelDia(fechaConsulta)));
        return "veterinario/consultas";
    }

    @GetMapping("/historiales")
    public String historiales(Model model) {
        addCommonModel(model, "historiales");
        model.addAttribute("mascotas", ordenarMascotas(mascotaService.listarTodos()));
        return "veterinario/historiales";
    }

    @GetMapping("/diagnosticos")
    public String diagnosticos(Model model) {
        addCommonModel(model, "diagnosticos");
        List<Cita> citasConObservaciones = citaService.listarTodas().stream()
                .filter(cita -> cita.getObservaciones() != null && !cita.getObservaciones().isBlank())
                .sorted(Comparator.comparing(Cita::getFechaHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        model.addAttribute("diagnosticos", citasConObservaciones);
        return "veterinario/diagnosticos";
    }

    @GetMapping("/tratamientos")
    public String tratamientos(Model model) {
        addCommonModel(model, "tratamientos");
        model.addAttribute("citasAtendidas", citaService.listarTodas().stream()
                .filter(cita -> "ATENDIDA".equalsIgnoreCase(cita.getEstado())
                        || "EN_CONSULTA".equalsIgnoreCase(cita.getEstado()))
                .sorted(Comparator.comparing(Cita::getFechaHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
        return "veterinario/tratamientos";
    }

    @GetMapping("/mascotas")
    public String mascotas(
            @RequestParam(required = false) String buscar,
            Model model) {
        addCommonModel(model, "mascotas");
        List<Mascota> mascotas = ordenarMascotas(mascotaService.listarTodos());
        if (buscar != null && !buscar.isBlank()) {
            String filtro = buscar.trim().toLowerCase(Locale.ROOT);
            mascotas = mascotas.stream()
                    .filter(mascota -> contiene(mascota.getNombre(), filtro)
                            || contiene(mascota.getEspecie(), filtro)
                            || contiene(mascota.getRaza(), filtro)
                            || (mascota.getCliente() != null && contiene(mascota.getCliente().getNombre(), filtro)))
                    .toList();
        }
        model.addAttribute("mascotas", mascotas);
        model.addAttribute("buscar", buscar);
        return "veterinario/mascotas";
    }

    private void addCommonModel(Model model, String currentPage) {
        Usuario usuario = usuarioRepository.findByUsername(getUsername()).orElse(null);
        model.addAttribute("currentPage", currentPage);
        model.addAttribute("nombreUsuario", usuario != null && usuario.getNombre() != null
                ? usuario.getNombre()
                : getUsername());
        model.addAttribute("rolUsuario", "Veterinario");
        model.addAttribute("estadoSistema", "Operativo");
    }

    private String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "veterinario";
    }

    private List<Cita> ordenarCitas(List<Cita> citas) {
        return new ArrayList<>(citas != null ? citas : List.of()).stream()
                .sorted(Comparator.comparing(Cita::getFechaHora, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<Mascota> ordenarMascotas(List<Mascota> mascotas) {
        return new ArrayList<>(mascotas != null ? mascotas : List.of()).stream()
                .sorted(Comparator.comparing(Mascota::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private boolean contiene(String valor, String filtro) {
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(filtro);
    }
}
