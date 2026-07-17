package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PromocionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.WebDataBinder;
import java.beans.PropertyEditorSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendedor/promociones")
public class VendedorPromocionController {

    private final PromocionService promocionService;
    private final ProductoService productoService;
    private final NotificacionService notificacionService;

    public VendedorPromocionController(PromocionService promocionService,
                                       ProductoService productoService,
                                       NotificacionService notificacionService) {
        this.promocionService = promocionService;
        this.productoService = productoService;
        this.notificacionService = notificacionService;
    }

    // Muestra TODAS las promociones (activas e inactivas) para poder administrarlas.
    @GetMapping
    public String listar(Model model) {
        List<Promocion> promociones = promocionService.getTodasPromociones();

        List<Map<String, Object>> vista = promociones.stream()
                .map(this::construirVistaPromocion)
                .collect(Collectors.toList());

        model.addAttribute("promociones", vista);
        return "Vendedor/promociones";
    }

    private Map<String, Object> construirVistaPromocion(Promocion p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("nombre", p.getNombre());
        m.put("descripcion", p.getDescripcion());
        m.put("tipo", p.getTipo());
        m.put("descuento", p.getDescuento());
        m.put("categoriaAplicable", p.getCategoriaAplicable());
        m.put("montoMinimo", p.getMontoMinimo());
        m.put("fechaInicio", p.getFechaInicio());
        m.put("fechaFin", p.getFechaFin());
        m.put("activo", p.getActivo());

        LocalDate hoy = LocalDate.now();
        String estadoVigencia;
        if (!Boolean.TRUE.equals(p.getActivo())) {
            estadoVigencia = "INACTIVA";
        } else if (p.getFechaInicio() != null && hoy.isBefore(p.getFechaInicio())) {
            estadoVigencia = "PROGRAMADA";
        } else if (p.getFechaFin() != null && hoy.isAfter(p.getFechaFin())) {
            estadoVigencia = "VENCIDA";
        } else {
            estadoVigencia = "ACTIVA";
        }
        m.put("estadoVigencia", estadoVigencia);

        String productoNombre = null;
        String productoCategoria = p.getCategoriaAplicable();
        BigDecimal precioOferta = null;

        if (p.getProductoId() != null) {
            try {
                Producto producto = productoService.buscarPorId(p.getProductoId());
                productoNombre = producto.getNombre();
                productoCategoria = producto.getCategoria();

                if (producto.getPrecio() != null && p.getDescuento() != null
                        && "PORCENTAJE".equalsIgnoreCase(p.getTipo())) {
                    BigDecimal factor = BigDecimal.ONE.subtract(
                            p.getDescuento().divide(new BigDecimal("100"))
                    );
                    precioOferta = producto.getPrecio().multiply(factor).setScale(2, RoundingMode.HALF_UP);
                }
            } catch (Exception ex) {
                productoNombre = "Producto no encontrado";
            }
        }

        m.put("productoNombre", productoNombre != null ? productoNombre : "Todos los productos");
        m.put("productoCategoria", productoCategoria);
        m.put("precioOferta", precioOferta);
        return m;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        List<Producto> productos = productoService.listarTodos();
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", obtenerCategoriasDisponibles(productos));
        if (!model.containsAttribute("promocion")) {
            model.addAttribute("promocion", new Promocion());
        }
        model.addAttribute("modo", "nuevo");
        model.addAttribute("diasSeleccionados", List.of());
        return "Vendedor/promocion-form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        List<Producto> productos = productoService.listarTodos();
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", obtenerCategoriasDisponibles(productos));
        if (!model.containsAttribute("promocion")) {
            Promocion promocion = promocionService.getPromocionPorId(id);
            model.addAttribute("promocion", promocion);
            model.addAttribute("diasSeleccionados", parseDias(promocion.getDiasSemana()));
        } else {
            model.addAttribute("diasSeleccionados", List.of());
        }
        model.addAttribute("modo", "editar");
        return "Vendedor/promocion-form";
    }

    private List<String> parseDias(String diasSemana) {
        if (diasSemana == null || diasSemana.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(diasSemana.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> obtenerCategoriasDisponibles(List<Producto> productos) {
        return productos.stream()
                .map(Producto::getCategoria)
                .filter(c -> c != null && !c.isBlank())
                .map(String::trim)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(BigDecimal.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : new BigDecimal(text.trim().replace(",", ".")));
            }
        });
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(text == null || text.isBlank() ? null : LocalDate.parse(text.trim()));
            }
        });
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Long id,
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam String tipo,
            @RequestParam(required = false) BigDecimal descuento,
            @RequestParam(required = false) String categoriaAplicable,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) BigDecimal montoMinimo,
            @RequestParam(required = false) List<String> diasSemana,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @RequestParam(required = false, defaultValue = "true") Boolean activo,
            RedirectAttributes redirectAttributes) {

        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String tipoLimpio = tipo == null ? "" : tipo.trim();

        if (nombreLimpio.isEmpty() || tipoLimpio.isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "Completa el nombre y el tipo de la promoción.");
            return "redirect:" + (id == null ? "/vendedor/promociones/nuevo" : "/vendedor/promociones/editar/" + id);
        }

        BigDecimal descuentoVal = descuento;
        if (descuentoVal == null) {
            if ("2X1".equalsIgnoreCase(tipoLimpio)) {
                descuentoVal = BigDecimal.ZERO;
            } else {
                redirectAttributes.addFlashAttribute("error", "El descuento es obligatorio para este tipo de promoción.");
                return "redirect:" + (id == null ? "/vendedor/promociones/nuevo" : "/vendedor/promociones/editar/" + id);
            }
        }

        try {
            Promocion promocion = new Promocion();
            promocion.setNombre(nombreLimpio);
            promocion.setDescripcion(descripcion);
            promocion.setTipo(tipoLimpio);
            promocion.setDescuento(descuentoVal);
            promocion.setCategoriaAplicable(categoriaAplicable != null && !categoriaAplicable.isBlank() ? categoriaAplicable.trim() : null);
            promocion.setProductoId(productoId);
            promocion.setMontoMinimo(montoMinimo);
            promocion.setDiasSemana(diasSemana != null && !diasSemana.isEmpty() ? String.join(",", diasSemana) : null);
            promocion.setFechaInicio(fechaInicio);
            promocion.setFechaFin(fechaFin);
            promocion.setActivo(activo != null ? activo : true);

            if (id == null) {
                promocionService.guardarPromocion(promocion);
                redirectAttributes.addFlashAttribute("success", "Promoción guardada correctamente.");
            } else {
                promocionService.actualizarPromocion(id, promocion);
                redirectAttributes.addFlashAttribute("success", "Promoción actualizada correctamente.");
            }

            notificacionService.enviarNotificacionUI(
                    "success",
                    "Promoción guardada" + (Boolean.TRUE.equals(activo) ? " (activa)" : " (inactiva)") + "."
            );
            return "redirect:/vendedor/promociones";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar la promoción: " + (e.getMessage() != null ? e.getMessage() : ""));
            return "redirect:" + (id == null ? "/vendedor/promociones/nuevo" : "/vendedor/promociones/editar/" + id);
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promocionService.eliminarPromocion(id);
            redirectAttributes.addFlashAttribute("success", "Promoción eliminada.");
            notificacionService.enviarNotificacionUI("warning", "Promoción eliminada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la promoción.");
        }
        return "redirect:/vendedor/promociones";
    }

}