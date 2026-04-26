package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vendedor")
@CrossOrigin(origins = "*") 
public class VendedorController {

    @Autowired
    private VentaService ventaService;

    @PostMapping("/ventas")
    public ResponseEntity<venta> registrarVenta(@RequestBody venta nuevaVenta) {
        venta ventaProcesada = ventaService.procesarVenta(nuevaVenta);
        return new ResponseEntity<>(ventaProcesada, HttpStatus.CREATED);
    }

    
    @PatchMapping("/pedidos/{id}/completar")
    public ResponseEntity<String> atenderPedido(@PathVariable Long id) {
        
        return ResponseEntity.ok("Pedido #" + id + " ha sido marcado como COMPLETADO y ENTREGADO.");
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
}