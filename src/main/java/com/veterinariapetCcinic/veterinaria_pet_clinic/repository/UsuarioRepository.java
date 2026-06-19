package com.veterinariapetCcinic.veterinaria_pet_clinic.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRolIgnoreCase(String rol);

    List<Usuario> findByRol(String rol);

    boolean existsByUsername(String username);
}
