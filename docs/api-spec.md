# API Specification - Club Imperio Juniors

## Base URL
`http://localhost:8080/api`

> Nota: no hay versionado de rutas (`/api/v1`) implementado todavía; todos los recursos cuelgan directo de `/api`.

## Formato de errores
No hay excepciones tipadas: cualquier `RuntimeException` lanzada por la capa de Servicio es capturada por un `@RestControllerAdvice` genérico y siempre responde **400 Bad Request** (incluso para "no encontrado", que conceptualmente sería 404) con este cuerpo:
```json
{
  "timestamp": "2026-07-03T10:15:30",
  "message": "Descripción del error de negocio",
  "status": 400
}
```

Comportamiento por defecto a tener en cuenta:
* Si un filtro de listado no devuelve resultados, el status sigue siendo **200 OK** con lista vacía `[]` (no 404).
* `GET /api/personas` sin query params trae **todas** las personas (activas e inactivas) — no filtra por defecto.
* El módulo **Personas** ya usa DTOs de request/response (ver más abajo) — no expone la entidad JPA `Persona` ni `Membresia` directamente. El resto de los módulos (`Actividad`, `Comision`, `Inscripcion`, `ClasePrueba`, `Asistencia`) todavía reciben y devuelven las entidades JPA tal cual (ver `docs/DEBT.md`, punto 4, sobre el riesgo de Mass Assignment).

---

## Recurso: Personas
El estado de socio ya **no** es un flag booleano (`esSocio`) en `Persona`; ahora se deriva de la lista de `Membresia` de la persona (traza altas/bajas). La API refleja esto vía DTOs:

* **`PersonaResponse`** (lo que devuelven todos los endpoints de lectura/escritura): `personaId` (UUID), `dni`, `nombre`, `apellido`, `fechaNacimiento`, `telefonoPrincipal`, `email`, `activo`, `fechaCreacion`, `esSocioActivo` (boolean, calculado — reemplaza al viejo `esSocio`), `membresias` (array de `MembresiaResponse`).
* **`MembresiaResponse`**: `id` (UUID), `fechaAlta`, `fechaBaja` (nullable), `motivoBaja` (nullable).
* **`PersonaCreateRequest`** (body de `POST`): `dni`, `nombre`, `apellido`, `fechaNacimiento`, `telefonoPrincipal`, `email`, `activo` (default `true` si se omite/null), `seAsocia` (boolean — si es `true`, crea la primera `Membresia`), `fechaAltaManual` (opcional; si se omite y `seAsocia=true`, se usa la fecha/hora actual del servidor).
* **`PersonaUpdateRequest`** (body de `PUT`): `nombre`, `apellido`, `email`, `telefonoPrincipal`, `activo`. No incluye `dni` ni nada de membresía — el alta/baja de membresía todavía no tiene endpoint dedicado (ver "Endpoints pendientes").

### 1. Listar personas
`GET /api/personas`
* **Query params (opcionales):** `activo` (boolean), `esSocio` (boolean — filtra por `esSocioActivo`). Se pueden combinar.
* **200 OK:** `PersonaResponse[]`

### 2. Buscar por DNI
`GET /api/personas/buscar?dni={dni}`
* **200 OK:** objeto `PersonaResponse`.
* **400 Bad Request:** si no existe ninguna persona con ese DNI.

### 3. Obtener por ID
`GET /api/personas/{id}`
* **200 OK:** objeto `PersonaResponse`.
* **400 Bad Request:** si el UUID no existe.

### 4. Crear persona
`POST /api/personas`
* **Body:** JSON `PersonaCreateRequest` (mínimo `dni`, `nombre`, `apellido`; `activo` default `true` si se omite/null).
* **Validación:** `dni` debe ser único (`existsByDni`).
* **201 Created:** `PersonaResponse` creado con su UUID (con `membresias: [{...}]` si `seAsocia=true`).
* **400 Bad Request:** `"Ya existe una persona con el dni: {dni}"`.

