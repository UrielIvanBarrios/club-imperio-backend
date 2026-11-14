# Esquema de Base de Datos - Club Imperio Juniors

Motor: **PostgreSQL**. El esquema no se gestiona con migraciones (no hay Flyway/Liquibase); se genera y actualiza automáticamente vía `spring.jpa.hibernate.ddl-auto=update` a partir de las entidades JPA. Todas las PK son `UUID` (v4, `GenerationType.UUID`) para IDs no predecibles.

> ⚠️ `ddl-auto=update` agrega tablas y columnas, pero **no** borra columnas ni corrige la nulabilidad de las existentes. La tabla `usuarios` venía de un diseño previo y hubo que ajustarla a mano (ver la sección `usuarios` y `DEBT.md` #10).

## Diagrama de relaciones

```
Actividad ──1─────*── Comision ──1─────*── Inscripcion ──*─────1── Persona
                                              │
                                              1
                                              │
                                              *
                                         Asistencia

Persona ──1─────*── ClasePrueba ──*─────1── Actividad

Persona ──1─────*── Membresia

Usuario   (independiente: quién opera el sistema, no es un socio)
   ▲
   └──── transacciones.usuario_creador_id / usuario_modificador_id / usuario_receptor_id   (tabla heredada, sin entidad JPA)
```

## Tabla: `personas`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `persona_id` | UUID | PK, no actualizable |
| `dni` | VARCHAR(20) | `UNIQUE`, `NOT NULL` |
| `nombre` | VARCHAR(100) | `NOT NULL` |
| `apellido` | VARCHAR(100) | `NOT NULL` |
| `fecha_nacimiento` | DATE | nullable |
| `telefono_principal` | VARCHAR(20) | nullable |
| `email` | VARCHAR (default) | nullable |
| `activo` | BOOLEAN | `NOT NULL`, default `true` (soft delete) |
| `fecha_creacion` | TIMESTAMP | seteada por `@PrePersist`, no actualizable |

El estado de socio ya no es una columna en `personas` — se reemplazó por la tabla `membresias` (ver abajo). "¿Es socio activo?" se deriva en código (`Persona.isSocioActivo()`), no se guarda como flag.

* **`dni`:** se guarda normalizado (solo dígitos, de 6 a 8) desde el alta manual y desde la importación. La regla vive en `PersonaService`; la columna en sí no tiene un CHECK.
* **Columna heredada:** en la base sigue existiendo `es_socio` (BOOLEAN, nullable), de antes del refactor a `Membresia`. No está mapeada en la entidad y no se usa. `ddl-auto=update` no borra columnas.

## Tabla: `membresias`
Historial de altas/bajas de socio de una persona (reemplaza al viejo flag `es_socio`).

| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `id` | UUID | PK |
| `persona_id` | UUID | FK → `personas.persona_id`, `NOT NULL` |
| `fecha_alta` | TIMESTAMP | `NOT NULL` |
| `fecha_baja` | TIMESTAMP | nullable — `NULL` significa membresía vigente |
| `motivo_baja` | VARCHAR (default) | nullable |

Una persona es "socio activo" si tiene alguna `Membresia` con `fecha_alta <= ahora` y (`fecha_baja IS NULL` o `fecha_baja > ahora`) — permite altas con fecha futura. Sin restricción `UNIQUE`/índice parcial a nivel de BD todavía para impedir dos membresías activas simultáneas en la misma persona (pendiente, ver `docs/BACKLOG.md` Ticket #2).

## Tabla: `actividades`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `actividad_id` | UUID | PK |
| `nombre` | VARCHAR(100) | `UNIQUE`, `NOT NULL` |
| `descripcion` | VARCHAR (default) | nullable |
| `precio_mensual` | DOUBLE | nullable a nivel de columna (validado `NOT NULL` y ≥ 0 en el Service) |
| `activo` | BOOLEAN | `NOT NULL`, default `true` (soft delete) |

## Tabla: `comisiones`
Turno/horario concreto de una actividad.

| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `comision_id` | UUID | PK, no actualizable |
| `actividad_id` | UUID | FK → `actividades.actividad_id`, `NOT NULL` |
| `nombre` | VARCHAR(100) | `NOT NULL` |
| `horario` | VARCHAR(250) | String libre (ej: "Lunes 18hs") — **deuda técnica**, ver `docs/DEBT.md` #1 |
| `cupo_maximo` | INTEGER | nullable (sin tope si es null) |
| `activa` | BOOLEAN | `NOT NULL`, default `true` |

**Restricción compuesta:** `UNIQUE (actividad_id, nombre, horario)` — evita duplicar el mismo turno para una actividad.

## Tabla: `inscripciones`
Vínculo Persona ↔ Comisión (el "contrato" del socio).

| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `inscripcion_id` | UUID | PK, no actualizable |
| `persona_id` | UUID | FK → `personas.persona_id`, `NOT NULL` |
| `comision_id` | UUID | FK → `comisiones.comision_id`, `NOT NULL` |
| `fecha_inscripcion` | DATE | `NOT NULL`, default hoy (`@PrePersist`) |
| `activo` | BOOLEAN | `NOT NULL`, default `true` (soft delete) |
| `fecha_creacion` | TIMESTAMP | seteada por `@PrePersist`, no actualizable |
| `fecha_baja` | DATE | nullable, seteada al dar de baja |

Sin restricción `UNIQUE` a nivel de BD para "una persona no puede tener dos inscripciones activas a la misma comisión". Esa regla y la del cupo se validan en `InscripcionService`, **con la fila de la comisión bloqueada** (`SELECT ... FOR UPDATE` sobre `comisiones`) mientras dura la transacción. Así, dos inscripciones simultáneas a la misma comisión se procesan de a una y ninguna de las dos reglas se puede saltar por concurrencia. Una restricción en la base para "activa" necesitaría un índice único parcial (`WHERE activo`), y eso queda para cuando haya migraciones (ver `DEBT.md` #10).

## Tabla: `clases_prueba`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `clase_prueba_id` | UUID | PK |
| `persona_id` | UUID | FK → `personas.persona_id`, `NOT NULL` |
| `actividad_id` | UUID | FK → `actividades.actividad_id`, `NOT NULL` |
| `fecha_solicitada` | TIMESTAMP | `NOT NULL` |
| `asistio` | BOOLEAN | `NOT NULL`, default `false` |
| `costo` | DOUBLE | nullable (no vinculado aún a `Arancel`, ver `docs/DEBT.md` #3) |
| `observaciones` | VARCHAR(500) | nullable, se usa como historial concatenado |
| `fecha_creacion` | TIMESTAMP | seteada por `@PrePersist`, no actualizable |

## Tabla: `asistencias`
| Columna | Tipo | Restricciones |
| :--- | :--- | :--- |
| `asistencia_id` | UUID | PK, no actualizable |
| `inscripcion_id` | UUID | FK → `inscripciones.inscripcion_id`, `NOT NULL` |
| `fecha` | DATE | `NOT NULL` |
| `presente` | BOOLEAN | default `true` |
| `observaciones` | VARCHAR(255) | nullable |

**Restricción `uk_asistencia_inscripcion_fecha` = `UNIQUE (inscripcion_id, fecha)`:** una sola asistencia por inscripción y fecha.
* **Está declarada en la entidad** (`@UniqueConstraint` en `Asistencia`). `AsistenciaService` además la chequea antes de guardar, para dar un mensaje claro en el caso normal.
* **Por qué también en la base:** dos requests simultáneas pueden pasar las dos el chequeo del service. La restricción frena a la segunda, y el service traduce esa violación al mismo 400 de "ya existe".
* **Con `ddl-auto=update` no conviene confiar en Hibernate** para crearla sobre una tabla que ya existe y tiene datos. Si encuentra duplicados, no la crea y no avisa. Por eso se aplicó a mano:
  ```sql
  -- 1) Verificar que no haya duplicados (tiene que devolver 0 filas):
  SELECT inscripcion_id, fecha, COUNT(*) FROM asistencias GROUP BY 1, 2 HAVING COUNT(*) > 1;
  -- 2) Crear la restricción:
  ALTER TABLE asistencias
      ADD CONSTRAINT uk_asistencia_inscripcion_fecha UNIQUE (inscripcion_id, fecha);
  ```
* **En una base nueva no hace falta:** Hibernate crea la tabla con la restricción.
* **Datos corregidos antes de aplicarla:** se borró una asistencia del 31/03/2026 que era anterior a su inscripción (05/04/2026), porque no podía ser real. No había duplicados.

## Tabla: `usuarios`
Usuarios que **operan** el sistema (login). Son independientes de `personas`. La tabla ya existía de un diseño previo y se reutilizó, porque `transacciones` la referencia.

| Columna | Tipo | Restricciones | Mapeada |
| :--- | :--- | :--- | :--- |
| `usuario_id` | UUID | PK, default `gen_random_uuid()` (igual, el id lo genera Hibernate) | ✅ |
| `email` | VARCHAR(150) | `UNIQUE`, `NOT NULL` — se guarda en minúsculas; es el login | ✅ |
| `nombre` | VARCHAR(100) | `NOT NULL` | ✅ |
| `password_hash` | VARCHAR(100) | `NOT NULL` — hash BCrypt (60 caracteres, incluye el salt) | ✅ |
| `rol` | VARCHAR(20) | `NOT NULL` — `ADMIN` o `USER` (`@Enumerated(STRING)`) | ✅ |
| `activo` | BOOLEAN | `NOT NULL`, default `true` (soft delete) | ✅ |
| `fecha_creacion` | TIMESTAMP | seteada por `@PrePersist`, no actualizable | ✅ |
| `fecha_ultima_modificacion` | TIMESTAMP | seteada por `@PrePersist` y `@PreUpdate` | ✅ |
| `username` | VARCHAR(50) | `UNIQUE`, nullable | ❌ heredada |
| `persona_id` | UUID | nullable, sin FK | ❌ heredada |
| `usuario_creador_id` | UUID | nullable, sin FK | ❌ heredada (futura auditoría, `DEBT.md` #7) |
| `usuario_modificador_id` | UUID | nullable, sin FK | ❌ heredada (futura auditoría, `DEBT.md` #7) |

**Ajustes manuales aplicados**, que no están en el repo porque no hay migraciones:
```sql
ALTER TABLE usuarios ALTER COLUMN username DROP NOT NULL;  -- la entidad no la usa; el login es por email
ALTER TABLE usuarios ALTER COLUMN rol      SET NOT NULL;   -- alinear la BD con la entidad
ALTER TABLE usuarios ALTER COLUMN activo   SET NOT NULL;
```
En una base **nueva**, Hibernate crea la tabla directamente con las columnas mapeadas y estos `ALTER` no hacen falta. Solo son necesarios sobre una base que ya traiga la tabla del diseño previo.

**Referenciada por:** `transacciones.usuario_creador_id`, `usuario_modificador_id` y `usuario_receptor_id` (UUID, FK `DEFERRABLE`). Los tipos son compatibles con `usuario_id`.

## Tablas heredadas sin entidad JPA
En la base existen tablas de un diseño previo del módulo de Pagos: `aranceles`, `transacciones`, `detalle_pagos` y `vinculos_familiares`. La aplicación no las usa todavía; se conservan para el futuro módulo de Tesorería (`BACKLOG.md` Ticket #7).

## Notas generales
* **Fetch por defecto `EAGER`** en todas las relaciones `@ManyToOne` (`Comision.actividad`, `Inscripcion.persona`, `Inscripcion.comision`, `ClasePrueba.persona`, `ClasePrueba.actividad`, `Asistencia.inscripcion`), **excepto `Membresia.persona`, que es `LAZY`**. Esto trae el objeto relacionado completo en cada consulta (salvo esa excepción) — puede impactar performance a medida que crezca el volumen de datos.
* **Soft delete generalizado**: `personas.activo`, `actividades.activo`, `comisiones.activa`, `inscripciones.activo`, `usuarios.activo` — ningún módulo borra filas físicamente. Las bajas van **en cascada**: Actividad → sus Comisiones → sus Inscripciones activas, y Comisión → sus Inscripciones activas, con `fecha_baja` = hoy. `membresias` no tiene columna `activo`: su vigencia se expresa con el rango `fecha_alta`/`fecha_baja` en vez de un flag booleano (igual que `inscripciones`, que combina ambos mecanismos).
* **`Persona → Membresia` es `cascade = {PERSIST, MERGE}` sin `orphanRemoval`**: guardar/actualizar una `Persona` persiste sus `Membresia` nuevas, pero nunca se borra una `Membresia` en cascada (ni por borrado de la `Persona`, ni por sacarla de la lista en memoria) — preserva el historial.
* **Sin auditoría de "quién"**: no se registra qué usuario hizo cada cambio, ni existen tablas de historial de estados (ver `docs/DEBT.md` #2, #5 y #7). Existe el "cuándo" en `usuarios.fecha_ultima_modificacion`. Las columnas `usuarios.usuario_creador_id` y `usuario_modificador_id` ya están en la base para cuando se implemente JPA Auditing.
* **Pendiente**: entidades JPA para el módulo de Pagos. Las tablas del diseño previo existen pero no están mapeadas (ver "Tablas heredadas sin entidad JPA").