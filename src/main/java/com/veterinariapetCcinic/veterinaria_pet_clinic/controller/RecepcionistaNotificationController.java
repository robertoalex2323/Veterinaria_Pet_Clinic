package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;

@RestController
@RequestMapping("/recepcionista/api/ui-notifications")
public class RecepcionistaNotificationController {

    private final NotificacionService notificacionService;

    public RecepcionistaNotificationController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionService.UINotification> getUINotifications() {
        return notificacionService.getAndClearUINotifications();
    }
}