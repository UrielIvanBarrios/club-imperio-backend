# 🏛️ Arquitectura del Sistema - Club Imperio Juniors

Este proyecto utiliza un diseño de **Monolito Modular**. Se prioriza la separación de intereses por dominios de negocio para garantizar la escalabilidad y el bajo acoplamiento.

## 🧱 Módulos Principales

| Módulo | Responsabilidad | Entidades Clave |
| :--- | :--- | :--- |
| **Membresía** | Gestión de identidad y vínculos legales del club. Incluye la importación masiva de socios (CSV/Excel). | Persona, Membresia, Actividad, Comisión, Inscripción, ClasePrueba. |
| **Asistencia** | Control operativo de presencia en campo. | Asistencia. |
| **Seguridad** | Autenticación (JWT), autorización por roles y gestión de los usuarios que operan el sistema. | Usuario, Rol. |
| **Pagos** (Próximamente) | Motor financiero y cuenta corriente. | Arancel, Pago. |

Cada módulo es un paquete bajo `com.clubimperio.gestionclub` con la misma estructura interna: `controllers` → `services` → `repositories` → `entities`, más `dtos` y, en Seguridad, `config`. Lo transversal vive aparte: `exceptions` (`GlobalExceptionHandler`) y `config` (OpenAPI/Swagger).

## 🛠️ Reglas de Comunicación
1. **Shared Kernel (Core):** Las entidades `Persona` y `Actividad` son el núcleo. Todos los módulos pueden referenciarlas.
2. **Independencia:** El módulo de `Asistencia` no debe modificar datos de `Membresía`. Solo consulta a través de servicios.
3. **Validación Cruzada:** La lógica de "no permitir asistencia si no hay inscripción activa" reside en el `AsistenciaService`.
4. **Seguridad como capa transversal:**
   * Los permisos se declaran en un único lugar (`SecurityConfig`, por URL), no repartidos en los controllers.
   * Los demás módulos no dependen de `seguridad`. Solo conocen al usuario logueado a través del JWT (`@AuthenticationPrincipal`) cuando lo necesitan.
   * `Usuario` (quién opera el sistema) y `Persona` (socios del club) son entidades separadas.
5. **Sin dependencias circulares entre services:** cuando un service necesita datos de otro que ya depende de él, usa directamente el repositorio. Por ejemplo, `ComisionService` y `ActividadService` usan `InscripcionRepository` y no `InscripcionService`.
6. **Errores uniformes:** los services lanzan excepciones con mensajes de negocio. `GlobalExceptionHandler` las traduce a JSON con un formato único: 400 negocio, 401 autenticación, 403 permisos, 413 archivo grande. Los rechazos de Spring Security, que ocurren antes de llegar al controller, se le reenvían a ese mismo handler.

## 🔐 Flujo de una request
```
Request → CORS → SecurityFilterChain (¿JWT válido? ¿rol suficiente?) → Controller → Service → Repository → PostgreSQL
                         │ no                                          │ error de negocio
                         ▼                                             ▼
                  401 / 403 (JSON)                             GlobalExceptionHandler → 400 (JSON)
```

## 🚀 Hoja de Ruta (Escalabilidad)
Para futuros crecimientos, se prevé la creación de:
* `alquileres`: Gestión de turnos y espacios físicos.
* `admin`: Auditoría y logs (`@CreatedBy` / `@LastModifiedBy`, ver `DEBT.md` #7). El control de acceso (RBAC) ya está implementado en el módulo `seguridad`.
* `reportes`: Consumo de datos transversal para KPIs directivos.