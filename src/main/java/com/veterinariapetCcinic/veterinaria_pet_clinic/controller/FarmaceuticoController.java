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

    private final ProveedorService proveedorService;
    private final MedicamentoService medicamentoService;
    private final VentaService ventaService;
    private final RecetaService recetaService;
    private final FarmaceuticoService farmaceuticoService;
    private final PdfReportService pdfService;
    private final NotificacionService notificacionService;
    private final SimpMessagingTemplate messagingTemplate;

    public FarmaceuticoController(ProveedorService proveedorService, 
                                 MedicamentoService medicamentoService,
                                 VentaService ventaService,
                                 RecetaService recetaService,
                                 FarmaceuticoService farmaceuticoService,
                                 PdfReportService pdfService,
                                 NotificacionService notificacionService,
                                 SimpMessagingTemplate messagingTemplate) {
        this.proveedorService = proveedorService;
        this.medicamentoService = medicamentoService;
        this.ventaService = ventaService;
        this.recetaService = recetaService;
        this.farmaceuticoService = farmaceuticoService;
        this.pdfService = pdfService;
        this.notificacionService = notificacionService;
        this.messagingTemplate = messagingTemplate;
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

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("bajoStockCount", medicamentoService.listarBajoStock().size());
        model.addAttribute("ventasHoy", ventaService.calcularVentasHoy());
        model.addAttribute("recetasPendientes", recetaService.listarPendientes().size());
        return "farmaceutico/dashboard";
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
                String nombreArchivo = UUID.randomUUID().toString() + "_" + imagenArchivo.getOriginalFilename();
                Path ruta = Paths.get("src/main/resources/static/Imagen/Medicamento/" + nombreArchivo);
                Files.createDirectories(ruta.getParent());
                Files.write(ruta, imagenArchivo.getBytes());
                medicamento.setImagenUrl("/Imagen/Medicamento/" + nombreArchivo);
            }
            boolean isNew = (medicamento.getId() == null);
            medicamentoService.guardar(medicamento);
            if (isNew) {
                sendNotification("success", "Nuevo medicamento agregado: " + medicamento.getNombre());
            } else {
                sendNotification("info", "Medicamento actualizado: " + medicamento.getNombre());
            }
            ra.addFlashAttribute("success", "Medicamento registrado en inventario.");
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
        try {
            boolean isNew = (proveedor.getId() == null);
            proveedorService.guardar(proveedor);
            if (isNew) {
                sendNotification("success", "Nuevo proveedor registrado: " + proveedor.getNombre());
            } else {
                sendNotification("info", "Proveedor actualizado: " + proveedor.getNombre());
            }
            ra.addFlashAttribute("success", "Proveedor guardado exitosamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar el proveedor: " + e.getMessage());
        }
        return "redirect:/farmaceutico/proveedores";
    }

    @GetMapping("/proveedores/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Long id, RedirectAttributes ra) {
        try {
            String nombre = proveedorService.buscarPorId(id).getNombre();
            proveedorService.eliminar(id);
            sendNotification("warning", "Proveedor eliminado: " + nombre);
            ra.addFlashAttribute("success", "Proveedor eliminado correctamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al eliminar proveedor: " + e.getMessage());
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
    // VISTA: procesar venta (POST tradicional)
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
        return "farmaceutico/perfil";
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
            String nombreArchivo = UUID.randomUUID().toString() + "_" + archivo.getOriginalFilename();
            Path ruta = Paths.get("src/main/resources/static/Imagen/Medicamento/" + nombreArchivo);
            Files.createDirectories(ruta.getParent());
            Files.write(ruta, archivo.getBytes());
            med.setImagenUrl("/Imagen/Medicamento/" + nombreArchivo);
            medicamentoService.guardar(med);
            ra.addFlashAttribute("success", "Imagen subida correctamente.");
            sendNotification("success", "Imagen actualizada para: " + med.getNombre());
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Error al subir la imagen: " + e.getMessage());
        }
        return "redirect:/farmaceutico/medicamentos";
    }
}
