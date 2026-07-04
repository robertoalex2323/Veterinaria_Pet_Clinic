package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;

@RestController
@RequestMapping("/recepcionista/api")
public class UniversalSearchController {

    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;

    public UniversalSearchController(ClienteRepository clienteRepository, MascotaRepository mascotaRepository) {
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
    }

    @GetMapping("/busqueda-universal")
    @ResponseBody
    public List<UniversalSuggestion> busquedaUniversal(@RequestParam String query) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) return List.of();

        String qLower = q.toLowerCase();
        List<UniversalSuggestion> results = new ArrayList<>();

        // --- Clientes ---
        List<Cliente> clientes = clienteRepository.findByNombreContainingIgnoreCase(q);
        for (Cliente c : clientes) {
            results.add(new UniversalSuggestion(
                    "CLIENTE",
                    c.getId(),
                    safe(c.getNombre()),
                    safe(c.getTelefono()),
                    "/recepcionista/clientes/ver/" + c.getId()));
        }

        // Teléfono exacto (si query parece teléfono)
        if (q.matches("[0-9+\\-\\s]+")) {
            clienteRepository.findByTelefono(q).ifPresent(c -> {
                results.add(new UniversalSuggestion(
                        "CLIENTE",
                        c.getId(),
                        safe(c.getNombre()),
                        safe(c.getTelefono()),
                        "/recepcionista/clientes/ver/" + c.getId()));
            });
        }


        // --- Mascotas ---
        List<Mascota> mascotas = mascotaRepository.findByNombreContainingIgnoreCase(q);
        for (Mascota m : mascotas) {
            String clienteNombre = (m.getCliente() != null) ? m.getCliente().getNombre() : "";
            results.add(new UniversalSuggestion(
                    "MASCOTA",
                    m.getId(),
                    safe(m.getNombre()),
                    clienteNombre,
                    "/recepcionista/mascotas/editar/" + m.getId()));
        }

        // Si no hay resultados por nombre de mascota, intentamos por especie/raza usando contains en memoria (con poca data)
        if (results.isEmpty() || results.stream().noneMatch(r -> "MASCOTA".equals(r.type))) {

            List<Mascota> allMascotas = mascotaRepository.findAllWithCliente();
            for (Mascota m : allMascotas) {
                String nombre = safe(m.getNombre()).toLowerCase();
                String especie = safe(m.getEspecie()).toLowerCase();
                String raza = safe(m.getRaza()).toLowerCase();
                if (nombre.contains(qLower) || especie.contains(qLower) || raza.contains(qLower)) {
                    String clienteNombre = (m.getCliente() != null) ? m.getCliente().getNombre() : "";
                    results.add(new UniversalSuggestion(
                            "MASCOTA",
                            m.getId(),
                            safe(m.getNombre()),
                            clienteNombre + (especie.isEmpty() ? "" : " - " + m.getEspecie()),
                            "/recepcionista/mascotas/editar/" + m.getId()));
                }
            }
        }

        // Deduplicar por tipo+id
        List<UniversalSuggestion> dedup = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (UniversalSuggestion s : results) {
            String key = s.type + ":" + s.id;
            if (seen.add(key)) dedup.add(s);
        }

        // Limitar
        int limit = Math.min(10, dedup.size());
        return dedup.subList(0, limit);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static class UniversalSuggestion {
        public String type; // CLIENTE | MASCOTA
        public Long id;
        public String label;
        public String sublabel;
        public String url;

        public UniversalSuggestion() {
        }

        public UniversalSuggestion(String type, Long id, String label, String sublabel, String url) {
            this.type = type;
            this.id = id;
            this.label = label;
            this.sublabel = sublabel;
            this.url = url;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getSublabel() {
            return sublabel;
        }

        public void setSublabel(String sublabel) {
            this.sublabel = sublabel;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getSublabelCompat() {
            return sublabel;
        }
    }
}

