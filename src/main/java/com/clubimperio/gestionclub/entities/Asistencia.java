package com.clubimperio.gestionclub.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "asistencias")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "asistencia_id", updatable = false, nullable = false)
    private UUID asistenciaId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "inscripcion_id", nullable = false)
    private Inscripcion inscripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @Builder.Default
    private boolean presente = true;

    @Column(length = 255)
    private String observaciones;
}
