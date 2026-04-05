package com.clubimperio.gestionclub.repositories;

import com.clubimperio.gestionclub.entities.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {
    long countByComision_ComisionIdAndActivoTrue(UUID comisionId);

    boolean existsByPersona_DniAndComision_ComisionIdAndActivoTrue(String dni, UUID comisionId);

    List<Inscripcion> findByPersona_DniAndActivoTrue(String dni);

    List<Inscripcion> findByComision_ComisionIdAndActivoTrue(UUID comisionId);

}
