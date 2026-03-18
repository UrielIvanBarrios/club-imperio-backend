package com.clubimperio.gestionclub.repositories;

import com.clubimperio.gestionclub.entities.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, UUID> {
    Optional<Persona> findByDni(String dni);
    boolean existsByDni(String dni);
}
