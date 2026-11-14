# API Specification - Club Imperio Juniors

## Base URL
`http://localhost:8080/api`

> Nota: no hay versionado de rutas (`/api/v1`) implementado todavía; todos los recursos cuelgan directo de `/api`.

## Autenticación
Todos los endpoints requieren un JWT, **excepto** `POST /api/auth/login` (y Swagger en el perfil `dev`). El token se obtiene en el login y se envía en cada request:
```
Authorization: Bearer <token>
```
* El token dura **8 horas** (`app.jwt.expiracion-horas`). No hay refresh token: al vencer, el back responde 401 y hay que volver a loguearse.
* Roles: `ADMIN` (todo, incluido el ABM de usuarios) y `USER` (todo excepto `/api/usuarios/**`).
* El token está firmado con HS256. Lleva `sub` (id del usuario), `email` y `rol`, va firmado pero **no cifrado**.
* **En cada request autenticada, el back carga al usuario desde la base por el `sub` del token** y usa su estado y su rol **actuales**:
  * si el usuario está desactivado, responde 401 con `"Tu usuario fue desactivado. Contactá a un administrador."`;
  * si el usuario ya no existe, responde 401 con `"Sesión inválida o vencida. Iniciá sesión nuevamente."`;
  * los permisos salen del rol de la base, no del claim `rol`. Si un ADMIN le cambia el rol a alguien, el cambio rige desde su siguiente request, aunque su token diga otro rol. El claim `rol` queda solo como dato informativo para el front.
  
  El costo es una consulta a la base por clave primaria en cada request.

### Swagger UI (solo perfil `dev`)
1. Levantar con el perfil dev: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. En IntelliJ, poner `dev` en *Active profiles*.
2. Abrir `http://localhost:8080/swagger-ui/index.html`. El JSON de OpenAPI está en `/v3/api-docs`.
3. Ejecutar `POST /api/auth/login`, copiar el `token` de la respuesta, tocar **Authorize** y pegarlo, sin el prefijo "Bearer".

A partir de ahí, Swagger manda el token en todas las requests. Sin el perfil `dev`, Swagger está deshabilitado y responde 404.

### CORS
Solo se aceptan llamadas desde los orígenes de `app.cors.allowed-origins` (por defecto `http://localhost:5173`, el front en Vite). La configuración está centralizada en `SecurityConfig`: ningún controller usa `@CrossOrigin`. Un preflight desde otro origen recibe **403**.

## Formato de errores
Todos los errores, incluidos los de seguridad, salen con el mismo cuerpo JSON (ver abajo). El código HTTP depende del tipo de error:

| Status | Cuándo | `message` |
|---|---|---|
| **401 Unauthorized** | Falta el token, es inválido o está vencido | `"Sesión inválida o vencida. Iniciá sesión nuevamente."` |
| **401 Unauthorized** | Login con email o contraseña incorrectos, o usuario inactivo | `"Email o contraseña incorrectos."` (mismo mensaje en todos los casos, para no revelar qué emails existen) |
| **401 Unauthorized** | Cualquier request con el token de un usuario que fue desactivado | `"Tu usuario fue desactivado. Contactá a un administrador."` |
| **401 Unauthorized** | Token válido de un usuario que ya no existe en la base | `"Sesión inválida o vencida. Iniciá sesión nuevamente."` |
| **403 Forbidden** | Token válido, pero el rol **actual** del usuario (el de la base) no alcanza (USER en `/api/usuarios`) | `"No tenés permisos para realizar esta acción."` |
| **400 Bad Request** | Error de negocio lanzado por un service | mensaje específico |

Para el front: **401 → cerrar sesión y redirigir al login; 403 → mostrar "sin permisos"; 400 → mostrar `message` al usuario.**

Los errores de negocio no tienen excepciones tipadas: cualquier `RuntimeException` lanzada por la capa de Servicio responde **400 Bad Request**, incluso "no encontrado", que conceptualmente sería 404. El cuerpo es este:
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

## Recurso: Auth
### 1. Login (público)
`POST /api/auth/login`
* **Body:** `{ "email": "...", "password": "..." }`. El email no distingue mayúsculas y se le quitan los espacios.
* **200 OK:** `{ "token": "<jwt>", "tipo": "Bearer", "expiraEn": "<instante ISO-8601 UTC>", "usuario": UsuarioResponse }`
* **401:** `"Email o contraseña incorrectos."`