### 5. Actualizar persona (parcial)
`PUT /api/personas/{id}`
* **Body:** JSON `PersonaUpdateRequest` con los campos a modificar (`nombre`, `apellido`, `email`, `telefonoPrincipal`, `activo`). Los campos `null` o strings en blanco se ignoran y no pisan el valor existente. **No permite editar `dni`, ni gestionar membresía** (eso quedó explícitamente fuera de este endpoint).
* **200 OK:** `PersonaResponse` actualizado.

### 6. Eliminar persona (baja lógica)
`DELETE /api/personas/{id}`
* Marca `activo = false`. No borra el registro.
* **204 No Content**.

### 7. Conteo / estadísticas
`GET /api/personas/stats/count?activo={boolean}&esSocio={boolean}`
* **Query params:** `activo` (**obligatorio**), `esSocio` (opcional).
* **200 OK:** `{ "total": <long> }`

---

## Recurso: Actividades
`Actividad`: `actividadId` (UUID), `nombre` (único), `descripcion`, `precioMensual`, `activo`.

### 1. Listar actividades
`GET /api/actividades`
* **200 OK:** `Actividad[]` (incluye activas e inactivas).

### 2. Crear actividad
`POST /api/actividades`
* **Body:** JSON `Actividad` (`nombre` obligatorio no vacío, `precioMensual` obligatorio ≥ 0).
* **Validación:** nombre único (case-sensitive vía `existsByNombre`, se guarda `trim()`).
* **201 Created:** objeto creado.
* **400 Bad Request:** nombre vacío, precio nulo/negativo, o nombre duplicado.

### 3. Obtener por ID
`GET /api/actividades/{id}`
* **200 OK** / **400 Bad Request** si no existe.

### 4. Buscar por nombre
`GET /api/actividades/buscar?nombre={nombre}`
* Búsqueda **exacta** (no parcial).
* **200 OK** / **400 Bad Request** si no existe.

### 5. Actualizar actividad (parcial)
`PUT /api/actividades/{id}`
* **Body:** JSON `Actividad` con `nombre`, `descripcion`, `precioMensual` y/o `activo`. Si se cambia el nombre, valida que no colisione con otra actividad existente (case-insensitive).
* **200 OK** / **400 Bad Request** (nombre en blanco, precio negativo, nombre duplicado).

### 6. Eliminar actividad (baja lógica en cascada)
`DELETE /api/actividades/{id}`
* Marca `activo = false` y **desactiva en cascada todas las comisiones** asociadas a esa actividad.
* **204 No Content**.
* **400 Bad Request:** si la actividad ya estaba desactivada.

---

## Recurso: Comisiones
`Comision` (turno de una actividad): `comisionId` (UUID), `actividad` (referencia completa a `Actividad`), `nombre`, `horario` (String libre, ver deuda técnica), `cupoMaximo`, `activa`.

### 1. Crear comisión
`POST /api/comisiones?actividadId={uuid}&nombre={string}&horario={string}&cupo={int?}`
* **Validación:** la actividad debe existir; no puede existir ya una comisión con la misma combinación `(actividad, nombre, horario)` (case-insensitive). `cupo` es opcional (sin tope si se omite).
* **201 Created:** objeto creado (`activa = true` por defecto).
* **400 Bad Request:** actividad inexistente o combinación duplicada.

### 2. Listar comisiones de una actividad
`GET /api/comisiones/actividad/{actividadId}`
* **200 OK:** `Comision[]` (incluye activas e inactivas).

### 3. Obtener por ID
`GET /api/comisiones/{id}`
* **200 OK** / **400 Bad Request** si no existe.

---

## Recurso: Inscripciones (el "contrato" Socio–Comisión)
`Inscripcion`: `inscripcionId` (UUID), `persona` (ref. completa), `comision` (ref. completa), `fechaInscripcion`, `activo`, `fechaCreacion`, `fechaBaja`.

### 1. Inscribir socio
`POST /api/inscripciones?dni={dni}&comisionId={uuid}`
* **Validaciones de negocio:**
  * La persona debe existir y estar `activo = true`.
  * La comisión debe existir y estar `activa = true`.
  * El socio no puede tener ya una inscripción **activa** en esa misma comisión.
  * Si la comisión tiene `cupoMaximo`, se rechaza si las inscripciones activas ya lo alcanzaron.
