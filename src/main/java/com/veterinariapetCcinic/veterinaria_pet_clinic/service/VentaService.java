package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.venta;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;

@Service
public class VentaService {

    
    public venta procesarVenta(venta venta) {
        if (venta.getPrecioUnitario() != null && venta.getCantidad() != null) {
            venta.setTotal(venta.getPrecioUnitario() * venta.getCantidad());
        }
        return venta; 
    }

    
    public Map<String, Object> generarBoletaDigital(Long id) {
        Map<String, Object> boleta = new HashMap<>();
        
        boleta.put("id_venta", id);
        boleta.put("fecha_emision", LocalDateTime.now());
        boleta.put("empresa", "Veterinaria Pet Clinic");
        boleta.put("estado", "PAGADO");
        boleta.put("mensaje", "Gracias por su compra en Pet Clinic 2026");
    
        return boleta;
    }
}