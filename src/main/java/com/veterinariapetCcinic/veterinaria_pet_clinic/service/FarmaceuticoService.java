package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Paciente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaItem;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Veterinario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.PacienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaMedicaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VeterinarioRepository;

@Service
public class FarmaceuticoService {

    private final RecetaMedicaRepository recetaRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final MascotaRepository mascotaRepository;
    private final PacienteRepository pacienteRepository;
    private final VeterinarioRepository veterinarioRepository;

    public FarmaceuticoService(
            RecetaMedicaRepository recetaRepository,
            MedicamentoRepository medicamentoRepository,
            MascotaRepository mascotaRepository,
            PacienteRepository pacienteRepository,
            VeterinarioRepository veterinarioRepository) {

        this.recetaRepository = recetaRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.mascotaRepository = mascotaRepository;
        this.pacienteRepository = pacienteRepository;
        this.veterinarioRepository = veterinarioRepository;
    }

    @Transactional
    public RecetaMedica crearSolicitudMedicamentos(Long mascotaId,
                                                   Long veterinarioId,
                                                   Long medicamentoId,
                                                   Integer cantidad,
                                                   String dosis,
                                                   String frecuencia,
                                                   String observaciones) {
        return crearSolicitudMedicamentos(
                mascotaId,
                veterinarioId,
                Collections.singletonList(medicamentoId),
                Collections.singletonList(cantidad),
                Collections.singletonList(dosis),
                Collections.singletonList(frecuencia),
                null,
                null,
                null,
                null,
                observaciones);
    }

    @Transactional
    public RecetaMedica crearSolicitudMedicamentos(Long mascotaId,
                                                   Long veterinarioId,
                                                   List<Long> medicamentoIds,
                                                   List<Integer> cantidades,
                                                   List<String> dosis,
                                                   List<String> frecuencias,
                                                   List<String> vias,
                                                   List<String> unidades,
                                                   List<String> duraciones,
                                                   List<String> notasItems,
                                                   String observaciones) {
        Mascota mascota = mascotaRepository.findById(mascotaId)
                .orElseThrow(() -> new NoSuchElementException("Mascota no encontrada"));

        Paciente paciente = pacienteRepository.findAll().stream()
                .filter(p -> mascota.getNombre() != null && mascota.getNombre().equals(p.getNombre()))
                .findFirst()
                .orElseGet(() -> {
                    Paciente nuevoPaciente = new Paciente();
                    nuevoPaciente.setNombre(mascota.getNombre());
                    nuevoPaciente.setEspecie(mascota.getEspecie());
                    nuevoPaciente.setRaza(mascota.getRaza());
                    return pacienteRepository.save(nuevoPaciente);
                });

        Veterinario veterinario = null;
        if (veterinarioId != null) {
            veterinario = veterinarioRepository.findById(veterinarioId).orElse(null);
        }
        if (veterinario == null) {
            veterinario = veterinarioRepository.findAll().stream()
                    .filter(v -> v.getNombre() != null && v.getNombre().toLowerCase().contains("alberca"))
                    .findFirst()
                    .orElseGet(() -> {
                Veterinario nuevoVeterinario = new Veterinario();
                nuevoVeterinario.setNombre("Dra. Alberca");
                nuevoVeterinario.setEspecialidad("Medicina veterinaria");
                return veterinarioRepository.save(nuevoVeterinario);
            });
        }

        if (medicamentoIds == null || medicamentoIds.isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un medicamento");
        }

        RecetaMedica receta = new RecetaMedica();
        receta.setPaciente(paciente);
        receta.setVeterinario(veterinario);
        receta.setObservaciones(observaciones != null && !observaciones.isBlank()
                ? observaciones
                : "Solicitud creada desde el dashboard");
        receta.setEstado(RecetaEstado.PENDIENTE);

        for (int i = 0; i < medicamentoIds.size(); i++) {
            Long medicamentoId = medicamentoIds.get(i);
            if (medicamentoId == null) {
                continue;
            }

            Medicamento medicamento = medicamentoRepository.findById(medicamentoId)
                    .orElseThrow(() -> new NoSuchElementException("Medicamento no encontrado"));
            Integer cantidadSolicitada = valorEntero(cantidades, i, 1);
            Integer stockDisponible = medicamento.getStock() != null ? medicamento.getStock() : 0;
            if (cantidadSolicitada > stockDisponible) {
                throw new IllegalArgumentException("La cantidad solicitada de " + medicamento.getNombre()
                        + " supera el stock disponible (" + stockDisponible + ")");
            }

            RecetaItem item = new RecetaItem();
            item.setReceta(receta);
            item.setMedicamento(medicamento);
            item.setCantidad(cantidadSolicitada);
            item.setDosis(valorTexto(dosis, i));
            item.setFrecuencia(valorTexto(frecuencias, i));
            item.setVia(valorTexto(vias, i));
            item.setUnidad(valorTexto(unidades, i));
            item.setDuracion(valorTexto(duraciones, i));
            item.setNotas(valorTexto(notasItems, i));

            receta.getItems().add(item);
        }

        if (receta.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un medicamento valido");
        }

        return recetaRepository.save(receta);
    }

