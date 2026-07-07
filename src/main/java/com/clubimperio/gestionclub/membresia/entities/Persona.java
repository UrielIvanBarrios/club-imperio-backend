package com.clubimperio.gestionclub.membresia.entities;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "personas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "persona_id", updatable = false, nullable = false)
    private UUID personaId;

    @Column(unique = true, nullable = false, length = 20)
    private String dni;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "telefono_principal", length = 20)
    private String telefonoPrincipal;

    private String email;

    @OneToMany(mappedBy = "persona", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default
    private List<Membresia> membresias = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }

    public boolean isSocioActivo() {
        if (this.membresias == null) return false;
        LocalDateTime ahora = LocalDateTime.now();

        return this.membresias.stream()
                .anyMatch(m -> !m.getFechaAlta().isAfter(ahora) // fechaAlta <= ahora
                        && (m.getFechaBaja() == null || m.getFechaBaja().isAfter(ahora))); // fechaBaja es null o futuro
    }
}
