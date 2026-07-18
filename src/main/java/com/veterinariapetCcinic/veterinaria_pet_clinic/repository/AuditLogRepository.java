package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop10ByOrderByFechaHoraDesc();

    List<AuditLog> findAllByOrderByFechaHoraDesc();
}
