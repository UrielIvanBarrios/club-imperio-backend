package com.clubimperio.gestionclub.membresia.repositories;

import com.clubimperio.gestionclub.membresia.dtos.InscripcionListadoResponse;

import java.util.List;
import java.util.UUID;

// Consultas de inscripciones que no se pueden escribir como un @Query fijo (ver InscripcionRepositoryCustomImpl).
public interface InscripcionRepositoryCustom {

    // Todos los filtros son opcionales: null significa "no filtrar por eso".
    // activo: true = activas, false = bajas, null = todas.
    // patronBusqueda: ya normalizado (minúsculas, sin tildes) y con los comodines de LIKE escapados.
    List<InscripcionListadoResponse> listarConFiltros(UUID actividadId, UUID comisionId, Boolean activo, String patronBusqueda);
}
