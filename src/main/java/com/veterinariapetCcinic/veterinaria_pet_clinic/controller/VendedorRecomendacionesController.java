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
import java.time.format.DateTimeFormatter;
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
     * Normaliza el tipo de promoción (quita espacios y pasa a mayúsculas)
     * para que las comparaciones nunca fallen por espacios o mayúsculas/minúsculas.
     */
    private String tipoNormalizado(Promocion promo) {
        if (promo == null || promo.getTipo() == null) return "";
        return promo.getTipo().trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Único punto de verdad para decidir si una promoción coincide con la
     * preferencia elegida por el vendedor. Si el vendedor pidió "2x1" y la
     * promoción es de otro tipo (o viceversa), NUNCA debe pasar este filtro.
     */
    private boolean coincideTipo(String prefQ, Promocion promo) {
        if (prefQ == null || prefQ.isBlank()) return true; // "Cualquiera"
        return prefQ.equals(tipoNormalizado(promo));
    }

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Calcula el estado real de la promoción según sus fechas y el flag "activo":
     * INACTIVA (desactivada manualmente), PROGRAMADA (aún no empieza),
     * VENCIDA (ya terminó) o ACTIVA (vigente hoy).
     */
    private String estadoPromo(Promocion promo) {
        if (promo == null) return "DESCONOCIDO";
        if (promo.getActivo() != null && !promo.getActivo()) return "INACTIVA";

        LocalDate hoy = LocalDate.now();
        if (promo.getFechaInicio() != null && hoy.isBefore(promo.getFechaInicio())) return "PROGRAMADA";
        if (promo.getFechaFin() != null && hoy.isAfter(promo.getFechaFin())) return "VENCIDA";
        return "ACTIVA";
    }

    private String rangoFechas(Promocion promo) {
        String inicio = promo.getFechaInicio() != null ? promo.getFechaInicio().format(FMT_FECHA) : "sin inicio";
        String fin = promo.getFechaFin() != null ? promo.getFechaFin().format(FMT_FECHA) : "sin fin";
        return inicio + " – " + fin;
    }

    /** Agrega fecha/estado de la promo al item que se envía al frontend. */
    private void adjuntarInfoPromo(Map<String, Object> item, Promocion promo) {
        item.put("estadoPromo", estadoPromo(promo));
        item.put("fechaInicio", promo.getFechaInicio() != null ? promo.getFechaInicio().format(FMT_FECHA) : null);
        item.put("fechaFin", promo.getFechaFin() != null ? promo.getFechaFin().format(FMT_FECHA) : null);
        item.put("rangoFechas", rangoFechas(promo));
    }

    private int calcularConfianzaPromo(Promocion promo, Producto p, boolean matchPet) {
        int score = 55;
        String tipo = tipoNormalizado(promo);
        switch (tipo) {
            case "2X1" -> score += 20;
            case "PORCENTAJE" -> {
                BigDecimal d = promo != null ? promo.getDescuento() : null;
                score += d != null ? Math.min(20, d.intValue() / 2) : 5;
            }
            case "FIJO" -> score += 12;
            default -> score += 5;
        }
        if (p != null && p.getStock() != null && p.getStock() <= 5) score += 12;
        if (matchPet) score += 8;
        return Math.min(99, score);
    }

    private int calcularConfianzaCatalogo(Producto p, boolean matchPet) {
        int score = 35;
        if (p != null && p.getStock() != null && p.getStock() <= 5) score += 20;
        if (matchPet) score += 10;
        return Math.min(70, score);
    }

    private String razonPromo(Promocion promo, int confianza, boolean stockBajo) {
        String base;
        if ("2X1".equalsIgnoreCase(promo.getTipo())) {
            base = "promoción 2x1 activa";
        } else {
            BigDecimal d = promo.getDescuento() == null ? BigDecimal.ZERO : promo.getDescuento();
            base = "descuento activo (" + d.toPlainString() + (promo.getTipo() != null && promo.getTipo().equalsIgnoreCase("PORCENTAJE") ? "%" : " S/") + ")";
        }
        String extra = stockBajo ? " y baja rotación de stock" : "";
        return "🤖 IA · " + confianza + "% de coincidencia — " + base + extra + ".";
    }


    @GetMapping(value = "/recomendaciones/api", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> recomendacionesApi(
            @RequestParam(value = "pet", required = false) String pet,
            @RequestParam(value = "pref", required = false) String pref) {

        String petQ = pet == null ? "" : pet.trim().toLowerCase(Locale.ROOT);
        String prefQ = pref == null ? "" : pref.trim().toUpperCase(Locale.ROOT);

        List<Promocion> activas = promocionService.getPromocionesActivas();

        List<Map<String, Object>> items = new ArrayList<>();
        java.util.Set<Long> idsAgregados = new java.util.HashSet<>();

        for (Promocion promo : (activas != null ? activas : new ArrayList<Promocion>())) {
            if (promo == null) continue;

            if (!coincideTipo(prefQ, promo)) continue;

            List<Producto> productosPromo = new ArrayList<>();

            if (promo.getProductoId() != null) {
                try {
                    Producto p = productoService.buscarPorId(promo.getProductoId());
                    if (p != null) productosPromo.add(p);
                } catch (Exception ex) {
                    continue;
                }
            } else if (promo.getCategoriaAplicable() != null && !promo.getCategoriaAplicable().isBlank()) {
                for (Producto p : productoService.listarTodos()) {
                    if (p != null && promo.getCategoriaAplicable().equalsIgnoreCase(p.getCategoria())) {
                        productosPromo.add(p);
                    }
                }
            } else {
                productosPromo.addAll(productoService.listarTodos());
            }

            for (Producto p : productosPromo) {
                if (p == null || p.getId() == null || idsAgregados.contains(p.getId())) continue;

                String nombre = p.getNombre() == null ? "" : p.getNombre().toLowerCase(Locale.ROOT);
                String categoria = p.getCategoria() == null ? "" : p.getCategoria().toLowerCase(Locale.ROOT);

                if (!petQ.isBlank() && !(nombre.contains(petQ) || categoria.contains(petQ))) continue;

                idsAgregados.add(p.getId());

                BigDecimal precio = p.getPrecio();
                String precioFormateado = precio == null ? "-" : precio.toPlainString();
                boolean stockBajo = p.getStock() != null && p.getStock() <= 5;
                boolean matchPet = !petQ.isBlank();
                int confianza = calcularConfianzaPromo(promo, p, matchPet);

                Map<String, Object> item = new HashMap<>();
                item.put("id", p.getId());
                item.put("nombre", p.getNombre());
                item.put("categoria", p.getCategoria());
                item.put("precioFormateado", precioFormateado);
                item.put("confianza", confianza);
                item.put("razon", razonPromo(promo, confianza, stockBajo));
                adjuntarInfoPromo(item, promo);
                items.add(item);
            }
        }

        // fallback 1: relajamos SOLO la categoría (pet), pero seguimos respetando el tipo elegido (pref)
        if (items.isEmpty() && activas != null && !activas.isEmpty()) {
            items = new ArrayList<>();
            java.util.Set<Long> idsFallback = new java.util.HashSet<>();
            for (Promocion promo : activas) {
                if (promo == null) continue;
                if (!coincideTipo(prefQ, promo)) continue;

                List<Producto> productosPromo = new ArrayList<>();
                if (promo.getProductoId() != null) {
                    try {
                        Producto p = productoService.buscarPorId(promo.getProductoId());
                        if (p != null) productosPromo.add(p);
                    } catch (Exception ex) {
                        continue;
                    }
                } else if (promo.getCategoriaAplicable() != null && !promo.getCategoriaAplicable().isBlank()) {
                    for (Producto p : productoService.listarTodos()) {
                        if (p != null && promo.getCategoriaAplicable().equalsIgnoreCase(p.getCategoria())) {
                            productosPromo.add(p);
                        }
                    }
                } else {
                    productosPromo.addAll(productoService.listarTodos());
                }

                for (Producto p : productosPromo) {
                    if (p == null || p.getId() == null || !idsFallback.add(p.getId())) continue;

                    BigDecimal precio = p.getPrecio();
                    String precioFormateado = precio == null ? "-" : precio.toPlainString();
                    boolean stockBajo = p.getStock() != null && p.getStock() <= 5;
                    int confianza = calcularConfianzaPromo(promo, p, false);

                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("nombre", p.getNombre());
                    item.put("categoria", p.getCategoria());
                    item.put("precioFormateado", precioFormateado);
                    item.put("confianza", confianza);
                    item.put("razon", razonPromo(promo, confianza, stockBajo));
                    adjuntarInfoPromo(item, promo);
                    items.add(item);
                }
            }
        }

        // fallback 2: SOLO si NO se pidió un tipo de promoción específico.
        // Si el vendedor eligió "2x1" (o cualquier otro tipo) y no hay ninguna
        // promoción activa de ese tipo, NO se debe mostrar nada del catálogo
        // general: mostrar otro producto aquí daría la falsa impresión de que
        // tiene esa promoción cuando no la tiene.
        boolean sePidioTipoEspecifico = !prefQ.isBlank();
        if (items.isEmpty() && !sePidioTipoEspecifico) {
            List<Producto> catalogo = productoService.listarTodos();
            if (catalogo != null) {
                for (Producto p : catalogo) {
                    if (p == null || p.getId() == null) continue;

                    String nombre = p.getNombre() == null ? "" : p.getNombre().toLowerCase(Locale.ROOT);
                    String categoria = p.getCategoria() == null ? "" : p.getCategoria().toLowerCase(Locale.ROOT);

                    if (!petQ.isBlank() && !(nombre.contains(petQ) || categoria.contains(petQ))) continue;

                    BigDecimal precio = p.getPrecio();
                    String precioFormateado = precio == null ? "-" : precio.toPlainString();

                    int stock = p.getStock() != null ? p.getStock() : 0;
                    boolean matchPet = !petQ.isBlank();
                    int confianza = calcularConfianzaCatalogo(p, matchPet);
                    String razon = stock <= 5
                            ? "🤖 IA · " + confianza + "% de coincidencia — bajo stock (" + stock + "), buen candidato para rotación."
                            : "🤖 IA · " + confianza + "% de coincidencia — sugerido según catálogo y preferencia.";

                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("nombre", p.getNombre());
                    item.put("categoria", p.getCategoria());
                    item.put("precioFormateado", precioFormateado);
                    item.put("confianza", confianza);
                    item.put("razon", razon);
                    items.add(item);

                    if (items.size() >= 12) break;
                }
            }
        }

        String aiMessage = "¡Hola! He analizado el catálogo y las preferencias solicitadas. ";
        if (items.isEmpty()) {
            aiMessage += !prefQ.isBlank()
                    ? "No encontré promociones activas de ese tipo para esta categoría."
                    : "No encontré promociones específicas para esta categoría o preferencia, así que te muestro nuestras mejores opciones con descuento.";
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