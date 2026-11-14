package com.clubimperio.gestionclub.asistencia.entities;

import com.clubimperio.gestionclub.membresia.entities.Inscripcion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
// Una sola asistencia por inscripción y fecha, garantizado por la base. El chequeo previo del service da un mensaje
// claro en el caso normal, pero dos requests simultáneas podrían pasar las dos ese chequeo: la restricción frena a la segunda.
// Con ddl-auto=update hay que crearla a mano en una base existente (ver docs/database-schema.md).
@Table(name = "asistencias", uniqueConstraints = {
        @UniqueConstraint(name = Asistencia.UK_INSCRIPCION_FECHA, columnNames = {"inscripcion_id", "fecha"})
})
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asistencia {
    // Nombre fijo para poder reconocer esta violación en el service y traducirla a un 400 claro
    public static final String UK_INSCRIPCION_FECHA = "uk_asistencia_inscripcion_fecha";

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
