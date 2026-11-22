package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.ChatbotMensaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatbotMensajeRepository extends JpaRepository<ChatbotMensaje, Long> {

    List<ChatbotMensaje> findBySessionIdOrderByFechaAsc(String sessionId);

    List<ChatbotMensaje> findTop50BySessionIdOrderByFechaDesc(String sessionId);

    @Transactional
    @Modifying
    void deleteBySessionId(String sessionId);

    long countBySessionId(String sessionId);
}
