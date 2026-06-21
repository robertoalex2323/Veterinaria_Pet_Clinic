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

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaItem;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaMedicaRepository;

@Service
public class FarmaceuticoService {

    private final RecetaMedicaRepository recetaRepository;
    private final MedicamentoRepository medicamentoRepository;

    public FarmaceuticoService(
            RecetaMedicaRepository recetaRepository,
            MedicamentoRepository medicamentoRepository) {

        this.recetaRepository = recetaRepository;
        this.medicamentoRepository = medicamentoRepository;
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
