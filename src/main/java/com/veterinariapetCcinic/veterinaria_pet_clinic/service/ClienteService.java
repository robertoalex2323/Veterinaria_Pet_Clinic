package com.veterinariapetCcinic.veterinaria_pet_clinic.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Cliente;
import com.veterinariapetCcinic.veterinaria_pet_clinic.Model.Mascota;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.ClienteRepository;
import com.veterinariapetCcinic.veterinaria_pet_clinic.repository.MascotaRepository;

@Service
public class ClienteService {
    
    private final ClienteRepository clienteRepository;
    private final MascotaRepository mascotaRepository;
    
    public ClienteService(ClienteRepository clienteRepository, MascotaRepository mascotaRepository) {
        this.clienteRepository = clienteRepository;
        this.mascotaRepository = mascotaRepository;
    }
    
    @Transactional
    public Cliente guardar(Cliente cliente) {
        if (clienteRepository.existsByTelefono(cliente.getTelefono())) {
            throw new RuntimeException("Ya existe un cliente con este número de teléfono");
        }
        cliente.setFechaRegistro(LocalDateTime.now());
        return clienteRepository.save(cliente);
    }
    
    @Transactional
    public Cliente registrarClienteConMascota(Cliente cliente, Mascota mascota) {
        Cliente clienteGuardado = guardar(cliente);
        mascota.setCliente(clienteGuardado);
        mascotaRepository.save(mascota);
        return clienteGuardado;
    }
    
    @Transactional
    public Cliente actualizar(Cliente cliente) {
        Cliente existente = buscarPorId(cliente.getId());
        existente.setNombre(cliente.getNombre());
        existente.setTelefono(cliente.getTelefono());
        existente.setEmail(cliente.getEmail());
        existente.setDireccion(cliente.getDireccion());
        return clienteRepository.save(existente);
    }
    
    public Cliente buscarPorId(Long id) {
        Optional<Cliente> optional = clienteRepository.findById(id);
        return optional.orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + id));
    }

    public Cliente buscarPorTelefono(String telefono) {
        Optional<Cliente> optional = clienteRepository.findByTelefono(telefono);
        return optional.orElseThrow(() -> new RuntimeException("Cliente no encontrado con teléfono: " + telefono));
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public List<Cliente> buscarClientes(String nombre) {
        if (nombre != null && !nombre.isEmpty()) {
            return clienteRepository.findByNombreContainingIgnoreCase(nombre);
        }
        return clienteRepository.findAll();
    }

    public Cliente obtenerClienteConMascotas(Long id) {
        Optional<Cliente> optional = clienteRepository.findByIdWithMascotas(id);
        return optional.orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }
    
    public long contarClientes() {
        return clienteRepository.count();
    }
    
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarPorId(id);
        clienteRepository.delete(cliente);
    }
}