### 2. Usuario logueado
`GET /api/auth/me`
* **200 OK:** `UsuarioResponse`
* **401:** token inválido o vencido, o usuario desactivado.

### 3. Cambiar mi contraseña
`PUT /api/auth/me/password`
* **Body:** `{ "passwordActual": "...", "passwordNueva": "..." }`
* **204 No Content.**
* **400:** contraseña actual incorrecta, nueva de menos de 8 caracteres, o nueva igual a la actual. Es 400 y no 401 para que el front no cierre la sesión.

---

## Recurso: Usuarios (solo ADMIN)
Con un token de `USER`, todos estos endpoints devuelven **403**.

* **`UsuarioResponse`:** `usuarioId` (UUID), `email`, `nombre`, `rol` (`ADMIN` | `USER`), `activo`, `fechaCreacion`, `fechaUltimaModificacion`. **Nunca** incluye la contraseña ni su hash.
* **`UsuarioCreateRequest`:** `email`, `nombre`, `password` (8 caracteres como mínimo) y `rol`. Todos son obligatorios.
* **`UsuarioUpdateRequest`:** `email`, `nombre`, `rol`, `activo`, `password`. Todos son opcionales y los `null` se ignoran. `password` permite que el ADMIN resetee la contraseña de **otro** usuario sin conocer la actual. Para la propia cuenta no se puede: hay que usar `PUT /api/auth/me/password`.

### 1. Listar usuarios
`GET /api/usuarios` → **200 OK:** `UsuarioResponse[]` (activos e inactivos)

### 2. Obtener por ID
`GET /api/usuarios/{id}` → **200 OK:** `UsuarioResponse` · **400** si no existe

### 3. Crear usuario
`POST /api/usuarios` → **201 Created:** `UsuarioResponse`
* **400:** email duplicado (sin distinguir mayúsculas), email con formato inválido, nombre vacío, rol faltante o contraseña de menos de 8 caracteres.

### 4. Actualizar usuario (parcial)
`PUT /api/usuarios/{id}` → **200 OK:** `UsuarioResponse`
* **400:**
  * un ADMIN no puede quitarse a sí mismo el rol ADMIN ni desactivarse, para que el sistema nunca se quede sin ADMIN;
  * si el `id` es el del usuario logueado y viene `password`, responde `"Para cambiar tu propia contraseña usá PUT /api/auth/me/password, que pide la contraseña actual."` y no se modifica ningún campo del request.

### 5. Desactivar usuario (baja lógica)
`DELETE /api/usuarios/{id}` → **204 No Content**. Marca `activo = false`.
* **400:** si es el propio usuario o si ya estaba desactivado.
* El usuario desactivado ya no puede loguearse, y el token que ya tenía deja de servir: **su siguiente request a cualquier endpoint responde 401**.

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
* **Validación de `dni`:**
  * Es obligatorio.
  * Se normaliza quitando espacios, puntos y guiones (`" 40.011.019 "` → `40011019`) y se guarda así.
  * Tiene que tener **de 6 a 8 dígitos**, sin letras.
  * Tiene que ser único (`existsByDni`).
  * Es la misma regla que aplica la importación (ver endpoint 8).
* **201 Created:** `PersonaResponse` creado con su UUID (con `membresias: [{...}]` si `seAsocia=true`).
* **400 Bad Request:**
  * `"El DNI es obligatorio."`
  * `"DNI con formato inválido: '{dni}' (deben ser de 6 a 8 dígitos, sin letras)."`
  * `"Ya existe una persona con el dni: {dni}"`

### 5. Actualizar persona (parcial)
`PUT /api/personas/{id}`
* **Body:** JSON `PersonaUpdateRequest` con los campos a modificar (`nombre`, `apellido`, `email`, `telefonoPrincipal`, `activo`). **No permite editar `dni`, ni gestionar membresía** (eso quedó explícitamente fuera de este endpoint).
* **Regla de los campos de texto** (la misma en `PUT /api/actividades/{id}`):
  * **Ausente o `null`:** no se modifica.
  * **`""` (o solo espacios) en un campo opcional** (`email`, `telefonoPrincipal`): se borra y queda en `null`.
  * **`""` (o solo espacios) en un campo obligatorio** (`nombre`, `apellido`): **400** con `"El nombre no puede estar vacío."` / `"El apellido no puede estar vacío."`.
  * **Cualquier otro valor:** se guarda sin espacios al principio ni al final.
* **200 OK:** `PersonaResponse` actualizado.
* **400 Bad Request:** `nombre` o `apellido` vacíos, o la persona no existe.

