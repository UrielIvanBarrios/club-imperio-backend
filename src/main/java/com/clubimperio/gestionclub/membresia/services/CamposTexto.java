package com.clubimperio.gestionclub.membresia.services;

// Regla única para los campos de texto en los PUT (actualización parcial):
//   - ausente o null  -> no se modifica (el service ni llama a estos métodos)
//   - "" o solo espacios -> en un campo opcional se borra (queda null); en uno obligatorio es un 400
//   - cualquier otro valor -> se guarda sin espacios al principio ni al final
// Está centralizada acá para que Personas y Actividades apliquen exactamente lo mismo.
final class CamposTexto {

    private CamposTexto() {
    }

    // Campo opcional (email, teléfono, descripción): el vacío significa "borrar el dato"
    static String opcional(String valor) {
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    // Campo obligatorio (nombre, apellido): el vacío no se puede guardar, se rechaza con un mensaje claro
    static String obligatorio(String valor, String mensajeSiVacio) {
        String limpio = valor.trim();
        if (limpio.isEmpty()) {
            throw new RuntimeException(mensajeSiVacio);
        }
        return limpio;
    }
}
