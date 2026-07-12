package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/vendedor/ventas")
public class VendedorHistorialVentaController {

    private final VentaRepository ventaRepository;

    public VendedorHistorialVentaController(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @GetMapping("/historial")
    public String historial(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model
    ) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "fecha")
        );

        // Nota: si tu VentaRepository no soporta búsqueda por q, este controller igual puede mostrar todas.
        Page<Venta> ventas = ventaRepository.findAll(pageable);

        model.addAttribute("ventas", ventas.getContent());
        model.addAttribute("page", ventas);
        model.addAttribute("size", size);
        model.addAttribute("q", q);
        model.addAttribute("pageInfoText", String.format("Mostrando %d - %d de %d", ventas.getNumber() * ventas.getSize() + 1,
                Math.min((ventas.getNumber() + 1) * ventas.getSize(), ventas.getTotalElements()), ventas.getTotalElements()));

        return "Vendedor/historial";
    }
}