### 6. Eliminar persona (baja lógica)
`DELETE /api/personas/{id}`
* Marca `activo = false`. No borra el registro.
* **204 No Content**.

### 7. Conteo / estadísticas
`GET /api/personas/stats/count?activo={boolean}&esSocio={boolean}`
* **Query params:** `activo` (**obligatorio**), `esSocio` (opcional).
* **200 OK:** `{ "total": <long> }`

### 8. Importar socios desde CSV / Excel (solo ADMIN)
`POST /api/personas/importar?simular={boolean}`, de tipo `multipart/form-data`, con el campo **`archivo`** (`.csv` o `.xlsx`).
* **`simular=true`:** valida y devuelve el reporte **sin guardar nada**. Sirve para mostrar una vista previa en el front antes de confirmar. Por defecto vale `false`.
* **Encabezado (fila 1):**
  * Obligatorias: `dni`, `nombre`, `apellido`.
  * Opcionales: `fecha_nacimiento`, `telefono`, `email`, `socio`.
  * No importan las mayúsculas, las tildes ni los espacios. También se aceptan alias: `documento`, `nombres`, `apellidos`, `nacimiento`, `celular`, `mail`, `es socio`. Las columnas desconocidas se ignoran, y el orden de las columnas es libre.
* **Formato:**
  * CSV con separador `;` o `,` (se detecta solo), en UTF-8 o Windows-1252, que es lo que exporta Excel.
  * Excel: se lee la primera hoja; las fechas y los números se toman de las celdas reales.
  * Límites: 5 MB (`spring.servlet.multipart.max-file-size`) y 5000 filas (`app.importacion.max-filas`).
* **Validaciones por fila.** Se informan **todos** los errores de cada fila, no solo el primero:
  * `dni`: es obligatorio y tiene que tener de 6 a 8 dígitos. Se aceptan puntos y espacios: `40.011.019` → `40011019`.
  * **DNI duplicado**, ya sea porque existe en la base o porque está repetido en el archivo (el mensaje indica la fila de la primera aparición).
  * `nombre` y `apellido`: obligatorios, hasta 100 caracteres.
  * `fecha_nacimiento`: formatos `dd/mm/aaaa`, `aaaa-mm-dd` o `dd-mm-aaaa`. Tiene que ser una fecha real (no acepta 31/02) y no puede ser futura.
  * `email`: formato válido. Se guarda en minúsculas.
  * `telefono`: hasta 20 caracteres.
  * `socio`: `SI`/`NO` (también `sí`, `s`, `x`, `1`, `true` / `n`, `0`, `false` o vacío). Con `SI`, se le crea la primera `Membresia`.
* **Política:**
  * Las filas con errores se saltean y **las válidas se guardan todas juntas en una transacción**: si falla el guardado de una, no se guarda ninguna.
  * Las filas vacías se ignoran.
  * Cada fila se da de alta con la misma lógica que el ABM manual (`PersonaService.guardar`).
* **200 OK:**
```json
{
  "archivo": "socios.csv", "simulacion": false,
  "totalFilas": 7, "filasValidas": 4, "filasConError": 3, "importadas": 4,
  "validas":  [ { "fila": 2, "dni": "30111222", "nombre": "Lucía", "apellido": "Fernández", "socio": true } ],
  "errores":  [ { "fila": 6, "dni": "34A55666", "errores": ["DNI con formato inválido: '34A55666' (deben ser de 6 a 8 dígitos, sin letras)."] } ]
}
```
`fila` es el número de fila tal como se ve en Excel (la 1 es el encabezado).
* **400:** sin archivo, extensión no soportada, archivo dañado, faltan columnas obligatorias, columna repetida, archivo sin filas o con más de 5000.
* **413:** el archivo supera los 5 MB.
* **403:** el token es de un `USER`.

### 9. Descargar plantilla de importación (solo ADMIN)
`GET /api/personas/importar/plantilla` → **200 OK:** archivo `plantilla-socios.csv`, con separador `;` y BOM UTF-8, así Excel en español lo abre con las columnas separadas y las tildes bien. Trae el encabezado y 2 filas de ejemplo.

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
* **Campos de texto:** misma regla que en `PUT /api/personas/{id}`. Si un campo viene ausente o `null`, no se modifica. `descripcion` es opcional, así que con `""` se borra y queda en `null`. `nombre` es obligatorio, así que con `""` devuelve 400 con `"El nombre no puede estar en blanco."`.
* **200 OK** / **400 Bad Request** (nombre en blanco, precio negativo, nombre duplicado).

