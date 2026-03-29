package com.clubimperio.gestionclub.services;

import com.clubimperio.gestionclub.entities.Persona;
import com.clubimperio.gestionclub.entities.Actividad;
import com.clubimperio.gestionclub.entities.ClasePrueba;
import com.clubimperio.gestionclub.repositories.ActividadRepository;
import com.clubimperio.gestionclub.repositories.ClasePruebaRepository;
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
        clase.setObservaciones(observaciones);
        clasePruebaRepository.save(clase);
    }

    @Transactional
    public void agregarObservacion(UUID claseId, String notaAdicional) {
        ClasePrueba clase = buscarPorId(claseId);

        // Concatenamos para no borrar lo que ya existía (historial)
        String notaNueva = (clase.getObservaciones() == null) ? notaAdicional
                : clase.getObservaciones() + " | Nota: " + notaAdicional;

        clase.setObservaciones(notaNueva);
        clasePruebaRepository.save(clase);
    }

    private ClasePrueba buscarPorId(UUID id) {
        return clasePruebaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clase de prueba no encontrada."));
    }

}
