package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.entities.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, UUID> {
    Optional<Persona> findByDni(String dni);
    boolean existsByDni(String dni);
    List<Persona> findByActivoAndEsSocio(Boolean activo, Boolean esSocio);
    List<Persona> findByActivo(Boolean activo);
    List<Persona> findByEsSocio(Boolean esSocio);
}
