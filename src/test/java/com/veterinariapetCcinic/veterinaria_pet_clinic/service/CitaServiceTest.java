package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinariapetCcinic.veterinaria_pet_clinic.config.AppProperties;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Cita;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.CitaRepository;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private AgendaService agendaService;

    @Mock
    private AppProperties appProperties;

    private CitaService citaService;

    @BeforeEach
    void setUp() {
        citaService = new CitaService(citaRepository, notificacionService, agendaService, appProperties);
    }

    @Test
    void marcaCitaAgendadaEnSalaDeEspera() {
        Cita cita = new Cita();
        cita.setEstado("AGENDADA");
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        Cita resultado = citaService.marcarEnSalaDeEspera(1L);

        assertEquals("EN_ESPERA", resultado.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void noPermiteRegistrarEnSalaUnaCitaCancelada() {
        Cita cita = new Cita();
        cita.setEstado("CANCELADA");
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));

        assertThrows(IllegalStateException.class, () -> citaService.marcarEnSalaDeEspera(1L));
        verify(citaRepository, never()).save(cita);
    }
}
