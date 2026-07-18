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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Promocion;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaItem;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ProductoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.PromocionRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaMedicaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VentaRepository;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final ClienteRepository clienteRepository;
    private final PromocionRepository promocionRepository;
    private final RecetaMedicaRepository recetaMedicaRepository;

    public VentaService(VentaRepository ventaRepository,
                        ProductoRepository productoRepository,
                        MedicamentoRepository medicamentoRepository,
                        ClienteRepository clienteRepository,
                        PromocionRepository promocionRepository,
                        RecetaMedicaRepository recetaMedicaRepository) {
        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.clienteRepository = clienteRepository;
        this.promocionRepository = promocionRepository;
        this.recetaMedicaRepository = recetaMedicaRepository;
    }

    // ============================================
    // 1. PROCESAR VENTA
    // ============================================
    @Transactional
    public Venta procesarVenta(Venta venta) {
        Cliente cliente = obtenerCliente(venta.getCliente());
        venta.setCliente(cliente);
        procesarDetalles(venta);
        venta.setFecha(LocalDateTime.now());
        venta.recalcularTotales();

        BigDecimal descuento = aplicarPromociones(venta);
        venta.setDescuentoAplicado(descuento);
        venta.setTotal(venta.getTotal().subtract(descuento));

        return ventaRepository.save(venta);
    }

    private Cliente obtenerCliente(Cliente cliente) {
        if (cliente == null || cliente.getNombre() == null) {
            throw new RuntimeException("El nombre del cliente es obligatorio");
        }

        String nombre = cliente.getNombre().trim();
        String telefono = cliente.getTelefono() != null ? cliente.getTelefono() : "999999999";

        Cliente existente = clienteRepository.findByNombre(nombre);
        if (existente == null) {
            existente = clienteRepository.findByTelefono(telefono).orElse(null);
        }

        if (existente == null) {
            Cliente nuevo = new Cliente();
            nuevo.setNombre(nombre);
            nuevo.setTelefono(telefono);
            nuevo.setEmail(cliente.getEmail() != null ? cliente.getEmail() : "cliente@petclinic.com");
            nuevo.setDireccion(cliente.getDireccion() != null ? cliente.getDireccion() : "No registrada");
            return clienteRepository.save(nuevo);
        }

        return existente;
    }

    private void procesarDetalles(Venta venta) {
        if (venta.getDetalles() == null || venta.getDetalles().isEmpty()) {
            throw new RuntimeException("La venta debe tener al menos un producto");
        }

        List<DetalleVenta> nuevosDetalles = new ArrayList<>();

        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle.getProducto() != null && detalle.getProducto().getId() != null) {
                Producto producto = productoRepository.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

                validarStock(producto, detalle.getCantidad());

                DetalleVenta nuevo = new DetalleVenta();
                nuevo.setProducto(producto);
                nuevo.setCantidad(detalle.getCantidad());
                nuevo.setPrecioUnitario(detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : producto.getPrecio());
                nuevo.calcularSubtotal();
                nuevo.setVenta(venta);

                descuentarStock(producto, detalle.getCantidad());

                nuevosDetalles.add(nuevo);
            } else if (detalle.getMedicamento() != null && detalle.getMedicamento().getId() != null) {
                Medicamento medicamento = medicamentoRepository.findById(detalle.getMedicamento().getId())
                        .orElseThrow(() -> new RuntimeException("Medicamento no encontrado"));

                DetalleVenta nuevo = new DetalleVenta();
                nuevo.setMedicamento(medicamento);
                nuevo.setCantidad(detalle.getCantidad());
                nuevo.setPrecioUnitario(detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : medicamento.getPrecio());
                nuevo.calcularSubtotal();
                nuevo.setVenta(venta);

                nuevosDetalles.add(nuevo);
            }
        }

        venta.getDetalles().clear();
        nuevosDetalles.forEach(venta::addDetalle);
    }

    private void validarStock(Producto producto, int cantidad) {
        if (producto.getStock() < cantidad && producto.getStock() < 999) {
            throw new RuntimeException("Stock insuficiente para: " + producto.getNombre() +
                    ". Disponible: " + producto.getStock());
        }
    }

    private void descuentarStock(Producto producto, int cantidad) {
        if (producto.getStock() < 999) {
            producto.setStock(producto.getStock() - cantidad);
            productoRepository.save(producto);
        }
    }

    public BigDecimal aplicarPromociones(Venta venta) {
    List<Promocion> promociones = promocionRepository.findPromocionesActivas(LocalDate.now());
    
    BigDecimal mejorDescuentoCategoria = BigDecimal.ZERO;
    BigDecimal mejorDescuentoMonto = BigDecimal.ZERO;
    BigDecimal descuentoProducto = BigDecimal.ZERO;

    for (Promocion promo : promociones) {
        if (!promocionAplicaAlProducto(venta, promo)) {
            continue;
        }
        
        if (!aplicaDiaSemana(promo) || !aplicaMontoMinimo(venta, promo)) {
            continue;
        }
        
        BigDecimal descuento = calcularDescuento(venta, promo);
        
       
        if (promo.getCategoriaAplicable() != null && !promo.getCategoriaAplicable().isEmpty()) {
            if (descuento.compareTo(mejorDescuentoCategoria) > 0) {
                mejorDescuentoCategoria = descuento;
            }
        } else if (promo.getProductoId() != null || promo.getTipo().equals("FIJO")) {
            descuentoProducto = descuentoProducto.add(descuento);
        } else {
            if (descuento.compareTo(mejorDescuentoMonto) > 0) {
                mejorDescuentoMonto = descuento;
            }
        }
    }
    
    BigDecimal descuentoTotal = mejorDescuentoCategoria
        .add(mejorDescuentoMonto)
        .add(descuentoProducto);
    
    if (descuentoTotal.compareTo(venta.getSubtotal()) > 0) {
        descuentoTotal = venta.getSubtotal();
    }
    
    System.out.println(" Descuento total aplicado: S/ " + descuentoTotal);
    return descuentoTotal;
}


