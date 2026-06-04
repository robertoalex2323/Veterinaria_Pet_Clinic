package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Agenda;
import com.veterinariapetCcinic.veterinaria_pet_clinic.config.AppProperties;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.AgendaRepository;

@Service
public class AgendaService {

    private final AgendaRepository agendaRepository;
    private final AppProperties appProperties;

    public AgendaService(AgendaRepository agendaRepository, AppProperties appProperties) {
        this.agendaRepository = agendaRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public Agenda guardar(Agenda agenda) {
        Objects.requireNonNull(agenda, "La agenda no puede ser nula");
        return Objects.requireNonNull(agendaRepository.save(agenda));
    }

    public List<Agenda> obtenerHorariosDelDia(LocalDate fecha) {
        return agendaRepository.findByFecha(fecha);
    }

    public List<Agenda> obtenerHorariosDisponibles(LocalDate fecha) {
        return agendaRepository.findHorariosDisponiblesByFecha(fecha);
    }

    public List<Agenda> obtenerAgendaSemanal(LocalDate inicio) {
        LocalDate fin = inicio.plusDays(6);
        return agendaRepository.findByFechaBetween(inicio, fin);
    }

    @Transactional
    public void bloquearHorario(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));
        agenda.setDisponible(false);
        agendaRepository.save(agenda);
    }

    /**
     * Bloquea un horario por fecha y hora para marcar una próxima cita programada
     * @param fechaHora LocalDateTime con fecha y hora a bloquear
     * @param motivo Razón por la cual se bloquea (ej: "Próxima cita del paciente XYZ")
     */
    @Transactional
    public void bloquearHorarioPorFechaHora(LocalDateTime fechaHora, String motivo) {
        if (fechaHora == null) {
            throw new IllegalArgumentException("Fecha y hora no pueden ser nulas");
        }

        LocalDate fecha = fechaHora.toLocalDate();
        LocalTime hora = fechaHora.toLocalTime();

        // Validación: No se puede programar en el pasado
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaHora.isBefore(ahora)) {
            throw new IllegalArgumentException("No se puede programar citas en el pasado");
        }

        // Validación: Hora debe estar dentro de horario laboral (8:00 - 18:00)
        LocalTime horaInicio = LocalTime.of(8, 0);
        LocalTime horaFin = LocalTime.of(18, 0);
        if (hora.isBefore(horaInicio) || hora.isAfter(horaFin)) {
            throw new IllegalArgumentException("El horario debe estar entre 08:00 y 18:00");
        }

        // Buscar y bloquear horario disponible
        Agenda agendaDisponible = buscarAgendaDisponible(fecha, hora);
        if (agendaDisponible != null) {
            agendaDisponible.setDisponible(false);
            agendaDisponible.setMotivo(motivo);
            agendaRepository.save(agendaDisponible);
        } else {
            // Si no existe agenda, crear una nueva marcada como no disponible
            Agenda agendaNueva = new Agenda();
            agendaNueva.setFecha(fecha);
            agendaNueva.setHoraInicio(hora);
            agendaNueva.setHoraFin(hora.plusMinutes(30));
            agendaNueva.setDuracionTurno(30);
            agendaNueva.setDisponible(false);
            agendaNueva.setMotivo(motivo);
            agendaRepository.save(agendaNueva);
        }
    }

    @Transactional
    public void liberarHorario(Long id) {
        Objects.requireNonNull(id, "El ID no puede ser nulo");
        Agenda agenda = agendaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado"));
        agenda.setDisponible(true);
        agendaRepository.save(agenda);
    }

    public Agenda buscarAgendaDisponible(LocalDate fecha, LocalTime hora) {
        List<Agenda> agendas = agendaRepository.findAgendaDisponiblePorFechaYHora(fecha, hora);
        if (!agendas.isEmpty()) {
            return agendas.get(0);
        }
        return null;
    }

    public Agenda buscarAgendaPorFechaYHora(LocalDate fecha, LocalTime hora) {
        List<Agenda> agendas = agendaRepository.findAgendaPorFechaYHora(fecha, hora);
        if (!agendas.isEmpty()) {
            return agendas.get(0);
        }
        return null;
    }

    @Transactional
    public void generarAgendaBaseSiVacia(
            com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario veterinarioDefecto) {
        if (agendaRepository.count() == 0) {
            LocalDate hoy = LocalDate.now();
            for (int i = 0; i < 30; i++) {
                LocalDate fecha = hoy.plusDays(i);
                // Excluir domingos
                if (fecha.getDayOfWeek() == java.time.DayOfWeek.SUNDAY)
                    continue;

                LocalTime cursor = LocalTime.parse(appProperties.getBusiness().getStartTime());
                LocalTime fin = LocalTime.parse(appProperties.getBusiness().getEndTime());
                int duracion = appProperties.getBusiness().getDefaultSlotDuration();

                while (cursor.plusMinutes(duracion).compareTo(fin) <= 0) {
                    Agenda agenda = new Agenda();
                    agenda.setFecha(fecha);
                    agenda.setHoraInicio(cursor);
                    agenda.setHoraFin(cursor.plusMinutes(duracion));
                    agenda.setDuracionTurno(duracion);
                    agenda.setDisponible(true);
                    agenda.setVeterinario(veterinarioDefecto);
                    agendaRepository.save(agenda);
                    cursor = cursor.plusMinutes(duracion);
                }
            }
        }
    }
}
