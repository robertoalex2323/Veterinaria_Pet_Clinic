package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;

@RestController
@RequestMapping("/vendedor/api/ui-notifications")
public class VendedorNotificationController {

    private final NotificacionService notificacionService;

    public VendedorNotificationController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public List<NotificacionService.UINotification> getUINotifications() {
        return notificacionService.getAndClearUINotifications();
    }
}

