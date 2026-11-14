package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.entities.Comision;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComisionRepository extends JpaRepository<Comision, UUID> {

    boolean existsByActividad_ActividadIdAndNombreIgnoreCaseAndHorarioIgnoreCase(
            UUID actividadId,
            String nombre,
            String horario
    );

    // Misma validación de duplicado pero excluyendo a la propia comisión (para el PUT)
    boolean existsByActividad_ActividadIdAndNombreIgnoreCaseAndHorarioIgnoreCaseAndComisionIdNot(
            UUID actividadId,
            String nombre,
            String horario,
            UUID comisionId
    );

    List<Comision> findByActividad_ActividadId(UUID actividadId);

    List<Comision> findByActividad_ActividadIdAndActivaTrue(UUID actividadId);

    List<Comision> findByActivaTrue();

    // SELECT ... FOR UPDATE: bloquea la fila de la comisión hasta que termine la transacción que la pidió.
    // Lo usa la inscripción para que dos altas simultáneas no lean el mismo conteo de inscriptos y superen el cupo:
    // la segunda espera a que la primera confirme y recién ahí cuenta. JPQL explícito para que la actividad
    // (EAGER) se cargue en otra consulta y el FOR UPDATE quede solo sobre la tabla comisiones.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comision c WHERE c.comisionId = :id")
    Optional<Comision> buscarPorIdConBloqueo(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Comision c SET c.activa = false WHERE c.actividad.actividadId = :actividadId")
    void desactivarComisionesPorActividad(UUID actividadId);
}