### 6. Eliminar actividad (baja lógica en cascada)
`DELETE /api/actividades/{id}`
* Marca `activo = false` y desactiva en cascada **todas las comisiones** de la actividad **y sus inscripciones activas** (`activo = false`, `fechaBaja = hoy`). Todo ocurre en una sola transacción, y el historial de asistencias se conserva.
* **204 No Content**.
* **400 Bad Request:** si la actividad ya estaba desactivada.

---

## Recurso: Comisiones
`Comision` (turno de una actividad): `comisionId` (UUID), `actividad` (referencia completa a `Actividad`), `nombre`, `horario` (String libre, ver deuda técnica), `cupoMaximo`, `activa`.

### 1. Crear comisión
`POST /api/comisiones?actividadId={uuid}&nombre={string}&horario={string}&cupo={int?}`
* **Validaciones:**
  * la actividad debe existir y **estar activa**: si está dada de baja, devuelve 400 con `"No se puede crear una comisión en una actividad dada de baja."`;
  * `nombre` y `horario` son obligatorios. Si vienen `""` o solo con espacios, devuelve 400 con `"El nombre de la comisión no puede estar vacío."` / `"El horario de la comisión no puede estar vacío."`. Se guardan sin espacios al principio ni al final, la misma regla de los PUT;
  * no puede existir ya una comisión con la misma combinación `(actividad, nombre, horario)`. La comparación no distingue mayúsculas y se hace con los valores ya recortados;
  * `cupo` es opcional: si se omite, la comisión queda sin tope. Si viene, **tiene que ser mayor a 0**, igual que en el PUT.
* **201 Created:** objeto creado (`activa = true` por defecto).
* **400 Bad Request:** cualquiera de las validaciones anteriores.

### 2. Listar comisiones de una actividad
`GET /api/comisiones/actividad/{actividadId}?incluirInactivas={boolean}`
* **Query param (opcional):** `incluirInactivas`, por defecto `false`.
* **200 OK:** `Comision[]`. Por defecto **solo trae las activas**; con `incluirInactivas=true` trae todas.

### 3. Obtener por ID
`GET /api/comisiones/{id}`
* **200 OK** / **400 Bad Request** si no existe.

### 4. Actualizar comisión (parcial)
`PUT /api/comisiones/{id}`
* **Body:** JSON `ComisionUpdateRequest` = `{ "nombre": "...", "horario": "...", "cupoMaximo": 20, "sinTope": false }`. Todos los campos son opcionales: los `null` se ignoran y a `nombre`/`horario` se les quitan los espacios. No permite cambiar la actividad ni el estado (la baja tiene su propio `DELETE`).
* **Cupo:**
  * `cupoMaximo: N` pone o cambia el tope.
  * `cupoMaximo` ausente o `null` no toca el cupo.
  * **`sinTope: true` quita el tope**: `cupoMaximo` queda en `null` y la comisión no tiene límite de inscriptos.
  * `sinTope` ausente, `null` o `false` no hace nada.
  * Mandar `sinTope: true` junto con un `cupoMaximo` devuelve 400, porque son pedidos contradictorios.
  * Se usa un campo aparte porque `null` en `cupoMaximo` ya significa "no modificar".
* **Validaciones:**
  * la comisión debe estar activa;
  * `nombre` y `horario` son obligatorios en la edición: con `""` (o solo espacios) devuelven 400 en vez de borrarse. Si vienen ausentes o `null`, no se modifican;
  * la combinación `(actividad, nombre, horario)` sigue siendo única, sin distinguir mayúsculas y excluyendo a la propia comisión;
  * `cupoMaximo` tiene que ser mayor a 0 y **no puede ser menor que la cantidad de inscriptos activos**;
  * `sinTope: true` no se puede combinar con un `cupoMaximo`.
* **200 OK:** `Comision` actualizada.
* **400 Bad Request:** cualquiera de las validaciones anteriores.

### 5. Dar de baja comisión (baja lógica en cascada)
`DELETE /api/comisiones/{id}`
* Marca `activa = false` y **da de baja todas sus inscripciones activas** (`activo = false`, `fechaBaja = hoy`), en una sola transacción. No borra registros, y el historial de asistencias se conserva.
* Después de la baja, la comisión ya no acepta inscripciones, y sus inscripciones no aceptan asistencias.
* **204 No Content.**
* **400 Bad Request:** si no existe o ya estaba dada de baja.

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