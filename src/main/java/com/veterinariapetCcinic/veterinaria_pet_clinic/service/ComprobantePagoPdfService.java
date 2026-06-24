package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Pago;

@Service
public class ComprobantePagoPdfService {

    private static final String LOGO_RESOURCE = "static/Imagen/Iconos/logo.png";

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font FONT_SUBTITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font FONT_NORMAL = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font FONT_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13);
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8);
    private static final Font FONT_HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    public byte[] generarComprobantePago(Pago pago, String responsable) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5);
        PdfWriter.getInstance(documento, out);

        documento.open();

        // Logo
        addLogo(documento, 70, 70);

        // Título
        Paragraph titulo = new Paragraph("COMPROBANTE DE PAGO", FONT_TITLE);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph subtitulo = new Paragraph("Veterinaria Pet Clinic", FONT_SUBTITLE);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(subtitulo);

        documento.add(Chunk.NEWLINE);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaHora = pago.getFechaPago() != null ? pago.getFechaPago().format(dtf) : "---";

        // Info comprobante
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setWidths(new float[] { 35, 65 });
        info.setSpacingAfter(8f);

        addField(info, "N° Comprobante:", safe(pago.getComprobante()));
        addField(info, "Fecha:", fechaHora);
        addField(info, "Método de Pago:", safe(pago.getMetodoPago()));
        addField(info, "Estado:", safe(pago.getEstado()));

        documento.add(info);

        // Cliente
        PdfPTable cliente = new PdfPTable(2);
        cliente.setWidthPercentage(100);
        cliente.setWidths(new float[] { 35, 65 });
        cliente.setSpacingAfter(10f);

        addField(cliente, "Cliente:", pago.getCliente() != null ? safe(pago.getCliente().getNombre()) : "---");
        addField(cliente, "DNI/Teléfono:", pago.getCliente() != null ? safe(pago.getCliente().getTelefono()) : "---");
        addField(cliente, "Email:", pago.getCliente() != null ? safe(pago.getCliente().getEmail()) : "---");
        addField(cliente, "Atendido por:", responsable != null && !responsable.isBlank() ? responsable : "---");

        documento.add(cliente);

        // Detalle (resumen)
        PdfPTable detalle = new PdfPTable(3);
        detalle.setWidthPercentage(100);
        detalle.setWidths(new float[] { 55, 15, 30 });
        detalle.setSpacingAfter(8f);

        addHeaderCell(detalle, "DESCRIPCIÓN");
        addHeaderCell(detalle, "CANT.");
        addHeaderCell(detalle, "IMPORTE");

        String desc;
        if (pago.getCita() != null && pago.getCita().getMascota() != null) {
            String mascota = safe(pago.getCita().getMascota().getNombre());
            String motivo = safe(pago.getCita().getMotivo());
            desc = "Atención: " + mascota + "\n" + "Motivo: " + motivo;
        } else {
            desc = "Cobro general en recepción";
        }

        detalle.addCell(new PdfPCell(new com.lowagie.text.Phrase(desc, FONT_NORMAL)));
        PdfPCell cantCell = new PdfPCell(new com.lowagie.text.Phrase("1", FONT_NORMAL));
        cantCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        detalle.addCell(cantCell);

        String monto = pago.getMonto() != null
                ? String.format(java.util.Locale.US, "%.2f", pago.getMonto())
                : "0.00";
        detalle.addCell(new PdfPCell(new com.lowagie.text.Phrase("S/ " + monto, FONT_BOLD)));


        documento.add(detalle);

        // Totales
        double total = pago.getMonto() != null ? pago.getMonto() : 0d;
        double subtotal = total / 1.18;
        double igv = total - subtotal;

        PdfPTable totales = new PdfPTable(2);
        totales.setWidthPercentage(60);
        totales.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totales.setWidths(new float[] { 50, 50 });

        addResumenRow(totales, "Subtotal:", "S/ " + round2(subtotal), FONT_NORMAL);
        addResumenRow(totales, "IGV (18%):", "S/ " + round2(igv), FONT_NORMAL);
        addResumenRow(totales, "TOTAL:", "S/ " + round2(total), FONT_TOTAL);

        documento.add(totales);
        documento.add(Chunk.NEWLINE);

        Paragraph footer = new Paragraph(
                "Comprobante válido como respaldo de pago.\nVeterinaria Pet Clinic - Av. Principal 123 - Lima",
                FONT_SMALL);
        footer.setAlignment(Element.ALIGN_CENTER);
        documento.add(footer);

        documento.close();
        return out.toByteArray();
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "---" : s;
    }

    private void addLogo(Document documento, float width, float height) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(LOGO_RESOURCE);
            if (is == null) {
                return;
            }
            Image logo = Image.getInstance(is.readAllBytes());
            logo.scaleToFit(width, height);
            logo.setAlignment(Element.ALIGN_CENTER);
            documento.add(logo);
        } catch (Exception e) {
            // no bloqueante
        }
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new com.lowagie.text.Phrase(text, FONT_HEADER));
        cell.setBackgroundColor(new java.awt.Color(41, 128, 185));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addField(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new com.lowagie.text.Phrase(label, FONT_BOLD));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(2);

        PdfPCell valueCell = new PdfPCell(new com.lowagie.text.Phrase(value, FONT_NORMAL));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(2);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addResumenRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new com.lowagie.text.Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell valueCell = new PdfPCell(new com.lowagie.text.Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String round2(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }
}

