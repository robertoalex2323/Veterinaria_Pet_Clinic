package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
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

@RestController
@RequestMapping("/api/v1/vendedor")
@CrossOrigin(origins = "*") 
public class VendedorController {

    private final VentaService ventaService;
    private final ProductoService productoService; 

    public VendedorController(VentaService ventaService, ProductoService productoService) {
        this.ventaService = ventaService;
        this.productoService = productoService;
    }


    @PostMapping("/ventas")
    public ResponseEntity<Venta> registrarVenta(@RequestBody Venta nuevaVenta) {
        Venta ventaProcesada = ventaService.procesarVenta(nuevaVenta);
        return new ResponseEntity<>(ventaProcesada, HttpStatus.CREATED);
    }

    @PatchMapping("/pedidos/{id}/completar")
    public ResponseEntity<Map<String, String>> atenderPedido(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("mensaje", "Pedido #" + id + " ha sido marcado como COMPLETADO y ENTREGADO."));
    }

    @GetMapping("/ventas/{id}/boleta")
    public ResponseEntity<Map<String, Object>> emitirBoleta(@PathVariable Long id) {
        Map<String, Object> boleta = ventaService.generarBoletaDigital(id);
        return ResponseEntity.ok(boleta);
    }

    @GetMapping("/promociones/activas")
    public ResponseEntity<List<String>> listarPromociones() {
        List<String> promociones = List.of(
            "10% de descuento en Alimentos Premium por compras mayores a S/120",
            "2x1 en Juguetes y Accesorios los días Martes y Viernes",
            "Descuento especial en Camas para mascotas por fin de temporada 2026"
        );
        return ResponseEntity.ok(promociones);
    }

    @GetMapping("/ventas")
    public ResponseEntity<List<Venta>> listarVentas() {
        return ResponseEntity.ok(ventaService.listarVentas());
    }

    @GetMapping("/ventas/hoy")
    public ResponseEntity<Map<String, Object>> ventasHoy() {
        List<Venta> ventas = ventaService.listarVentasHoy();
        BigDecimal total = ventaService.calcularVentasHoy();
        
        Map<String, Object> response = new HashMap<>();
        response.put("total", total != null ? total : BigDecimal.ZERO);
        response.put("cantidad", ventas != null ? ventas.size() : 0);
        response.put("ventas", ventas != null ? ventas : List.of());
        
        return ResponseEntity.ok(response);
    }
   
@GetMapping("/ventas/ultimos-7-dias")
public ResponseEntity<Map<String, Object>> ventasUltimos7Dias() {
    Map<String, Object> response = new HashMap<>();
    List<String> labels = new ArrayList<>();
    List<Double> values = new ArrayList<>();
    
    LocalDateTime hoy = LocalDateTime.now();
    
    for (int i = 6; i >= 0; i--) {
        LocalDateTime fecha = hoy.minusDays(i);
        LocalDateTime inicio = fecha.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime fin = fecha.withHour(23).withMinute(59).withSecond(59);
        
        String label = fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM"));
        labels.add(label);
        
        BigDecimal totalDia = ventaService.calcularVentasEntreFechas(inicio, fin);
        values.add(totalDia != null ? totalDia.doubleValue() : 0.0);
    }
    
    response.put("labels", labels);
    response.put("values", values);
    
    return ResponseEntity.ok(response);
}

   
    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }

   
    @GetMapping("/ventas/{id}/boleta-pdf")
    public ResponseEntity<byte[]> generarBoletaPDF(@PathVariable Long id) {
        try {
            byte[] pdf = ventaService.generarBoletaPDFReal(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Boleta_Venta_" + id + ".pdf");
            
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}