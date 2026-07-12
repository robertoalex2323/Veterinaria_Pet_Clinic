package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/vendedor")
public class VendedorCajaController {

    @GetMapping("/caja")
    public String caja(Model model, Principal principal, Authentication authentication) {
        // Solo datos de vendedor (sin mezclar farmaceutico)
        model.addAttribute("nombreUsuario", principal != null ? principal.getName() : null);
        return "Vendedor/caja";
    }
}