    private Integer valorEntero(List<Integer> valores, int index, Integer valorPorDefecto) {
        if (valores == null || index >= valores.size() || valores.get(index) == null || valores.get(index) <= 0) {
            return valorPorDefecto;
        }
        return valores.get(index);
    }

    private String valorTexto(List<String> valores, int index) {
        if (valores == null || index >= valores.size()) {
            return null;
        }
        String valor = valores.get(index);
        return valor != null && !valor.isBlank() ? valor.trim() : null;
    }

    public List<RecetaMedica> obtenerTodasLasRecetas() {
        return recetaRepository.findAll();
    }

    public List<RecetaMedica> obtenerRecetasPendientes() {
        return recetaRepository.findByEstado(RecetaEstado.PENDIENTE);
    }

    public long contarRecetasPendientes() {
        return recetaRepository.findByEstado(RecetaEstado.PENDIENTE).size();
    }


    public long contarStockBajo() {
        return medicamentoRepository.findByStockLessThan(5).size();
    }

    public List<Medicamento> listarMedicamentosBajoStock() {
        return medicamentoRepository.findByStockLessThan(5);
    }


    @Transactional(readOnly = true)
    public ValidacionReceta validarReceta(Long recetaId) {

        RecetaMedica receta = recetaRepository.findById(recetaId)
                .orElseThrow(() ->
                        new NoSuchElementException("Receta no encontrada"));

        List<String> errores = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        if (receta.getPaciente() == null) {
            errores.add("Faltan datos del paciente.");
        }

        if (receta.getVeterinario() == null) {
            errores.add("Faltan datos del veterinario.");
        }

        if (receta.getEstado() == RecetaEstado.DISPENSADA) {
            errores.add("La receta ya fue dispensada.");
        }

        if (receta.getItems() == null || receta.getItems().isEmpty()) {
            errores.add("La receta no contiene medicamentos.");
        }


        if (receta.getItems() != null) {

            for (RecetaItem item : receta.getItems()) {

                Medicamento med = item.getMedicamento();

                if (med == null) {
                    errores.add("Existe un medicamento sin información.");
                    continue;
                }

                if (item.getCantidad() == null || item.getCantidad() <= 0) {
                    errores.add(
                            "Cantidad inválida para "
                                    + med.getNombre()
                    );
                }

                if (item.getDosis() == null ||
                        item.getDosis().isBlank()) {

                    advertencias.add(
                            "Falta especificar dosis para "
                                    + med.getNombre()
                    );
                }

                else if (!item.getDosis().matches(".*\\d+.*")) {

                    advertencias.add(
                            "La dosis de "
                                    + med.getNombre()
                                    + " podría ser inválida."
                    );
                }

                // Stock insuficiente
                if (med.getStock() == null ||
                        med.getStock() < item.getCantidad()) {

                    errores.add(
                            "Stock insuficiente para "
                                    + med.getNombre()
                    );
                }
            }
        }

        advertencias.addAll(verificarInteracciones(receta));


        Map<Long, List<Medicamento>> alternativas =
                encontrarAlternativas(receta);

        boolean valida = errores.isEmpty();

        return new ValidacionReceta(
                receta,
                valida,
                errores,
                advertencias,
                alternativas
        );
    }

