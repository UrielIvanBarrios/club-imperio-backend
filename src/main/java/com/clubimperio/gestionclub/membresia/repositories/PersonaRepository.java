package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.entities.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, UUID> {
    Optional<Persona> findByDni(String dni);
    boolean existsByDni(String dni);
    List<Persona> findByActivo(Boolean activo);
    long countByActivo(Boolean activo);

    // ¡EXTRA DE TECH LEAD!: Buscar inscripciones planificadas a FUTURO (Útil para administración)
    @Query("SELECT DISTINCT p FROM Persona p JOIN p.membresias m " +
            "WHERE p.activo = true " +
            "AND m.fechaAlta > :ahora")
    List<Persona> findInscripcionesAFuturo(@Param("ahora") LocalDateTime ahora);
}
