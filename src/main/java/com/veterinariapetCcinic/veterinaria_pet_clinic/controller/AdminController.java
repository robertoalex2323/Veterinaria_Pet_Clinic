package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.AuditLog;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Pago;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.AgendaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.AuditLogService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ClienteService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MascotaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PagoService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final List<String> ROLES = Arrays.asList(
            "ADMIN", "VETERINARIO", "RECEPCIONISTA", "VENDEDOR", "FARMACEUTICO");

    private final AgendaRepository agendaRepository;
    private final CitaService citaService;
    private final ClienteService clienteService;
    private final MascotaService mascotaService;
    private final PagoService pagoService;
    private final UsuarioRepository usuarioRepository;
    private final VentaRepository ventaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AdminController(AgendaRepository agendaRepository,
            CitaService citaService,
            ClienteService clienteService,
            MascotaService mascotaService,
            PagoService pagoService,
            UsuarioRepository usuarioRepository,
            VentaRepository ventaRepository,
            MedicamentoRepository medicamentoRepository,
            AuditLogService auditLogService,
            BCryptPasswordEncoder passwordEncoder) {
        this.agendaRepository = agendaRepository;
        this.citaService = citaService;
        this.clienteService = clienteService;
        this.mascotaService = mascotaService;
        this.pagoService = pagoService;
        this.usuarioRepository = usuarioRepository;
        this.ventaRepository = ventaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    private String getNombreUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "Administrador";
    }

    @GetMapping({"", "/"})
    public String inicio() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioDia = hoy.atStartOfDay();
        LocalDateTime finDia = hoy.atTime(LocalTime.MAX);
        List<Medicamento> proximosVencer = medicamentoRepository
                .findByFechaVencimientoBetweenOrderByFechaVencimientoAsc(hoy, hoy.plusDays(30));

        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("totalClientes", clienteService.contarClientes());
        model.addAttribute("totalMascotas", mascotaService.contarMascotas());
        model.addAttribute("totalCitas", citaService.listarTodas().size());
        model.addAttribute("totalPagos", pagoService.listarTodos().size());
        model.addAttribute("totalAgendas", agendaRepository.count());
        model.addAttribute("ventasDia", valorSeguro(ventaRepository.sumVentasEntreFechas(inicioDia, finDia)));
        model.addAttribute("medicamentosProximosVencer", proximosVencer.size());
        model.addAttribute("listaMedicamentosProximosVencer", proximosVencer.stream().limit(6).toList());
        model.addAttribute("fechaActual", hoy);
        model.addAttribute("estadoSistema", "Operativo");
        model.addAttribute("ultimosUsuarios", usuarioRepository.findAll().stream()
                .sorted(Comparator.comparing(Usuario::getFechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList());
        model.addAttribute("ultimasMascotas", mascotaService.listarTodos().stream()
                .sorted(Comparator.comparing(Mascota::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList());
        model.addAttribute("ultimasCitas", citaService.listarTodas().stream()
                .sorted(Comparator.comparing(Cita::getFechaHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList());
        model.addAttribute("usuariosPorRol", contarUsuariosPorRol(usuarioRepository.findAll()));
        model.addAttribute("citasPorEstado", contarCitasPorEstado(citaService.listarTodas()));
        model.addAttribute("auditoriaReciente", auditLogService.recientes());
        return "admin/dashboard";
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private Map<String, Long> contarUsuariosPorRol(List<Usuario> usuarios) {
        return usuarios.stream()
                .collect(Collectors.groupingBy(
                        usuario -> usuario.getRol() != null ? usuario.getRol() : "SIN_ROL",
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    private Map<String, Long> contarCitasPorEstado(List<Cita> citas) {
        return citas.stream()
                .collect(Collectors.groupingBy(
                        cita -> cita.getEstado() != null ? cita.getEstado() : "SIN_ESTADO",
                        LinkedHashMap::new,
                        Collectors.counting()));
    }

    @GetMapping({"/usuarios", "/usuarios-admin"})
    public String listarUsuarios(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios != null ? usuarios : new ArrayList<>());
        model.addAttribute("roles", ROLES);
        model.addAttribute("usuarioForm", new Usuario());
        return "admin/usuarios-admin";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@RequestParam String username,
                               @RequestParam String nombre,
                               @RequestParam(required = false) String email,
                               @RequestParam String rol,
                               @RequestParam String password,
                               RedirectAttributes redirectAttributes) {
        String actor = getNombreUsuario();
        if (usuarioRepository.existsByUsername(username)) {
            redirectAttributes.addFlashAttribute("error", "El usuario ya existe.");
            return "redirect:/admin/usuarios";
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username.trim());
        usuario.setNombre(nombre.trim());
        usuario.setEmail(email != null && !email.isBlank() ? email.trim() : null);
        usuario.setRol(normalizarRol(rol));
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setActivo(true);
        usuario.setBloqueado(false);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setCreadoPor(actor);
        usuarioRepository.save(usuario);

        auditLogService.registrar("CREAR", "Usuario", usuario.getId(), actor,
                "Creó usuario " + usuario.getUsername() + " con rol " + usuario.getRol());
        redirectAttributes.addFlashAttribute("success", "Usuario creado correctamente.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/actualizar")
    public String actualizarUsuario(@PathVariable Long id,
                                    @RequestParam String nombre,
                                    @RequestParam(required = false) String email,
                                    @RequestParam String rol,
                                    RedirectAttributes redirectAttributes) {
        String actor = getNombreUsuario();
        usuarioRepository.findById(id).ifPresentOrElse(usuario -> {
            usuario.setNombre(nombre.trim());
            usuario.setEmail(email != null && !email.isBlank() ? email.trim() : null);
            usuario.setRol(normalizarRol(rol));
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuario.setEditadoPor(actor);
            usuarioRepository.save(usuario);
            auditLogService.registrar("EDITAR", "Usuario", usuario.getId(), actor,
                    "Actualizó datos y rol de " + usuario.getUsername());
            redirectAttributes.addFlashAttribute("success", "Usuario actualizado.");
        }, () -> redirectAttributes.addFlashAttribute("error", "Usuario no encontrado."));
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/password")
    public String restablecerPassword(@PathVariable Long id,
                                      @RequestParam String nuevaPassword,
                                      RedirectAttributes redirectAttributes) {
        String actor = getNombreUsuario();
        usuarioRepository.findById(id).ifPresentOrElse(usuario -> {
            usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            usuario.setPasswordResetAt(LocalDateTime.now());
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuario.setEditadoPor(actor);
            usuarioRepository.save(usuario);
            auditLogService.registrar("RESTABLECER_PASSWORD", "Usuario", usuario.getId(), actor,
                    "Restableció contraseña de " + usuario.getUsername());
            redirectAttributes.addFlashAttribute("success", "Contraseña restablecida.");
        }, () -> redirectAttributes.addFlashAttribute("error", "Usuario no encontrado."));
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/bloqueo")
    public String alternarBloqueo(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String actor = getNombreUsuario();
        usuarioRepository.findById(id).ifPresentOrElse(usuario -> {
            if (usuario.getUsername().equals(actor)) {
                redirectAttributes.addFlashAttribute("error", "No puedes bloquear tu propio usuario.");
                return;
            }
            boolean bloquear = !Boolean.TRUE.equals(usuario.getBloqueado());
            usuario.setBloqueado(bloquear);
            usuario.setActivo(!bloquear);
            usuario.setFechaBloqueo(bloquear ? LocalDateTime.now() : null);
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuario.setEditadoPor(actor);
            usuarioRepository.save(usuario);
            auditLogService.registrar(bloquear ? "BLOQUEAR" : "DESBLOQUEAR", "Usuario", usuario.getId(), actor,
                    (bloquear ? "Bloqueó" : "Desbloqueó") + " usuario " + usuario.getUsername());
            redirectAttributes.addFlashAttribute("success", bloquear ? "Usuario bloqueado." : "Usuario desbloqueado.");
        }, () -> redirectAttributes.addFlashAttribute("error", "Usuario no encontrado."));
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String actor = getNombreUsuario();
        usuarioRepository.findById(id).ifPresentOrElse(usuario -> {
            if (usuario.getUsername().equals(actor)) {
                redirectAttributes.addFlashAttribute("error", "No puedes eliminar tu propio usuario.");
                return;
            }
            String username = usuario.getUsername();
            try {
                usuarioRepository.delete(usuario);
                auditLogService.registrar("ELIMINAR", "Usuario", id, actor, "Eliminó usuario " + username);
                redirectAttributes.addFlashAttribute("success", "Usuario eliminado.");
            } catch (Exception ex) {
                usuario.setActivo(false);
                usuario.setBloqueado(true);
                usuario.setFechaBloqueo(LocalDateTime.now());
                usuario.setFechaActualizacion(LocalDateTime.now());
                usuario.setEditadoPor(actor);
                usuarioRepository.save(usuario);
                auditLogService.registrar("BLOQUEAR", "Usuario", id, actor,
                        "No se pudo eliminar por registros asociados; quedó bloqueado: " + username);
                redirectAttributes.addFlashAttribute("error", "El usuario tiene registros asociados; se bloqueó en lugar de eliminarse.");
            }
        }, () -> redirectAttributes.addFlashAttribute("error", "Usuario no encontrado."));
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("roles", ROLES);
        model.addAttribute("usuariosPorRol", contarUsuariosPorRol(usuarios));
        return "admin/roles";
    }

    @GetMapping("/auditoria")
    public String auditoria(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<AuditLog> logs = auditLogService.listarTodos();
        model.addAttribute("logs", logs != null ? logs : new ArrayList<>());
        return "admin/auditoria";
    }

    private String normalizarRol(String rol) {
        String normalizado = rol != null ? rol.trim().toUpperCase() : "RECEPCIONISTA";
        return ROLES.contains(normalizado) ? normalizado : "RECEPCIONISTA";
    }

    @GetMapping({"/clientes", "/clientes-admin"})
    public String listarClientes(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Cliente> clientes = clienteService.listarTodos();
        model.addAttribute("clientes", clientes != null ? clientes : new ArrayList<>());
        return "admin/cliente-admin";
    }

    @GetMapping({"/mascotas", "/mascotas-admin"})
    public String listarMascotas(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Mascota> mascotas = mascotaService.listarTodos();
        model.addAttribute("mascotas", mascotas != null ? mascotas : new ArrayList<>());
        return "admin/mascota-admin";
    }

    @GetMapping({"/citas", "/citas-admin"})
    public String listarCitas(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Cita> citas = citaService.listarTodas();
        model.addAttribute("citas", citas != null ? citas : new ArrayList<>());
        return "admin/cita-admin";
    }

    @GetMapping({"/agenda", "/agenda-admin"})
    public String listarAgenda(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Agenda> agendas = agendaRepository.findAll();
        model.addAttribute("agendas", agendas != null ? agendas : new ArrayList<>());
        return "admin/agenda-admin";
    }

    @GetMapping({"/pagos", "/pagos-admin"})
    public String listarPagos(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Pago> pagos = pagoService.listarTodos();
        model.addAttribute("pagos", pagos != null ? pagos : new ArrayList<>());
        return "admin/pagos-admin";
    }
}
