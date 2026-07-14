package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;

import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecomendacionRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PromocionService;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final PromocionService promocionService;
    private final RecomendacionRepository recomendacionRepository;
    private final com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository clienteRepository;

    public VendedorController(ProductoService productoService,
                               VentaRepository ventaRepository,
                               UsuarioRepository usuarioRepository,
                               BCryptPasswordEncoder passwordEncoder,
                               PromocionService promocionService,
                               RecomendacionRepository recomendacionRepository,
                               com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository clienteRepository) {
        this.productoService = productoService;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.promocionService = promocionService;
        this.recomendacionRepository = recomendacionRepository;
        this.clienteRepository = clienteRepository;
    }   


    @GetMapping({"", "/"})
    public String dashboard(Authentication authentication, Model model, Principal principal) {
        model.addAttribute("nombreUsuario", principal != null ? principal.getName() : null);
        return "Vendedor/dashboard";
    }

    @GetMapping("/historial")
    public String historialRedirect() {
        return "redirect:/vendedor/ventas/historial";
    }


    @GetMapping({"/dashboard"})

    public String dashboard2(Authentication authentication, Model model, Principal principal) {
        model.addAttribute("nombreUsuario", principal != null ? principal.getName() : null);
        return "Vendedor/dashboard";
    }

    @GetMapping("/api/dashboard-metrics")
    @ResponseBody
    public Map<String, Object> dashboardMetrics(Principal principal) {
        Map<String, Object> resp = new HashMap<>();

        try {
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

            // Promociones/Recomendaciones
            long promosActivas = promocionService.listarActivas().size();
            resp.put("promosActivas", promosActivas);

            long recomendacionesHoy = recomendacionRepository.countByFechaBetween(inicioHoy, finHoy);
            resp.put("recomendacionesGeneradas", recomendacionesHoy);

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

            // Distribución por categorías segun los productos registrados
            List<Producto> productosActivos = productoService.listarTodos();
            Map<String, Long> categoriasCount_productos = new HashMap<>();

            for (Producto p : productosActivos) {
                if (p == null) continue;
                String categoria = p.getCategoria();
                if (categoria == null || categoria.isBlank()) continue;

                String cat = categoria.trim();
                categoriasCount_productos.put(cat, categoriasCount_productos.getOrDefault(cat, 0L) + 1L);
            }

            List<Map<String, Object>> categoriasList_productos = categoriasCount_productos.entrySet().stream()
                    .map(e -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("label", e.getKey());
                        m.put("value", e.getValue());
                        return m;
                    })
                    .collect(Collectors.toList());

            resp.put("categorias", categoriasList_productos);

            // Ingresos del día (total recaudado)
            BigDecimal ingresosHoy = ventaRepository.sumVentasEntreFechas(inicioHoy, finHoy);
            resp.put("ingresosHoy", ingresosHoy != null ? ingresosHoy : BigDecimal.ZERO);

            return resp;
        } catch (Exception e) {
            // IMPORTANTE: evitar 500/Whitelabel en UI; registrar error para corregir la consulta.
            log.error("Error al calcular dashboard-metrics para vendedor: {}", e.getMessage(), e);

            resp.put("ventasHoy", 0L);
            resp.put("promosActivas", 0L);
            resp.put("boletasEmitidas", 0L);
            resp.put("recomendacionesGeneradas", 0L);
            resp.put("ventas7dias", Collections.emptyList());
            resp.put("categorias", Collections.emptyList());
            resp.put("ingresosHoy", BigDecimal.ZERO);

            return resp;
        }
    }
    // ============ SUGERENCIA ============

    @GetMapping("/api/clientes/sugerencias")
        @ResponseBody
        public List<Map<String, String>> sugerenciasClientes(@RequestParam(required = false, defaultValue = "") String q) {
            String query = q.trim();
            if (query.isEmpty()) {
                return Collections.emptyList();
            }
            return clienteRepository.findByNombreContainingIgnoreCase(query).stream()
                    .limit(8)
                    .map(c -> {
                        Map<String, String> item = new HashMap<>();
                        item.put("nombre", c.getNombre() != null ? c.getNombre() : "");
                        item.put("telefono", c.getTelefono() != null ? c.getTelefono() : "");
                        return item;
                    })
                    .collect(Collectors.toList());
        }

    // ============ ULTIMAS VENTAS (para Caja POS) ============
    @GetMapping("/api/ventas/ultimas")
    @ResponseBody
    public List<Map<String, Object>> ultimasVentas() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        List<Venta> ventasHoy = ventaRepository.findVentasDesde(inicioHoy);

        return ventasHoy.stream()
                .sorted((a, b) -> {
                    if (a.getFecha() == null) return 1;
                    if (b.getFecha() == null) return -1;
                    return b.getFecha().compareTo(a.getFecha());
                })
                .limit(5)
                .map(v -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", v.getId());
                    m.put("fecha", v.getFecha() != null ? v.getFecha().toString() : null);
                    m.put("total", v.getTotal());
                    m.put("clienteNombre", v.getCliente() != null ? v.getCliente().getNombre() : "Cliente");
                    m.put("metodoPago", v.getMetodoPago());
                    return m;
                })
                .collect(Collectors.toList());
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

    private List<String> categoriasExistentes() {
        return productoService.listarTodos().stream()
                .map(Producto::getCategoria)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
    }

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
        // Si venimos de un redirect con datos preservados (flash), no lo sobrescribimos.
        if (!model.containsAttribute("producto")) {
            model.addAttribute("producto", new Producto());
        }
        model.addAttribute("categorias", categoriasExistentes());
        return "Vendedor/producto-form";
    }

    @GetMapping("/productos/editar/{id}")
    public String editarProducto(@PathVariable Long id, Model model) {
        model.addAttribute("nombreUsuario", getNombreUsuario());
        if (!model.containsAttribute("producto")) {
            Producto producto = productoService.buscarPorId(id);
            model.addAttribute("producto", producto);
        }
        model.addAttribute("categorias", categoriasExistentes());
        return "Vendedor/producto-form";
    }


    @GetMapping("/productos/ver/{id}")
    public String verProducto(@PathVariable Long id, Model model) {
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
    public String eliminarProducto(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminar(id);
            redirectAttributes.addFlashAttribute("exito", "Producto eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar el producto: " + e.getMessage());
        }
        return "redirect:/vendedor/productos";
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(
                                   @RequestParam(required = false) Long id,
                                   @RequestParam String nombre,
                                   @RequestParam String categoria,
                                   @RequestParam(required = false) String precio,
                                   @RequestParam(required = false) String stock,
                                   @RequestParam(required = false) String descripcion,
                                   @RequestParam(required = false, defaultValue = "false") boolean eliminarFoto,
                                   @RequestParam(required = false) MultipartFile foto,
                                   RedirectAttributes redirectAttributes) {

        String redirectDestino = (id != null) ? "/vendedor/productos/editar/" + id : "/vendedor/productos/nuevo";

        // Producto de trabajo: si es edición, partimos del existente en BD (para no perder
        // campos que el form no maneja, como fechas). Si no existe, avisamos y salimos.
        Producto producto;
        if (id != null) {
            try {
                producto = productoService.buscarPorId(id);
            } catch (RuntimeException ex) {
                redirectAttributes.addFlashAttribute("error", "El producto que intentas editar ya no existe.");
                return "redirect:/vendedor/productos";
            }
        } else {
            producto = new Producto();
        }

        String nombreLimpio = nombre == null ? "" : nombre.trim();
        String categoriaLimpia = categoria == null ? "" : categoria.trim();

        List<String> errores = new ArrayList<>();

        if (nombreLimpio.isEmpty()) {
            errores.add("El nombre del producto es obligatorio.");
        } else if (nombreLimpio.length() > 255) {
            errores.add("El nombre no puede superar los 255 caracteres.");
        }

        if (categoriaLimpia.isEmpty()) {
            errores.add("La categoría es obligatoria.");
        } else if (categoriaLimpia.length() > 50) {
            errores.add("La categoría no puede superar los 50 caracteres.");
        }

        BigDecimal precioVal = null;
        if (precio == null || precio.trim().isEmpty()) {
            errores.add("El precio es obligatorio.");
        } else {
            try {
                precioVal = new BigDecimal(precio.trim().replace(",", "."));
                if (precioVal.compareTo(BigDecimal.ZERO) <= 0) {
                    errores.add("El precio debe ser mayor a 0.");
                } else if (precioVal.compareTo(new BigDecimal("999999.99")) > 0) {
                    errores.add("El precio ingresado es demasiado alto.");
                }
            } catch (NumberFormatException ex) {
                errores.add("El precio ingresado no es válido.");
            }
        }

        Integer stockVal;
        if (stock == null || stock.trim().isEmpty()) {
            stockVal = 0;
        } else {
            try {
                stockVal = Integer.parseInt(stock.trim());
                if (stockVal < 0) {
                    errores.add("El stock no puede ser negativo.");
                }
            } catch (NumberFormatException ex) {
                stockVal = null;
                errores.add("El stock ingresado no es válido.");
            }
        }

        boolean hayFotoNueva = foto != null && !foto.isEmpty();
        if (hayFotoNueva) {
            String contentType = foto.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                errores.add("El archivo de foto debe ser una imagen (jpg, png, webp o gif).");
            }
            if (foto.getSize() > 2 * 1024 * 1024) {
                errores.add("La imagen es demasiado grande (máx. 2MB).");
            }
        }

        if (!errores.isEmpty()) {
            // Reconstruimos el producto con lo que el vendedor tecleó para no perder su trabajo.
            producto.setId(id);
            producto.setNombre(nombreLimpio);
            producto.setCategoria(categoriaLimpia);
            producto.setDescripcion(descripcion);
            if (precioVal != null) producto.setPrecio(precioVal);
            if (stockVal != null) producto.setStock(stockVal);

            redirectAttributes.addFlashAttribute("producto", producto);
            redirectAttributes.addFlashAttribute("error", String.join(" ", errores));
            return "redirect:" + redirectDestino;
        }

        try {
            producto.setNombre(nombreLimpio);
            producto.setCategoria(categoriaLimpia);
            producto.setPrecio(precioVal);
            producto.setStock(stockVal);
            producto.setDescripcion(descripcion != null ? descripcion.trim() : "");

            if (eliminarFoto && !hayFotoNueva) {
                producto.setFoto(null);
            }

            // Guardar foto si viene una nueva (tiene prioridad sobre "eliminar foto")
            if (hayFotoNueva) {
                String uploadDir = "src/main/resources/static/Imagen/Productos/";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new RuntimeException("No se pudo crear la carpeta de subida de imágenes.");
                }

                String original = foto.getOriginalFilename() != null ? foto.getOriginalFilename() : "foto";
                String ext = original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";
                String safeExt = ext.toLowerCase();
                if (!(safeExt.equals(".png") || safeExt.equals(".jpg") || safeExt.equals(".jpeg") || safeExt.equals(".webp") || safeExt.equals(".gif"))) {
                    safeExt = ".png";
                }

                String fileName = "producto_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID() + safeExt;
                java.nio.file.Path target = java.nio.file.Paths.get(uploadDir).resolve(fileName);
                java.nio.file.Files.copy(foto.getInputStream(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                producto.setFoto("/Imagen/Productos/" + fileName);
            }

            productoService.guardar(producto);

            redirectAttributes.addFlashAttribute("success",
                    id != null ? "Producto actualizado correctamente." : "Producto creado correctamente.");
            return "redirect:/vendedor/productos";
        } catch (Exception e) {
            log.error("Error al guardar producto: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("producto", producto);
            redirectAttributes.addFlashAttribute("error", "Error al guardar el producto: " + (e.getMessage() != null ? e.getMessage() : ""));
            return "redirect:" + redirectDestino;
        }
    }
}