* **201 Created:** objeto `Inscripcion` con `fechaInscripcion = hoy`.
* **400 Bad Request:** persona/comisión inexistente o inactiva, inscripción duplicada, o cupo agotado.

### 2. Listar inscripciones activas de un socio
`GET /api/inscripciones/socio/{dni}`
* **200 OK:** `Inscripcion[]` (solo activas).

### 3. Listar inscripciones activas de una comisión
`GET /api/inscripciones/comision/{comisionId}`
* **200 OK:** `Inscripcion[]` (solo activas).

### 4. Dar de baja (lógica)
`DELETE /api/inscripciones/{id}`
* Marca `activo = false` y `fechaBaja = hoy`. Libera el cupo de la comisión.
* **204 No Content**.

---

## Recurso: Clases de Prueba
`ClasePrueba`: `clasePruebaId` (UUID), `persona` (ref.), `actividad` (ref.), `fechaSolicitada`, `asistio` (bool, default `false`), `costo`, `observaciones`, `fechaCreacion`.

### 1. Registrar clase de prueba
`POST /api/clases-prueba/registrar?dni={dni}&actividadId={uuid}&fecha={ISO_DATE_TIME}`
* `fecha` en formato ISO `yyyy-MM-ddTHH:mm:ss`.
* **Validaciones:** persona y actividad deben existir y estar activas; la persona no puede tener ya una clase de prueba **pendiente** (`asistio = false`) para la misma actividad.
* **201 Created:** objeto creado (`asistio = false`).
* **400 Bad Request:** persona/actividad inactiva o inexistente, o clase pendiente duplicada.

### 2. Listar todas
`GET /api/clases-prueba`
* **200 OK:** `ClasePrueba[]`.

### 3. Marcar asistencia
`PATCH /api/clases-prueba/{id}/asistencia?asistio={boolean}&observaciones={string?}`
* Sobreescribe `asistio` y `observaciones` (no concatena).
* **204 No Content** / **400 Bad Request** si la clase no existe.

### 4. Agregar observación
`PATCH /api/clases-prueba/{id}/observacion?nota={string}`
* **Concatena** la nota nueva al historial existente con el formato `"... | Nota: {nota}"` (no pisa lo anterior).
* **204 No Content** / **400 Bad Request** si la clase no existe.

---

## Recurso: Asistencias
`Asistencia`: `asistenciaId` (UUID), `inscripcion` (ref. completa), `fecha`, `presente` (default `true`), `observaciones`.

### 1. Registrar asistencia
`POST /api/asistencias?inscripcionId={uuid}&fecha={ISO_DATE?}&presente={boolean=true}&observaciones={string?}`
* `fecha` es opcional; si se omite, se usa la fecha actual del servidor.
* **Validaciones:** la inscripción debe existir y estar `activo = true`; no puede existir ya un registro de asistencia para esa inscripción en esa misma fecha (previene doble carga).
* **201 Created:** objeto creado.
* **400 Bad Request:** inscripción inexistente/inactiva, o asistencia duplicada para esa fecha.

### 2. Historial de asistencia de una inscripción
`GET /api/asistencias/historial/{inscripcionId}`
* **200 OK:** `Asistencia[]` ordenado por fecha descendente (más reciente primero).

---

## Endpoints pendientes / no implementados
Referenciados en `docs/BACKLOG.md` pero sin código todavía:
* Alta/baja de membresía como endpoint dedicado (Ticket #2). Hoy la única forma de crear una `Membresia` es al crear la `Persona`, vía `seAsocia`/`fechaAltaManual` en `PersonaCreateRequest`; no existe forma de dar de alta o de baja una membresía para una persona ya existente.
* Módulo de Tesorería/Pagos (`Arancel`, `Pago`, cuenta corriente).
* Autenticación/autorización (todos los endpoints son públicos, `PersonaController` además tiene `@CrossOrigin(origins = "*")`).