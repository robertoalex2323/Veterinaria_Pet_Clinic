package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Venta;

@Service
public class PdfReportService {

    public byte[] generarReporteStockBajo(List<Medicamento> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4);
        PdfWriter.getInstance(documento, out);

        documento.open();
        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph titulo = new Paragraph("Reporte de Medicamentos - Stock Crítico", fuenteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);
        documento.add(Chunk.NEWLINE);

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.addCell("Medicamento");
        tabla.addCell("Presentación");
        tabla.addCell("Stock Actual");
        tabla.addCell("Stock Mínimo");

        for (Medicamento m : items) {
            tabla.addCell(m.getNombre());
            tabla.addCell(m.getPresentacion());
            tabla.addCell(String.valueOf(m.getStock()));
            tabla.addCell(String.valueOf(m.getStockMinimo()));
        }

        documento.add(tabla);
        documento.add(new Paragraph("\nGenerado automáticamente por el Sistema Pet Clinic"));
        documento.close();

        return out.toByteArray();
    }

    public byte[] generarComprobanteVenta(Venta venta) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5); // Tamaño más pequeño tipo ticket
        PdfWriter.getInstance(documento, out);

        documento.open();
        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph titulo = new Paragraph("COMPROBANTE DE VENTA\nPet Clinic", fuenteTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);
        
        documento.add(new Paragraph("Fecha: " + venta.getFecha().toString()));
        documento.add(new Paragraph("Cliente: " + (venta.getCliente() != null ? venta.getCliente().getNombre() : "General")));
        documento.add(Chunk.NEWLINE);

        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        tabla.addCell("Prod.");
        tabla.addCell("Cant.");
        tabla.addCell("Subt.");

        for (DetalleVenta d : venta.getDetalles()) {
            tabla.addCell(d.getMedicamento().getNombre());
            tabla.addCell(String.valueOf(d.getCantidad()));
            tabla.addCell("S/ " + d.calcularImporteTotal());
        }
        documento.add(tabla);

        Paragraph total = new Paragraph("\nTOTAL A PAGAR: S/ " + venta.getTotal(), fuenteTitulo);
        total.setAlignment(Element.ALIGN_RIGHT);
        documento.add(total);

        documento.add(new Paragraph("\n¡Gracias por su compra!", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        documento.close();

        return out.toByteArray();
    }
}