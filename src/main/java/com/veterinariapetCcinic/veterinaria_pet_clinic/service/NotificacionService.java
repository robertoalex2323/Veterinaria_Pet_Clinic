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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.config.AppProperties;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.CitaRepository;

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
                ✅ Su cita ha sido agendada exitosamente.
                📅 Fecha: %s
                🐕 Mascota: %s
                🏥 Motivo: %s

                Gracias por confiar en nosotros.""",
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

    private void enviarEmail(String email, String asunto, String mensaje) {
        if (email != null && !email.trim().isEmpty() && mailSender != null) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(email);
                mailMessage.setSubject(asunto);
                mailMessage.setText(mensaje);
                mailSender.send(mailMessage);
                log.info("✅ Correo enviado a: {}", email);
            } catch (Exception e) {
                log.error("❌ Error al enviar correo a {}: {}", email, e.getMessage());
            }
        } else {
            if (email == null || email.trim().isEmpty()) {
                log.warn("⚠️ Cliente sin email registrado");
            }
            if (mailSender == null) {
                log.warn("⚠️ Servicio de correo no configurado");
            }
        }
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

    public void enviarConfirmacionPago(Cliente cliente, Double monto, String metodoPago) {
        String mensaje = String.format("""
                💰 PAGO REGISTRADO

                Cliente: %s
                Monto: S/ %.2f
                Método: %s

                Gracias por su pago.""",
                cliente.getNombre(),
                monto,
                metodoPago);
        log.info("📧 Confirmación de pago para: {}", cliente.getTelefono());
        log.info("📝 Mensaje:\n{}", mensaje);

        enviarEmail(cliente.getEmail(), "Confirmación de Pago", mensaje);
        addUINotification("success", "Pago registrado para " + cliente.getNombre() + ": S/ " + String.format("%.2f", monto));
    }
}