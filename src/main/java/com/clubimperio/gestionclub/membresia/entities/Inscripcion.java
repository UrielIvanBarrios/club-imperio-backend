package com.clubimperio.gestionclub.membresia.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inscripciones")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inscripcion_id", updatable = false, nullable = false )
    private UUID inscripcionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "comision_id", nullable = false)
    private Comision comision;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.fechaInscripcion == null) {
            this.fechaInscripcion = LocalDate.now();
        }
    }

    public void setActivo(Boolean activo) {
        this.activo = (activo == null) ? true : activo;
    }
}
