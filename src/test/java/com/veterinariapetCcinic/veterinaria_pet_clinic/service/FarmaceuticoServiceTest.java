package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Medicamento;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Paciente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaEstado;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.RecetaMedica;
import com.veterinariapetCcinic.veterinaria_pet_clinic.model.Veterinario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MedicamentoRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.PacienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.RecetaMedicaRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.VeterinarioRepository;

@ExtendWith(MockitoExtension.class)
class FarmaceuticoServiceTest {

    @Mock
    private RecetaMedicaRepository recetaRepository;

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private VeterinarioRepository veterinarioRepository;

    @InjectMocks
    private FarmaceuticoService farmaceuticoService;

    @Test
    void crearSolicitudMedicamentosGuardaRecetaPendienteConItems() {
        Mascota mascota = new Mascota();
        mascota.setId(10L);
        mascota.setNombre("Luna");
        mascota.setEspecie("Perro");
        mascota.setRaza("Labrador");

        Medicamento medicamento = new Medicamento();
        medicamento.setId(1L);
        medicamento.setNombre("Amoxicilina");
        medicamento.setStock(20);

        Veterinario veterinario = new Veterinario();
        veterinario.setId(2L);
        veterinario.setNombre("Dra. Pérez");

        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(pacienteRepository.findAll()).thenReturn(List.of());
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(veterinarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(medicamentoRepository.findById(1L)).thenReturn(Optional.of(medicamento));
        when(recetaRepository.save(any(RecetaMedica.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecetaMedica receta = farmaceuticoService.crearSolicitudMedicamentos(
                10L,
                2L,
                1L,
                2,
                "1 tableta cada 8 horas",
                "Cada 12 horas",
                "Solicitada desde dashboard"
        );

        assertThat(receta.getEstado()).isEqualTo(RecetaEstado.PENDIENTE);
        assertThat(receta.getPaciente()).isNotNull();
        assertThat(receta.getPaciente().getNombre()).isEqualTo("Luna");
        assertThat(receta.getItems()).hasSize(1);
        assertThat(receta.getItems().get(0).getMedicamento().getNombre()).isEqualTo("Amoxicilina");
    }
}