    @Transactional
    public ValidacionReceta validarYMarcarReceta(Long recetaId) {
        ValidacionReceta validacion = validarReceta(recetaId);
        if (validacion.valida()
                && validacion.receta().getEstado() == RecetaEstado.PENDIENTE) {
            RecetaMedica receta = validacion.receta();
            receta.setEstado(RecetaEstado.VALIDADA);
            recetaRepository.save(receta);
            return validarReceta(recetaId);
        }
        return validacion;
    }

    @Transactional
    public DispensaResult dispensarReceta(Long recetaId) {

        ValidacionReceta validacion = validarReceta(recetaId);

        if (!validacion.valida()) {

            return new DispensaResult(
                    false,
                    validacion.errores(),
                    validacion.advertencias()
            );
        }

        RecetaMedica receta = validacion.receta();

        for (RecetaItem item : receta.getItems()) {

            Medicamento med = item.getMedicamento();

            int nuevaCantidad =
                    med.getStock() - item.getCantidad();

            // Protección extra
            if (nuevaCantidad < 0) {

                return new DispensaResult(
                        false,
                        Collections.singletonList(
                                "Stock insuficiente para "
                                        + med.getNombre()
                        ),
                        Collections.emptyList()
                );
            }

            med.setStock(nuevaCantidad);

            medicamentoRepository.save(med);
        }

        receta.setEstado(RecetaEstado.DISPENSADA);

        recetaRepository.save(receta);

        return new DispensaResult(
                true,
                Collections.emptyList(),
                Collections.singletonList(
                        "Receta dispensada con éxito."
                )
        );
    }

    private List<String> verificarInteracciones(
            RecetaMedica receta) {

        List<String> advertencias = new ArrayList<>();

        List<String> interaccionesCriticas = List.of(
                "Aspirina|Ibuprofeno",
                "Amoxicilina|Doxiciclina"
        );

        List<String> nombres =
                receta.getItems().stream()
                        .map(RecetaItem::getMedicamento)
                        .filter(Objects::nonNull)
                        .map(Medicamento::getNombre)
                        .collect(Collectors.toList());

        for (String regla : interaccionesCriticas) {

            String[] pair = regla.split("\\|");

            if (nombres.contains(pair[0]) &&
                    nombres.contains(pair[1])) {

                advertencias.add(
                        "Interacción detectada entre "
                                + pair[0]
                                + " y "
                                + pair[1]
                );
            }
        }

        return advertencias;
    }

    private Map<Long, List<Medicamento>> encontrarAlternativas(
            RecetaMedica receta) {

        Map<Long, List<Medicamento>> alternativas =
                new HashMap<>();

        for (RecetaItem item : receta.getItems()) {

            Medicamento med = item.getMedicamento();

            if (med == null) {
                continue;
            }

            if (med.getStock() != null &&
                    med.getStock() < item.getCantidad()) {

                List<Medicamento> iguales =
                        medicamentoRepository
                                .findByPresentacion(
                                        med.getPresentacion()
                                )
                                .stream()
                                .filter(m ->
                                        !m.getId().equals(med.getId())
                                                && m.getStock() != null
                                                && m.getStock() >= item.getCantidad()
                                )
                                .collect(Collectors.toList());

                alternativas.put(med.getId(), iguales);
            }
        }

        return alternativas;
    }

    public record ValidacionReceta(

            RecetaMedica receta,

            boolean valida,

            List<String> errores,

            List<String> advertencias,

            Map<Long, List<Medicamento>> alternativasPorMedicamento
    ) {
    }

    public record DispensaResult(

            boolean dispensado,

            List<String> errores,

            List<String> advertencias
    ) {
    }
}
