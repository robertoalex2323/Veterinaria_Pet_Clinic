package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PromocionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.VentaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendedor/ventas")
public class VendedorVentaController {

    private final ProductoService productoService;
    private final VentaService ventaService;
    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PromocionService promocionService;

    public VendedorVentaController(ProductoService productoService,
                                   VentaService ventaService,
                                   NotificacionService notificacionService,
                                   UsuarioRepository usuarioRepository,
                                   ClienteRepository clienteRepository,
                                   PromocionService promocionService) {
        this.productoService = productoService;
        this.ventaService = ventaService;
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.promocionService = promocionService;
    }

    @GetMapping("/registrar")
    public String registrar(Model model) {
        List<Producto> productos = productoService.listarTodos();
        List<Promocion> promocionesActivas = promocionService.getPromocionesActivas();

        // IDs de producto y categorías con promoción vigente, para resaltarlos en la tabla.
        List<Long> productosConOferta = promocionesActivas.stream()
                .map(Promocion::getProductoId)
                .filter(Objects::nonNull)
                .toList();
        List<String> categoriasConOferta = promocionesActivas.stream()
                .map(Promocion::getCategoriaAplicable)
                .filter(c -> c != null && !c.isBlank())
                .map(String::toLowerCase)
                .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("promocionesActivas", promocionesActivas);
        model.addAttribute("productosConOferta", productosConOferta);
        model.addAttribute("categoriasConOferta", categoriasConOferta);
        model.addAttribute("igvTasa", Venta.IGV_TASA);
        return "Vendedor/registrar";
    }

    @PostMapping("/registrar")
    public String registrarPost(
            @RequestParam String clienteNombre,
            @RequestParam String clienteTelefono,
            @RequestParam(required = false) String clienteEmail,
            @RequestParam(required = false) String clienteDireccion,
            @RequestParam String metodoPago,
            @RequestParam(required = false) String codigoOperacion,
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

            boolean requiereCodigoOperacion = "Yape".equalsIgnoreCase(metodoPago) || "Plin".equalsIgnoreCase(metodoPago);
            if (requiereCodigoOperacion && (codigoOperacion == null || codigoOperacion.isBlank())) {
                redirectAttributes.addFlashAttribute("error",
                        "Ingresa el código de operación de " + metodoPago + " para continuar.");
                return "redirect:/vendedor/ventas/registrar";
            }

            Venta venta = new Venta();
            venta.setFecha(LocalDateTime.now());
            venta.setMetodoPago(metodoPago);
            venta.setCodigoOperacion(requiereCodigoOperacion ? codigoOperacion.trim() : null);

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
                BigDecimal precioUnitario = producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO;
                detalle.setPrecioUnitario(precioUnitario);
                detalles.add(detalle);
            }

            venta.setDetalles(detalles);

            Venta ventaGuardada = ventaService.procesarVenta(venta);

            byte[] pdf = ventaService.generarBoletaPDFReal(ventaGuardada.getId());
            notificacionService.enviarVentaConComprobante(ventaGuardada, pdf);

            redirectAttributes.addFlashAttribute("success", "Venta registrada correctamente. Se envió el comprobante al correo del cliente (si existe)." );
            return "redirect:/vendedor/ventas/historial";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Error al registrar la venta");
            return "redirect:/vendedor/ventas/registrar";
        }
    }

    @GetMapping("/clientes/buscar")
    @ResponseBody
    public List<ClienteSugerencia> buscarClientes(@RequestParam(required = false) String query) {
        String q = query == null ? "" : query.trim();
        if (q.length() < 2) {
            return List.of();
        }

        List<Cliente> encontrados = clienteRepository.findByNombreContainingIgnoreCase(q);

        if (q.matches("[0-9+\\-\\s]+")) {
            clienteRepository.findByTelefono(q).ifPresent(c -> {
                if (encontrados.stream().noneMatch(existing -> existing.getId().equals(c.getId()))) {
                    encontrados.add(0, c);
                }
            });
        }

        return encontrados.stream()
                .limit(8)
                .map(c -> new ClienteSugerencia(
                        c.getId(),
                        c.getNombre(),
                        c.getTelefono(),
                        c.getEmail(),
                        c.getDireccion()))
                .collect(Collectors.toList());
    }

    public record ClienteSugerencia(Long id, String nombre, String telefono, String email, String direccion) {}

    @GetMapping("/emitir")
    public String mostrarEmitirBoleta() {
        return "Vendedor/emitir-boletas";
    }

    @PostMapping("/emitir")
    public String emitirBoleta(
            @RequestParam Long ventaId,
            RedirectAttributes redirectAttributes) {

        try {
            if (ventaId == null || ventaId <= 0) {
                redirectAttributes.addFlashAttribute("error", "Venta inválida. Selecciona un ID válido.");
                return "redirect:/vendedor/ventas/emitir";
            }

            Venta venta = ventaService.buscarPorId(ventaId);
            if (venta == null) {
                redirectAttributes.addFlashAttribute("error", "No se encontró la venta con ID: " + ventaId);
                return "redirect:/vendedor/ventas/emitir";
            }

            byte[] pdf = ventaService.generarBoletaPDFReal(ventaId);

            notificacionService.enviarVentaConComprobante(venta, pdf);

            redirectAttributes.addFlashAttribute("success",
                    "Comprobante generado y enviado al correo del cliente (si existe).");
            return "redirect:/vendedor/ventas/emitir";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    Objects.requireNonNullElse(e.getMessage(), "Error al emitir la boleta."));
            return "redirect:/vendedor/ventas/emitir";
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

    @GetMapping("/historial")
    public String historialVentas(
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            Model model) {

        List<Venta> ventas;

        if (fechaInicio != null && !fechaInicio.isBlank()
                && fechaFin != null && !fechaFin.isBlank()) {

            LocalDateTime inicio = LocalDate.parse(fechaInicio).atStartOfDay();
            LocalDateTime fin = LocalDate.parse(fechaFin).atTime(23, 59, 59);

            ventas = ventaService.buscarVentasPorFecha(inicio, fin);

        } else {
            ventas = ventaService.listarTodasLasVentas();
        }

        model.addAttribute("ventas", ventas);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);

        return "Vendedor/historial";
    }
}