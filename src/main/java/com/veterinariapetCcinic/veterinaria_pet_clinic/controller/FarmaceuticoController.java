package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Proveedor;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.FarmaceuticoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MedicamentoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PdfReportService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProveedorService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.RecetaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.VentaService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/farmaceutico")
public class FarmaceuticoController {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final java.util.regex.Pattern EMAIL_PATTERN = java.util.regex.Pattern.compile(EMAIL_REGEX);

    private final ProveedorService proveedorService;
    private final MedicamentoService medicamentoService;
    private final VentaService ventaService;
    private final RecetaService recetaService;
    private final FarmaceuticoService farmaceuticoService;
    private final PdfReportService pdfService;
    private final NotificacionService notificacionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public FarmaceuticoController(ProveedorService proveedorService,
                                 MedicamentoService medicamentoService,
                                 VentaService ventaService,
                                 RecetaService recetaService,
                                 FarmaceuticoService farmaceuticoService,
                                 PdfReportService pdfService,
                                 NotificacionService notificacionService,
                                 SimpMessagingTemplate messagingTemplate,
                                 UsuarioRepository usuarioRepository,
                                 BCryptPasswordEncoder passwordEncoder) {
        this.proveedorService = proveedorService;
        this.medicamentoService = medicamentoService;
        this.ventaService = ventaService;
        this.recetaService = recetaService;
        this.farmaceuticoService = farmaceuticoService;
        this.pdfService = pdfService;
        this.notificacionService = notificacionService;
        this.messagingTemplate = messagingTemplate;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String getNombreUsuario() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "Farmaceutico";
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private void sendNotification(String type, String message) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            Map<String, String> notif = new HashMap<>();
            notif.put("type", type);
            notif.put("message", message);
            notif.put("timestamp", timestamp);
            messagingTemplate.convertAndSend("/topic/notifications", notif);
            notificacionService.enviarNotificacionUI(type, message);
        } catch (Exception e) {
            // WebSocket may not be connected
        }
    }

    // ===========================================================
    // Subida segura de imágenes de medicamento
    // ===========================================================
    private static final java.util.Set<String> EXTENSIONES_IMAGEN_PERMITIDAS =
            java.util.Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long TAMANO_MAXIMO_IMAGEN_BYTES = 5L * 1024 * 1024; // 5 MB


    private String guardarImagenMedicamento(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            return null;
        }

        if (archivo.getSize() > TAMANO_MAXIMO_IMAGEN_BYTES) {
            throw new IllegalArgumentException("La imagen supera el tamaño máximo permitido (5 MB).");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen válida (JPG, PNG, GIF o WEBP).");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase();
        }

        if (!EXTENSIONES_IMAGEN_PERMITIDAS.contains(extension)) {
            throw new IllegalArgumentException("Formato de imagen no permitido. Usa JPG, PNG, GIF o WEBP.");
        }

        String nombreArchivo = UUID.randomUUID().toString() + "." + extension;

        Path carpeta = Paths.get("src/main/resources/static/Imagen/Medicamento/").toAbsolutePath().normalize();
        Files.createDirectories(carpeta);

        Path ruta = carpeta.resolve(nombreArchivo).normalize();
        if (!ruta.startsWith(carpeta)) {
            // Defensa extra: si por algún motivo la ruta resultante se sale de la carpeta esperada.
            throw new IllegalArgumentException("Nombre de archivo inválido.");
        }

        Files.write(ruta, archivo.getBytes());
        return "/Imagen/Medicamento/" + nombreArchivo;
    }

    // ===========================================================
    // DASHBOARD (con estadísticas reales desde la base de datos)
    // ===========================================================
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");

        // ----- Datos base (tarjetas principales) -----
        List<Medicamento> todosMedicamentos = medicamentoService.listarTodos();
        int totalMedicamentos = todosMedicamentos.size();
        int bajoStockCount = medicamentoService.listarBajoStock().size();
        int recetasPendientes = recetaService.listarPendientes().size();

        model.addAttribute("ventasHoy", ventaService.calcularVentasHoy());
        model.addAttribute("bajoStockCount", bajoStockCount);
        model.addAttribute("recetasPendientes", recetasPendientes);
        model.addAttribute("medicamentosActivos", totalMedicamentos);

        int totalProveedores = proveedorService.listarTodos().size();

        var todasLasRecetas = farmaceuticoService.obtenerTodasLasRecetas();
        long recetasDispensadas = todasLasRecetas.stream()
                .filter(r -> r.getEstado() == RecetaEstado.DISPENSADA)
                .count();
        int totalRecetas = todasLasRecetas.size();
        int eficienciaPorc = (totalRecetas > 0)
                ? Math.round(recetasDispensadas * 100f / totalRecetas)
                : 0;

        model.addAttribute("totalMedicamentos", totalMedicamentos);
        model.addAttribute("recetasCompletadas", (int) recetasDispensadas);
        model.addAttribute("proveedoresActivos", totalProveedores);
        model.addAttribute("eficienciaPorc", eficienciaPorc);

        // Distribución de stock para el gráfico doughnut (normal vs bajo)
        int stockNormal = Math.max(totalMedicamentos - bajoStockCount, 0);
        model.addAttribute("stockNormalCount", stockNormal);

        return "farmaceutico/dashboard";
    }

    // ===========================================================
    // API: Estadísticas del dashboard (para refresco automático)
    // ===========================================================
    @GetMapping("/api/dashboard/estadisticas")
    @ResponseBody
    public Map<String, Object> estadisticasDashboard() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<Medicamento> todos = medicamentoService.listarTodos();
            int totalMedicamentos = todos.size();
            int bajoStock = medicamentoService.listarBajoStock().size();

            var todasLasRecetas = farmaceuticoService.obtenerTodasLasRecetas();
            long dispensadas = todasLasRecetas.stream()
                    .filter(r -> r.getEstado() == RecetaEstado.DISPENSADA)
                    .count();
            int totalRecetas = todasLasRecetas.size();
            int eficiencia = (totalRecetas > 0) ? Math.round(dispensadas * 100f / totalRecetas) : 0;

            stats.put("success", true);
            stats.put("ventasHoy", ventaService.calcularVentasHoy());
            stats.put("totalMedicamentos", totalMedicamentos);
            stats.put("bajoStockCount", bajoStock);
            stats.put("stockNormalCount", Math.max(totalMedicamentos - bajoStock, 0));
            stats.put("recetasPendientes", recetaService.listarPendientes().size());
            stats.put("recetasCompletadas", (int) dispensadas);
            stats.put("proveedoresActivos", proveedorService.listarTodos().size());
            stats.put("eficienciaPorc", eficiencia);
            stats.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        } catch (Exception e) {
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }
        return stats;
    }

    @GetMapping("/inventario")
    public String gestionarInventario(Model model) {
        model.addAttribute("medicamentos", medicamentoService.listarTodos());
        model.addAttribute("proveedores", proveedorService.listarTodos());
        model.addAttribute("medicamento", new Medicamento());
        model.addAttribute("currentPage", "inventario");
        return "farmaceutico/inventario";
    }

    @GetMapping("/medicamentos")
    public String informacionMedicamentos(Model model) {
        model.addAttribute("medicamentos", medicamentoService.listarTodos());
        model.addAttribute("currentPage", "medicamentos");
        return "farmaceutico/medicamentos";
    }

    @PostMapping("/inventario/guardar")
    public String guardarMedicamento(@ModelAttribute Medicamento medicamento,
                                     @RequestParam(required = false) MultipartFile imagenArchivo,
                                     RedirectAttributes ra) {
        try {
            if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
                medicamento.setImagenUrl(guardarImagenMedicamento(imagenArchivo));
            }
            boolean isNew = (medicamento.getId() == null);
            medicamentoService.guardar(medicamento);
            if (isNew) {
                sendNotification("success", "Nuevo medicamento agregado: " + medicamento.getNombre());
            } else {
                sendNotification("info", "Medicamento actualizado: " + medicamento.getNombre());
            }
            ra.addFlashAttribute("success", "Medicamento registrado en inventario.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/farmaceutico/inventario";
    }

    @GetMapping("/inventario/editar/{id}")
    public String editarMedicamento(@PathVariable Long id, Model model) {
        model.addAttribute("medicamento", medicamentoService.buscarPorId(id));
        model.addAttribute("currentPage", "inventario");
        return "farmaceutico/form-medicamento";
    }

    @PostMapping("/inventario/eliminar/{id}")
    public String eliminarMedicamento(@PathVariable Long id, RedirectAttributes ra) {
        try {
            String nombre = medicamentoService.buscarPorId(id).getNombre();
            medicamentoService.eliminar(id);
            sendNotification("warning", "Medicamento eliminado: " + nombre);
            ra.addFlashAttribute("success", "Medicamento eliminado correctamente del inventario.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar el medicamento. Puede que esté asociado a una venta.");
        }
        return "redirect:/farmaceutico/inventario";
    }

    @GetMapping("/proveedores")
    public String listarProveedores(Model model) {
        model.addAttribute("proveedores", proveedorService.listarTodos());
        model.addAttribute("currentPage", "proveedores");
        return "farmaceutico/proveedores";
    }

    @GetMapping("/proveedores/nuevo")
    public String formularioProveedor(@RequestParam(required = false) Long id, Model model) {
        if (id != null) {
            model.addAttribute("proveedor", proveedorService.buscarPorId(id));
        } else {
            model.addAttribute("proveedor", new Proveedor());
        }
        model.addAttribute("currentPage", "proveedores");
        return "farmaceutico/form-proveedor";
    }

    @PostMapping("/proveedores/guardar")
    public String guardarProveedor(@ModelAttribute Proveedor proveedor, RedirectAttributes ra) {
        if (proveedor.getNombre() != null) proveedor.setNombre(proveedor.getNombre().trim());
        if (proveedor.getRuc() != null) proveedor.setRuc(proveedor.getRuc().trim());
        if (proveedor.getContacto() != null) proveedor.setContacto(proveedor.getContacto().trim());
        if (proveedor.getTelefono() != null) proveedor.setTelefono(proveedor.getTelefono().trim());
        if (proveedor.getEmail() != null) proveedor.setEmail(proveedor.getEmail().trim());
        if (proveedor.getDireccion() != null) proveedor.setDireccion(proveedor.getDireccion().trim());

        if (proveedor.getNombre() == null || proveedor.getNombre().isEmpty()) {
            ra.addFlashAttribute("error", "El nombre del proveedor es obligatorio.");
            return "redirect:/farmaceutico/proveedores" + (proveedor.getId() != null ? "/nuevo?id=" + proveedor.getId() : "/nuevo");
        }
        if (proveedor.getRuc() == null || !proveedor.getRuc().matches("\\d{11}")) {
            ra.addFlashAttribute("error", "El RUC debe tener exactamente 11 dígitos.");
            return "redirect:/farmaceutico/proveedores" + (proveedor.getId() != null ? "/nuevo?id=" + proveedor.getId() : "/nuevo");
        }

        Proveedor existente = proveedorService.buscarPorRuc(proveedor.getRuc());
        boolean rucEnUso = existente != null && !existente.getId().equals(proveedor.getId());
        if (rucEnUso) {
            ra.addFlashAttribute("error", "Ya existe un proveedor registrado con el RUC " + proveedor.getRuc() + ".");
            return "redirect:/farmaceutico/proveedores" + (proveedor.getId() != null ? "/nuevo?id=" + proveedor.getId() : "/nuevo");
        }

        try {
            boolean isNew = (proveedor.getId() == null);
            proveedorService.guardar(proveedor);
            if (isNew) {
                sendNotification("success", "Nuevo proveedor registrado: " + proveedor.getNombre());
                ra.addFlashAttribute("success", "Proveedor registrado exitosamente.");
            } else {
                sendNotification("info", "Proveedor actualizado: " + proveedor.getNombre());
                ra.addFlashAttribute("success", "Proveedor actualizado exitosamente.");
            }
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar el proveedor: " + e.getMessage());
        }
        return "redirect:/farmaceutico/proveedores";
    }

    @PostMapping("/proveedores/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Long id, RedirectAttributes ra) {
        try {
            String nombre = proveedorService.buscarPorId(id).getNombre();
            proveedorService.eliminar(id);
            sendNotification("warning", "Proveedor eliminado: " + nombre);
            ra.addFlashAttribute("success", "Proveedor eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar el proveedor. Puede que esté asociado a medicamentos registrados.");
        }
        return "redirect:/farmaceutico/proveedores";
    }

    @GetMapping("/recetas")
    public String listarRecetas(Model model) {
        model.addAttribute("recetas", recetaService.listarTodas());
        model.addAttribute("recetasMedicas", farmaceuticoService.obtenerTodasLasRecetas());
        model.addAttribute("currentPage", "recetas");
        return "farmaceutico/recetas";
    }

    @PostMapping("/recetas/entregar/{id}")
    public String entregarReceta(@PathVariable Long id, RedirectAttributes ra) {
        recetaService.marcarComoEntregada(id);
        ra.addFlashAttribute("success", "Receta entregada y marcada como completada.");
        return "redirect:/farmaceutico/recetas";
    }

    @PostMapping("/recetas/validar/{id}")
    @ResponseBody
    public FarmaceuticoService.ValidacionReceta validarReceta(@PathVariable Long id) {
        return farmaceuticoService.validarReceta(id);
    }

    @PostMapping("/recetas/dispensar/{id}")
    @ResponseBody
    public FarmaceuticoService.DispensaResult dispensarReceta(@PathVariable Long id) {
        return farmaceuticoService.dispensarReceta(id);
    }

    // ===========================
    // VENTAS - Página principal
    // ===========================
    @GetMapping("/ventas")
    public String registrarVentas(Model model) {
        model.addAttribute("venta", new Venta());
        model.addAttribute("medicamentos", medicamentoService.listarTodos());
        model.addAttribute("ventas", ventaService.listarVentas());
        model.addAttribute("recetasDispensadas", farmaceuticoService.obtenerTodasLasRecetas().stream()
                .filter(r -> r.getEstado() == RecetaEstado.DISPENSADA)
                .toList());
        model.addAttribute("currentPage", "ventas");
        return "farmaceutico/ventas";
    }

    // ===========================
    // API: Listar ventas (JSON)
    // ===========================
    @GetMapping("/api/ventas")
    @ResponseBody
    public List<Venta> listarVentasApi() {
        return ventaService.listarVentas();
    }

    // ===========================
    // API: Crear venta desde receta
    // ===========================
    @PostMapping("/api/ventas/crear-desde-receta")
    @ResponseBody
    public Map<String, Object> crearVentaDesdeReceta(@RequestParam Long recetaId,
                                                      @RequestParam String metodoPago,
                                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                result.put("success", false);
                result.put("message", "Usuario no autenticado");
                return result;
            }
            Venta venta = ventaService.crearVentaDesdeReceta(recetaId, metodoPago, usuario);
            // Generar PDF y enviar comprobante
            byte[] pdf = pdfService.generarComprobanteVenta(venta);
            notificacionService.enviarVentaConComprobante(venta, pdf);

            sendNotification("success", "Venta #VTA-" + String.format("%05d", venta.getId()) + " generada desde receta");
            result.put("success", true);
            result.put("message", "Venta generada con éxito. El comprobante ha sido enviado al correo del cliente.");
            result.put("ventaId", venta.getId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ===========================
    // API: Procesar venta manual
    // ===========================
    @PostMapping("/api/ventas/procesar")
    @ResponseBody
    public Map<String, Object> procesarVentaApi(@RequestParam(required = false) String metodoPago,
                                                 @RequestParam(required = false) String clienteNombre,
                                                 @RequestParam(required = false) String itemsJson,
                                                 HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            Usuario usuario = (Usuario) session.getAttribute("usuario");
            if (usuario == null) {
                result.put("success", false);
                result.put("message", "Usuario no autenticado");
                return result;
            }
            result.put("success", true);
            result.put("message", "Venta procesada correctamente");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ===========================
    // VISTA: procesar venta 
    // ===========================
    @PostMapping("/ventas/procesar")
    public String procesarVenta(@ModelAttribute Venta venta, RedirectAttributes ra) {
        try {
            Venta ventaGuardada = ventaService.procesarVenta(venta);
            byte[] pdf = pdfService.generarComprobanteVenta(ventaGuardada);
            notificacionService.enviarVentaConComprobante(ventaGuardada, pdf);
            ra.addFlashAttribute("success", "Venta procesada con éxito. El comprobante ha sido enviado al correo del cliente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error en la venta: " + e.getMessage());
        }
        return "redirect:/farmaceutico/ventas";
    }

    @GetMapping("/perfil")
    public String verPerfil(Model model) {
        model.addAttribute("currentPage", "perfil");

        String username = getNombreUsuario();
        model.addAttribute("nombreUsuario", username);

        usuarioRepository.findByUsername(username).ifPresent(usuario -> {
            model.addAttribute("usuario", usuario);
            model.addAttribute("nombreCompleto", usuario.getNombre());
        });

        return "farmaceutico/perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@RequestParam String nombre,
            @RequestParam String email,
            @RequestParam(required = false) String currentPassword,
            @RequestParam(required = false) String newPassword,
            RedirectAttributes redirectAttributes) {

        String username = getNombreUsuario();

        try {
            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "El correo electrónico no puede estar vacío. Por favor, ingrese un email válido.");
                return "redirect:/farmaceutico/perfil";
            }

            if (!isValidEmail(email)) {
                redirectAttributes.addFlashAttribute("error",
                        "Formato de correo electrónico inválido. Por favor, ingrese un email válido (ejemplo: usuario@dominio.com).");
                return "redirect:/farmaceutico/perfil";
            }

            Usuario usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            usuarioRepository.findByEmail(email.trim()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(usuario.getId())) {
                    throw new RuntimeException("El correo electrónico '" + email.trim() + "' ya está registrado por otro usuario.");
                }
            });

            usuario.setNombre(nombre);
            usuario.setEmail(email.trim());

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

        return "redirect:/farmaceutico/perfil";
    }

    // ===========================
    // REPORTES - Página principal
    // ===========================
    @GetMapping("/reportes")
    public String reportes(Model model) {
        model.addAttribute("currentPage", "reportes");
        model.addAttribute("bajoStockCount", medicamentoService.listarBajoStock().size());
        model.addAttribute("ventasHoy", ventaService.calcularVentasHoy());
        return "farmaceutico/reportes";
    }

    // ===========================
    // API: Stock bajo (JSON)
    // ===========================
    @GetMapping("/api/reportes/stock-bajo")
    @ResponseBody
    public List<Medicamento> reporteStockBajoApi() {
        return medicamentoService.listarBajoStock();
    }

    // ===========================
    // API: Ventas para reportes (JSON)
    // ===========================
    @GetMapping("/api/reportes/ventas")
    @ResponseBody
    public List<Venta> reporteVentasApi(@RequestParam(required = false) String desde,
                                        @RequestParam(required = false) String hasta) {
        List<Venta> ventas = ventaService.listarVentas();
        return filtrarVentasPorRango(ventas, desde, hasta);
    }

    // ===========================
    // PDF: Reporte de stock bajo (con logo)
    // ===========================
    @GetMapping("/reportes/stock-bajo")
    public ResponseEntity<byte[]> descargarReporteStock() {
        byte[] pdfData = pdfService.generarReporteStockBajo(medicamentoService.listarBajoStock());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_stock_bajo.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    // ===========================
    // PDF: Reporte de ventas por rango (con logo)
    // ===========================
    @GetMapping("/reportes/ventas/pdf")
    public ResponseEntity<byte[]> descargarReporteVentas(@RequestParam(required = false) String desde,
                                                         @RequestParam(required = false) String hasta) {
        List<Venta> ventas = filtrarVentasPorRango(ventaService.listarVentas(), desde, hasta);

        LocalDateTime fechaDesde = parseFecha(desde);
        LocalDateTime fechaHasta = parseFecha(hasta);

        byte[] pdfData = pdfService.generarReporteVentas(ventas, fechaDesde, fechaHasta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_ventas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    // ===========================
    // HELPERS: Filtrado y parseo de fechas
    // ===========================
    private List<Venta> filtrarVentasPorRango(List<Venta> ventas, String desde, String hasta) {
        LocalDateTime fechaDesde = parseFecha(desde);
        LocalDateTime fechaHasta = parseFecha(hasta);

        if (fechaDesde == null && fechaHasta == null) {
            return ventas;
        }

        return ventas.stream()
                .filter(v -> {
                    if (v.getFecha() == null) return false;
                    if (fechaDesde != null && v.getFecha().isBefore(fechaDesde)) return false;
                    if (fechaHasta != null && v.getFecha().isAfter(fechaHasta)) return false;
                    return true;
                })
                .toList();
    }

    private LocalDateTime parseFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        try {
            // Formato esperado: yyyy-MM-dd (input type=date)
            return java.time.LocalDate.parse(fecha).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/ventas/comprobante/{id}")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Long id) {
        try {
            Venta venta = ventaService.buscarPorId(id);
            byte[] pdfData = pdfService.generarComprobanteVenta(venta);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=comprobante_VTA" + String.format("%05d", id) + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfData);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/medicamentos/subir-imagen/{id}")
    public String subirImagen(@PathVariable Long id, @RequestParam("archivo") MultipartFile archivo, RedirectAttributes ra) {
        try {
            Medicamento med = medicamentoService.buscarPorId(id);
            med.setImagenUrl(guardarImagenMedicamento(archivo));
            medicamentoService.guardar(med);
            ra.addFlashAttribute("success", "Imagen subida correctamente.");
            sendNotification("success", "Imagen actualizada para: " + med.getNombre());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al subir la imagen: " + e.getMessage());
        }
        return "redirect:/farmaceutico/medicamentos";
    }
}