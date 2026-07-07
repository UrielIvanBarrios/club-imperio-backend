package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.entities.Membresia;
import com.clubimperio.gestionclub.membresia.entities.Persona;
import com.clubimperio.gestionclub.membresia.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;

    // Listar todas las personas
    @Transactional(readOnly = true)
    public List<Persona> listarConFiltros(Boolean activo, Boolean esSocio) {
        List<Persona> base = (activo != null)
                ? personaRepository.findByActivo(activo)
                : personaRepository.findAll();

        if (esSocio == null) {
            return base;
        }
        return base.stream()
                .filter(p -> p.isSocioActivo() == esSocio)
                .toList();
    }

    @Transactional(readOnly = true)
    public Persona buscarPorId(UUID id) {
        return personaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró al socio con el identificador técnico proporcionado."));
    }

    @Transactional(readOnly = true)
    public Persona buscarPorDni(String dni) {
        return personaRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("No existe socio con DNI: " + dni));
    }

    @Transactional
    public Persona guardar(Persona persona, boolean seAsocia, LocalDateTime fechaAltaManual) {
        if(personaRepository.existsByDni(persona.getDni())) {
            throw new RuntimeException("Ya existe una persona con el dni: "+persona.getDni());
        }
        if (seAsocia) {
            LocalDateTime inicioMembresia = (fechaAltaManual != null)
                    ? fechaAltaManual
                    : LocalDateTime.now();

            Membresia primeraMembresia = Membresia.builder()
                    .persona(persona)
                    .fechaAlta(inicioMembresia)
                    .build();
            persona.getMembresias().add(primeraMembresia);
        }
        return personaRepository.save(persona);
    }

    @Transactional
    public Persona actualizar(UUID id, Persona datosNuevos) {
        Persona personaExistente = buscarPorId(id);

        if (datosNuevos.getNombre() != null && !datosNuevos.getNombre().isBlank()) {
            personaExistente.setNombre(datosNuevos.getNombre());
        }
        if (datosNuevos.getApellido() != null && !datosNuevos.getApellido().isBlank()) {
            personaExistente.setApellido(datosNuevos.getApellido());
        }
        if (datosNuevos.getEmail() != null && !datosNuevos.getEmail().isBlank()) {
            personaExistente.setEmail(datosNuevos.getEmail());
        }
        if (datosNuevos.getTelefonoPrincipal() != null && !datosNuevos.getTelefonoPrincipal().isBlank()) {
            personaExistente.setTelefonoPrincipal(datosNuevos.getTelefonoPrincipal());
        }
        if (datosNuevos.getActivo() != null) {
            personaExistente.setActivo(datosNuevos.getActivo());
        }

        return personaRepository.save(personaExistente);
    }

    @Transactional
    public void eliminar(UUID id) {
        Persona persona = buscarPorId(id);
        persona.setActivo(false);
        personaRepository.save(persona);
    }

    @Transactional(readOnly = true)
    public long contarPorEstado(Boolean activo) {
        return personaRepository.countByActivo(activo);
    }

    @Transactional(readOnly = true)
    public long contarSociosPorEstado(Boolean activo, Boolean esSocio) {
        return listarConFiltros(activo, esSocio).size();
    }
}
