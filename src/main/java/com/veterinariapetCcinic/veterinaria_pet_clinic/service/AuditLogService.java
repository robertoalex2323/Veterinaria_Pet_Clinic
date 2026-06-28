package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.AuditLog;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void registrar(String accion, String entidad, Long entidadId, String usuario, String detalle) {
        AuditLog log = new AuditLog();
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setUsuario(usuario != null ? usuario : "sistema");
        log.setDetalle(detalle);
        auditLogRepository.save(log);
    }

    public List<AuditLog> recientes() {
        return auditLogRepository.findTop10ByOrderByFechaHoraDesc();
    }

    public List<AuditLog> listarTodos() {
        return auditLogRepository.findAllByOrderByFechaHoraDesc();
    }
}
