package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinariapetCcinic.veterinaria_pet_clinic.dto.DashboardDTO;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.Veterinaria.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardDTO obtenerDashboard() {
        return dashboardService.obtenerDashboard();
    }
}