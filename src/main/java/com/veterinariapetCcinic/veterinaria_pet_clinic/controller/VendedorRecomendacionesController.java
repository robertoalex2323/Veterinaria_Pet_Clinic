package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PromocionService;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import java.math.BigDecimal;
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

    public VendedorRecomendacionesController(ProductoService productoService,
                                             PromocionService promocionService) {
        this.productoService = productoService;
        this.promocionService = promocionService;
    }

    @GetMapping("/recomendaciones")
    public String recomendaciones(Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("nombreUsuario", auth != null ? auth.getName() : null);
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

        // Promos activas del sistema
        List<Promocion> activas = promocionService.listarActivas();

        // Filtrado opcional por texto: si no hay coincidencia, igualmente mostramos promos.
        List<Map<String, Object>> items = new ArrayList<>();

        for (Promocion promo : activas) {
            if (promo == null || promo.getProducto() == null) continue;

            Producto p = promo.getProducto();
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

        // fallback: si no había matches por pet/pref, devolvemos todas las promos activas (sin filtrar)
        if (items.isEmpty() && activas != null && !activas.isEmpty()) {
            items = new ArrayList<>();
            for (Promocion promo : activas) {
                if (promo == null || promo.getProducto() == null || promo.getProducto().getId() == null) continue;
                Producto p = promo.getProducto();

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

        return Map.of("items", items);
    }
}

