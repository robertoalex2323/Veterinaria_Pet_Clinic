package com.veterinariapetCcinic.veterinaria_pet_clinic.config;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.UsuarioRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.service.AgendaService;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Autowired
    private AgendaService agendaService;

    @Override
    public void run(String... args) throws Exception {
        
        // 1. ADMIN
        if (!usuarioRepository.existsByUsername("admin")) {
            Usuario admin = new Usuario();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin_pet_clinic"));
            admin.setNombre("Administrador Principal");
            admin.setEmail("admin@veterinaria.com");
            admin.setRol("ADMIN");
            admin.setActivo(true);
            admin.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(admin);
            System.out.println("✅ Usuario ADMIN creado exitosamente");
        }

        // 2. VETERINARIO
        if (!usuarioRepository.existsByUsername("veterinario")) {
            Usuario vet = new Usuario();
            vet.setUsername("veterinario");
            vet.setPassword(passwordEncoder.encode("veterinaria_pet_clinic"));
            vet.setNombre("Dra. Katherine Alberca");
            vet.setEmail("katyalb@petclinic.com");
            vet.setRol("VETERINARIO");
            vet.setActivo(true);
            vet.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(vet);
            System.out.println("✅ Usuario VETERINARIO creado exitosamente");
        }

        // 3. RECEPCIONISTA
        if (!usuarioRepository.existsByUsername("recepcionista")) {
            Usuario recep = new Usuario();
            recep.setUsername("recepcionista");
            recep.setPassword(passwordEncoder.encode("recep_pet_clinic"));
            recep.setNombre("Roberto Anton");
            recep.setEmail("robertoanton@petclinic.com");
            recep.setRol("RECEPCIONISTA");
            recep.setActivo(true);
            recep.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(recep);
            System.out.println("✅ Usuario RECEPCIONISTA creado exitosamente");
        }

        // 4. VENDEDOR
        if (!usuarioRepository.existsByUsername("vendedor")) {
            Usuario vendedor = new Usuario();
            vendedor.setUsername("vendedor");
            vendedor.setPassword(passwordEncoder.encode("vendedor_pet_clinic"));
            vendedor.setNombre("Alessandro Llacshuanga");
            vendedor.setEmail("Alessandro_Llacshuanga@veterinaria.com");
            vendedor.setRol("VENDEDOR");
            vendedor.setActivo(true);
            vendedor.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(vendedor);
            System.out.println("✅ Usuario VENDEDOR creado exitosamente");
        }

        // 5. FARMACÉUTICO
        if (!usuarioRepository.existsByUsername("farmaceutico")) {
            Usuario farmaceutico = new Usuario();
            farmaceutico.setUsername("farmaceutico");
            farmaceutico.setPassword(passwordEncoder.encode("farmaceutico_pet_clinic"));
            farmaceutico.setNombre("Dr. Dario Arroyo");
            farmaceutico.setEmail("Dario.Arroyo@veterinaria.com");
            farmaceutico.setRol("FARMACEUTICO");
            farmaceutico.setActivo(true);
            farmaceutico.setFechaCreacion(LocalDateTime.now());
            usuarioRepository.save(farmaceutico);
            System.out.println("✅ Usuario FARMACEUTICO creado exitosamente");
        }
        // 6. Generar Agenda Base
        Usuario veterinarioObj = null;
        for (Usuario u : usuarioRepository.findAll()) {
            if ("VETERINARIO".equals(u.getRol())) {
                veterinarioObj = u;
                break;
            }
        }
        
        if (veterinarioObj != null) {
            agendaService.generarAgendaBaseSiVacia(veterinarioObj);
            System.out.println("✅ Agenda base generada o verificada exitosamente");
        }
        
        System.out.println("🎉 Todos los usuarios iniciales han sido creados");
    }
}