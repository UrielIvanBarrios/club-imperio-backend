# Registro de Deuda Técnica - Club Imperio

Este documento detalla las simplificaciones tomadas para el MVP y las mejoras necesarias a futuro.

## 1. Módulo Comisiones: Horarios como String
* **Estado actual:** El campo `horario` es un `String` libre (ej: "Lunes 18hs").
* **Riesgo:** Inconsistencia de datos, errores de tipeo y dificultad para validar solapamientos de profesores o salones.
* **Mejora necesaria:** Crear una entidad `Horario` con `DiaSemana` (Enum), `HoraInicio` y `HoraFin`.

## 2. Módulo Personas: Auditoría de Bajas
* **Estado actual:** Usamos un booleano `activo`.
* **Riesgo:** Si se da de baja a alguien, no sabemos el motivo ni quién lo hizo.
* **Mejora necesaria:** Implementar una tabla de logs o campos de auditoría `motivo_baja` y `fecha_baja` real.

## 3. Clases de Prueba: Integración con Pagos
* **Estado actual:** Tienen un campo `costo` manual.
* **Mejora necesaria:** Vincular la Clase de Prueba con un `Arancel` específico para que pase por la caja oficial del club.

## 4. Capa de Seguridad: Implementación de DTOs
* **Estado actual:** El Controller recibe y devuelve Entidades directas (@Entity).
* **Riesgo:** "Mass Assignment Vulnerability". Un usuario malintencionado podría enviar un JSON con `"activo": true` o `"esSocio": true` en un endpoint de actualización y el sistema lo guardaría sin validar.
* **Mejora:** Crear `PersonaDTO`, `InscripcionRequest`, etc., para filtrar qué datos entran y salen del sistema.
* **Avance:**
  * Ya usan DTOs: **Personas** (`PersonaCreateRequest`/`UpdateRequest`/`Response`), **Usuarios** (los DTOs son obligatorios para no exponer el hash de la contraseña) y el **PUT de Comisiones** (`ComisionUpdateRequest`).
  * Las respuestas de **Inscripciones, Asistencias y Clases de Prueba** ya son DTOs (`InscripcionResponse`, `AsistenciaResponse`, `ClasePruebaResponse`). Hacía falta: llevaban la entidad `Persona` adentro y con un socio el JSON entraba en recursión (`Persona` → `Membresia` → `Persona`).
  * Todavía devuelven entidades: Actividades y Comisiones (también anidadas en esos DTOs). No tienen relaciones hacia atrás, así que no tienen el problema de la recursión; lo que queda pendiente es el riesgo de Mass Assignment de los requests.

## 5. Auditoría Blindada: Historial de Estados
* **Estado actual:** Booleano `activo` simple.
* **Riesgo:** Pérdida de trazabilidad. No sabemos por qué un socio dejó el club (Mora vs. Baja Voluntaria).
* **Mejora:** Tabla `historial_estados_persona` para registrar: Fecha, Estado Anterior, Estado Nuevo y Motivo.

## 6. Transición de Datos: Migrador de Excel
* **Estado actual:** Carga manual de datos.
* **Necesidad:** El club tiene miles de registros en Excel. La carga manual es propensa a errores.
* **Mejora:** Desarrollar un servicio de importación masiva con validación de integridad (limpieza de DNI duplicados, formatos de fecha, etc.).
* **✅ Resuelto:** `POST /api/personas/importar` acepta `.csv` y `.xlsx`, valida cada fila y devuelve un reporte con las filas válidas y las filas con error. Se puede usar en modo vista previa con `?simular=true` (ver `api-spec.md`).
* ~~**Pendiente:** el alta manual no validaba el formato del DNI~~ → resuelto: la regla está en `PersonaService` y la comparten el alta manual y la importación.

## 7. Trazabilidad de Usuario (Spring Security)
* **Estado actual:**
  * Ya hay login con JWT y roles ADMIN/USER.
  * Todavía no se registra **quién** hizo cada cambio, solo cuándo (en `usuarios.fecha_ultima_modificacion`).
  * La tabla `usuarios` ya trae las columnas `usuario_creador_id` y `usuario_modificador_id`, heredadas del diseño anterior y hoy sin usar.
* **Mejora:** Implementar `@CreatedBy` y `@LastModifiedBy`, con un `AuditorAware` que lea el `sub` del JWT. En el club es vital saber qué administrativo borró una clase o cobró un arancel por seguridad contable.

