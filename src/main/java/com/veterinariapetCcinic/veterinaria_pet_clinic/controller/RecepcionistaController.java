package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Pago;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import java.time.Year;

import com.veterinariapetCcinic.veterinaria_pet_clinic.service.AgendaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ClienteService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ComprobantePagoPdfService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MascotaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PagoService;

@Controller
@RequestMapping("/recepcionista")
public class RecepcionistaController {

    private static final Logger log = LoggerFactory.getLogger(RecepcionistaController.class);

    // Regex para validación de formato de correo electrónico
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    // Regex para validación de teléfono (solo dígitos)
    private static final String PHONE_REGEX = "^[0-9]+$";
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
    
    // Longitud mínima para el teléfono
    private static final int MIN_PHONE_LENGTH = 9;

    private final ClienteService clienteService;
    private final MascotaService mascotaService;
    private final CitaService citaService;
    private final PagoService pagoService;
    private final AgendaService agendaService;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder;
    private final ComprobantePagoPdfService comprobantePagoPdfService;

    // Constructor con inyección de dependencias
    public RecepcionistaController(ClienteService clienteService,
                                   MascotaService mascotaService,
                                   CitaService citaService,
                                   PagoService pagoService,
                                   AgendaService agendaService,
                                   NotificacionService notificacionService,
                                   UsuarioRepository usuarioRepository,
                                   org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder,
                                   ComprobantePagoPdfService comprobantePagoPdfService) {
        this.comprobantePagoPdfService = comprobantePagoPdfService;


        this.clienteService = clienteService;
        this.mascotaService = mascotaService;
        this.citaService = citaService;
        this.pagoService = pagoService;
        this.agendaService = agendaService;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String getNombreUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "Recepcionista";
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isValidPhone(String telefono) {
        // Validar que no sea null o vacío
        if (telefono == null || telefono.trim().isEmpty()) {
            return false;
        }
        
        String phoneTrimmed = telefono.trim();
        
        // Validar que solo contenga números
        if (!PHONE_PATTERN.matcher(phoneTrimmed).matches()) {
            return false;
        }
        
        // Validar longitud mínima
        return phoneTrimmed.length() >= MIN_PHONE_LENGTH;
    }

    // ============ DASHBOARD ============
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String username = getNombreUsuario();
        model.addAttribute("nombreUsuario", username);

        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            model.addAttribute("nombreCompleto", usuario.getNombre());
        });

        try {
            model.addAttribute("totalClientes", clienteService.contarClientes());
            model.addAttribute("totalMascotas", mascotaService.contarMascotas());
            model.addAttribute("citasHoy", citaService.contarCitasHoy());

            Double ingresosHoy = pagoService.getTotalPagosDelDia();
            model.addAttribute("ingresosHoy", ingresosHoy != null ? ingresosHoy : 0.0);

            List<Cita> proximasCitas = citaService.obtenerCitasDelDia(LocalDate.now());
            // Inicializar relaciones para evitar LazyInitializationException
            if (proximasCitas != null) {
                for (Cita c : proximasCitas) {
                    if (c.getMascota() != null) {
                        c.getMascota().getNombre();
                        if (c.getMascota().getCliente() != null) {
                            c.getMascota().getCliente().getNombre();
                        }
                    }
                }
            }
            model.addAttribute("proximasCitas", proximasCitas != null ? proximasCitas : new ArrayList<>());

            List<Pago> ultimosPagos = pagoService.obtenerPagosDelDia();
            model.addAttribute("ultimosPagos", ultimosPagos != null ? ultimosPagos : new ArrayList<>());
        } catch (Exception e) {
            log.error("Error al cargar dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar datos del dashboard: " + e.getMessage());
            model.addAttribute("totalClientes", 0L);
            model.addAttribute("totalMascotas", 0L);
            model.addAttribute("citasHoy", 0L);
            model.addAttribute("ingresosHoy", 0.0);
            model.addAttribute("proximasCitas", new ArrayList<>());
            model.addAttribute("ultimosPagos", new ArrayList<>());
        }

        return "Recepcionista/dashboard";
    }

    // ============ GESTIÓN DE CLIENTES ============
    @GetMapping("/clientes")
    public String listarClientes(@RequestParam(required = false) String buscar, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());

        List<Cliente> clientes;
        if (buscar != null && !buscar.isEmpty()) {
            clientes = clienteService.buscarClientes(buscar);
        } else {
            clientes = clienteService.listarTodos();
        }
        model.addAttribute("clientes", clientes != null ? clientes : new ArrayList<>());
        model.addAttribute("buscar", buscar);
        return "Recepcionista/clientes";
    }

    @GetMapping("/clientes/nuevo")
    public String nuevoClienteForm(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("cliente", new Cliente());
        return "Recepcionista/cliente-form";
    }

    @PostMapping("/clientes/guardar")
    public String guardarCliente(@ModelAttribute Cliente cliente, RedirectAttributes redirectAttributes) {
        try {
            // ===== VALIDACIÓN DE EMAIL =====
            if (cliente.getEmail() != null && !cliente.getEmail().isEmpty()) {
                if (!isValidEmail(cliente.getEmail())) {
                    log.warn("Intento de guardar cliente con email inválido: {}", cliente.getEmail());
                    redirectAttributes.addFlashAttribute("error", "Formato de correo electrónico inválido. Por favor, ingrese un email válido.");
                    return "redirect:/recepcionista/clientes/nuevo";
                }
            }
            
            // ===== VALIDACIÓN DE TELÉFONO =====
            String telefono = cliente.getTelefono();
            
            // Validar que el teléfono no esté vacío
            if (telefono == null || telefono.trim().isEmpty()) {
                log.warn("Intento de guardar cliente sin teléfono");
                redirectAttributes.addFlashAttribute("error", "El teléfono no puede estar vacío.");
                return "redirect:/recepcionista/clientes/nuevo";
            }
            
            // Validar que solo contenga números
            if (!isValidPhone(telefono)) {
                String telefonoTrimmed = telefono.trim();
                // Determinar el mensaje de error específico
                if (!PHONE_PATTERN.matcher(telefonoTrimmed).matches()) {
                    log.warn("Intento de guardar cliente con teléfono que contiene caracteres no numéricos: {}", telefono);
                    redirectAttributes.addFlashAttribute("error", "El teléfono solo debe contener números.");
                    return "redirect:/recepcionista/clientes/nuevo";
                }
                if (telefonoTrimmed.length() < MIN_PHONE_LENGTH) {
                    log.warn("Intento de guardar cliente con teléfono muy corto ({} dígitos): {}", telefonoTrimmed.length(), telefono);
                    redirectAttributes.addFlashAttribute("error", "El teléfono debe tener al menos " + MIN_PHONE_LENGTH + " dígitos.");
                    return "redirect:/recepcionista/clientes/nuevo";
                }
            }
            
            // Si todas las validaciones pasan, guardar el cliente
            clienteService.guardar(cliente);
            log.info("Cliente guardado exitosamente con teléfono: {}", cliente.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Cliente guardado exitosamente");
        } catch (Exception e) {
            log.error("Error al guardar cliente: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/clientes";
    }

    @GetMapping("/clientes/editar/{id}")
    public String editarClienteForm(@PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        try {
            Cliente cliente = clienteService.buscarPorId(id);
            model.addAttribute("cliente", cliente);
        } catch (Exception e) {
            log.error("Error al editar cliente {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Cliente no encontrado");
            return "redirect:/recepcionista/clientes";
        }
        return "Recepcionista/cliente-form";
    }

    @PostMapping("/clientes/actualizar/{id}")
    public String actualizarCliente(@PathVariable Long id, @ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        try {
            // ===== VALIDACIÓN DE EMAIL =====
            if (cliente.getEmail() != null && !cliente.getEmail().isEmpty()) {
                if (!isValidEmail(cliente.getEmail())) {
                    log.warn("Intento de actualizar cliente con email inválido: {}", cliente.getEmail());
                    redirectAttributes.addFlashAttribute("error", "Formato de correo electrónico inválido. Por favor, ingrese un email válido.");
                    return "redirect:/recepcionista/clientes/editar/" + id;
                }
            }
            
            // ===== VALIDACIÓN DE TELÉFONO =====
            String telefono = cliente.getTelefono();
            
            // Validar que el teléfono no esté vacío
            if (telefono == null || telefono.trim().isEmpty()) {
                log.warn("Intento de actualizar cliente sin teléfono");
                redirectAttributes.addFlashAttribute("error", "El teléfono no puede estar vacío.");
                return "redirect:/recepcionista/clientes/editar/" + id;
            }
            
            // Validar que solo contenga números
            if (!isValidPhone(telefono)) {
                String telefonoTrimmed = telefono.trim();
                // Determinar el mensaje de error específico
                if (!PHONE_PATTERN.matcher(telefonoTrimmed).matches()) {
                    log.warn("Intento de actualizar cliente con teléfono que contiene caracteres no numéricos: {}", telefono);
                    redirectAttributes.addFlashAttribute("error", "El teléfono solo debe contener números.");
                    return "redirect:/recepcionista/clientes/editar/" + id;
                }
                if (telefonoTrimmed.length() < MIN_PHONE_LENGTH) {
                    log.warn("Intento de actualizar cliente con teléfono muy corto ({} dígitos): {}", telefonoTrimmed.length(), telefono);
                    redirectAttributes.addFlashAttribute("error", "El teléfono debe tener al menos " + MIN_PHONE_LENGTH + " dígitos.");
                    return "redirect:/recepcionista/clientes/editar/" + id;
                }
            }
            
            // Si todas las validaciones pasan, actualizar el cliente
            cliente.setId(id);
            clienteService.actualizar(cliente);
            log.info("Cliente actualizado exitosamente con teléfono: {}", cliente.getTelefono());
            redirectAttributes.addFlashAttribute("success", "Cliente actualizado exitosamente");
        } catch (Exception e) {
            log.error("Error al actualizar cliente {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/clientes";
    }

    @GetMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            clienteService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Cliente eliminado exitosamente");
        } catch (Exception e) {
            log.error("Error al eliminar cliente {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/clientes";
    }

    @GetMapping("/clientes/ver/{id}")
    public String verCliente(@PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        try {
            Cliente cliente = clienteService.obtenerClienteConMascotas(id);
            model.addAttribute("cliente", cliente);
        } catch (Exception e) {
            log.error("Error al ver cliente {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Cliente no encontrado");
            return "redirect:/recepcionista/clientes";
        }
        return "Recepcionista/cliente-detalle";
    }

    // ============ GESTIÓN DE MASCOTAS ============
    @GetMapping("/mascotas")
    public String listarMascotas(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Mascota> mascotas = mascotaService.listarTodos();
        model.addAttribute("mascotas", mascotas != null ? mascotas : new ArrayList<>());
        return "Recepcionista/mascotas";
    }

    @GetMapping("/mascotas/nuevo")
    public String nuevaMascotaForm(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("mascota", new Mascota());
        model.addAttribute("clientes", clienteService.listarTodos());
        return "Recepcionista/mascota-form";
    }

    @PostMapping("/mascotas/guardar")
    public String guardarMascota(@ModelAttribute Mascota mascota, @RequestParam Long clienteId,
            RedirectAttributes redirectAttributes) {
        try {
            mascotaService.registrarMascota(clienteId, mascota);
            redirectAttributes.addFlashAttribute("success", "Mascota registrada exitosamente");
        } catch (Exception e) {
            log.error("Error al guardar mascota: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/mascotas";
    }

    @GetMapping("/mascotas/editar/{id}")
    public String editarMascotaForm(@PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        try {
            Mascota mascota = mascotaService.buscarPorId(id);
            model.addAttribute("mascota", mascota);
            model.addAttribute("clientes", clienteService.listarTodos());
        } catch (Exception e) {
            log.error("Error al editar mascota {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Mascota no encontrada");
            return "redirect:/recepcionista/mascotas";
        }
        return "Recepcionista/mascota-form";
    }

    @PostMapping("/mascotas/actualizar/{id}")
    public String actualizarMascota(@PathVariable Long id, @ModelAttribute Mascota mascota,
            RedirectAttributes redirectAttributes) {
        try {
            mascota.setId(id);
            mascotaService.actualizar(mascota);
            redirectAttributes.addFlashAttribute("success", "Mascota actualizada exitosamente");
        } catch (Exception e) {
            log.error("Error al actualizar mascota {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/mascotas";
    }

    @GetMapping("/mascotas/eliminar/{id}")
    public String eliminarMascota(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            mascotaService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Mascota eliminada exitosamente");
        } catch (Exception e) {
            log.error("Error al eliminar mascota {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/mascotas";
    }

    // ============ GESTIÓN DE CITAS ============
    @GetMapping("/citas")
    public String listarCitas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());

        if (fecha == null) {
            fecha = LocalDate.now();
        }

        List<Cita> citas = citaService.obtenerCitasDelDia(fecha);
        // Inicializar relaciones
        if (citas != null) {
            for (Cita c : citas) {
                if (c.getMascota() != null) {
                    c.getMascota().getNombre();
                    if (c.getMascota().getCliente() != null) {
                        c.getMascota().getCliente().getNombre();
                    }
                }
            }
        }
        model.addAttribute("citas", citas != null ? citas : new ArrayList<>());
        model.addAttribute("fecha", fecha);
        return "Recepcionista/citas";
    }

    @GetMapping("/citas/nueva")
    public String nuevaCitaForm(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("cita", new Cita());
        model.addAttribute("mascotas", mascotaService.listarTodos());
        return "Recepcionista/cita-form";
    }

    @PostMapping("/citas/guardar")
    public String guardarCita(@RequestParam Long mascotaId,
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam(required = false) Long veterinarioId,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) Long reprogramarDesdeId,
            @RequestParam(required = false) String motivoReprogramacion,
            RedirectAttributes redirectAttributes) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime nuevaFechaHora = LocalDateTime.parse(fecha + " " + hora, formatter);

            // --- Flujo de reprogramación ---
            if (reprogramarDesdeId != null) {
                if (motivoReprogramacion == null || motivoReprogramacion.isBlank()) {
                    motivoReprogramacion = "Reprogramada por recepcionista";
                }
                citaService.reprogramarCita(reprogramarDesdeId, nuevaFechaHora, motivoReprogramacion);
                redirectAttributes.addFlashAttribute("success", "Cita reprogramada exitosamente");
                return "redirect:/recepcionista/citas";
            }

            // --- Flujo normal de agendar ---
            Mascota mascota = mascotaService.buscarPorId(mascotaId);

            Cita cita = new Cita();
            cita.setMascota(mascota);
            cita.setMotivo(motivo);
            cita.setEstado("AGENDADA");
            cita.setFechaHora(nuevaFechaHora);

            if (veterinarioId == null) {
                throw new RuntimeException("Debe seleccionar un horario que tenga veterinario asignado.");
            }
            Usuario veterinario = usuarioRepository.findById(veterinarioId)
                    .orElseThrow(() -> new RuntimeException("Veterinario no encontrado."));
            cita.setVeterinario(veterinario);

            com.veterinariapetCcinic.veterinaria_pet_clinic.model.Agenda agendaDisponible = agendaService
                    .buscarAgendaDisponible(nuevaFechaHora.toLocalDate(), nuevaFechaHora.toLocalTime());
            if (agendaDisponible == null) {
                throw new RuntimeException("El horario seleccionado no existe en la agenda o ya no está disponible.");
            }
            if (agendaDisponible.getVeterinario() == null || agendaDisponible.getVeterinario().getId() == null
                    || !agendaDisponible.getVeterinario().getId().equals(veterinarioId)) {
                throw new RuntimeException("El horario seleccionado no corresponde al veterinario asignado.");
            }

            citaService.guardar(cita);
            redirectAttributes.addFlashAttribute("success", "Cita agendada exitosamente");
        } catch (Exception e) {
            log.error("Error al guardar cita: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al agendar cita: " + e.getMessage());
        }
        return "redirect:/recepcionista/citas";
    }

    @GetMapping("/citas/editar/{id}")
    public String editarCitaForm(@PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        try {
            Cita cita = citaService.buscarPorId(id);
            model.addAttribute("cita", cita);
            model.addAttribute("mascotas", mascotaService.listarTodos());
            return "Recepcionista/cita-form";
        } catch (Exception e) {
            log.error("Error al editar cita {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Cita no encontrada");
            return "redirect:/recepcionista/citas";
        }
    }

    @PostMapping("/citas/actualizar/{id}")
    public String actualizarCita(@PathVariable Long id,
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) Long veterinarioId,
            RedirectAttributes redirectAttributes) {
        try {
            Cita cita = citaService.buscarPorId(id);
            cita.setMotivo(motivo);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime fechaHora = LocalDateTime.parse(fecha + " " + hora, formatter);
            cita.setFechaHora(fechaHora);

            if (veterinarioId != null) {
                Usuario vet = usuarioRepository.findById(veterinarioId).orElse(null);
                cita.setVeterinario(vet);
            }

            citaService.actualizar(cita);
            redirectAttributes.addFlashAttribute("success", "Cita actualizada exitosamente");
        } catch (Exception e) {
            log.error("Error al actualizar cita {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar cita: " + e.getMessage());
        }
        return "redirect:/recepcionista/citas";
    }

    @GetMapping("/citas/cancelar/{id}")
    public String cancelarCita(@PathVariable Long id, @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes) {
        try {
            String motivoCancelacion = motivo != null ? motivo : "Cancelada por recepcionista";
            citaService.cancelarCita(id, motivoCancelacion);
            redirectAttributes.addFlashAttribute("success", "Cita cancelada exitosamente");
        } catch (Exception e) {
            log.error("Error al cancelar cita {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/citas";
    }

    // ============ GESTIÓN DE AGENDA ============
    @GetMapping("/agenda")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String verAgenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {
        log.info("==> Entrando a verAgenda con fecha: {}", fecha);

        // Atributos para el fragmento
        String username = getNombreUsuario();
        model.addAttribute("nombreUsuario", username);

        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            model.addAttribute("nombreCompleto", usuario.getNombre());
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombre", usuario.getNombre());
        });

        // Atributos de la página
        if (fecha == null) {
            fecha = LocalDate.now();
        }
        model.addAttribute("fecha", fecha);

        try {
            List<Cita> citasDelDia = citaService.obtenerCitasDelDia(fecha);
            if (citasDelDia == null) {
                citasDelDia = new ArrayList<>();
            }

            for (Cita c : citasDelDia) {
                if (c == null) {
                    continue;
                }

                if (c.getMascota() != null) {
                    c.getMascota().getNombre();
                    c.getMascota().getEspecie();

                    if (c.getMascota().getCliente() != null) {
                        c.getMascota().getCliente().getNombre();
                        c.getMascota().getCliente().getTelefono();
                    }
                }

                if (c.getVeterinario() != null) {
                    c.getVeterinario().getNombre();
                }

                // Forzar defaults para que Thymeleaf no choque con nulls
                if (c.getFechaHora() == null) {
                    log.warn("Cita ID {} tiene fechaHora null, se asignará now()", c.getId());
                    c.setFechaHora(LocalDateTime.now());
                }

                if (c.getEstado() == null) {
                    log.warn("Cita ID {} tiene estado null, se asignará 'AGENDADA'", c.getId());
                    c.setEstado("AGENDADA");
                }

                if (c.getMotivo() == null) {
                    c.setMotivo("");
                }

                if (c.getObservaciones() == null) {
                    c.setObservaciones("");
                }

                // tocar getters para inicializar relaciones (si aplica)
                c.getMotivo();
                c.getObservaciones();
            }

            model.addAttribute("citas", citasDelDia);
        } catch (Exception e) {
            log.error("Error al obtener citas", e);
            model.addAttribute("citas", new ArrayList<>());
            model.addAttribute("error", "Error al cargar citas: " + e.getMessage());
        }

        log.info("<== Saliendo de verAgenda");
        return "Recepcionista/agenda";
    }

    // ============ GESTIÓN DE PAGOS ============
    @GetMapping("/pagos")
    public String listarPagos(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());

        try {
            List<Pago> pagos = pagoService.listarTodos();
            model.addAttribute("pagos", pagos != null ? pagos : new ArrayList<>());

            model.addAttribute("totalPagos", pagoService.getTotalPagos());
            model.addAttribute("totalHoy", pagoService.getTotalPagosDelDia());
            model.addAttribute("pendientes", pagoService.contarPagosPendientes());

            model.addAttribute("clientes", clienteService.listarTodos());

        } catch (Exception e) {
            log.error("Error al listar pagos: {}", e.getMessage(), e);
            model.addAttribute("error", "Error al cargar los pagos: " + e.getMessage());
            model.addAttribute("pagos", new ArrayList<>());
            model.addAttribute("totalPagos", 0.0);
            model.addAttribute("totalHoy", 0.0);
            model.addAttribute("pendientes", 0L);
        }

        return "Recepcionista/pagos";
    }

    @GetMapping("/pagos/nuevo")
    public String nuevoPagoForm(@RequestParam(required = false) Long citaId, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("pago", new Pago());
        model.addAttribute("clientes", clienteService.listarTodos());

        if (citaId != null) {
            try {
                if (pagoService.estaPagadaLaCita(citaId)) {
                    redirectAttributes.addFlashAttribute("error", "Usted ya ha pagado esta cita. No se puede registrar otro pago.");
                    return "redirect:/recepcionista/citas";
                }

                Cita cita = citaService.buscarPorId(citaId);
                model.addAttribute("citaPreId", citaId);
                model.addAttribute("citaPreMascota",
                    cita.getMascota() != null ? cita.getMascota().getNombre() : "-");
                model.addAttribute("citaPreCliente",
                    cita.getMascota() != null && cita.getMascota().getCliente() != null
                        ? cita.getMascota().getCliente().getId() : null);
                model.addAttribute("citaPreFecha",
                    cita.getFechaHora() != null
                        ? cita.getFechaHora().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : "-");
                model.addAttribute("citaPreMotivo",
                    cita.getMotivo() != null ? cita.getMotivo() : "-");
                model.addAttribute("citaPreEstado", cita.getEstado());
                if (cita.getPago() != null) {
                    model.addAttribute("citaPreMonto", cita.getPago().getMonto());
                }
            } catch (Exception e) {
                model.addAttribute("errorCita", "No se encontró la cita con ID: " + citaId);
            }
        }
        return "Recepcionista/pago-form";
    }


    @GetMapping("/pagos/estado-cita/{citaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> estadoCitaPagada(@PathVariable Long citaId) {
        Map<String, Object> resp = new HashMap<>();
        try {
            boolean pagada = pagoService.estaPagadaLaCita(citaId);
            resp.put("ok", true);
            resp.put("pagada", pagada);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("error", e.getMessage());
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/pagos/info-cita/{citaId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> infoCita(@PathVariable Long citaId) {

        Map<String, Object> info = new HashMap<>();
        try {
            Cita cita = citaService.buscarPorId(citaId);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            info.put("mascota", cita.getMascota() != null ? cita.getMascota().getNombre() : "-");
            info.put("cliente", cita.getMascota() != null && cita.getMascota().getCliente() != null
                    ? cita.getMascota().getCliente().getNombre() : "-");
            info.put("fechaHora", cita.getFechaHora() != null ? cita.getFechaHora().format(fmt) : "-");
            info.put("motivo", cita.getMotivo() != null ? cita.getMotivo() : "-");
            info.put("estado", cita.getEstado());
            // Si ya tiene pago vinculado, devuelve el monto
            if (cita.getPago() != null) {
                info.put("montoPrevio", cita.getPago().getMonto());
            }
            info.put("ok", true);
        } catch (Exception e) {
            info.put("ok", false);
            info.put("error", "Cita no encontrada con ID: " + citaId);
        }
        return ResponseEntity.ok(info);
    }
  
    @PostMapping("/pagos/guardar")
    public String guardarPago(@RequestParam Long clienteId,
            @RequestParam Double monto,
            @RequestParam String metodoPago,
            @RequestParam(required = false) Long citaId,
            @RequestParam(required = false) String numeroComprobanteYape,
            RedirectAttributes redirectAttributes) {

        try {

            // Validar si la cita ya fue pagada
            if (citaId != null && citaId > 0) {
                if (pagoService.estaPagadaLaCita(citaId)) {
                    redirectAttributes.addFlashAttribute(
                            "error",
                            "Usted ya ha pagado esta cita. No se puede registrar otro pago.");
                    return "redirect:/recepcionista/citas";
                }
            }

            // Validación para Yape/Plin
            if ("Yape/Plin".equals(metodoPago)) {
                if (numeroComprobanteYape == null || numeroComprobanteYape.isBlank()) {
                    redirectAttributes.addFlashAttribute(
                            "error",
                            "El comprobante de Yape/Plin es obligatorio.");
                    return "redirect:/recepcionista/pagos";
                }
            }

            Cliente cliente = clienteService.buscarPorId(clienteId);

            Pago pago = new Pago();
            pago.setCliente(cliente);
            pago.setMonto(monto);
            pago.setMetodoPago(metodoPago);
            pago.setEstado("PAGADO");

            // Generar número de comprobante (año dinámico)
            String prefijo = "PET" + Year.now().getValue() + "-";

            String ultimoMax = pagoService.obtenerMaxComprobantePorPrefijo(prefijo);

            long consecutivo = 1;

            if (ultimoMax != null && !ultimoMax.isBlank() && ultimoMax.startsWith(prefijo)) {
                try {
                    String sufijo = ultimoMax.substring(prefijo.length());
                    consecutivo = Long.parseLong(sufijo) + 1;
                } catch (NumberFormatException e) {
                    consecutivo = 1;
                }
            }

            String comprobante = String.format(prefijo + "%05d", consecutivo);
            pago.setComprobante(comprobante);



            // Asociar cita si corresponde
            if (citaId != null && citaId > 0) {
                Cita cita = citaService.buscarPorId(citaId);
                pago.setCita(cita);
            }

            // Guardar pago
            pagoService.guardar(pago);

            // Notificar UI (WebSocket) para que suene la campanita
            notificacionService.enviarConfirmacionPago(cliente, monto, metodoPago);


            // Enviar comprobante PDF por correo
            if (cliente.getEmail() != null && !cliente.getEmail().isBlank()) {

                try {

                    String responsable = getNombreUsuario();

                    byte[] pdf = comprobantePagoPdfService
                            .generarComprobantePago(pago, responsable);

                    String archivoComprobante =
                            "comprobante_pago_" + pago.getId() + ".pdf";

                    notificacionService.enviarEmailConAdjunto(
                            cliente.getEmail(),
                            "Confirmación de Pago - Pet Clinic",
                            String.format("""
                                    Estimado(a) %s,

                                    Le informamos que hemos registrado su pago exitosamente.

                                    DETALLE DEL PAGO
                                    --------------------------
                                    N° Comprobante: %s
                                    Monto: S/ %.2f
                                    Método de pago: %s
                                    Estado: PAGADO

                                    Adjuntamos su comprobante de pago en formato PDF.

                                    Gracias por confiar en Veterinaria Pet Clinic.

                                    Atentamente,
                                    Veterinaria Pet Clinic
                                    """,
                                    cliente.getNombre(),
                                    pago.getComprobante(),
                                    monto,
                                    metodoPago),
                            pdf,
                            archivoComprobante);

                } catch (Exception ex) {
                    log.error("Error al enviar comprobante por correo: {}", ex.getMessage(), ex);
                }
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Pago registrado exitosamente.");

            return "redirect:/recepcionista/pagos/ver/" + pago.getId();

        } catch (Exception e) {

            log.error("Error al guardar pago: {}", e.getMessage(), e);

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Error al registrar el pago: " + e.getMessage());

            return "redirect:/recepcionista/pagos";
        }
    }

    @GetMapping("/pagos/ver/{id}")
    public String verDetallePago(@PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        usuarioRepository.findByUsername(getNombreUsuario())
                .ifPresent(u -> model.addAttribute("nombreUsuarioCompleto", u.getNombre()));


        try {
            Pago pago = pagoService.buscarPorId(id);
            model.addAttribute("pago", pago);
            return "Recepcionista/pago-detalle";
        } catch (Exception e) {
            log.error("Error al ver pago {}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Pago no encontrado");
            return "redirect:/recepcionista/pagos";
        }
    }

    @PostMapping("/pagos/actualizar-estado/{id}")
    public String actualizarEstadoPago(@PathVariable Long id,
            @RequestParam String estado,
            RedirectAttributes redirectAttributes) {
        try {
            pagoService.actualizarEstado(id, estado);
            redirectAttributes.addFlashAttribute("success", "Estado del pago actualizado a: " + estado);
        } catch (Exception e) {
            log.error("Error al actualizar estado de pago {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el estado: " + e.getMessage());
        }
        return "redirect:/recepcionista/pagos";
    }

    // ============ PERFIL ============
    @GetMapping("/perfil")
    public String perfil(Model model) {
        String username = getNombreUsuario();
        model.addAttribute("nombreUsuario", username);

        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombreCompleto", usuario.getNombre());
        });

        return "Recepcionista/perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@RequestParam String nombre,
            @RequestParam String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {
        String username = getNombreUsuario();
        
        try {
            // ===== VALIDACIONES DE EMAIL =====
            // 1. Validar que el email no esté vacío
            if (email == null || email.trim().isEmpty()) {
                log.warn("Intento de actualizar perfil con email vacío para usuario: {}", username);
                redirectAttributes.addFlashAttribute("error", "El correo electrónico no puede estar vacío. Por favor, ingrese un email válido.");
                return "redirect:/recepcionista/perfil";
            }

            // 2. Validar formato del email usando regex
            if (!isValidEmail(email)) {
                log.warn("Intento de actualizar perfil con email inválido: {} para usuario: {}", email, username);
                redirectAttributes.addFlashAttribute("error", "Formato de correo electrónico inválido. Por favor, ingrese un email válido (ejemplo: usuario@dominio.com).");
                return "redirect:/recepcionista/perfil";
            }

            // 3. Obtener el usuario actual
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // 4. Verificar que el email no esté siendo usado por otro usuario
            usuarioRepository.findByEmail(email.trim()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(usuario.getId())) {
                    throw new RuntimeException("El correo electrónico '" + email.trim() + "' ya está registrado por otro usuario.");
                }
            });

            // 5. Actualizar datos del usuario
            usuario.setNombre(nombre);
            usuario.setEmail(email.trim());

            // 6. Cambio de contraseña (si se proporciona)
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                // Validar longitud mínima de contraseña
                if (newPassword.length() < 6) {
                    throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres.");
                }
                
                if (currentPassword == null || !passwordEncoder.matches(currentPassword, usuario.getPassword())) {
                    throw new RuntimeException("La contraseña actual es incorrecta.");
                }
                usuario.setPassword(passwordEncoder.encode(newPassword));
                log.info("Contraseña actualizada para usuario: {}", username);
            }

            // 7. Guardar cambios
            usuarioRepository.save(usuario);
            log.info("Perfil actualizado exitosamente para usuario: {} con email: {}", username, email.trim());
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
            
        } catch (Exception e) {
            log.error("Error al actualizar perfil para usuario {}: {}", username, e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (errorMessage.contains("ya está registrado")) {
                redirectAttributes.addFlashAttribute("error", errorMessage);
            } else if (errorMessage.contains("contraseña")) {
                redirectAttributes.addFlashAttribute("error", "Error de seguridad: " + errorMessage);
            } else if (errorMessage.contains("formato")) {
                redirectAttributes.addFlashAttribute("error", errorMessage);
            } else {
                redirectAttributes.addFlashAttribute("error", "Error al actualizar el perfil. Por favor, verifique los datos ingresados.");
            }
        }
        return "redirect:/recepcionista/perfil";
    }

    // ============ MACHINE LEARNING ============
    @GetMapping("/diagnostico")
    public String diagnostico(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Mascota> mascotas = mascotaService.listarTodos();
        model.addAttribute("mascotas", mascotas != null ? mascotas : new ArrayList<>());
        return "Recepcionista/diagnostico";
    }
}