private boolean promocionAplicaAlProducto(Venta venta, Promocion promo) {
    if (promo.getTipo().equals("PORCENTAJE") && 
        (promo.getCategoriaAplicable() == null || promo.getCategoriaAplicable().isEmpty()) &&
        promo.getProductoId() == null) {
        return true;
    }
    
    if (promo.getTipo().equals("2X1") && 
        (promo.getCategoriaAplicable() == null || promo.getCategoriaAplicable().isEmpty())) {
        return true;
    }
    
    for (DetalleVenta detalle : venta.getDetalles()) {
        Producto producto = detalle.getProducto();
        if (producto == null) continue;
        
        if (promo.getCategoriaAplicable() != null && !promo.getCategoriaAplicable().isEmpty()) {
            if (promo.getCategoriaAplicable().equals(producto.getCategoria())) {
                return true;
            }
        }
        
        if (promo.getProductoId() != null && promo.getProductoId().equals(producto.getId())) {
            return true;
        }
    }
    
    return false;
}

private BigDecimal calcularDescuento(Venta venta, Promocion promo) {
    return switch (promo.getTipo().toUpperCase()) {
        case "PORCENTAJE" -> aplicarDescuentoPorcentaje(venta, promo);
        case "2X1" -> aplicarDescuento2x1(venta, promo);
        case "FIJO" -> aplicarDescuentoFijo(venta, promo);
        default -> BigDecimal.ZERO;
    };
}

private boolean aplicaDiaSemana(Promocion promo) {
    if (promo.getDiasSemana() == null || promo.getDiasSemana().isEmpty()) {
        return true;
    }

    int diaActual = LocalDate.now().getDayOfWeek().getValue();
    for (String d : promo.getDiasSemana().split(",")) {
        if (Integer.parseInt(d.trim()) == diaActual) {
            return true;
        }
    }
    return false;
}

private boolean aplicaMontoMinimo(Venta venta, Promocion promo) {
    return promo.getMontoMinimo() == null ||
            venta.getTotal().compareTo(promo.getMontoMinimo()) >= 0;
}

private BigDecimal aplicarDescuentoPorcentaje(Venta venta, Promocion promo) {
    if (promo.getCategoriaAplicable() != null && !promo.getCategoriaAplicable().isEmpty()) {
        BigDecimal montoCategoria = calcularMontoPorCategoria(venta, promo.getCategoriaAplicable());
        return montoCategoria.multiply(promo.getDescuento().divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
    }
    return venta.getTotal().multiply(promo.getDescuento().divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP));
}

private BigDecimal aplicarDescuento2x1(Venta venta, Promocion promo) {
    BigDecimal descuento = BigDecimal.ZERO;

    for (DetalleVenta detalle : venta.getDetalles()) {
        if (detalle.getProducto() != null && detalle.getCantidad() >= 2) {
            String categoria = detalle.getProducto().getCategoria();
            if (promo.getCategoriaAplicable() == null || promo.getCategoriaAplicable().equals(categoria)) {
                int pares = detalle.getCantidad() / 2;
                descuento = descuento.add(detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(pares)));
            }
        }
    }
    return descuento;
}

private BigDecimal aplicarDescuentoFijo(Venta venta, Promocion promo) {
    if (promo.getProductoId() == null) {
        return promo.getDescuento();
    }

    for (DetalleVenta detalle : venta.getDetalles()) {
        if (detalle.getProducto() != null && detalle.getProducto().getId().equals(promo.getProductoId())) {
            return promo.getDescuento();
        }
    }
    return BigDecimal.ZERO;
}

