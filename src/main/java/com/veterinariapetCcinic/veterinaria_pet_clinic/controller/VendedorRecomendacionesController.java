package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;


import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Recomendacion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecomendacionRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PromocionService;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/vendedor")
public class VendedorRecomendacionesController {

    private final ProductoService productoService;
    private final PromocionService promocionService;
    private final RecomendacionRepository recomendacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;

    public VendedorRecomendacionesController(ProductoService productoService,
                                             PromocionService promocionService,
                                             RecomendacionRepository recomendacionRepository,
                                             UsuarioRepository usuarioRepository,
                                             ClienteRepository clienteRepository) {
        this.productoService = productoService;
        this.promocionService = promocionService;
        this.recomendacionRepository = recomendacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping("/recomendaciones")
    public String recomendaciones(Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("nombreUsuario", auth != null ? auth.getName() : null);
        
        List<String> categorias = productoService.listarTodos().stream()
            .map(Producto::getCategoria)
            .filter(c -> c != null && !c.trim().isEmpty())
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        model.addAttribute("categorias", categorias);

        return "Vendedor/recomendaciones";
    }

    /**
     * Endpoint relacionado con el módulo real del sistema:
     * devuelve productos que tienen PROMOCIÓN ACTIVA.
     */
    @GetMapping(value = "/recomendaciones/api", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> recomendacionesApi(
            @RequestParam(value = "pet", required = false) String pet,
            @RequestParam(value = "pref", required = false) String pref) {

        String petQ = pet == null ? "" : pet.trim().toLowerCase(Locale.ROOT);
        String prefQ = pref == null ? "" : pref.trim().toLowerCase(Locale.ROOT);

        // Promos activas del sistema (fuente 1 de recomendaciones)
        List<Promocion> activas = promocionService.getPromocionesActivas();


        // Filtrado opcional por texto: si no hay coincidencia, hacemos fallback con productos activos.
        List<Map<String, Object>> items = new ArrayList<>();

        for (Promocion promo : (activas != null ? activas : new ArrayList<Promocion>())) {
            if (promo == null || promo.getProductoId() == null) continue;

            Producto p = productoService.buscarPorId(promo.getProductoId());

            if (p.getId() == null) continue;

            String nombre = p.getNombre() == null ? "" : p.getNombre().toLowerCase(Locale.ROOT);
            String categoria = p.getCategoria() == null ? "" : p.getCategoria().toLowerCase(Locale.ROOT);

            boolean matchText = true;
            if (!petQ.isBlank()) {
                matchText = nombre.contains(petQ) || categoria.contains(petQ);
            }
            // Si pref existe, lo mapeamos a palabras presentes en categoría/nombre (regla mínima, pero sobre promociones reales)
            if (matchText && !prefQ.isBlank()) {
                matchText = switch (prefQ) {
                    case "descuento" -> nombre.contains("promo") || nombre.contains("oferta") || categoria.contains("promo") || categoria.contains("oferta");
                    case "gastro" -> categoria.contains("gastro") || nombre.contains("gastro");
                    case "piel" -> categoria.contains("piel") || nombre.contains("piel") || categoria.contains("pelaje") || nombre.contains("pelaje");
                    case "vacunas" -> categoria.contains("vacun") || nombre.contains("vacun");
                    default -> true;
                };
            }

            if (!matchText) continue;

            BigDecimal precio = p.getPrecio();
            String precioFormateado = precio == null ? "-" : precio.toPlainString();

            Map<String, Object> item = new HashMap<>();
            item.put("id", p.getId());
            item.put("nombre", p.getNombre());
            item.put("categoria", p.getCategoria());
            item.put("precioFormateado", precioFormateado);

            BigDecimal descuento = promo.getDescuento();
            if (descuento == null) descuento = BigDecimal.ZERO;

            item.put("razon", "Producto con promoción activa (descuento: " + descuento.toPlainString() + ").");

            items.add(item);
        }

        // fallback 1: si no había matches por pet/pref, devolvemos todas las promos activas (sin filtrar)
        if (items.isEmpty() && activas != null && !activas.isEmpty()) {
            items = new ArrayList<>();
            for (Promocion promo : activas) {
                if (promo == null || promo.getProductoId() == null) continue;
                Producto p = productoService.buscarPorId(promo.getProductoId());
                if (p == null || p.getId() == null) continue;

                BigDecimal precio = p.getPrecio();

                String precioFormateado = precio == null ? "-" : precio.toPlainString();
                BigDecimal descuento = promo.getDescuento() == null ? BigDecimal.ZERO : promo.getDescuento();

                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("nombre", p.getNombre());
                item.put("categoria", p.getCategoria());
                item.put("precioFormateado", precioFormateado);
                item.put("razon", "Producto con promoción activa (descuento: " + descuento.toPlainString() + ").");
                items.add(item);
            }
        }

        // fallback 2: si sigue vacío (ej: no hay promos activas), usamos productos activos y aplicamos filtros mínimos
        if (items.isEmpty()) {
            List<Producto> catalogo = productoService.listarTodos();
            if (catalogo != null) {
                for (Producto p : catalogo) {
                    if (p == null || p.getId() == null) continue;

                    String nombre = p.getNombre() == null ? "" : p.getNombre().toLowerCase(Locale.ROOT);
                    String categoria = p.getCategoria() == null ? "" : p.getCategoria().toLowerCase(Locale.ROOT);

                    boolean match = true;
                    if (!petQ.isBlank()) {
                        match = nombre.contains(petQ) || categoria.contains(petQ);
                    }
                    if (match && !prefQ.isBlank()) {
                        match = switch (prefQ) {
                            case "descuento" -> nombre.contains("promo") || nombre.contains("oferta") || categoria.contains("promo") || categoria.contains("oferta");
                            case "gastro" -> categoria.contains("gastro") || nombre.contains("gastro");
                            case "piel" -> categoria.contains("piel") || nombre.contains("piel") || categoria.contains("pelaje") || nombre.contains("pelaje");
                            case "vacunas" -> categoria.contains("vacun") || nombre.contains("vacun");
                            default -> true;
                        };
                    }
                    if (!match) continue;

                    BigDecimal precio = p.getPrecio();
                    String precioFormateado = precio == null ? "-" : precio.toPlainString();

                    int stock = p.getStock() != null ? p.getStock() : 0;
                    String razon;
                    if (stock <= 5) {
                        razon = "📦 Bajo stock (" + stock + "). Recomendado para rotación.";
                    } else {
                        razon = "🎯 Recomendación por catálogo y preferencia.";
                    }

                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("nombre", p.getNombre());
                    item.put("categoria", p.getCategoria());
                    item.put("precioFormateado", precioFormateado);
                    item.put("razon", razon);
                    items.add(item);

                    if (items.size() >= 12) break;
                }
            }
        }

        String aiMessage = "¡Hola! He analizado el catálogo y las preferencias solicitadas. ";
        if (items.isEmpty()) {
            aiMessage += "No encontré promociones específicas para esta categoría o preferencia, así que te muestro nuestras mejores opciones con descuento.";
        } else {
            aiMessage += "Aquí tienes las recomendaciones ideales que he seleccionado especialmente para el cliente.";
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("message", aiMessage);
        
        return response;
    }

    @PostMapping(value = "/recomendaciones/registrar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> registrarRecomendacion(
            @RequestParam(value = "productoId") Long productoId,
            @RequestParam(value = "categoria", required = false) String categoria,
            @RequestParam(value = "razon", required = false) String razon,
            @RequestParam(value = "clienteId", required = false) Long clienteId) {

        Map<String, Object> resp = new HashMap<>();

        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            if (username == null || username.isBlank()) {
                resp.put("success", false);
                resp.put("message", "No autenticado");
                return resp;
            }

            Usuario vendedor = usuarioRepository.findByUsername(username).orElse(null);
            if (vendedor == null) {
                resp.put("success", false);
                resp.put("message", "Vendedor no encontrado");
                return resp;
            }

            Producto producto = productoService.buscarPorId(productoId);

            String categoriaFinal = (categoria == null || categoria.isBlank())
                    ? (producto.getCategoria() != null ? producto.getCategoria() : "Sin categoría")
                    : categoria;

            String razonFinal = (razon == null || razon.isBlank())
                    ? "Recomendación registrada"
                    : razon;

            Cliente cliente = null;
            if (clienteId != null) {
                cliente = clienteRepository.findById(clienteId).orElse(null);
            }

            Recomendacion rec = new Recomendacion();
            rec.setProducto(producto);
            rec.setCategoria(categoriaFinal);
            rec.setRazon(razonFinal);
            rec.setVendedor(vendedor);
            rec.setCliente(cliente);
            rec.setFecha(LocalDateTime.now());

            recomendacionRepository.save(rec);

            resp.put("success", true);
            resp.put("message", "Recomendación registrada como exitosa");
            return resp;

        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", e.getMessage() != null ? e.getMessage() : "Error al registrar");
            return resp;
        }
    }
}


