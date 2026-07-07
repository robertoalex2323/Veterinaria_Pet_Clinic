package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.VentaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/vendedor/ventas")
public class VendedorVentaController {

    private final ProductoService productoService;
    private final VentaService ventaService;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    public VendedorVentaController(ProductoService productoService,
                                   VentaService ventaService,
                                   NotificacionService notificacionService,
                                   UsuarioRepository usuarioRepository) {
        this.productoService = productoService;
        this.ventaService = ventaService;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/registrar")
    public String registrar(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "Vendedor/registrar";
    }

    @PostMapping("/registrar")
    public String registrarPost(
            @RequestParam String clienteNombre,
            @RequestParam String clienteTelefono,
            @RequestParam(required = false) String clienteEmail,
            @RequestParam(required = false) String clienteDireccion,
            @RequestParam String metodoPago,
            @RequestParam String productoIds,
            @RequestParam String cantidades,
            RedirectAttributes redirectAttributes) {

        try {
            List<Long> ids = parseLongList(productoIds);
            List<Integer> cants = parseIntList(cantidades);

            if (ids.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Selecciona al menos un producto.");
                return "redirect:/vendedor/ventas/registrar";
            }
            if (ids.size() != cants.size()) {
                redirectAttributes.addFlashAttribute("error", "Datos de productos/cantidades no coinciden.");
                return "redirect:/vendedor/ventas/registrar";
            }

            Venta venta = new Venta();
            venta.setFecha(LocalDateTime.now());
            venta.setMetodoPago(metodoPago);

            // Cliente (VentaService puede reutilizar existente por nombre/teléfono)
            Cliente cliente = new Cliente();
            cliente.setNombre(clienteNombre);
            cliente.setTelefono(clienteTelefono);
            cliente.setEmail(clienteEmail);
            cliente.setDireccion(clienteDireccion);
            venta.setCliente(cliente);

            // Usuario actual (opcional para tu lógica, pero Venta model lo tiene)
            Usuario usuario = usuarioRepository
                    .findByUsername(getNombreUsuario())
                    .orElse(null);
            venta.setUsuario(usuario);

            // Detalles: SOLO los productos marcados (checkbox A)
            List<DetalleVenta> detalles = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                int cantidad = cants.get(i);
                if (cantidad <= 0) continue;

                Producto producto = productoService.buscarPorId(ids.get(i));
                DetalleVenta detalle = new DetalleVenta();
                detalle.setProducto(producto);
                detalle.setCantidad(cantidad);
                // VentaService recalcula precio unitario si viene null, pero lo seteo por claridad
                BigDecimal precioUnitario = producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO;
                detalle.setPrecioUnitario(precioUnitario);
                detalles.add(detalle);
            }

            venta.setDetalles(detalles);

            Venta ventaGuardada = ventaService.procesarVenta(venta);

            // Comprobante PDF + correo al cliente
            byte[] pdf = ventaService.generarBoletaPDFReal(ventaGuardada.getId());
            notificacionService.enviarVentaConComprobante(ventaGuardada, pdf);

            redirectAttributes.addFlashAttribute("success", "Venta registrada correctamente. Se envió el comprobante al correo del cliente (si existe)." );
            return "redirect:/vendedor/ventas/historial";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Error al registrar la venta");
            return "redirect:/vendedor/ventas/registrar";
        }
    }

    private String getNombreUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "Vendedor";
    }

    private List<Long> parseLongList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private List<Integer> parseIntList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .toList();
    }
}

