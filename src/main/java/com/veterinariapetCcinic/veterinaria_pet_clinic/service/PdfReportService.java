package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;

@Service
public class PdfReportService {

    private static final Logger log = LoggerFactory.getLogger(PdfReportService.class);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    private static final String LOGO_PATH = "/static/Imagen/Iconos/logo.png";

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
        addHeaderCell(tabla, "Medicamento");
        addHeaderCell(tabla, "Presentación");
        addHeaderCell(tabla, "Stock Actual");
        addHeaderCell(tabla, "Stock Mínimo");

        for (Medicamento m : items) {
            tabla.addCell(new Phrase(m.getNombre(), FONT_NORMAL));
            tabla.addCell(new Phrase(m.getPresentacion(), FONT_NORMAL));
            tabla.addCell(new Phrase(String.valueOf(m.getStock()), FONT_NORMAL));
            tabla.addCell(new Phrase(String.valueOf(m.getStockMinimo()), FONT_NORMAL));
        }

        documento.add(tabla);
        documento.add(new Paragraph("\nGenerado automáticamente por el Sistema Pet Clinic"));
        documento.close();

        return out.toByteArray();
    }

    public byte[] generarComprobanteVenta(Venta venta) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5);
        PdfWriter.getInstance(documento, out);

        documento.open();

        // --- LOGO ---
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("static/Imagen/Iconos/logo.png");
            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.scaleToFit(80, 80);
                logo.setAlignment(Element.ALIGN_CENTER);
                documento.add(logo);
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo para el PDF: {}", e.getMessage());
        }

        // --- ENCABEZADO ---
        Paragraph titulo = new Paragraph("COMPROBANTE DE VENTA", FONT_TITLE);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph subtitulo = new Paragraph("Veterinaria Pet Clinic", FONT_SUBTITLE);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(subtitulo);

        Paragraph rucLine = new Paragraph("RUC: 20612345678  |  BOLETA ELECTRÓNICA", FONT_NORMAL);
        rucLine.setAlignment(Element.ALIGN_CENTER);
        documento.add(rucLine);

        documento.add(Chunk.NEWLINE);

        // --- DATOS COMPROBANTE ---
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaStr = venta.getFecha() != null ? venta.getFecha().format(dtf) : "---";

        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{30, 70});
        infoTable.setSpacingAfter(10f);

        addField(infoTable, "N° Venta:", "VTA-" + String.format("%05d", venta.getId()));
        addField(infoTable, "Fecha:", fechaStr);
        addField(infoTable, "Cliente:", venta.getCliente() != null ? venta.getCliente().getNombre() : "---");
        addField(infoTable, "Atendido por:", venta.getUsuario() != null ? venta.getUsuario().getNombre() : "---");
        addField(infoTable, "Método Pago:", venta.getMetodoPago() != null ? venta.getMetodoPago() : "---");

        documento.add(infoTable);

        // --- TABLA DE DETALLE ---
        PdfPTable detalleTable = new PdfPTable(4);
        detalleTable.setWidthPercentage(100);
        detalleTable.setWidths(new float[]{40, 15, 20, 25});

        // Cabeceras
        addHeaderCell(detalleTable, "MEDICAMENTO");
        addHeaderCell(detalleTable, "CANT.");
        addHeaderCell(detalleTable, "P. UNIT.");
        addHeaderCell(detalleTable, "IMPORTE");

        // Filas
        for (DetalleVenta d : venta.getDetalles()) {
            String nombre = d.getMedicamento() != null ? d.getMedicamento().getNombre() : "---";
            String cantidad = String.valueOf(d.getCantidad());
            String precioUnit = "S/ " + (d.getPrecioUnitario() != null ? d.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP).toString() : "0.00");
            BigDecimal importe = d.getPrecioUnitario() != null
                    ? d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            String importeStr = "S/ " + importe.toString();

            detalleTable.addCell(new Phrase(nombre, FONT_NORMAL));
            detalleTable.addCell(new Phrase(cantidad, FONT_NORMAL));
            detalleTable.addCell(new Phrase(precioUnit, FONT_NORMAL));
            detalleTable.addCell(new Phrase(importeStr, FONT_BOLD));
        }

        documento.add(detalleTable);
        documento.add(Chunk.NEWLINE);

        // --- RESUMEN FINAL (Subtotal, IGV, Total) ---
        BigDecimal subtotal = venta.getSubtotal() != null ? venta.getSubtotal() : BigDecimal.ZERO;
        BigDecimal igv = venta.getIgv() != null ? venta.getIgv() : BigDecimal.ZERO;
        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;

        PdfPTable resumenTable = new PdfPTable(2);
        resumenTable.setWidthPercentage(60);
        resumenTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        resumenTable.setWidths(new float[]{50, 50});

        addResumenRow(resumenTable, "SUBTOTAL:", "S/ " + subtotal.setScale(2, RoundingMode.HALF_UP).toString(), FONT_NORMAL);
        addResumenRow(resumenTable, "IGV (18%):", "S/ " + igv.setScale(2, RoundingMode.HALF_UP).toString(), FONT_NORMAL);
        addResumenRow(resumenTable, "TOTAL:", "S/ " + total.setScale(2, RoundingMode.HALF_UP).toString(), FONT_TOTAL);

        documento.add(resumenTable);

        documento.add(Chunk.NEWLINE);

        // --- PIE ---
        Paragraph thanks = new Paragraph("¡Gracias por su compra!", FONT_SUBTITLE);
        thanks.setAlignment(Element.ALIGN_CENTER);
        documento.add(thanks);

        Paragraph footer = new Paragraph("Este comprobante es válido como respaldo de su compra.\n" +
                "Veterinaria Pet Clinic - Av. Principal 123 - Lima", FONT_NORMAL);
        footer.setAlignment(Element.ALIGN_CENTER);
        documento.add(footer);

        documento.close();

        return out.toByteArray();
    }

    // --- HELPERS ---

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_HEADER));
        cell.setBackgroundColor(new java.awt.Color(41, 128, 185));
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addField(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_BOLD));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, FONT_NORMAL));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(2);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addResumenRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }
}
