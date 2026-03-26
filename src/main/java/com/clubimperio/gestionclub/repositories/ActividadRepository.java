package com.clubimperio.gestionclub.repositories;

import com.clubimperio.gestionclub.entities.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, UUID>{
    Optional<Actividad> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
    List<Actividad> findByNombreContainingIgnoreCase(String nombre);
}
