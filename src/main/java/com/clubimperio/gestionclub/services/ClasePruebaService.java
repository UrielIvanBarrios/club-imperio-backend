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
        if(!actividad.getActivo()){
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
        ClasePrueba clase = clasePruebaRepository.findById(claseId)
                .orElseThrow(() -> new RuntimeException("Clase de prueba no encontrada."));
        clase.setAsistio(asistio);
        clase.setObservaciones(observaciones);
        clasePruebaRepository.save(clase);
    }

}
