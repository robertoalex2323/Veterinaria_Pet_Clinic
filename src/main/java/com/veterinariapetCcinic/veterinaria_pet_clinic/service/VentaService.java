package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.DetalleVenta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Producto;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaItem;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ProductoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaMedicaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final ProductoRepository productoRepository;
    private final RecetaMedicaRepository recetaMedicaRepository;
    private final ClienteRepository clienteRepository;

    public VentaService(VentaRepository ventaRepository,
                       MedicamentoRepository medicamentoRepository,
                       ProductoRepository productoRepository,
                       RecetaMedicaRepository recetaMedicaRepository,
                       ClienteRepository clienteRepository) {
        this.ventaRepository = ventaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.productoRepository = productoRepository;
        this.recetaMedicaRepository = recetaMedicaRepository;
        this.clienteRepository = clienteRepository;
    }

    
@Transactional
public Venta procesarVenta(Venta venta) {
    System.out.println("📦 Procesando venta...");
    
    if (venta.getCliente() == null || venta.getCliente().getNombre() == null) {
        throw new RuntimeException(" El nombre del cliente es obligatorio");
    }
    
    String nombreCliente = venta.getCliente().getNombre().trim();
    String telefonoCliente = venta.getCliente().getTelefono() != null ? 
        venta.getCliente().getTelefono() : "999999999";
    String emailCliente = venta.getCliente().getEmail() != null ? 
        venta.getCliente().getEmail() : "cliente@petclinic.com";
    
    Cliente clienteExistente = clienteRepository.findByNombre(nombreCliente);
    
    if (clienteExistente == null) {
        clienteExistente = clienteRepository.findByTelefono(telefonoCliente).orElse(null);
    }
    
    if (clienteExistente == null) {
        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setNombre(nombreCliente);
        nuevoCliente.setTelefono(telefonoCliente);
        nuevoCliente.setEmail(emailCliente);
        nuevoCliente.setDireccion(venta.getCliente().getDireccion() != null ? 
            venta.getCliente().getDireccion() : "No registrada");
        
        clienteExistente = clienteRepository.save(nuevoCliente);
        System.out.println("Cliente creado: " + nombreCliente + " (Tel: " + telefonoCliente + ")");
    } else {
        System.out.println(" Cliente existente: " + clienteExistente.getNombre() + " (ID: " + clienteExistente.getId() + ")");
    }
    venta.setCliente(clienteExistente);
    
    if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
        throw new RuntimeException("La venta debe tener al menos un producto");
    }
    
    System.out.println(" Cantidad de detalles recibidos: " + venta.getDetalles().size());
    
    List<DetalleVenta> detallesOriginales = new ArrayList<>(venta.getDetalles());
    
    venta.getDetalles().clear();
    
    for (DetalleVenta detalle : detallesOriginales) {
        System.out.println("   Procesando detalle...");
        System.out.println("     Producto ID: " + (detalle.getProducto() != null ? detalle.getProducto().getId() : "NULL"));
        System.out.println("     Medicamento ID: " + (detalle.getMedicamento() != null ? detalle.getMedicamento().getId() : "NULL"));
        System.out.println("     Cantidad: " + detalle.getCantidad());
        System.out.println("     Precio: " + detalle.getPrecioUnitario());
        
        if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
            Long productoId = detalle.getProducto().getId();
            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException(" Producto no encontrado con ID: " + productoId));
            
            System.out.println("   Producto encontrado: " + producto.getNombre() + " (ID: " + producto.getId() + ")");
            System.out.println("     Stock actual: " + producto.getStock() + ", Cantidad solicitada: " + detalle.getCantidad());
            
            if (producto.getStock() < detalle.getCantidad() && producto.getStock() < 999) {
                throw new RuntimeException(" Stock insuficiente para: " + producto.getNombre() + 
                    ". Disponible: " + producto.getStock());
            }
            
            DetalleVenta nuevoDetalle = new DetalleVenta();
            nuevoDetalle.setProducto(producto);
            nuevoDetalle.setCantidad(detalle.getCantidad());
            nuevoDetalle.setPrecioUnitario(detalle.getPrecioUnitario() != null ? 
                detalle.getPrecioUnitario() : producto.getPrecio());
            nuevoDetalle.calcularSubtotal();
            
            nuevoDetalle.setVenta(venta);
            
            venta.addDetalle(nuevoDetalle);
            
            if (producto.getStock() < 999) {
                producto.setStock(producto.getStock() - detalle.getCantidad());
                productoRepository.save(producto);
                System.out.println("     Nuevo stock: " + producto.getStock());
            }
            
            System.out.println("  Detalle agregado correctamente");
        } 
        else if (detalle.getMedicamento() != null && detalle.getMedicamento().getId() != null) {
            Long medicamentoId = detalle.getMedicamento().getId();
            Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
                    .orElseThrow(() -> new RuntimeException(" Medicamento no encontrado con ID: " + medicamentoId));
            
            System.out.println("   Medicamento encontrado: " + medicamento.getNombre() + " (ID: " + medicamento.getId() + ")");
            
            DetalleVenta nuevoDetalle = new DetalleVenta();
            nuevoDetalle.setMedicamento(medicamento);
            nuevoDetalle.setCantidad(detalle.getCantidad());
            nuevoDetalle.setPrecioUnitario(detalle.getPrecioUnitario() != null ? 
                detalle.getPrecioUnitario() : medicamento.getPrecio());
            nuevoDetalle.calcularSubtotal();
            
            nuevoDetalle.setVenta(venta);
            
            venta.addDetalle(nuevoDetalle);
            
            System.out.println("   Detalle agregado correctamente");
        } 
        else {
            System.out.println("   ERROR: Producto y Medicamento son NULL o no tienen ID!");
            throw new RuntimeException(" Producto o Medicamento no especificado correctamente");
        }
    }
    
    venta.setFecha(LocalDateTime.now());
    venta.recalcularTotales();

    System.out.println(" Total calculado: S/ " + venta.getTotal());
    System.out.println(" Detalles en venta ANTES de guardar: " + venta.getDetalles().size());
    for (DetalleVenta d : venta.getDetalles()) {
        System.out.println("  - Producto: " + (d.getProducto() != null ? d.getProducto().getNombre() : "NULL"));
        System.out.println("    Cantidad: " + d.getCantidad());
        System.out.println("    Precio: " + d.getPrecioUnitario());
        System.out.println("    Subtotal: " + d.getSubtotal());
    }
    
    System.out.println(" Guardando venta en base de datos...");
    Venta ventaGuardada = ventaRepository.save(venta);
    System.out.println(" Venta guardada con ID: " + ventaGuardada.getId());
    
    System.out.println(" Verificando detalles guardados...");
    Venta verificada = ventaRepository.findByIdWithDetalles(ventaGuardada.getId()).orElse(null);
    
    if (verificada != null && verificada.getDetalles() != null) {
        System.out.println("Detalles DESPUÉS de guardar: " + verificada.getDetalles().size());
        for (DetalleVenta d : verificada.getDetalles()) {
            System.out.println("  - Producto: " + (d.getProducto() != null ? d.getProducto().getNombre() : "NULL"));
            System.out.println("    Cantidad: " + d.getCantidad());
            System.out.println("    Precio: " + d.getPrecioUnitario());
            System.out.println("    Subtotal: " + d.getSubtotal());
        }
    } else {
        System.out.println(" ERROR: No se encontraron detalles guardados!");
    }
    
    return ventaGuardada;
}
    @Transactional
    public Venta crearVentaDesdeReceta(Long recetaId, String metodoPago, Usuario usuario) {
        RecetaMedica receta = recetaMedicaRepository.findById(recetaId)
                .orElseThrow(() -> new RuntimeException("Receta no encontrada"));

        if (receta.getEstado() != RecetaEstado.DISPENSADA) {
            throw new RuntimeException("La receta debe estar dispensada antes de generar la venta. Estado actual: " + receta.getEstado());
        }

        if (ventaRepository.findByRecetaMedicaId(recetaId).isPresent()) {
            throw new RuntimeException("Ya existe una venta registrada para esta receta");
        }
        

        Venta venta = new Venta();
        venta.setRecetaMedica(receta);
        venta.setMetodoPago(metodoPago);
        venta.setUsuario(usuario);

        if (receta.getPaciente() != null) {
            List<Cliente> clientes = clienteRepository.findByMascotasNombre(receta.getPaciente().getNombre());
            if (!clientes.isEmpty()) {
                venta.setCliente(clientes.get(0));
            }
        }

        List<DetalleVenta> detalles = new ArrayList<>();
        for (RecetaItem item : receta.getItems()) {
            Medicamento med = item.getMedicamento();
            if (med == null) continue;

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setMedicamento(med);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(med.getPrecio() != null ? med.getPrecio() : BigDecimal.ZERO);
            detalles.add(detalle);
        }

        venta.setDetalles(detalles);
        venta.recalcularTotales();

        return ventaRepository.save(venta);
    }

    public List<Venta> listarVentas() {
        return ventaRepository.findByOrderByFechaDesc();
    }

    public List<Venta> listarVentasHoy() {
        return ventaRepository.findVentasDesde(LocalDate.now().atStartOfDay());
    }
    public BigDecimal calcularVentasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
    return ventaRepository.sumVentasEntreFechas(inicio, fin);
}


    public BigDecimal calcularVentasHoy() {
        BigDecimal total = ventaRepository.sumVentasDesde(LocalDate.now().atStartOfDay());
        return total != null ? total : BigDecimal.ZERO;
    }

    public Venta buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
    }

 
    public Map<String, Object> generarBoletaDigital(Long id) {
        Venta venta = ventaRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada con ID: " + id));

        Map<String, Object> boleta = new LinkedHashMap<>();
        
        boleta.put("id_venta", venta.getId());
        boleta.put("fecha_emision", LocalDateTime.now());
        boleta.put("empresa", "Veterinaria Pet Clinic");
        boleta.put("estado", "PAGADO");
        boleta.put("metodo_pago", venta.getMetodoPago());
        boleta.put("total_pagado", venta.getTotal());
        
        List<Map<String, Object>> productos = new ArrayList<>();
        if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
            for (DetalleVenta detalle : venta.getDetalles()) {
                Map<String, Object> item = new LinkedHashMap<>();
                
                if (detalle.getProducto() != null) {
                    item.put("producto", detalle.getProducto().getNombre());
                } else if (detalle.getMedicamento() != null) {
                    item.put("producto", detalle.getMedicamento().getNombre());
                } else {
                    item.put("producto", "Producto no disponible");
                }
                
                item.put("cantidad", detalle.getCantidad());
                item.put("precio_unitario", detalle.getPrecioUnitario());
                item.put("subtotal", detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
                productos.add(item);
            }
        }
        boleta.put("productos", productos);
        boleta.put("mensaje", "Gracias por su compra en Pet Clinic 2026");

        return boleta;
    }

    
    public byte[] generarBoletaPDFReal(Long id) {
    Venta venta = ventaRepository.findByIdWithDetalles(id)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada con ID: " + id));
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();
            
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 20, Font.BOLD);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL);
            
            Color verdePet = new Color(5, 150, 105);
            Font boldFontVerde = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLD, verdePet);
            Font estadoFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, verdePet);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.NORMAL);
            
            try {
                String imagePath = "src/main/resources/static/Imagen/Iconos/logo.png";
                Image logo = Image.getInstance(imagePath);
                logo.setAlignment(Element.ALIGN_CENTER);
                logo.scaleToFit(80, 80);
                document.add(logo);
            } catch (Exception e) {
                System.out.println(" Logo no encontrado, continuando sin logo: " + e.getMessage());
            }
            
            Paragraph title = new Paragraph("🏥 Pet Clinic", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph subtitle = new Paragraph("Boleta de Venta N° " + venta.getId(), headerFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);
            
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Fecha: " + venta.getFechaFormateada(), normalFont));
            document.add(new Paragraph(" "));
            
            PdfPTable separator = new PdfPTable(1);
            separator.setWidthPercentage(100);
            PdfPCell sepCell = new PdfPCell();
            sepCell.setBorder(PdfPCell.BOTTOM);
            sepCell.setBorderColor(verdePet);
            sepCell.setBorderWidth(2);
            sepCell.setPadding(0);
            separator.addCell(sepCell);
            document.add(separator);
            document.add(new Paragraph(" "));
            
            String clienteNombre = venta.getCliente() != null ? venta.getCliente().getNombre() : "N/A";
            String clienteTelefono = venta.getCliente() != null ? venta.getCliente().getTelefono() : "N/A";
            
            document.add(new Paragraph("Cliente: " + clienteNombre, normalFont));
            document.add(new Paragraph("Teléfono: " + clienteTelefono, normalFont));
            document.add(new Paragraph("Método de Pago: " + venta.getMetodoPago(), normalFont));
            document.add(new Paragraph(" "));
            
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});
            
            String[] headers = {"Producto/Servicio", "Cant.", "Precio Unit.", "Subtotal"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(verdePet);
                cell.setPadding(8);
                cell.setBorderColor(verdePet);
                table.addCell(cell);
            }
            
            for (DetalleVenta detalle : venta.getDetalles()) {
                String nombreProducto = "Producto";
                if (detalle.getProducto() != null) {
                    nombreProducto = detalle.getProducto().getNombre();
                } else if (detalle.getMedicamento() != null) {
                    nombreProducto = detalle.getMedicamento().getNombre();
                }
                BigDecimal subtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));
                
                table.addCell(new Phrase(nombreProducto, normalFont));
                table.addCell(new Phrase(String.valueOf(detalle.getCantidad()), normalFont));
                table.addCell(new Phrase("S/ " + detalle.getPrecioUnitario(), normalFont));
                table.addCell(new Phrase("S/ " + subtotal, normalFont));
            }
            
            document.add(table);
            document.add(new Paragraph(" "));
            
            PdfPTable separator2 = new PdfPTable(1);
            separator2.setWidthPercentage(100);
            PdfPCell sepCell2 = new PdfPCell();
            sepCell2.setBorder(PdfPCell.BOTTOM);
            sepCell2.setBorderColor(verdePet);
            sepCell2.setBorderWidth(1);
            sepCell2.setPadding(0);
            separator2.addCell(sepCell2);
            document.add(separator2);
            document.add(new Paragraph(" "));
            
            Paragraph subtotalPara = new Paragraph("Subtotal: S/ " + venta.getSubtotal(), normalFont);
            subtotalPara.setAlignment(Element.ALIGN_RIGHT);
            document.add(subtotalPara);
            
            Paragraph igvPara = new Paragraph("IGV (18%): S/ " + venta.getIgv(), normalFont);
            igvPara.setAlignment(Element.ALIGN_RIGHT);
            document.add(igvPara);
            
            Paragraph totalPara = new Paragraph("TOTAL: S/ " + venta.getTotal(), boldFontVerde);
            totalPara.setAlignment(Element.ALIGN_RIGHT);
            document.add(totalPara);
            
            document.add(new Paragraph(" "));
            
            Paragraph estadoPara = new Paragraph(" PAGADO", estadoFont);
            estadoPara.setAlignment(Element.ALIGN_CENTER);
            document.add(estadoPara);
            
            document.add(new Paragraph(" "));
            
            Paragraph footer = new Paragraph("¡Gracias por su compra!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            Paragraph footer2 = new Paragraph("Pet Clinic - Cuidando a tu mejor amigo ", footerFont);
            footer2.setAlignment(Element.ALIGN_CENTER);
            document.add(footer2);
            
            document.close();
            baos.close();
            
            return baos.toByteArray();
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al generar el PDF: " + e.getMessage(), e);
        }
    }
}