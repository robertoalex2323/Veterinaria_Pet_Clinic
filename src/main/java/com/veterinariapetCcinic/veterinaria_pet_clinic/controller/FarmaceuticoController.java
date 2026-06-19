package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Proveedor;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.MedicamentoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.NotificacionService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PdfReportService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProveedorService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.RecetaService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.VentaService;

@Controller
@RequestMapping("/farmaceutico")
public class FarmaceuticoController {

    private final ProveedorService proveedorService;
    private final MedicamentoService medicamentoService;
    private final VentaService ventaService;
    private final RecetaService recetaService;
    private final PdfReportService pdfService;
    private final NotificacionService notificacionService;

    public FarmaceuticoController(ProveedorService proveedorService, 
                                 MedicamentoService medicamentoService,
                                 VentaService ventaService,
                                 RecetaService recetaService,
                                 PdfReportService pdfService,
                                 NotificacionService notificacionService) {
        this.proveedorService = proveedorService;
        this.medicamentoService = medicamentoService;
        this.ventaService = ventaService;
        this.recetaService = recetaService;
        this.pdfService = pdfService;
        this.notificacionService = notificacionService;
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
        return "farmaceutico/medicamentos-info";
    }

    @PostMapping("/inventario/guardar")
    public String guardarMedicamento(@ModelAttribute Medicamento medicamento, RedirectAttributes ra) {
        try {
            medicamentoService.guardar(medicamento);
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
            medicamentoService.eliminar(id);
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
    public String formularioProveedor(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        model.addAttribute("currentPage", "proveedores");
        return "farmaceutico/form-proveedor";
    }

    @PostMapping("/proveedores/guardar")
    public String guardarProveedor(@ModelAttribute Proveedor proveedor, RedirectAttributes ra) {
        try {
            proveedorService.guardar(proveedor);
            ra.addFlashAttribute("success", "Proveedor guardado exitosamente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al guardar el proveedor: " + e.getMessage());
        }
        return "redirect:/farmaceutico/proveedores";
    }

    @GetMapping("/proveedores/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Long id, RedirectAttributes ra) {
        proveedorService.eliminar(id);
        ra.addFlashAttribute("success", "Proveedor eliminado correctamente.");
        return "redirect:/farmaceutico/proveedores";
    }

    @GetMapping("/recetas")
    public String listarRecetas(Model model) {
        model.addAttribute("recetas", recetaService.listarTodas());
        model.addAttribute("currentPage", "recetas");
        return "farmaceutico/recetas";
    }

    @PostMapping("/recetas/entregar/{id}")
    public String entregarReceta(@PathVariable Long id, RedirectAttributes ra) {
        recetaService.marcarComoEntregada(id);
        ra.addFlashAttribute("success", "Receta entregada y marcada como completada.");
        return "redirect:/farmaceutico/recetas";
    }

    @GetMapping("/ventas")
    public String registrarVentas(Model model) {
        model.addAttribute("venta", new Venta());
        model.addAttribute("medicamentos", medicamentoService.listarTodos());
        model.addAttribute("currentPage", "ventas");
        return "farmaceutico/registrar-venta";
    }

    @PostMapping("/ventas/procesar")
    public String procesarVenta(@ModelAttribute Venta venta, RedirectAttributes ra) {
        try {
            Venta ventaGuardada = ventaService.procesarVenta(venta);
            
            // Generar PDF y enviar por correo automáticamente
            byte[] pdf = pdfService.generarComprobanteVenta(ventaGuardada);
            notificacionService.enviarVentaConComprobante(ventaGuardada, pdf);

            ra.addFlashAttribute("success", "Venta procesada con éxito. El comprobante ha sido enviado al correo del cliente.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error en la venta: " + e.getMessage());
        }
        return "redirect:/farmaceutico/dashboard";
    }

    @GetMapping("/perfil")
    public String verPerfil(Model model) {
        model.addAttribute("currentPage", "perfil");
        return "farmaceutico/perfil";
    }

    @GetMapping("/reportes/stock-bajo")
    public ResponseEntity<byte[]> descargarReporteStock() {
        byte[] pdfData = pdfService.generarReporteStockBajo(medicamentoService.listarBajoStock());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_stock_bajo.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }

    @GetMapping("/ventas/comprobante/{id}")
    public ResponseEntity<byte[]> descargarComprobante(@PathVariable Long id) {
        Venta venta = ventaService.listarVentas().stream()
                .filter(v -> v.getId().equals(id)).findFirst().orElse(null);
        byte[] pdfData = pdfService.generarComprobanteVenta(venta);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=comprobante_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfData);
    }
}
