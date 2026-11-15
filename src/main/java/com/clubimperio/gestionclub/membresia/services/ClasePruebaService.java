package com.clubimperio.gestionclub.membresia.services;

import com.clubimperio.gestionclub.membresia.entities.Persona;
import com.clubimperio.gestionclub.membresia.entities.Actividad;
import com.clubimperio.gestionclub.membresia.entities.ClasePrueba;
import com.clubimperio.gestionclub.membresia.repositories.ClasePruebaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClasePruebaService {
    private final ClasePruebaRepository clasePruebaRepository;
    private final PersonaService personaService;
    private final ActividadService actividadService;

    @Transactional
    public ClasePrueba registrarClasePrueba(String dni, UUID actividadId, LocalDateTime fechaSolicitada) {
        Persona persona = personaService.buscarPorDni(dni);
        Actividad actividad = actividadService.buscarPorId(actividadId);
        if (persona.getActivo() == null || !persona.getActivo()) {
            throw new RuntimeException("No se puede registrar una clase de prueba: La persona está inactiva.");
        }
        if(actividad.getActivo() == null ||!actividad.getActivo()){
            throw new RuntimeException("No se puede registrar una clase de prueba en una actividad inactiva.");
        }
        boolean tienePendiente = clasePruebaRepository.findByPersona_Dni(dni).stream()
                .anyMatch(cp -> cp.getActividad().getActividadId().equals(actividadId) && !cp.isAsistio());

        if (tienePendiente) {
            throw new RuntimeException("La persona ya tiene una clase de prueba pendiente para esta actividad.");
        }

        ClasePrueba nuevaClase = new ClasePrueba();
        nuevaClase.setPersona(persona);
        nuevaClase.setActividad(actividad);
        nuevaClase.setFechaSolicitada(fechaSolicitada);
        nuevaClase.setAsistio(false);

        return clasePruebaRepository.save(nuevaClase);
    }

    @Transactional(readOnly = true)
    public List<ClasePrueba> listarTodas(){
        return clasePruebaRepository.findAll();
    }

    @Transactional
    public void marcarAsistencia(UUID claseId, boolean asistio, String observaciones){
        ClasePrueba clase = buscarPorId(claseId);
        clase.setAsistio(asistio);
        // Sin observaciones no se toca lo que ya había (antes se pisaba con null y se perdían las notas)
        if (observaciones != null) {
            clase.setObservaciones(validarObservaciones(observaciones));
        }
        clasePruebaRepository.save(clase);
    }

    @Transactional
    public void agregarObservacion(UUID claseId, String notaAdicional) {
        ClasePrueba clase = buscarPorId(claseId);
        String nota = CamposTexto.obligatorio(notaAdicional, "La nota no puede estar vacía.");

        // Concatenamos para no borrar lo que ya existía (historial)
        String notaNueva = (clase.getObservaciones() == null) ? nota
                : clase.getObservaciones() + " | Nota: " + nota;

        // Se valida el texto completo, que es lo que entra en la columna
        if (notaNueva.length() > ClasePrueba.LARGO_MAX_OBSERVACIONES) {
            throw new RuntimeException("No se puede agregar la nota: las observaciones quedarían con " + notaNueva.length()
                    + " caracteres y el máximo es " + ClasePrueba.LARGO_MAX_OBSERVACIONES + ".");
        }
        clase.setObservaciones(notaNueva);
        clasePruebaRepository.save(clase);
    }

    // Observaciones de clase de prueba: no se acepta el texto vacío ni uno más largo que la columna.
    // Se valida acá para responder un 400 claro en vez del error de PostgreSQL (value too long).
    private String validarObservaciones(String observaciones) {
        String limpio = CamposTexto.obligatorio(observaciones, "Las observaciones no pueden estar vacías.");
        if (limpio.length() > ClasePrueba.LARGO_MAX_OBSERVACIONES) {
            throw new RuntimeException("Las observaciones no pueden superar los " + ClasePrueba.LARGO_MAX_OBSERVACIONES
                    + " caracteres (tienen " + limpio.length() + ").");
        }
        return limpio;
    }

    private ClasePrueba buscarPorId(UUID id) {
        return clasePruebaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clase de prueba no encontrada."));
    }

}
