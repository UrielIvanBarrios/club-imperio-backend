package com.clubimperio.gestionclub.services;

import com.clubimperio.gestionclub.entities.Persona;
import com.clubimperio.gestionclub.repositories.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonaService {

    private final PersonaRepository personaRepository;

    // Listar todas las personas
    @Transactional(readOnly = true)
    public List<Persona> listarTodos() {
        return personaRepository.findAll();
    }

    @Transactional
    public Persona guardar(Persona persona) {
        if(personaRepository.existsByDni(persona.getDni())) {
            throw new RuntimeException("Ya existe una persona con el dni: "+persona.getDni());
        }
        return personaRepository.save(persona);
    }

    @Transactional(readOnly = true)
    public Persona buscarPorDni(String dni) {
        return personaRepository.findByDni(dni)
                .orElseThrow(() -> new RuntimeException("Persona no encontrada con ese DNI"));
    }


}
