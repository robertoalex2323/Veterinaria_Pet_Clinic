package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendedor")
public class VendedorController {

    private static final Logger log = LoggerFactory.getLogger(VendedorController.class);

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile(EMAIL_REGEX);

    private final ProductoService productoService;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;


    public VendedorController(ProductoService productoService,
                               VentaRepository ventaRepository,
                               UsuarioRepository usuarioRepository,
                               BCryptPasswordEncoder passwordEncoder) {
        this.productoService = productoService;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @GetMapping({"", "/"})
    public String dashboard(Authentication authentication, Model model, Principal principal) {
        model.addAttribute("nombreUsuario", principal != null ? principal.getName() : null);
        return "Vendedor/dashboard";
    }


    @GetMapping({"/dashboard"})
    public String dashboard2(Authentication authentication, Model model, Principal principal) {
        model.addAttribute("nombreUsuario", principal != null ? principal.getName() : null);
        return "Vendedor/dashboard";
    }

    // --- Datos para dashboard (opcional si el JS consume) ---
    // Si tu front ya hace fetch, deja estos endpoints para que no falle.
    @GetMapping("/api/dashboard-metrics")
    @ResponseBody
    public Map<String, Object> dashboardMetrics(Principal principal) {
        // Por ahora devolvemos datos derivados del backend existente.
        // Si no existe data en ventas/promos, salen vacíos.
        Map<String, Object> resp = new HashMap<>();

        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = hoy.plusDays(1).atStartOfDay();

        // Ventas hoy (contar ventas en rango usando repository existente)
        List<Venta> ventasHoyLista = ventaRepository.findVentasDesde(inicioHoy);
        long ventasHoy = ventasHoyLista.stream()
                .filter(v -> v.getFecha() != null && !v.getFecha().isBefore(inicioHoy) && v.getFecha().isBefore(finHoy))
                .count();
        resp.put("ventasHoy", ventasHoy);

        // Boletas emitidas: comprobanteEnviado true (si no hay datos aún, devuelve 0)
        long boletasEmitidas = ventaRepository.findByOrderByFechaDesc().stream()
                .filter(v -> Boolean.TRUE.equals(v.getComprobanteEnviado()))
                .count();
        resp.put("boletasEmitidas", boletasEmitidas);

        // Promociones/Recomendaciones: aún no hay modelo/endpoints en el código actual.
        resp.put("promosActivas", 0);
        resp.put("recomendacionesGeneradas", 0);

        // Ventas últimos 7 días por día
        LocalDateTime inicio7 = hoy.minusDays(6).atStartOfDay();
        LocalDateTime fin7 = finHoy;
        List<Venta> ventas7 = ventaRepository.findVentasDesde(inicio7);
        Map<java.time.LocalDate, List<Venta>> grouped = ventas7.stream()
                .filter(v -> v.getFecha() != null && !v.getFecha().isBefore(inicio7) && v.getFecha().isBefore(fin7))
                .collect(Collectors.groupingBy(v -> v.getFecha().toLocalDate()));

        List<Map<String, Object>> ventas7dias = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("label", e.getKey().toString());
                    m.put("value", e.getValue().size());
                    return m;
                })
                .collect(Collectors.toList());

        resp.put("ventas7dias", ventas7dias);


        // Ventas por categoría (no hay información de categoría en el modelo Venta/DetalleVenta actual para este endpoint)
        resp.put("categorias", Collections.emptyList());

