package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Agenda;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Pago;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.AgendaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ClienteService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MascotaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PagoService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AgendaRepository agendaRepository;
    private final CitaService citaService;
    private final ClienteService clienteService;
    private final MascotaService mascotaService;
    private final PagoService pagoService;
    private final UsuarioRepository usuarioRepository;

    public AdminController(AgendaRepository agendaRepository,
            CitaService citaService,
            ClienteService clienteService,
            MascotaService mascotaService,
            PagoService pagoService,
            UsuarioRepository usuarioRepository) {
        this.agendaRepository = agendaRepository;
        this.citaService = citaService;
        this.clienteService = clienteService;
        this.mascotaService = mascotaService;
        this.pagoService = pagoService;
        this.usuarioRepository = usuarioRepository;
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
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("totalClientes", clienteService.contarClientes());
        model.addAttribute("totalMascotas", mascotaService.contarMascotas());
        model.addAttribute("totalCitas", citaService.listarTodas().size());
        model.addAttribute("totalPagos", pagoService.listarTodos().size());
        model.addAttribute("totalAgendas", agendaRepository.count());
        return "admin/dashboard";
    }

    @GetMapping({"/usuarios", "/usuarios-admin"})
    public String listarUsuarios(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios != null ? usuarios : new ArrayList<>());
        return "admin/usuarios-admin";
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
