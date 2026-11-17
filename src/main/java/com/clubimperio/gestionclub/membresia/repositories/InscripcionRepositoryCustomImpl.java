package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.dtos.InscripcionListadoResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Spring Data encuentra esta clase por el nombre (interfaz del fragmento + "Impl") y la suma a InscripcionRepository.
//
// Por qué la consulta se arma en código y no con un @Query fijo con "(:param IS NULL OR ...)":
// con PostgreSQL, un parámetro UUID que llega en null se manda sin tipo y la base responde
// "could not determine data type of parameter". Agregando solo las condiciones de los filtros que vinieron,
// la consulta nunca recibe parámetros null y además queda más simple para la base.
public class InscripcionRepositoryCustomImpl implements InscripcionRepositoryCustom {

    // translate() reemplaza cada letra de la primera lista por la de la misma posición en la segunda.
    // Con lower() después, "Núñez", "NUÑEZ" y "nunez" quedan iguales.
    private static final String SIN_TILDES = "lower(translate(%s, 'ÁÉÍÓÚÜÑáéíóúüñ', 'AEIOUUNaeiouun'))";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<InscripcionListadoResponse> listarConFiltros(UUID actividadId, UUID comisionId, Boolean activo, String patronBusqueda) {
        // Un solo SELECT con JOIN a persona, comisión y actividad: no se cargan entidades, se construye el DTO directo.
        // esSocioActivo se calcula con un EXISTS sobre membresías (la misma regla que Persona.isSocioActivo()),
        // en vez de cargar las membresías de cada persona, que sería una consulta más por fila (N+1).
        StringBuilder jpql = new StringBuilder("""
                SELECT new com.clubimperio.gestionclub.membresia.dtos.InscripcionListadoResponse(
                    i.inscripcionId, i.fechaInscripcion, i.activo, i.fechaBaja,
                    p.personaId, p.dni, p.nombre, p.apellido,
                    CASE WHEN EXISTS (
                        SELECT 1 FROM Membresia m
                        WHERE m.persona = p
                          AND m.fechaAlta <= :ahora
                          AND (m.fechaBaja IS NULL OR m.fechaBaja > :ahora)
                    ) THEN true ELSE false END,
                    c.comisionId, c.nombre, c.horario,
                    a.actividadId, a.nombre)
                FROM Inscripcion i
                JOIN i.persona p
                JOIN i.comision c
                JOIN c.actividad a
                WHERE 1 = 1
                """);
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ahora", LocalDateTime.now());

        if (actividadId != null) {
            jpql.append(" AND a.actividadId = :actividadId");
            parametros.put("actividadId", actividadId);
        }
        if (comisionId != null) {
            jpql.append(" AND c.comisionId = :comisionId");
            parametros.put("comisionId", comisionId);
        }
        if (activo != null) {
            jpql.append(" AND i.activo = :activo");
            parametros.put("activo", activo);
        }
        if (patronBusqueda != null) {
            // DNI, "nombre apellido" o "apellido nombre": así "juan perez" y "perez juan" encuentran a la misma persona.
            jpql.append(" AND (p.dni LIKE :patron ESCAPE '\\'")
                .append(" OR ").append(SIN_TILDES.formatted("concat(p.nombre, ' ', p.apellido)")).append(" LIKE :patron ESCAPE '\\'")
                .append(" OR ").append(SIN_TILDES.formatted("concat(p.apellido, ' ', p.nombre)")).append(" LIKE :patron ESCAPE '\\')");
            parametros.put("patron", patronBusqueda);
        }

        // Desempate por actividad, comisión y fecha: una persona con varias inscripciones sale siempre en el mismo orden.
        jpql.append(" ORDER BY p.apellido, p.nombre, a.nombre, c.nombre, i.fechaInscripcion DESC");

        TypedQuery<InscripcionListadoResponse> query =
                entityManager.createQuery(jpql.toString(), InscripcionListadoResponse.class);
        parametros.forEach(query::setParameter);
        return query.getResultList();
    }
}
