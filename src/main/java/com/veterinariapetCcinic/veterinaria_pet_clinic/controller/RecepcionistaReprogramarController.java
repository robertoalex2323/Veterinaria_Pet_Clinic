package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;

@RestController
@RequestMapping("/recepcionista/citas")
public class RecepcionistaReprogramarController {

    private final CitaService citaService;

    public RecepcionistaReprogramarController(CitaService citaService) {
        this.citaService = citaService;
    }

    @PostMapping("/reprogramar-modal")
    public ResponseEntity<Map<String, Object>> reprogramarModal(
            @RequestParam Long reprogramarDesdeId,
            @RequestParam String fechaNueva,
            @RequestParam String horaNueva,
            @RequestParam(required = false) String motivo) {

        try {
            fechaNueva = fechaNueva == null ? null : fechaNueva.trim();
            horaNueva = horaNueva == null ? null : horaNueva.trim();

            if (fechaNueva == null || fechaNueva.isBlank() || horaNueva == null || horaNueva.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "error", "Fecha y hora nuevas son obligatorias."
                ));
            }

            // Validación de formato para evitar parseos incorrectos
            if (!fechaNueva.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "error", "Formato de fecha inválido (se espera yyyy-MM-dd)."
                ));
            }

            if (!horaNueva.matches("\\d{2}:\\d{2}")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "ok", false,
                        "error", "Formato de hora inválido (se espera HH:mm)."
                ));
            }

            // fechaNueva: yyyy-MM-dd
            // horaNueva: HH:mm
            // Construimos LocalDateTime manualmente para evitar problemas de parseo
            DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("HH:mm");
            java.time.LocalDateTime nuevaFechaHora = java.time.LocalDateTime.of(
                    java.time.LocalDate.parse(fechaNueva),
                    java.time.LocalTime.parse(horaNueva, hourFmt)
            );



            String motivoFinal = (motivo == null || motivo.isBlank()) ? "Reprogramada por recepcionista" : motivo.trim();
            citaService.reprogramarCita(reprogramarDesdeId, nuevaFechaHora, motivoFinal);

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Cita reprogramada exitosamente"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", e.getMessage()
            ));
        }
    }
}

