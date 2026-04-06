package com.clubimperio.gestionclub.asistencia.repositories;

import com.clubimperio.gestionclub.asistencia.entities.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public interface AsistenciaRepository extends JpaRepository<Asistencia, UUID> {
    boolean existsByInscripcion_InscripcionIdAndFecha(UUID inscripcionId, LocalDate fecha);
    List<Asistencia> findByInscripcion_InscripcionIdOrderByFechaDesc(UUID inscripcionId);
}
