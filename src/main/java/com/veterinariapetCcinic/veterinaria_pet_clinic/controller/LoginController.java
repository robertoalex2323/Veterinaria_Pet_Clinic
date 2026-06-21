package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           Model model) {
        if (error != null) {
            model.addAttribute("error", "Usuario o contraseña incorrectos");
        }
        if (logout != null) {
            model.addAttribute("message", "Has cerrado sesión exitosamente");
        }
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/recepcionista/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}