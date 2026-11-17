package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.entities.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID>, InscripcionRepositoryCustom {
    long countByComision_ComisionIdAndActivoTrue(UUID comisionId);

    boolean existsByPersona_DniAndComision_ComisionIdAndActivoTrue(String dni, UUID comisionId);

    List<Inscripcion> findByPersona_DniAndActivoTrue(String dni);

    List<Inscripcion> findByComision_ComisionIdAndActivoTrue(UUID comisionId);

    // Baja lógica en bloque de las inscripciones activas de una comisión. Devuelve cuántas se dieron de baja.
    @Modifying
    @Query("UPDATE Inscripcion i SET i.activo = false, i.fechaBaja = :fechaBaja " +
            "WHERE i.comision.comisionId = :comisionId AND i.activo = true")
    int darDeBajaActivasPorComision(@Param("comisionId") UUID comisionId, @Param("fechaBaja") LocalDate fechaBaja);

    // Igual que la anterior, pero para todas las comisiones de una actividad (baja de Actividad en cascada).
    // Subconsulta en vez de "i.comision.actividad...": un UPDATE masivo en JPQL no admite joins implícitos de forma portable.
    @Modifying
    @Query("UPDATE Inscripcion i SET i.activo = false, i.fechaBaja = :fechaBaja " +
            "WHERE i.activo = true AND i.comision.comisionId IN " +
            "(SELECT c.comisionId FROM Comision c WHERE c.actividad.actividadId = :actividadId)")
    int darDeBajaActivasPorActividad(@Param("actividadId") UUID actividadId, @Param("fechaBaja") LocalDate fechaBaja);

}
