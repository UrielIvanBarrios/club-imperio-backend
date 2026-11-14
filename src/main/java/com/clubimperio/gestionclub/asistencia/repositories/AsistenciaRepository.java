package com.clubimperio.gestionclub.asistencia.repositories;

import com.clubimperio.gestionclub.asistencia.entities.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {
    boolean existsByInscripcion_InscripcionIdAndFecha(UUID inscripcionId, LocalDate fecha);

    // Para el PUT: mismo chequeo de duplicado, excluyendo a la propia asistencia
    boolean existsByInscripcion_InscripcionIdAndFechaAndAsistenciaIdNot(UUID inscripcionId, LocalDate fecha, UUID asistenciaId);

    List<Asistencia> findByInscripcion_InscripcionIdOrderByFechaDesc(UUID inscripcionId);

    // Asistencia de una clase: todas las de una comisión en una fecha, ordenadas por apellido y nombre
    List<Asistencia> findByInscripcion_Comision_ComisionIdAndFechaOrderByInscripcion_Persona_ApellidoAscInscripcion_Persona_NombreAsc(
            UUID comisionId, LocalDate fecha);
}
