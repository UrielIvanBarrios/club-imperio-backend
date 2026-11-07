package com.clubimperio.gestionclub.membresia.dtos;

import java.util.List;

/**
 * Reporte de una importación de socios. "fila" es el número de fila tal como se ve en Excel
 * (la fila 1 es el encabezado), para que el usuario encuentre el error directo en su archivo.
 */
public record ImportacionResultado(
        String archivo,
        boolean simulacion,        // true = solo se validó, no se guardó nada
        int totalFilas,            // filas con datos (las vacías no cuentan)
        int filasValidas,
        int filasConError,
        int importadas,            // 0 si fue simulación
        List<FilaValida> validas,
        List<FilaConError> errores
) {
    public record FilaValida(int fila, String dni, String nombre, String apellido, boolean socio) {}

    // Una fila puede tener varios errores a la vez: se informan todos para corregirlos de una sola pasada.
    public record FilaConError(int fila, String dni, List<String> errores) {}
}
