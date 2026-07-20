package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.config.AppProperties;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Venta;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.CitaRepository;

import jakarta.mail.internet.MimeMessage;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private final JavaMailSender mailSender;
    private final CitaRepository citaRepository;
    private final AppProperties appProperties;
    private final SimpMessagingTemplate messagingTemplate;

    // Nota: En Spring 4.3+, si solo hay un constructor, @Autowired es opcional.
    // Mantenemos el calificador required=false solo para el mailSender.
    public NotificacionService(
            @Autowired(required = false) JavaMailSender mailSender,
            CitaRepository citaRepository,
            AppProperties appProperties,
            SimpMessagingTemplate messagingTemplate) {
        this.mailSender = mailSender;
        this.citaRepository = citaRepository;
        this.appProperties = appProperties;
        this.messagingTemplate = messagingTemplate;
    }

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final List<UINotification> uiNotifications = new CopyOnWriteArrayList<>();

    public record UINotification(String type, String message, String timestamp) {
        public UINotification(String type, String message) {
            this(type, message, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    private void addUINotification(String type, String message) {
        UINotification notification = new UINotification(type, message);
        uiNotifications.add(notification);
        if (uiNotifications.size() > 50) { // Keep only the last 50 notifications
            uiNotifications.remove(0); // Remove the oldest notification
        }
        // Enviar notificación en tiempo real vía WebSocket
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    public void enviarConfirmacionCita(Cita cita) {

        String mensaje = String.format("""
                Hola %s,

                Hemos agendado su cita exitosamente en Pet Clinic.

                Fecha: %s
                Mascota: %s
                Motivo: %s

                Si necesita modificar o cancelar su cita, contáctenos por este medio.

                Atentamente,
                Veterinaria Pet Clinic
                """,
                cita.getMascota().getCliente() != null && cita.getMascota().getCliente().getNombre() != null
                        ? cita.getMascota().getCliente().getNombre().trim()
                        : "Cliente",
                cita.getFechaHora().format(FORMATTER),
                cita.getMascota().getNombre(),
                cita.getMotivo());

        Cliente cliente = cita.getMascota().getCliente();
        log.info("📧 Enviando notificación a: {}", cliente.getTelefono());
        log.info("📝 Mensaje:\n{}", mensaje);

        // Enviar Correo Electrónico
        enviarEmail(cliente.getEmail(),
                "Confirmación de Cita Veterinaria - " + cita.getMascota().getNombre(),
                mensaje);
        addUINotification("success", "Cita agendada: " + cita.getMascota().getNombre() + " el " + cita.getFechaHora().format(FORMATTER));

        log.info("--- Notificación enviada ---\n");
    }

    public void enviarCancelacionCita(Cita cita) {

        String mensaje = String.format("""
                ⚠️ Su cita para el %s ha sido CANCELADA.
                🐕 Mascota: %s

                Para reagendar, comuníquese al %s.""",
                cita.getFechaHora().format(FORMATTER),
                cita.getMascota().getNombre(),
                appProperties.getCancellationPhone());

        Cliente cliente = cita.getMascota().getCliente();
        log.info("📧 Notificación de cancelación para: {}", cliente.getTelefono());
        log.info("📝 Mensaje:\n{}", mensaje);

        enviarEmail(cliente.getEmail(),
                "Cancelación de Cita Veterinaria - " + cita.getMascota().getNombre(),
                mensaje);
        addUINotification("warning", "Cita cancelada: " + cita.getMascota().getNombre() + " el " + cita.getFechaHora().format(FORMATTER) + ". Contacto: " + appProperties.getCancellationPhone());
    }

    public void enviarRecordatorioCita(Cita cita) {
        String mensaje = String.format("""
                🔔 RECORDATORIO: Mañana %s a las %s tiene una cita para su mascota %s.
                Por favor llegar con 10 minutos de anticipación.""",
                cita.getFechaHora().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                cita.getFechaHora().toLocalTime(),
                cita.getMascota().getNombre());

        Cliente cliente = cita.getMascota().getCliente();
        log.info("📧 Recordatorio para: {}", cliente.getTelefono());
        log.info("📝 Mensaje:\n{}", mensaje);

        enviarEmail(cliente.getEmail(),
                "Recordatorio de Cita Veterinaria - " + cita.getMascota().getNombre(),
                mensaje);
        addUINotification("info", "Recordatorio enviado para: " + cita.getMascota().getNombre() + " mañana " + cita.getFechaHora().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    public void enviarReprogramacionCita(Cita citaOriginal, Cita citaNueva, String motivoReprogramacion) {
        Cliente cliente = (citaNueva != null && citaNueva.getMascota() != null) ? citaNueva.getMascota().getCliente() : null;
        if (cliente == null) return;

        String mensaje = String.format("""
                🔁 Su cita ha sido REPROGRAMADA.

                Antes:
                Fecha y hora: %s
                Mascota: %s

                Ahora:
                Fecha y hora: %s
                Mascota: %s

                Motivo: %s

                Si desea cancelar o volver a reprogramar, contáctenos por este medio.

                Atentamente,
                Veterinaria Pet Clinic
                """,
                citaOriginal != null && citaOriginal.getFechaHora() != null ? citaOriginal.getFechaHora().format(FORMATTER) : "---",
                citaOriginal != null && citaOriginal.getMascota() != null ? citaOriginal.getMascota().getNombre() : "---",
                citaNueva != null && citaNueva.getFechaHora() != null ? citaNueva.getFechaHora().format(FORMATTER) : "---",
                citaNueva != null && citaNueva.getMascota() != null ? citaNueva.getMascota().getNombre() : "---",
                motivoReprogramacion != null ? motivoReprogramacion : "---"
        );

        enviarEmail(cliente.getEmail(),
                "Reprogramación de Cita Veterinaria - " + (citaNueva.getMascota() != null ? citaNueva.getMascota().getNombre() : "Mascota"),
                mensaje);

        addUINotification("info", "Cita reprogramada: " + (citaNueva.getMascota() != null ? citaNueva.getMascota().getNombre() : "Mascota"));
    }

    private void enviarEmail(String email, String asunto, String mensaje) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("⚠️ Cliente sin email registrado");
            return;
        }
        if (mailSender == null) {
            log.warn("⚠️ Servicio de correo no configurado");
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject(asunto);

            // Convertir el mensaje (texto plano) a HTML y agregar logo embebido.
            String safeText = mensaje == null ? "" : mensaje
                    .replace("&", "&amp;")
                    .replace("<", "<")
                    .replace(">", ">")
                    .replace("\r\n", "\n")
                    .replace("\r", "\n")
                    .replace("\n", "<br/>");

            String html = """
                <div style='font-family: Arial, Helvetica, sans-serif; color:#1f2937;'>
                  <div style='text-align:center; margin-bottom:18px;'>
                    <img src='cid:logoPetClinic' style='max-width:160px;' alt='Pet Clinic'/>
                  </div>
                  <div style='white-space:normal; font-size:14px; line-height:1.5;'>
                    %s
                  </div>
                  <div style='margin-top:20px; font-size:12px; color:#6b7280;'>
                    Veterinaria Pet Clinic
                  </div>
                </div>
            """.formatted(safeText);

            helper.setText(html, true);

            // Adjuntar el logo embebido por CID
            try (java.io.InputStream is = getClass().getClassLoader()
                    .getResourceAsStream("static/Imagen/Iconos/logo.png")) {
                if (is != null) {
                    byte[] bytes = is.readAllBytes();
                    helper.addInline("logoPetClinic", new ByteArrayResource(bytes), "image/png");
                } else {
                    log.warn("No se encontró el logo para el correo (static/Imagen/Iconos/logo.png)");
                }
            }

            mailSender.send(mimeMessage);
            log.info("✅ Correo enviado a: {}", email);
        } catch (Exception e) {
            log.error("❌ Error al enviar correo a {}: {}", email, e.getMessage());
        }
    }


    public boolean enviarEmailConAdjunto(String email, String asunto, String mensaje, byte[] adjunto, String nombreArchivo) {
        if (email != null && !email.trim().isEmpty() && mailSender != null) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                helper.setTo(email);
                helper.setSubject(asunto);
                helper.setText(mensaje);
                helper.addAttachment(nombreArchivo, new ByteArrayResource(adjunto));

                mailSender.send(mimeMessage);
                log.info("✅ Correo con PDF enviado a: {}", email);
                return true;
            } catch (Exception e) {
                log.error("❌ Error al enviar correo con adjunto a {}: {}", email, e.getMessage());
                return false;
            }
        } else {
            log.warn("⚠️ No se pudo enviar el correo con adjunto. Email: {}, MailSender configurado: {}", 
                email, (mailSender != null));
            return false;
        }
    }

    /**
     * Envía el comprobante de una venta al correo del cliente.
     * @return true si el correo realmente se entregó al servidor SMTP, false si falló
     *         (sin email registrado, correo no configurado, o error al enviar).
     */
    public boolean enviarVentaConComprobante(Venta venta, byte[] pdf) {
        if (venta.getCliente() == null || venta.getCliente().getEmail() == null || venta.getCliente().getEmail().isBlank()) {
            log.warn("⚠️ No se puede enviar comprobante: El cliente no tiene un email registrado.");
            addUINotification("warning", "Venta registrada, pero el cliente no tiene email para el comprobante.");
            return false;
        }

        String nombreCliente = venta.getCliente().getNombre();
        String emailCliente = venta.getCliente().getEmail();
        String asunto = "Su comprobante de compra - Pet Clinic";
        String mensaje = String.format("""
                Estimado(a) %s,
                
                Adjunto encontrará el comprobante de pago por su compra de medicamentos realizada hoy.
                Total pagado: S/ %.2f
                
                ¡Gracias por confiar en Pet Clinic!
                """, nombreCliente, venta.getTotal());

        String nombreArchivo = "Comprobante_PetClinic_" + venta.getId() + ".pdf";

        boolean enviado = enviarEmailConAdjunto(emailCliente, asunto, mensaje, pdf, nombreArchivo);

        if (enviado) {
            addUINotification("success", "Comprobante enviado y entregado al correo de " + nombreCliente + " (" + emailCliente + ")");
        } else {
            addUINotification("error", "No se pudo enviar el comprobante al correo de " + nombreCliente + ". Verifica la configuración de correo e inténtalo nuevamente.");
        }
        return enviado;
    }

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void programarRecordatoriosManana() {
        LocalDate manana = LocalDate.now().plusDays(1);
        log.info("⏳ Enviando recordatorios para citas de mañana ({})...", manana);

        List<Cita> citasManana = citaRepository.findCitasPendientesParaRecordatorio(manana);

        if (citasManana.isEmpty()) {
            log.info("✅ No hay citas para recordar mañana.");
            return;
        }

        int exitosos = 0;
        for (Cita cita : citasManana) {
            try {
                enviarRecordatorioCita(cita);
                cita.setRecordatorioEnviado(true);
                citaRepository.save(cita);
                exitosos++;
            } catch (Exception e) {
                log.error("❌ Error en recordatorio cita {}: {}", cita.getId(), e.getMessage());
            }
        }
        log.info("✅ Recordatorios enviados: {}/{}", exitosos, citasManana.size());
    }
    
    public List<UINotification> getAndClearUINotifications() {
        List<UINotification> currentNotifications = new ArrayList<>(uiNotifications);
        uiNotifications.clear();
        return currentNotifications;
    }

    public void enviarNotificacionVeterinario(Cita cita) {
        String mensaje = String.format("""
                🏥 Nueva cita asignada:
                📅 Fecha: %s
                🐕 Mascota: %s
                👤 Dueño: %s
                📝 Motivo: %s""",
                cita.getFechaHora().format(FORMATTER),
                cita.getMascota().getNombre(),
                cita.getMascota().getCliente().getNombre(),
                cita.getMotivo());
        log.info("📧 Notificando al veterinario ID: {}",
                (cita.getVeterinario() != null ? cita.getVeterinario().getId() : "No asignado"));
        log.info("📝 Mensaje:\n{}", mensaje);
        if (cita.getVeterinario() != null) {
            enviarEmail(cita.getVeterinario().getEmail(),
                    "Nueva cita asignada - Pet Clinic", mensaje);
        }
        addUINotification("appointment", "Nueva cita asignada: " + cita.getMascota().getNombre()
                + " el " + cita.getFechaHora().format(FORMATTER));
    }

    public void enviarInformeCliente(Cliente cliente, String mensaje) {
        String informe = String.format("""
                📋 INFORMACIÓN IMPORTANTE

                Estimado(a) %s,

                %s

                Atentamente,
                Veterinaria Pet Clinic""",
                cliente.getNombre(),
                mensaje);
        log.info("📧 Enviando informe a: {} / {}", cliente.getTelefono(), cliente.getEmail());
        log.info("📝 Mensaje:\n{}", informe);

        enviarEmail(cliente.getEmail(), "Informe Veterinario", informe);
    }

    public void enviarNotificacionUI(String type, String message) {
        addUINotification(type, message);
    }

    public void enviarNotificacionNuevaReceta(String pacienteNombre, String veterinarioNombre, Long recetaId) {
        String mensaje = "📋 Nueva receta #" + recetaId + " creada para " + pacienteNombre + " por " + veterinarioNombre;
        addUINotification("info", mensaje);
        log.info("📋 Notificación de nueva receta: {}", mensaje);
    }

    public void enviarConfirmacionPago(Cliente cliente, Double monto, String metodoPago) {
        String nombreCliente = (cliente.getNombre() != null && !cliente.getNombre().trim().isEmpty())
                ? cliente.getNombre().trim()
                : "Cliente";

        String email = cliente.getEmail();
        String metodo = (metodoPago != null && !metodoPago.trim().isEmpty()) ? metodoPago.trim() : "No especificado";
        double montoSafe = (monto != null) ? monto : 0.0;

        String mensaje = String.format("""
                Estimado(a) %s,

                Le informamos que hemos registrado su pago en Pet Clinic.

                Detalle del pago
                -----------------
                Monto: S/ %.2f
                Método de pago: %s

                Si tiene alguna consulta, por favor contáctenos.

                Atentamente,
                Veterinaria Pet Clinic
                """,
                nombreCliente,
                montoSafe,
                metodo);
        log.info("📧 Confirmación de pago para: {}", cliente.getTelefono());
        log.info("📝 Mensaje:\n{}", mensaje);

        enviarEmail(cliente.getEmail(), "Confirmación de Pago", mensaje);
        addUINotification("success", "Pago registrado para " + cliente.getNombre() + ": S/ " + String.format("%.2f", monto));
    }
}