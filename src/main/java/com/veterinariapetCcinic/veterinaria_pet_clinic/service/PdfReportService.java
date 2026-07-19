package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.AlertaCritica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Consulta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.SignosVitales;
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
    private static final Font FONT_SMALL = FontFactory.getFont(FontFactory.HELVETICA, 8);

    private static final String LOGO_PATH = "/static/Imagen/Iconos/logo.png";

    /**
     * Reporte de medicamentos con stock crítico (incluye logo).
     */
    public byte[] generarReporteStockBajo(List<Medicamento> items) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4);
        PdfWriter.getInstance(documento, out);

        documento.open();

        // --- LOGO + ENCABEZADO ---
        addLogo(documento, 90, 90);

        Paragraph titulo = new Paragraph("Reporte de Medicamentos - Stock Crítico", FONT_TITLE);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph subtitulo = new Paragraph("Veterinaria Pet Clinic", FONT_SUBTITLE);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(subtitulo);

        Paragraph rucLine = new Paragraph("RUC: 20612345678", FONT_NORMAL);
        rucLine.setAlignment(Element.ALIGN_CENTER);
        documento.add(rucLine);

        Paragraph fechaGen = new Paragraph(
                "Generado el: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                FONT_NORMAL);
        fechaGen.setAlignment(Element.ALIGN_CENTER);
        documento.add(fechaGen);

        documento.add(Chunk.NEWLINE);

        // --- RESUMEN ---
        PdfPTable resumen = new PdfPTable(2);
        resumen.setWidthPercentage(100);
        resumen.setWidths(new float[]{70, 30});
        resumen.setSpacingAfter(10f);
        addField(resumen, "Total de medicamentos en stock crítico:", String.valueOf(items.size()));
        documento.add(resumen);

        // --- TABLA ---
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{30, 20, 15, 15, 20});
        addHeaderCell(tabla, "Medicamento");
        addHeaderCell(tabla, "Presentación");
        addHeaderCell(tabla, "Stock Actual");
        addHeaderCell(tabla, "Stock Mínimo");
        addHeaderCell(tabla, "Estado");

        for (Medicamento m : items) {
            tabla.addCell(new Phrase(m.getNombre(), FONT_NORMAL));
            tabla.addCell(new Phrase(m.getPresentacion() != null ? m.getPresentacion() : "---", FONT_NORMAL));
            tabla.addCell(new Phrase(String.valueOf(m.getStock()), FONT_BOLD));
            tabla.addCell(new Phrase(String.valueOf(m.getStockMinimo()), FONT_NORMAL));

            int stock = m.getStock() != null ? m.getStock() : 0;
            int min = m.getStockMinimo() != null ? m.getStockMinimo() : 0;
            String estado = stock <= 0 ? "AGOTADO" : (stock <= min ? "CRÍTICO" : "BAJO");
            tabla.addCell(new Phrase(estado, FONT_BOLD));
        }

        documento.add(tabla);
        documento.add(Chunk.NEWLINE);
        documento.add(new Paragraph("Generado automáticamente por el Sistema Pet Clinic", FONT_SMALL));

        documento.close();
        return out.toByteArray();
    }

    /**
     * Reporte general de ventas en un rango de fechas (incluye logo).
     */
    public byte[] generarReporteVentas(List<Venta> ventas, LocalDateTime desde, LocalDateTime hasta) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(documento, out);

        documento.open();

        // --- LOGO + ENCABEZADO ---
        addLogo(documento, 90, 90);

        Paragraph titulo = new Paragraph("Reporte de Ventas", FONT_TITLE);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph subtitulo = new Paragraph("Veterinaria Pet Clinic", FONT_SUBTITLE);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(subtitulo);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String rango = "Periodo: " + (desde != null ? desde.format(dtf) : "Inicio") +
                "  -  " + (hasta != null ? hasta.format(dtf) : "Ahora");
        Paragraph rangoLine = new Paragraph(rango, FONT_NORMAL);
        rangoLine.setAlignment(Element.ALIGN_CENTER);
        documento.add(rangoLine);

        documento.add(Chunk.NEWLINE);

        // --- KPIs ---
        BigDecimal totalSubtotal = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;
        BigDecimal totalTotal = BigDecimal.ZERO;
        Map<String, Integer> contadorMetodos = new HashMap<>();

        for (Venta v : ventas) {
            totalSubtotal = totalSubtotal.add(v.getSubtotal() != null ? v.getSubtotal() : BigDecimal.ZERO);
            totalIgv = totalIgv.add(v.getIgv() != null ? v.getIgv() : BigDecimal.ZERO);
            totalTotal = totalTotal.add(v.getTotal() != null ? v.getTotal() : BigDecimal.ZERO);
            String mp = v.getMetodoPago() != null ? v.getMetodoPago() : "SIN_ESPECIFICAR";
            contadorMetodos.merge(mp, 1, Integer::sum);
        }

        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setWidths(new float[]{25, 25, 25, 25});
        kpiTable.setSpacingAfter(10f);
        addField(kpiTable, "N° Ventas:", String.valueOf(ventas.size()));
        addField(kpiTable, "Subtotal:", "S/ " + totalSubtotal.setScale(2, RoundingMode.HALF_UP).toString());
        addField(kpiTable, "IGV (18%):", "S/ " + totalIgv.setScale(2, RoundingMode.HALF_UP).toString());
        addField(kpiTable, "Total:", "S/ " + totalTotal.setScale(2, RoundingMode.HALF_UP).toString());
        documento.add(kpiTable);

        // --- TABLA DE VENTAS ---
        PdfPTable tabla = new PdfPTable(7);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{12, 15, 22, 15, 12, 12, 12});
        addHeaderCell(tabla, "N° VENTA");
        addHeaderCell(tabla, "FECHA");
        addHeaderCell(tabla, "CLIENTE");
        addHeaderCell(tabla, "MÉTODO PAGO");
        addHeaderCell(tabla, "SUBTOTAL");
        addHeaderCell(tabla, "IGV");
        addHeaderCell(tabla, "TOTAL");

        for (Venta v : ventas) {
            String numVenta = "VTA-" + String.format("%05d", v.getId());
            String fecha = v.getFecha() != null ? v.getFecha().format(dtf) : "---";
            String cliente = v.getCliente() != null ? v.getCliente().getNombre() : "---";
            String metodo = v.getMetodoPago() != null ? v.getMetodoPago() : "---";
            String subtotal = "S/ " + (v.getSubtotal() != null ? v.getSubtotal().setScale(2, RoundingMode.HALF_UP).toString() : "0.00");
            String igv = "S/ " + (v.getIgv() != null ? v.getIgv().setScale(2, RoundingMode.HALF_UP).toString() : "0.00");
            String total = "S/ " + (v.getTotal() != null ? v.getTotal().setScale(2, RoundingMode.HALF_UP).toString() : "0.00");

            tabla.addCell(new Phrase(numVenta, FONT_BOLD));
            tabla.addCell(new Phrase(fecha, FONT_NORMAL));
            tabla.addCell(new Phrase(cliente, FONT_NORMAL));
            tabla.addCell(new Phrase(metodo, FONT_NORMAL));
            tabla.addCell(new Phrase(subtotal, FONT_NORMAL));
            tabla.addCell(new Phrase(igv, FONT_NORMAL));
            tabla.addCell(new Phrase(total, FONT_BOLD));
        }

        documento.add(tabla);
        documento.add(Chunk.NEWLINE);

        // --- RESUMEN POR MÉTODO DE PAGO ---
        if (!contadorMetodos.isEmpty()) {
            Paragraph subResumen = new Paragraph("Resumen por Método de Pago", FONT_SUBTITLE);
            documento.add(subResumen);
            documento.add(Chunk.NEWLINE);

            PdfPTable metodosTable = new PdfPTable(2);
            metodosTable.setWidthPercentage(50);
            metodosTable.setWidths(new float[]{70, 30});
            addHeaderCell(metodosTable, "Método de Pago");
            addHeaderCell(metodosTable, "N° Ventas");

            // Ordenar por cantidad descendente
            Map<String, Integer> ordenado = new LinkedHashMap<>();
            contadorMetodos.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> ordenado.put(e.getKey(), e.getValue()));

            for (Map.Entry<String, Integer> entry : ordenado.entrySet()) {
                metodosTable.addCell(new Phrase(entry.getKey(), FONT_NORMAL));
                metodosTable.addCell(new Phrase(String.valueOf(entry.getValue()), FONT_BOLD));
            }
            documento.add(metodosTable);
            documento.add(Chunk.NEWLINE);
        }

        documento.add(new Paragraph("Generado automáticamente por el Sistema Pet Clinic - " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), FONT_SMALL));

        documento.close();
        return out.toByteArray();
    }

    public byte[] generarComprobanteVenta(Venta venta) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5);
        PdfWriter.getInstance(documento, out);

        documento.open();

        // --- LOGO ---
        addLogo(documento, 80, 80);

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

    /**
     * Genera un PDF clinico cuyo contenido depende del tipo solicitado:
     * consulta, tratamiento o completo.
     */
    public byte[] generarReporteClinico(Mascota mascota, String tipo, List<Consulta> consultas,
                                        SignosVitales ultimoSigno, List<AlertaCritica> alertas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 42, 42, 42, 42);
        PdfWriter.getInstance(documento, out);
        documento.open();

        String titulo = switch (tipo) {
            case "tratamiento" -> "Plan de Tratamiento";
            case "completo" -> "Reporte Clinico Completo";
            default -> "Reporte de Consulta";
        };
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        addLogo(documento, 70, 70);
        Paragraph encabezado = new Paragraph("Veterinaria Pet Clinic", FONT_TITLE);
        encabezado.setAlignment(Element.ALIGN_CENTER);
        documento.add(encabezado);
        Paragraph subtitulo = new Paragraph(titulo, FONT_SUBTITLE);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(subtitulo);
        Paragraph generado = new Paragraph("Generado el: " + LocalDateTime.now().format(fecha), FONT_SMALL);
        generado.setAlignment(Element.ALIGN_CENTER);
        documento.add(generado);
        documento.add(Chunk.NEWLINE);

        agregarTituloSeccion(documento, "Datos del paciente");
        PdfPTable paciente = new PdfPTable(2);
        paciente.setWidthPercentage(100);
        addField(paciente, "Paciente:", texto(mascota.getNombre()));
        addField(paciente, "Especie:", texto(mascota.getEspecie()));
        addField(paciente, "Raza:", texto(mascota.getRaza()));
        addField(paciente, "Edad:", mascota.getEdad() != null ? mascota.getEdad() + " anos" : "-");
        addField(paciente, "Dueno:", mascota.getCliente() != null ? texto(mascota.getCliente().getNombre()) : "-");
        documento.add(paciente);
        documento.add(Chunk.NEWLINE);

        if ("consulta".equals(tipo) || "completo".equals(tipo)) {
            agregarTituloSeccion(documento, "Consultas registradas");
            if (consultas.isEmpty()) {
                documento.add(new Paragraph("No hay consultas registradas.", FONT_NORMAL));
            } else {
                for (Consulta consulta : consultas) {
                    String fechaConsulta = consulta.getFechaConsulta() != null
                            ? consulta.getFechaConsulta().format(fecha) : "Sin fecha";
                    Paragraph detalle = new Paragraph(
                            "Fecha: " + fechaConsulta + "\nMotivo: " + texto(consulta.getMotivoConsulta())
                                    + "\nObservaciones / diagnostico y tratamiento: " + texto(consulta.getObservaciones()),
                            FONT_NORMAL);
                    detalle.setSpacingAfter(10f);
                    documento.add(detalle);
                }
            }
            documento.add(Chunk.NEWLINE);
        }

        if ("tratamiento".equals(tipo) || "completo".equals(tipo)) {
            agregarTituloSeccion(documento, "Plan e indicaciones");
            Consulta ultimaConsulta = consultas.stream()
                    .filter(consulta -> consulta.getFechaConsulta() != null)
                    .max(java.util.Comparator.comparing(Consulta::getFechaConsulta))
                    .orElse(consultas.isEmpty() ? null : consultas.get(0));
            if (ultimaConsulta == null || ultimaConsulta.getObservaciones() == null
                    || ultimaConsulta.getObservaciones().isBlank()) {
                documento.add(new Paragraph("No hay plan o indicaciones registrados para este paciente.", FONT_NORMAL));
            } else {
                documento.add(new Paragraph(ultimaConsulta.getObservaciones(), FONT_NORMAL));
            }
            documento.add(Chunk.NEWLINE);
        }

        if ("completo".equals(tipo)) {
            if (ultimoSigno != null) {
                agregarTituloSeccion(documento, "Signos vitales actuales");
                PdfPTable signos = new PdfPTable(2);
                signos.setWidthPercentage(100);
                addField(signos, "Temperatura:", ultimoSigno.getTemperatura() != null ? ultimoSigno.getTemperatura() + " C" : "-");
                addField(signos, "Peso:", ultimoSigno.getPeso() != null ? ultimoSigno.getPeso() + " kg" : "-");
                addField(signos, "Frecuencia cardiaca:", ultimoSigno.getFrecuenciaCardiaca() != null ? ultimoSigno.getFrecuenciaCardiaca() + " bpm" : "-");
                addField(signos, "Frecuencia respiratoria:", ultimoSigno.getFrecuenciaRespiratoria() != null ? ultimoSigno.getFrecuenciaRespiratoria() + " rpm" : "-");
                addField(signos, "Estado general:", texto(ultimoSigno.getEstadoGeneral()));
                addField(signos, "Advertencia clinica:", texto(ultimoSigno.getAdvertencia()));
                documento.add(signos);
                documento.add(Chunk.NEWLINE);
            }

            agregarTituloSeccion(documento, "Alertas activas");
            if (alertas.isEmpty()) {
                documento.add(new Paragraph("No hay alertas activas.", FONT_NORMAL));
            } else {
                for (AlertaCritica alerta : alertas) {
                    documento.add(new Paragraph(
                            "[" + texto(alerta.getPrioridad()) + "] " + texto(alerta.getDescripcion()), FONT_NORMAL));
                }
            }
        }

        documento.close();
        return out.toByteArray();
    }

    // --- HELPERS ---

    private void agregarTituloSeccion(Document documento, String titulo) {
        Paragraph seccion = new Paragraph(titulo, FONT_SUBTITLE);
        seccion.setSpacingBefore(4f);
        seccion.setSpacingAfter(7f);
        documento.add(seccion);
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor;
    }

    /**
     * Agrega el logo de la veterinaria al documento PDF (centrado).
     */
    private void addLogo(Document documento, float width, float height) {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("static/Imagen/Iconos/logo.png");
            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.scaleToFit(width, height);
                logo.setAlignment(Element.ALIGN_CENTER);
                documento.add(logo);
            } else {
                log.warn("No se encontró el recurso del logo: static/Imagen/Iconos/logo.png");
            }
        } catch (Exception e) {
            log.warn("No se pudo cargar el logo para el PDF: {}", e.getMessage());
        }
    }

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
