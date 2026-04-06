package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.entities.ClasePrueba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClasePruebaRepository extends JpaRepository<ClasePrueba, UUID> {
    List<ClasePrueba> findByPersona_Dni(String dni);

    List<ClasePrueba> findByActividad_NombreContainingIgnoreCase(String nombre);

    List<ClasePrueba> findByFechaSolicitadaBetween(LocalDateTime fechaSolicitada, LocalDateTime fechaSolicitada2);

    List<ClasePrueba> findByAsistioFalse();
}
