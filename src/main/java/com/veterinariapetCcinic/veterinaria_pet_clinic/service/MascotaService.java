package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;

@Service
public class MascotaService {

    private final MascotaRepository mascotaRepository;
    private final ClienteRepository clienteRepository;

    public MascotaService(MascotaRepository mascotaRepository, ClienteRepository clienteRepository) {
        this.mascotaRepository = mascotaRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public Mascota guardar(Mascota mascota) {
        return mascotaRepository.save(mascota);
    }

    @Transactional
    public Mascota registrarMascota(Long clienteId, Mascota mascota) {
        Optional<?> optional = clienteRepository.findById(clienteId);
        if (optional.isPresent()) {
            Cliente cliente = (Cliente) optional.get();
            mascota.setCliente(cliente);
            return mascotaRepository.save(mascota);
        }
        throw new RuntimeException("Cliente no encontrado");
    }

    @Transactional
    public Mascota actualizar(Mascota mascota) {
        Mascota existente = buscarPorId(mascota.getId());
        existente.setNombre(mascota.getNombre());
        existente.setEspecie(mascota.getEspecie());
        existente.setRaza(mascota.getRaza());
        existente.setFechaNacimiento(mascota.getFechaNacimiento());
        existente.setEdad(mascota.getEdad());
        existente.setAlergias(mascota.getAlergias());
        existente.setColor(mascota.getColor());
        existente.setPeso(mascota.getPeso());
        existente.setFotoUrl(mascota.getFotoUrl());
        return mascotaRepository.save(existente);
    }

    public Mascota buscarPorId(Long id) {
        Optional<?> optional = mascotaRepository.findById(id);
        if (optional.isPresent()) {
            return (Mascota) optional.get();
        }
        throw new RuntimeException("Mascota no encontrada con ID: " + id);
    }

    public Mascota buscarPorIdConCliente(Long id) {
        Optional<?> optional = mascotaRepository.findByIdWithCliente(id);
        if (optional.isPresent()) {
            return (Mascota) optional.get();
        }
        throw new RuntimeException("Mascota no encontrada con ID: " + id);
    }

    public List<Mascota> listarTodos() {
        return mascotaRepository.findAll();
    }

    public List<Mascota> listarTodosConCliente() {
        return mascotaRepository.findAllWithCliente();
    }

    public List<Mascota> buscarPorCliente(Long clienteId) {
        return mascotaRepository.findByClienteId(clienteId);
    }

    public List<Mascota> buscarPorNombre(String nombre) {
        return mascotaRepository.findByNombreContainingIgnoreCase(nombre);
    }

    public long contarMascotas() {
        return mascotaRepository.count();
    }

    public long contarMascotasPorCliente(Long clienteId) {
        return mascotaRepository.countByClienteId(clienteId);
    }

    @Transactional
    public Mascota darDeBaja(Long id) {
        Mascota mascota = buscarPorId(id);
        mascota.setActivo(false);
        mascota.setFechaBaja(LocalDate.now());
        return mascotaRepository.save(mascota);
    }

    @Transactional
    public Mascota readmitir(Long id) {
        Mascota mascota = buscarPorId(id);
        mascota.setActivo(true);
        mascota.setFechaBaja(null);
        return mascotaRepository.save(mascota);
    }

    @Transactional
    public void eliminar(Long id) {
        mascotaRepository.deleteById(id);
    }
}
