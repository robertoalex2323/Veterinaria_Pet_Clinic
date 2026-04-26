package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FarmaceuticoController {

    @GetMapping("/farmaceutico")
    public String farmaceuticoPanel() {
        return "farmaceutico";
    }
}