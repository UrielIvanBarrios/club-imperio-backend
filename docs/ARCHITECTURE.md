# 🏛️ Arquitectura del Sistema - Club Imperio Juniors

Este proyecto utiliza un diseño de **Monolito Modular**. Se prioriza la separación de intereses por dominios de negocio para garantizar la escalabilidad y el bajo acoplamiento.

## 🧱 Módulos Principales

| Módulo | Responsabilidad | Entidades Clave |
| :--- | :--- | :--- |
| **Membresía** | Gestión de identidad y vínculos legales del club. | Persona, Actividad, Comisión, Inscripción. |
| **Asistencia** | Control operativo de presencia en campo. | Asistencia. |
| **Pagos** (Próximamente) | Motor financiero y cuenta corriente. | Arancel, Pago. |

## 🛠️ Reglas de Comunicación
1. **Shared Kernel (Core):** Las entidades `Persona` y `Actividad` son el núcleo. Todos los módulos pueden referenciarlas.
2. **Independencia:** El módulo de `Asistencia` no debe modificar datos de `Membresía`. Solo consulta a través de servicios.
3. **Validación Cruzada:** La lógica de "no permitir asistencia si no hay inscripción activa" reside en el `AsistenciaService`.

## 🚀 Hoja de Ruta (Escalabilidad)
Para futuros crecimientos, se prevé la creación de:
* `alquileres`: Gestión de turnos y espacios físicos.
* `admin`: Auditoría, logs y control de acceso (RBAC).
* `reportes`: Consumo de datos transversal para KPIs directivos.