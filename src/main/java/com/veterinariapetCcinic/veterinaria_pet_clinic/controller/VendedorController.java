package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ProductoService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.VentaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ===== VENTAS =====
    @PostMapping("/ventas")
    public ResponseEntity<Venta> registrarVenta(@RequestBody Venta nuevaVenta) {
        return new ResponseEntity<>(ventaService.procesarVenta(nuevaVenta), HttpStatus.CREATED);
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

            labels.add(fecha.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")));

            BigDecimal totalDia = ventaService.calcularVentasEntreFechas(inicio, fin);
            values.add(totalDia != null ? totalDia.doubleValue() : 0.0);
        }

        response.put("labels", labels);
        response.put("values", values);

        return ResponseEntity.ok(response);
    }

    // ===== BOLETAS =====
    @GetMapping("/ventas/{id}/boleta")
    public ResponseEntity<Map<String, Object>> emitirBoleta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.generarBoletaDigital(id));
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

    // ===== PRODUCTOS =====
    @GetMapping("/productos")
    public ResponseEntity<List<Producto>> listarProductos() {
        return ResponseEntity.ok(productoService.listarTodos());
    }


    // ===== DESCUENTO EN TIEMPO REAL =====
    @PostMapping("/calcular-descuento")
    public ResponseEntity<Map<String, Object>> calcularDescuento(@RequestBody Map<String, Object> request) {
        try {
            Long productoId = Long.valueOf(request.get("productoId").toString());
            Integer cantidad = Integer.valueOf(request.get("cantidad").toString());
            BigDecimal precioUnitario = new BigDecimal(request.get("precioUnitario").toString());
            String clienteNombre = request.get("clienteNombre").toString();

            // Crear venta temporal para calcular descuentos
            Venta ventaTemp = new Venta();

            Cliente cliente = new Cliente();
            cliente.setNombre(clienteNombre);
            ventaTemp.setCliente(cliente);

            Producto producto = productoService.buscarPorId(productoId);
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setVenta(ventaTemp);
            detalle.calcularSubtotal();

            List<DetalleVenta> detalles = new ArrayList<>();
            detalles.add(detalle);
            ventaTemp.setDetalles(detalles);
            ventaTemp.recalcularTotales();

            BigDecimal descuento = ventaService.calcularDescuentoPrevio(ventaTemp);
            List<String> promociones = ventaService.obtenerPromocionesAplicables(ventaTemp);

            Map<String, Object> response = new HashMap<>();
            response.put("descuento", descuento != null ? descuento : BigDecimal.ZERO);
            response.put("promociones", promociones != null ? promociones : new ArrayList<>());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("descuento", BigDecimal.ZERO);
            error.put("promociones", new ArrayList<>());
            return ResponseEntity.ok(error);
        }
    }
}