        return resp;
    }

    private String getNombreUsuario() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "Vendedor";
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    // ============ PERFIL ============
    @GetMapping("/perfil")
    public String perfil(Model model) {
        String username = getNombreUsuario();
        model.addAttribute("nombreUsuario", username);

        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombreCompleto", usuario.getNombre());
        });

        return "Vendedor/perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@RequestParam String nombre,
            @RequestParam String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {

        String username = getNombreUsuario();

        try {
            // Validar que el email no esté vacío
            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "El correo electrónico no puede estar vacío. Por favor, ingrese un email válido.");
                return "redirect:/vendedor/perfil";
            }

            // Validar formato del email
            if (!isValidEmail(email)) {
                redirectAttributes.addFlashAttribute("error",
                        "Formato de correo electrónico inválido. Por favor, ingrese un email válido (ejemplo: usuario@dominio.com). ");
                return "redirect:/vendedor/perfil";
            }

            // Obtener usuario actual
            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar email no usado por otro usuario
            usuarioRepository.findByEmail(email.trim()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(usuario.getId())) {
                    throw new RuntimeException("El correo electrónico '" + email.trim() + "' ya está registrado por otro usuario.");
                }
            });

            // Actualizar datos
            usuario.setNombre(nombre);
            usuario.setEmail(email.trim());

            // Cambio de contraseña (si se proporciona)
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (newPassword.length() < 6) {
                    throw new RuntimeException("La nueva contraseña debe tener al menos 6 caracteres.");
                }

                if (currentPassword == null || !passwordEncoder.matches(currentPassword, usuario.getPassword())) {
                    throw new RuntimeException("La contraseña actual es incorrecta.");
                }
                usuario.setPassword(passwordEncoder.encode(newPassword));
            }

            usuarioRepository.save(usuario);
            redirectAttributes.addFlashAttribute("success", "Perfil actualizado correctamente");

        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("ya está registrado")) {
                redirectAttributes.addFlashAttribute("error", errorMessage);
            } else if (errorMessage != null && errorMessage.contains("contraseña")) {
                redirectAttributes.addFlashAttribute("error", "Error de seguridad: " + errorMessage);
            } else if (errorMessage != null && errorMessage.contains("formato")) {
                redirectAttributes.addFlashAttribute("error", errorMessage);
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Error al actualizar el perfil. Por favor, verifique los datos ingresados.");
            }
        }

        return "redirect:/vendedor/perfil";
    }

    
    // =========================
    // PRODUCTOS (Vendedor)
    // =========================

    @GetMapping("/productos")
    public String listarProductos(@RequestParam(required = false, defaultValue = "") String buscar, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("buscar", buscar);

        // Búsqueda simple en memoria (ajustar si luego necesitas query en BD)
        List<Producto> productos = productoService.listarTodos();
        if (buscar != null && !buscar.trim().isEmpty()) {
            String q = buscar.trim().toLowerCase();
            productos = productos.stream()
                    .filter(p -> (p.getNombre() != null && p.getNombre().toLowerCase().contains(q))
                            || (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("productos", productos);
        return "Vendedor/productos";
    }

    @GetMapping("/productos/nuevo")
    public String nuevoProducto(Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        model.addAttribute("producto", new Producto());
        return "Vendedor/producto-form";
    }

    @GetMapping("/productos/editar/{id}")
    public String editarProducto(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        Producto producto = productoService.buscarPorId(id);
        model.addAttribute("producto", producto);
        return "Vendedor/producto-form";
    }


    @GetMapping("/productos/ver/{id}")
    public String verProducto(@org.springframework.web.bind.annotation.PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        Producto producto;
        try {
            producto = productoService.buscarPorId(id);
        } catch (Exception e) {
            producto = null;
        }
        model.addAttribute("producto", producto);
        return "Vendedor/producto-ver";
    }

    @GetMapping("/productos/eliminar/{id}")
    public String eliminarProducto(@org.springframework.web.bind.annotation.PathVariable Long id, Model model,
                                     RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "Eliminar no implementado aún.");
        return "redirect:/vendedor/productos";
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(
                                   @RequestParam(required = false) Long id,
                                   @RequestParam String nombre,
                                   @RequestParam String categoria,
                                   @RequestParam(required = false) Double precio,
                                   @RequestParam(required = false, defaultValue = "0") Integer stock,
                                   @RequestParam(required = false) String descripcion,
                                   @org.springframework.web.bind.annotation.RequestParam(required = false) org.springframework.web.multipart.MultipartFile foto,
                                   RedirectAttributes redirectAttributes) {

        try {

            // Validaciones mínimas
            if (nombre == null || nombre.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "El nombre del producto es obligatorio.");
                return "redirect:/vendedor/productos/nuevo";
            }
            if (categoria == null || categoria.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "La categoría es obligatoria.");
                return "redirect:/vendedor/productos/nuevo";
            }

            Producto producto;
            if (id != null) {
                // Editar: cargamos el producto existente para que no quede “vacío”
                producto = productoService.buscarPorId(id);
            } else {
                // Crear: producto nuevo
                producto = new Producto();
            }

            producto.setNombre(nombre.trim());
            producto.setCategoria(categoria.trim());
            producto.setPrecio(precio != null ? java.math.BigDecimal.valueOf(precio) : java.math.BigDecimal.ZERO);
            producto.setStock(stock != null ? stock : 0);
            producto.setDescripcion(descripcion != null ? descripcion : "");

            // Foto: por ahora no se actualiza en editar/guardar, para no romper el flujo.
            // Si luego quieres guardar la imagen, lo añadimos en ProductoService/Controller.

            productoService.guardar(producto);

            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente.");
            return "redirect:/vendedor/productos";
        } catch (Exception e) {
            log.error("Error al guardar producto: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el producto: " + (e.getMessage() != null ? e.getMessage() : ""));
            return "redirect:/vendedor/productos/nuevo";
        }
    }


}



