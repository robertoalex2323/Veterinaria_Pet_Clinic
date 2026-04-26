package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Pago;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ClienteService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MascotaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PagoService;

@Controller
@RequestMapping("/recepcionista")
public class RecepcionistaController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private MascotaService mascotaService;

    @Autowired
    private CitaService citaService;

    @Autowired
    private PagoService pagoService;

    // AgendaService removed because it was unused

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private String getNombreUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "Recepcionista";
    }

    // ============ DASHBOARD ============
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());

        model.addAttribute("totalClientes", clienteService.contarClientes());
        model.addAttribute("totalMascotas", mascotaService.contarMascotas());
        model.addAttribute("citasHoy", citaService.contarCitasHoy());

        Double ingresosHoy = pagoService.getTotalPagosDelDia();
        model.addAttribute("ingresosHoy", ingresosHoy != null ? ingresosHoy : 0.0);

        List<Cita> proximasCitas = citaService.obtenerCitasDelDia(LocalDate.now());
        model.addAttribute("proximasCitas", proximasCitas != null ? proximasCitas : new ArrayList<>());

        List<Pago> ultimosPagos = pagoService.obtenerPagosDelDia();
        model.addAttribute("ultimosPagos", ultimosPagos != null ? ultimosPagos : new ArrayList<>());

        return "recepcionista/dashboard";
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
        return "recepcionista/clientes";
    }

    @GetMapping("/clientes/nuevo")
    public String nuevoClienteForm(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("cliente", new Cliente());
        return "recepcionista/cliente-form";
    }

    @PostMapping("/clientes/guardar")
    public String guardarCliente(@ModelAttribute Cliente cliente, RedirectAttributes redirectAttributes) {
        try {
            clienteService.guardar(cliente);
            redirectAttributes.addFlashAttribute("success", "Cliente guardado exitosamente");
        } catch (Exception e) {
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
            model.addAttribute("error", "Cliente no encontrado");
            return "redirect:/recepcionista/clientes";
        }
        return "recepcionista/cliente-form";
    }

    @PostMapping("/clientes/actualizar/{id}")
    public String actualizarCliente(@PathVariable Long id, @ModelAttribute Cliente cliente,
            RedirectAttributes redirectAttributes) {
        try {
            cliente.setId(id);
            clienteService.actualizar(cliente);
            redirectAttributes.addFlashAttribute("success", "Cliente actualizado exitosamente");
        } catch (Exception e) {
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
            model.addAttribute("error", "Cliente no encontrado");
            return "redirect:/recepcionista/clientes";
        }
        return "recepcionista/cliente-detalle";
    }

    // ============ GESTIÓN DE MASCOTAS ============
    @GetMapping("/mascotas")
    public String listarMascotas(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Mascota> mascotas = mascotaService.listarTodos();
        model.addAttribute("mascotas", mascotas != null ? mascotas : new ArrayList<>());
        return "recepcionista/mascotas";
    }

    @GetMapping("/mascotas/nuevo")
    public String nuevaMascotaForm(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("mascota", new Mascota());
        model.addAttribute("clientes", clienteService.listarTodos());
        return "recepcionista/mascota-form";
    }

    @PostMapping("/mascotas/guardar")
    public String guardarMascota(@ModelAttribute Mascota mascota, @RequestParam Long clienteId,
            RedirectAttributes redirectAttributes) {
        try {
            mascotaService.registrarMascota(clienteId, mascota);
            redirectAttributes.addFlashAttribute("success", "Mascota registrada exitosamente");
        } catch (Exception e) {
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
            model.addAttribute("error", "Mascota no encontrada");
            return "redirect:/recepcionista/mascotas";
        }
        return "recepcionista/mascota-form";
    }

    @PostMapping("/mascotas/actualizar/{id}")
    public String actualizarMascota(@PathVariable Long id, @ModelAttribute Mascota mascota,
            RedirectAttributes redirectAttributes) {
        try {
            mascota.setId(id);
            mascotaService.actualizar(mascota);
            redirectAttributes.addFlashAttribute("success", "Mascota actualizada exitosamente");
        } catch (Exception e) {
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
        model.addAttribute("citas", citas != null ? citas : new ArrayList<>());
        model.addAttribute("fecha", fecha);
        return "recepcionista/citas";
    }

    @GetMapping("/citas/nueva")
    public String nuevaCitaForm(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("cita", new Cita());
        model.addAttribute("mascotas", mascotaService.listarTodos());
        return "recepcionista/cita-form";
    }

    @PostMapping("/citas/guardar")
    public String guardarCita(@RequestParam Long mascotaId,
            @RequestParam String fecha,
            @RequestParam String hora,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes) {
        try {
            Mascota mascota = mascotaService.buscarPorId(mascotaId);

            Cita cita = new Cita();
            cita.setMascota(mascota);
            cita.setMotivo(motivo);
            cita.setEstado("AGENDADA");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime fechaHora = LocalDateTime.parse(fecha + " " + hora, formatter);
            cita.setFechaHora(fechaHora);

            citaService.guardar(cita);

            redirectAttributes.addFlashAttribute("success", "Cita agendada exitosamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
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
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/recepcionista/citas";
    }

    // ============ GESTIÓN DE AGENDA ============
    @GetMapping("/agenda")
    public String verAgenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());

        if (fecha == null) {
            fecha = LocalDate.now();
        }

        List<Cita> citasDelDia = citaService.obtenerCitasDelDia(fecha);
        model.addAttribute("citas", citasDelDia != null ? citasDelDia : new ArrayList<>());
        model.addAttribute("fecha", fecha);
        return "recepcionista/agenda";
    }

    // ============ GESTIÓN DE PAGOS (ACTUALIZADO) ============
    @GetMapping("/pagos")
    public String listarPagos(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());

        try {
            List<Pago> pagos = pagoService.listarTodos();
            model.addAttribute("pagos", pagos != null ? pagos : new ArrayList<>());

            // Datos adicionales para la vista (opcionales pero útiles)
            model.addAttribute("totalPagos", pagoService.getTotalPagos());
            model.addAttribute("totalHoy", pagoService.getTotalPagosDelDia());
            model.addAttribute("pendientes", pagoService.contarPagosPendientes());

            // Para el modal de nuevo pago
            model.addAttribute("clientes", clienteService.listarTodos());

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar los pagos: " + e.getMessage());
            model.addAttribute("pagos", new ArrayList<>());
            model.addAttribute("totalPagos", 0.0);
            model.addAttribute("totalHoy", 0.0);
            model.addAttribute("pendientes", 0L);
        }

        return "recepcionista/pagos";
    }

    @GetMapping("/pagos/nuevo")
    public String nuevoPagoForm(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("pago", new Pago());
        model.addAttribute("clientes", clienteService.listarTodos());
        return "recepcionista/pago-form";
    }

    @PostMapping("/pagos/guardar")
    public String guardarPago(@RequestParam Long clienteId,
            @RequestParam Double monto,
            @RequestParam String metodoPago,
            @RequestParam(required = false) Long citaId,
            RedirectAttributes redirectAttributes) {
        try {
            Cliente cliente = clienteService.buscarPorId(clienteId);

            Pago pago = new Pago();
            pago.setCliente(cliente);
            pago.setMonto(monto);
            pago.setMetodoPago(metodoPago);
            pago.setEstado("PAGADO");

            if (citaId != null && citaId > 0) {
                pagoService.registrarPago(pago, citaId);
            } else {
                pagoService.guardar(pago);
            }

            notificacionService.enviarConfirmacionPago(cliente, monto, metodoPago);

            redirectAttributes.addFlashAttribute("success", "Pago registrado exitosamente");
            return "redirect:/recepcionista/pagos/ver/" + pago.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar el pago: " + e.getMessage());
            return "redirect:/recepcionista/pagos";
        }
    }

    @GetMapping("/pagos/ver/{id}")
    public String verDetallePago(@PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        try {
            Pago pago = pagoService.buscarPorId(id);
            model.addAttribute("pago", pago);
            return "recepcionista/pago-detalle";
        } catch (Exception e) {
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
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el estado: " + e.getMessage());
        }
        return "redirect:/recepcionista/pagos";
    }

    // ============ PERFIL ============
    @GetMapping("/perfil")
    public String perfil(Model model) {
        String username = getNombreUsuario();
        model.addAttribute("nombreUsuario", username);

        // Fetch actual user from DB
        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombreCompleto", usuario.getNombre());
        });

        return "recepcionista/perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@RequestParam String nombre,
            @RequestParam String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {
        String username = getNombreUsuario();
        try {
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Basic update
            usuario.setNombre(nombre);
            usuario.setEmail(email);

            // Logic for password change would typically go here using PasswordEncoder
            // If we have an endpoint we could do it, but for now we just update the model
            // fields

            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar perfil: " + e.getMessage());
        }
        return "redirect:/recepcionista/perfil";
    }

    // ============ MACHINE LEARNING ============
    @GetMapping("/diagnostico")
    public String diagnostico(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Mascota> mascotas = mascotaService.listarTodos();
        model.addAttribute("mascotas", mascotas != null ? mascotas : new ArrayList<>());
        return "recepcionista/diagnostico";
    }
}