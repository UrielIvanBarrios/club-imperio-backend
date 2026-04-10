package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.entities.Comision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComisionRepository extends JpaRepository<Comision, UUID> {

    boolean existsByActividad_ActividadIdAndNombreIgnoreCaseAndHorarioIgnoreCase(
            UUID actividadId,
            String nombre,
            String horario
    );

    List<Comision> findByActividad_ActividadId(UUID actividadId);

    List<Comision> findByActivaTrue();

    @Modifying
    @Query("UPDATE Comision c SET c.activa = false WHERE c.actividad.actividadId = :actividadId")
    void desactivarComisionesPorActividad(UUID actividadId);
}
