package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PromocionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

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

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("promociones", promocionService.listarActivas());
        return "Vendedor/promociones";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        List<Producto> productos = productoService.listarTodos();
        model.addAttribute("productos", productos);
        model.addAttribute("promocion", new Promocion());
        model.addAttribute("modo", "nuevo");
        return "Vendedor/promocion-form";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        List<Producto> productos = productoService.listarTodos();
        Promocion promocion = promocionService.buscarPorId(id);
        model.addAttribute("productos", productos);
        model.addAttribute("promocion", promocion);
        model.addAttribute("modo", "editar");
        return "Vendedor/promocion-form";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam(required = false) Long id,
            @RequestParam Long productoId,
            @RequestParam(required = false) BigDecimal descuento,
            @RequestParam(required = false, defaultValue = "true") Boolean activa,
            RedirectAttributes redirectAttributes) {

        try {
            promocionService.guardar(id, productoId, descuento, activa);
            redirectAttributes.addFlashAttribute("success", "Promoción guardada correctamente.");
            notificacionService.enviarNotificacionUI(
                    "success",
                    "Promoción guardada" + (activa != null && activa ? " (activa)" : " (inactiva)") + "."
            );
            return "redirect:/vendedor/promociones";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar la promoción: " + (e.getMessage() != null ? e.getMessage() : ""));
            return "redirect:/vendedor/promociones";
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            promocionService.eliminar(id);
            redirectAttributes.addFlashAttribute("success", "Promoción eliminada.");
            notificacionService.enviarNotificacionUI("warning", "Promoción eliminada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la promoción.");
        }
        return "redirect:/vendedor/promociones";
    }
}

