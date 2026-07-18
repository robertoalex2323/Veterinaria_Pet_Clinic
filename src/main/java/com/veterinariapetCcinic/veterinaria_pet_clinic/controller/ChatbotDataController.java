package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ClienteService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MascotaService;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotDataController {

    private final CitaService citaService;
    private final ClienteService clienteService;
    private final MascotaService mascotaService;

    public ChatbotDataController(CitaService citaService,
            ClienteService clienteService,
            MascotaService mascotaService) {
        this.citaService = citaService;
        this.clienteService = clienteService;
        this.mascotaService = mascotaService;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        LocalDate hoy = LocalDate.now();
        return Map.of(
                "fecha", hoy.toString(),
                "citasHoy", citaService.obtenerCitasDelDia(hoy).size(),
                "citasPendientes", citaService.obtenerCitasPendientes().size(),
                "clientes", clienteService.contarClientes(),
                "mascotas", mascotaService.contarMascotas());
    }

    @GetMapping("/mascotas")
    public List<Map<String, Object>> mascotas() {
        return mascotaService.listarTodos().stream()
                .limit(30)
                .map(this::mapMascota)
                .toList();
    }

    @GetMapping("/clientes")
    public List<Map<String, Object>> clientes() {
        return clienteService.listarTodos().stream()
                .limit(30)
                .map(this::mapCliente)
                .toList();
    }

    private Map<String, Object> mapMascota(Mascota mascota) {
        return Map.of(
                "id", mascota.getId(),
                "nombre", mascota.getNombre() != null ? mascota.getNombre() : "",
                "especie", mascota.getEspecie() != null ? mascota.getEspecie() : "",
                "cliente", mascota.getCliente() != null ? mascota.getCliente().getNombre() : "");
    }

    private Map<String, Object> mapCliente(Cliente cliente) {
        return Map.of(
                "id", cliente.getId(),
                "nombre", cliente.getNombre() != null ? cliente.getNombre() : "",
                "telefono", cliente.getTelefono() != null ? cliente.getTelefono() : "");
    }
}