## 8. Clases de Prueba: Validación de Disponibilidad Horaria
* **Estado actual:** La `fechaSolicitada` es un `LocalDateTime` libre que no se valida contra los horarios reales de la `Comision`.
* **Riesgo:** Agendar una clase de prueba en un día o horario donde la actividad no se dicta (ej: pedir Fútbol un domingo a las 3 AM), generando frustración en el potencial socio y desorden operativo.
* **Mejora necesaria:** Una vez implementado el **Punto 1 (Entidad Horario)**, desarrollar un validador en `ClasePruebaService` que verifique que el `DayOfWeek` y el rango horario de la solicitud coincidan con alguna de las comisiones activas de la actividad elegida.
## 9. Personas: búsqueda por DNI sin normalizar
* **Estado actual:**
  * El alta (manual o por importación) normaliza el DNI (`40.011.019` → `40011019`) y valida su formato.
  * Las búsquedas no lo normalizan: `GET /api/personas/buscar`, y `dni` en Inscripciones y Clases de Prueba.
* **Riesgo:** buscar `40.011.019` responde "No existe socio" aunque la persona exista. Normalizar **solo** la búsqueda sería peor: `InscripcionService` busca la persona con el DNI normalizado, pero chequea la inscripción duplicada con el DNI crudo (`existsByPersona_DniAndComision...`). Un DNI con puntos pasaría el chequeo y permitiría inscribir dos veces al mismo socio.
* **Mejora:** normalizar con `PersonaService.normalizarDni` en **todos** los puntos de entrada que reciben un DNI. O mejor: que `InscripcionService` use el DNI de la `Persona` encontrada (o su id) para el chequeo de duplicados, en vez del parámetro recibido.

## 10. Base de datos: esquema sin migraciones versionadas
* **Estado actual:** el esquema lo maneja Hibernate con `ddl-auto=update`, que agrega tablas y columnas pero **nunca** borra columnas ni corrige la nulabilidad de las que ya existen. Para compatibilizar la tabla `usuarios` heredada hubo que ejecutar `ALTER`s a mano en la base local:
  * `username DROP NOT NULL`
  * `rol SET NOT NULL`
  * `activo SET NOT NULL`
* **Riesgo:**
  * Esos cambios no están en el repositorio: una base nueva en otra máquina queda distinta.
  * Quedan columnas heredadas sin uso: `personas.es_socio` (de antes de `Membresia`), y `usuarios.username` y `usuarios.persona_id`.
  * El índice único parcial de Membresía (una sola activa por persona) sigue bloqueado.
* **Mejora:** adoptar Flyway o Liquibase con un script base que refleje el esquema real, incluidos los `ALTER` y la limpieza de columnas heredadas. Se descartó por alcance para el TP3.

## 11. Seguridad: tokens no revocables y login sin límite de intentos
* **Estado actual:**
  * El JWT es stateless y dura 8 h.
  * No hay refresh token ni lista de revocación.
  * `POST /api/auth/login` no limita la cantidad de intentos.
  * El ADMIN inicial se crea con una contraseña leída de `application.properties` y no se fuerza a cambiarla.
* **Riesgo:**
  * Un usuario desactivado conserva su token hasta que vence (hasta 8 h). `GET /api/auth/me` le responde 401, pero el resto de los endpoints siguen aceptando el token.
  * Sin límite de intentos, el login es vulnerable a fuerza bruta.
* **Mejora:**
  * Chequear `activo` en cada request (por ejemplo, en el `JwtAuthenticationConverter`), o bajar la duración y agregar refresh token.
  * Limitar intentos por IP o email (por ejemplo, con Bucket4j) y bloquear temporalmente tras N fallos.
  * Forzar el cambio de contraseña del ADMIN en el primer ingreso.

## 12. Errores sin tipar y validación manual
* **Estado actual:**
  * Los services lanzan `RuntimeException` con un mensaje. El `GlobalExceptionHandler` solo distingue errores de seguridad (401/403) y archivos grandes (413); todo lo demás es **400**, incluido "no encontrado", que debería ser **404**.
  * Las validaciones de entrada están escritas a mano en cada service; no se usa Bean Validation (`@Valid`, `@NotBlank`, `@Email`).
* **Riesgo:**
  * El front no puede distinguir "no existe" de "dato inválido".
  * Cualquier `RuntimeException` inesperada (un bug) sale como 400 con su mensaje técnico, en vez de un 500 genérico.