private BigDecimal calcularMontoPorCategoria(Venta venta, String categoria) {
    return venta.getDetalles().stream()
            .filter(d -> d.getProducto() != null && categoria.equals(d.getProducto().getCategoria()))
            .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}


public BigDecimal calcularDescuentoPrevio(Venta venta) {
    return aplicarPromociones(venta);
}

public List<String> obtenerPromocionesAplicables(Venta venta) {
    List<Promocion> promociones = promocionRepository.findPromocionesActivas(LocalDate.now());
    List<String> nombres = new ArrayList<>();

    for (Promocion promo : promociones) {
        if (!promocionAplicaAlProducto(venta, promo)) {
            continue;
        }
        
        if (!aplicaDiaSemana(promo) || !aplicaMontoMinimo(venta, promo)) {
            continue;
        }
        
        BigDecimal desc = calcularDescuento(venta, promo);
        if (desc.compareTo(BigDecimal.ZERO) > 0) {
            nombres.add(promo.getNombre() + " (-S/ " + desc + ")");
        }
    }

    return nombres;
}

    @Transactional
    public void eliminarVenta(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle.getProducto() != null) {
                Producto producto = detalle.getProducto();
                producto.setStock(producto.getStock() + detalle.getCantidad());
                productoRepository.save(producto);
            }
        }
        ventaRepository.delete(venta);
    }

    // ============================================
    // 5. CREAR VENTA DESDE RECETA
    // ============================================
    @Transactional
    public Venta crearVentaDesdeReceta(Long recetaId, String metodoPago, Usuario usuario) {
        RecetaMedica receta = recetaMedicaRepository.findById(recetaId)
                .orElseThrow(() -> new RuntimeException("Receta no encontrada"));

        if (receta.getEstado() != RecetaEstado.DISPENSADA) {
            throw new RuntimeException("La receta debe estar dispensada");
        }

        if (ventaRepository.findByRecetaMedicaId(recetaId).isPresent()) {
            throw new RuntimeException("Ya existe una venta para esta receta");
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
            detalle.setPrecioUnitario(med.getPrecio());
            detalle.calcularSubtotal();
            detalles.add(detalle);
        }

        venta.setDetalles(detalles);
        venta.recalcularTotales();

        return ventaRepository.save(venta);
    }

    // ============================================
    // 6. LISTAR VENTAS
    // ============================================
    public List<Venta> listarVentas() {
        return ventaRepository.findByOrderByFechaDesc();
    }

    public List<Venta> listarTodasLasVentas() {
        return listarVentas();
    }

    public List<Venta> buscarVentasPorFecha(LocalDateTime inicio, LocalDateTime fin) {
        return ventaRepository.findByFechaBetween(inicio, fin);
    }

    public Page<Venta> buscarHistorial(String q, LocalDateTime inicio, LocalDateTime fin, Pageable pageable) {
    String query = (q == null) ? "" : q.trim();
    return ventaRepository.buscarHistorial(query, inicio, fin, pageable);
    }

    public List<Venta> listarVentasHoy() {
        return ventaRepository.findVentasDesde(LocalDate.now().atStartOfDay());
    }

    public BigDecimal calcularVentasHoy() {
        BigDecimal total = ventaRepository.sumVentasDesde(LocalDate.now().atStartOfDay());
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal calcularVentasEntreFechas(LocalDateTime inicio, LocalDateTime fin) {
        return ventaRepository.sumVentasEntreFechas(inicio, fin);
    }

    public Venta buscarPorId(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
    }


    // ============================================
    // 7. BOLETA DIGITAL
    // ============================================
    public Map<String, Object> generarBoletaDigital(Long id) {
        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));

        Map<String, Object> boleta = new LinkedHashMap<>();
        boleta.put("id_venta", venta.getId());
        boleta.put("fecha_emision", LocalDateTime.now());
        boleta.put("empresa", "Veterinaria Pet Clinic");
        boleta.put("estado", "PAGADO");
        boleta.put("metodo_pago", venta.getMetodoPago());
        boleta.put("total_pagado", venta.getTotal());
        boleta.put("descuento_aplicado", venta.getDescuentoAplicado());

        List<Map<String, Object>> productos = new ArrayList<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            Map<String, Object> item = new LinkedHashMap<>();
            if (detalle.getProducto() != null) {
                item.put("producto", detalle.getProducto().getNombre());
                item.put("categoria", detalle.getProducto().getCategoria());
            } else if (detalle.getMedicamento() != null) {
                item.put("producto", detalle.getMedicamento().getNombre());
                item.put("categoria", "Medicamento");
            }
            item.put("cantidad", detalle.getCantidad());
            item.put("precio_unitario", detalle.getPrecioUnitario());
            item.put("subtotal", detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad())));
            productos.add(item);
        }
        boleta.put("productos", productos);
        boleta.put("mensaje", "Gracias por su compra");

        return boleta;
    }

    // ============================================
    // 8. BOLETA PDF
    // ============================================
    public byte[] generarBoletaPDFReal(Long id) {
        Venta venta = ventaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

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
            Font descuentoFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, new Color(220, 38, 38));

            try {
                String imagePath = "src/main/resources/static/Imagen/Iconos/logo.png";
                Image logo = Image.getInstance(imagePath);
                logo.setAlignment(Element.ALIGN_CENTER);
                logo.scaleToFit(120, 120);
                document.add(logo);
                document.add(new Paragraph(" "));
            } catch (Exception e) {
                System.out.println("⚠️ Logo no encontrado: " + e.getMessage());
                Paragraph fallback = new Paragraph("🏥 Pet Clinic", 
                    FontFactory.getFont(FontFactory.HELVETICA, 24, Font.BOLD, verdePet));
                fallback.setAlignment(Element.ALIGN_CENTER);
                document.add(fallback);
                document.add(new Paragraph(" "));
            }

            Paragraph title = new Paragraph("Pet Clinic", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Boleta de Venta N° " + venta.getId(), headerFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Fecha: " + venta.getFechaFormateada(), normalFont));
            document.add(new Paragraph(" "));

            PdfPTable separator = crearSeparador(verdePet);
            document.add(separator);
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Cliente: " + (venta.getCliente() != null ? venta.getCliente().getNombre() : "N/A"), normalFont));
            document.add(new Paragraph("Teléfono: " + (venta.getCliente() != null ? venta.getCliente().getTelefono() : "N/A"), normalFont));
            document.add(new Paragraph("Método de Pago: " + venta.getMetodoPago(), normalFont));
            if (venta.getCodigoOperacion() != null && !venta.getCodigoOperacion().isBlank()) {
                document.add(new Paragraph("N° de Operación: " + venta.getCodigoOperacion(), normalFont));
            }

            if (venta.getDescuentoAplicado().compareTo(BigDecimal.ZERO) > 0) {
                document.add(new Paragraph("Descuento aplicado: S/ " + venta.getDescuentoAplicado(), descuentoFont));
            }
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f});

            String[] headers = {"Producto/Servicio", "Cant.", "Precio Unit.", "Subtotal"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(verdePet);
                cell.setPadding(8);
                table.addCell(cell);
            }

            for (DetalleVenta detalle : venta.getDetalles()) {
                String nombre = detalle.getProducto() != null ? detalle.getProducto().getNombre() :
                        detalle.getMedicamento() != null ? detalle.getMedicamento().getNombre() : "Producto";
                BigDecimal subtotal = detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(detalle.getCantidad()));

                table.addCell(new Phrase(nombre, normalFont));
                table.addCell(new Phrase(String.valueOf(detalle.getCantidad()), normalFont));
                table.addCell(new Phrase("S/ " + detalle.getPrecioUnitario(), normalFont));
                table.addCell(new Phrase("S/ " + subtotal, normalFont));
            }

            document.add(table);
            document.add(new Paragraph(" "));

            PdfPTable separator2 = crearSeparador(verdePet);
            document.add(separator2);
            document.add(new Paragraph(" "));

            addTotal(document, "Subtotal: S/ " + venta.getSubtotal(), normalFont);
            addTotal(document, "IGV (18%): S/ " + venta.getIgv(), normalFont);
            if (venta.getDescuentoAplicado().compareTo(BigDecimal.ZERO) > 0) {
                addTotal(document, "Descuento: -S/ " + venta.getDescuentoAplicado(), descuentoFont);
            }
            addTotal(document, "TOTAL: S/ " + venta.getTotal(), boldFontVerde);

            document.add(new Paragraph(" "));

            Paragraph estado = new Paragraph("PAGADO", estadoFont);
            estado.setAlignment(Element.ALIGN_CENTER);
            document.add(estado);

            document.add(new Paragraph(" "));

            Paragraph footer = new Paragraph("¡Gracias por su compra!", normalFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            Paragraph footer2 = new Paragraph("Pet Clinic - Cuidando a tu mejor amigo", footerFont);
            footer2.setAlignment(Element.ALIGN_CENTER);
            document.add(footer2);

            document.close();
            baos.close();

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF: " + e.getMessage(), e);
        }
    }

    private PdfPTable crearSeparador(Color color) {
        PdfPTable separator = new PdfPTable(1);
        separator.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderColor(color);
        cell.setBorderWidth(1);
        cell.setPadding(0);
        separator.addCell(cell);
        return separator;
    }

    private void addTotal(Document document, String text, Font font) throws Exception {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_RIGHT);
        document.add(p);
    }
}