package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/vendedor")
public class VendedorCajaController {

    // Horario laboral de la caja (ajustar según el horario real de la veterinaria)
    private static final LocalTime HORA_APERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(20, 0);
    private static final DateTimeFormatter HORA_FORMATO = DateTimeFormatter.ofPattern("HH:mm");

    @GetMapping("/caja")
    public String caja(Model model, Principal principal, Authentication authentication) {
        // Solo datos de vendedor (sin mezclar farmaceutico)
        model.addAttribute("nombreUsuario", principal != null ? principal.getName() : null);
        return "Vendedor/caja";
    }

    // ============ ESTADO DEL TURNO (para el badge de Caja) ============
    @GetMapping("/api/turno-status")
    @ResponseBody
    public Map<String, Object> turnoStatus() {
        Map<String, Object> resp = new HashMap<>();

        LocalTime ahora = LocalTime.now();
        boolean abierto = !ahora.isBefore(HORA_APERTURA) && ahora.isBefore(HORA_CIERRE);

        resp.put("abierto", abierto);
        resp.put("mensaje", abierto ? "Turno Abierto" : "Turno Cerrado");
        resp.put("horaApertura", HORA_APERTURA.format(HORA_FORMATO));
        resp.put("horaCierre", HORA_CIERRE.format(HORA_FORMATO));
        resp.put("horaActual", ahora.format(HORA_FORMATO));

        return resp;
    }
}