* **Mejora:**
  * Excepciones de dominio (`RecursoNoEncontradoException` → 404, `ReglaNegocioException` → 400, `ConflictoException` → 409).
  * Un handler de `Exception` que devuelva 500 sin exponer detalles.
  * Bean Validation en los DTOs.

## 13. Calidad: sin tests automatizados
* **Estado actual:** el único test es `contextLoads()`. Todo se verificó a mano (curl y `.http`).
* **Riesgo:** un cambio puede romper una regla existente sin que nadie se entere: cupos, duplicados, cascadas de baja, permisos por rol, validaciones de la importación.
* **Mejora:**
  * Tests unitarios de los services con más reglas: `InscripcionService` (cupo lleno, duplicado), `ComisionService` (cupo menor a los inscriptos, cascada), `ImportacionPersonasService` (cada tipo de error de fila) y `UsuarioService` (un ADMIN no puede degradarse).
  * Tests de integración con `@WebMvcTest` y `spring-security-test` (ya incluido en el `pom`) para los 401/403.

## 14. API: estilo inconsistente y listados sin paginación
* **Estado actual:**
  * Los POST de Comisiones, Inscripciones, Clases de Prueba y Asistencias reciben parámetros por URL (`@RequestParam`). En cambio, Personas, Usuarios y el PUT de Comisiones reciben un body JSON.
  * Ningún listado pagina: `GET /api/personas` trae todas las filas.
  * La asistencia se registra de a un alumno por request, y su historial existe solo por inscripción, no por socio.
* **Riesgo:**
  * El front tiene que manejar dos estilos distintos.
  * Con miles de socios importados, los listados devuelven todo de una vez.
  * Tomar lista a una comisión de 30 alumnos son 30 requests.
* **Mejora:**
  * Unificar en body JSON con DTOs (va de la mano con el punto 4).
  * Paginar con `Pageable` / `Page<T>`.
  * Agregar `POST /api/asistencias/comision/{id}` en bloque y un historial de asistencias por socio.

## 15. Entidades: `Persona.activo` sin `@Builder.Default`
* **Estado actual:** `Persona` inicializa `activo = true`, pero sin `@Builder.Default`, así que `Persona.builder()...build()` lo deja en **null**. `Usuario`, `Comision` e `Inscripcion` sí usan `@Builder.Default`.
* **Riesgo:** cualquier código nuevo que construya una `Persona` con el builder y no setee `activo` falla contra la base (`NOT NULL`). Hoy el controller y la importación lo setean a mano.
* **Mejora:** agregar `@Builder.Default` a `activo` y quitar los seteos manuales.

## 16. Importación: síncrona y en memoria
* **Estado actual:**
  * La importación procesa el archivo completo dentro de la request HTTP, con el Excel entero cargado en memoria (`XSSFWorkbook`).
  * Está acotada a 5 MB y 5000 filas, y responde en menos de un segundo con 5000 filas.
  * El rollback ante un fallo a mitad del guardado (por ejemplo, otro usuario carga el mismo DNI en ese instante) está cubierto por `@Transactional` y por el UNIQUE de `dni`, pero no se probó forzándolo.
* **Riesgo:** para archivos más grandes, la request puede tardar demasiado o quedarse sin memoria.
* **Mejora:**
  * Procesar en segundo plano con un estado consultable (`POST` devuelve un id de importación; `GET` devuelve el progreso y el reporte).
  * Leer Excel en streaming (SAX / `XSSFReader`).
  * Agregar un test que fuerce el rollback.

## 17. Performance: `open-in-view` activo y relaciones EAGER
* **Estado actual:**
  * `spring.jpa.open-in-view` está en `true`, el valor por defecto (Spring lo advierte en el log al arrancar).
  * Varias relaciones usan `FetchType.EAGER`: `Comision.actividad`, `Inscripcion.persona` y `.comision`, `ClasePrueba.persona` y `.actividad`, y `Asistencia.inscripcion`. Al traer una asistencia, se carga en cadena inscripción → persona + comisión → actividad.
  * Las entidades se serializan directo al JSON.
* **Riesgo:** consultas a la base durante la serialización y consultas de más (N+1) en los listados. No se nota con pocos datos, pero sí con miles de socios.
* **Mejora:** `spring.jpa.open-in-view=false`, relaciones `LAZY` con `JOIN FETCH` donde haga falta, y DTOs de respuesta (punto 4).
