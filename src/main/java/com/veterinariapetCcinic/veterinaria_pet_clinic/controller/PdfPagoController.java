package com.veterinariapetCcinic.veterinaria_pet_clinic.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Pago;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.ComprobantePagoPdfService;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.PagoService;

@RestController
@RequestMapping("/recepcionista/pagos")
public class PdfPagoController {

    private final PagoService pagoService;
    private final ComprobantePagoPdfService comprobantePagoPdfService;

    public PdfPagoController(PagoService pagoService, ComprobantePagoPdfService comprobantePagoPdfService) {
        this.pagoService = pagoService;
        this.comprobantePagoPdfService = comprobantePagoPdfService;
    }

    @GetMapping("/ver/{id}/comprobante.pdf")
    public ResponseEntity<ByteArrayResource> descargarComprobante(@PathVariable Long id) {
        Pago pago = pagoService.buscarPorId(id);

        String responsable = getNombreUsuario();

        byte[] pdf = comprobantePagoPdfService.generarComprobantePago(pago, responsable);

        String comprobante = pago.getComprobante();
        if (comprobante == null || comprobante.isBlank()) {
            comprobante = "PET2026-00001";
        }

        String filename = comprobante + ".pdf";

        ByteArrayResource resource = new ByteArrayResource(pdf);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);

        return ResponseEntity.ok().headers(headers).contentLength(pdf.length).body(resource);
    }

    private String getNombreUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "Recepcionista";
    }
}

