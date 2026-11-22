package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.ChatbotMensaje;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ChatbotMensajeRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.CitaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ClienteService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MascotaService;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotDataController {

    private final CitaService citaService;
    private final ClienteService clienteService;
    private final MascotaService mascotaService;
    private final ChatbotMensajeRepository chatbotMensajeRepository;

    public ChatbotDataController(CitaService citaService,
            ClienteService clienteService,
            MascotaService mascotaService,
            ChatbotMensajeRepository chatbotMensajeRepository) {
        this.citaService = citaService;
        this.clienteService = clienteService;
        this.mascotaService = mascotaService;
        this.chatbotMensajeRepository = chatbotMensajeRepository;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        LocalDate hoy = LocalDate.now();
        return Map.of(
                "fecha", hoy.toString(),
                "citasHoy", citaService.obtenerCitasDelDia(hoy).size(),
                "citasPendientes", citaService.obtenerCitasPendientes().size(),
                "clientes", clienteService.contarClientes(),
                "mascotas", mascotaService.contarMascotas(),
                "totalMensajesChatbot", chatbotMensajeRepository.count());
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

    @PostMapping("/mensajes")
    public ResponseEntity<Map<String, Object>> guardarMensaje(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.getOrDefault("session_id", payload.get("sessionId"));
        String rol = (String) payload.getOrDefault("role", payload.get("rol"));
        String contenido = (String) payload.getOrDefault("content", payload.get("contenido"));
        String intencion = (String) payload.getOrDefault("intent", payload.get("intencion"));
        String proveedor = (String) payload.getOrDefault("provider", payload.get("proveedor"));
        
        Integer confianza = null;
        if (payload.containsKey("confianza") && payload.get("confianza") != null) {
            try {
                confianza = Integer.valueOf(payload.get("confianza").toString());
            } catch (NumberFormatException ignored) {}
        }

        if (sessionId == null || sessionId.isBlank() || rol == null || contenido == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "session_id, role y content son obligatorios."));
        }

        ChatbotMensaje mensaje = new ChatbotMensaje(sessionId, rol, contenido, intencion, proveedor, confianza);
        ChatbotMensaje guardado = chatbotMensajeRepository.save(mensaje);

        return ResponseEntity.ok(Map.of(
                "status", "saved",
                "id", guardado.getId(),
                "sessionId", guardado.getSessionId(),
                "fecha", guardado.getFecha().toString()));
    }

    @GetMapping("/historial/{sessionId}")
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorial(@PathVariable String sessionId) {
        List<ChatbotMensaje> mensajes = chatbotMensajeRepository.findBySessionIdOrderByFechaAsc(sessionId);
        List<Map<String, Object>> resultado = mensajes.stream()
                .map(m -> Map.<String, Object>of(
                        "id", m.getId(),
                        "role", m.getRol(),
                        "content", m.getContenido(),
                        "intent", m.getIntencion() != null ? m.getIntencion() : "",
                        "provider", m.getProveedor() != null ? m.getProveedor() : "",
                        "confianza", m.getConfianza() != null ? m.getConfianza() : 0,
                        "timestamp", m.getFecha().toString()))
                .toList();
        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/historial/{sessionId}")
    public ResponseEntity<Map<String, Object>> borrarHistorial(@PathVariable String sessionId) {
        chatbotMensajeRepository.deleteBySessionId(sessionId);
        return ResponseEntity.ok(Map.of("status", "deleted", "sessionId", sessionId));
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
