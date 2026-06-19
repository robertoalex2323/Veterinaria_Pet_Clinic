package com.veterinariapetCcinic.veterinaria_pet_clinic.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Paciente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaItem;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Veterinario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.PacienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaMedicaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VeterinarioRepository;

@Configuration
public class DataLoader {

    @Bean

    CommandLineRunner initData(
            MedicamentoRepository medicamentoRepository,
            PacienteRepository pacienteRepository,
            VeterinarioRepository veterinarioRepository,
            RecetaMedicaRepository recetaRepository) {

        return args -> {
              if (medicamentoRepository.count() > 0) {
                System.out.println("Los datos ya existen");
                return;
                }
            Medicamento analgesico = new Medicamento();
            analgesico.setNombre("Aspirin");
            analgesico.setPresentacion("Tabletas 300mg");
            analgesico.setStock(12);
            analgesico.setStockMinimo(10);
            analgesico.setPrecio(new BigDecimal("20.00"));
            analgesico.setDescripcion("Analgesico y antiinflamatorio");
            analgesico.setContraindicaciones("No usar en gatos");
            analgesico.setInteracciones("Ibuprofeno");
            medicamentoRepository.save(analgesico);

            Medicamento antibiotico = new Medicamento();
            antibiotico.setNombre("Amoxicilina");
            antibiotico.setPresentacion("Capsulas 500 mg");
            antibiotico.setStock(0);
            antibiotico.setStockMinimo(5);
            antibiotico.setPrecio(new BigDecimal("24.00"));
            antibiotico.setDescripcion("Antibiótico de amplio espectro");
            antibiotico.setContraindicaciones("No usar con alérgia a penicilina");
            antibiotico.setInteracciones("Doxiciclina");
            antibiotico.setImagenUrl("/Imagen/Medicamento/amoxicilina.png");
            medicamentoRepository.save(antibiotico);

            Medicamento alternativa = new Medicamento();
            alternativa.setNombre("Cefalexina");
            alternativa.setPresentacion("Jarabe 100ml");
            alternativa.setStock(20);
            alternativa.setStockMinimo(5);
            alternativa.setPrecio(new BigDecimal("55.00"));
            alternativa.setDescripcion("Alternativa para infecciones leves");
            alternativa.setContraindicaciones("No usar con alergia a cefalosporinas");
            alternativa.setInteracciones("");
            alternativa.setImagenUrl("/Imagen/Medicamento/cefalexina.png");
            medicamentoRepository.save(alternativa);

            Paciente paciente = new Paciente();
            paciente.setNombre("Luna");
            paciente.setEspecie("Perro");
            paciente.setRaza("Labrador");
            pacienteRepository.save(paciente);

            Veterinario vet = new Veterinario();
            vet.setNombre("Dr. Gómez");
            vet.setEspecialidad("Medicina interna");
            veterinarioRepository.save(vet);

            RecetaMedica receta = new RecetaMedica();
            receta.setPaciente(paciente);
            receta.setVeterinario(vet);
            receta.setObservaciones("Revisar alergias antes de dispensar.");

            RecetaItem item = new RecetaItem();
            item.setReceta(receta);
            item.setMedicamento(antibiotico);
            item.setCantidad(2);
            item.setDosis("5 ml");
            item.setFrecuencia("Cada 12 horas");

            receta.getItems().add(item);
            recetaRepository.save(receta);
        };
    }
}
