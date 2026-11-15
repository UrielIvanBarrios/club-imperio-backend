package com.clubimperio.gestionclub.membresia.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "clases_prueba")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClasePrueba {
    // Largo de la columna observaciones; el service lo valida antes de guardar para dar un 400 claro
    public static final int LARGO_MAX_OBSERVACIONES = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "clase_prueba_id")
    private UUID clasePruebaId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitada;

    @Column(nullable = false)
    private boolean asistio = false;

    private Double costo;

    @Column(length = LARGO_MAX_OBSERVACIONES)
    private String observaciones;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
