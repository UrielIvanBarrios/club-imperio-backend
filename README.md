# 🏰 Club Imperio Juniors - Sistema de Gestión

Sistema integral de gestión para clubes de barrio, desarrollado en Java con Spring Boot. Este software busca digitalizar la administración de socios, actividades, comisiones, asistencias y cobranzas mediante una arquitectura profesional y escalable.

## 🚀 Estado Actual del Proyecto: MVP (Fase 2)
El sistema es un **Monolito Modular** con los núcleos de membresía y de operativa de campo (asistencias) funcionales. Ahora suma **autenticación con roles**, **importación masiva de socios** y el **ABM completo de comisiones**. La API está lista para ser consumida por un front web (CORS configurado).

### ✨ Características Implementadas
* **Autenticación y roles:** login con JWT; roles `ADMIN` (jefe de administrativos) y `USER` (administrativo de campo); contraseñas con BCrypt; cambio de contraseña propio; ABM de usuarios exclusivo para ADMIN.
* **Gestión de Membresía:** alta, edición, baja lógica y búsqueda de socios por DNI. El estado de socio se deriva del historial de `Membresia`. El DNI se normaliza (`40.011.019` → `40011019`) y se valida su formato.
* **Importación masiva de socios:** carga desde **CSV o Excel (.xlsx)** con un reporte fila por fila de los registros válidos y de los errores (DNI duplicado o con formato inválido, fechas imposibles, emails mal escritos, etc.). Tiene modo vista previa sin guardar y una plantilla descargable.
* **Catálogo de Actividades:** definición de deportes y parámetros operativos. La baja de una actividad desactiva en cascada sus comisiones e inscripciones.
* **Control de Comisiones:** ABM de turnos, con validación de cupos máximos (nunca por debajo de los inscriptos actuales) y prevención de duplicidad de horarios. La baja desactiva sus inscripciones.
* **Motor de Inscripciones:** vinculación de socios con comisiones, validando disponibilidad de lugar y evitando inscripciones duplicadas.
* **Gestión de Asistencia:** registro de presente o ausente por clase, historial por inscripción y validación de "doble carga" en el mismo día.
* **Ciberdefensa de Datos:**
  * UUIDs (v4) como IDs no predecibles.
  * Validaciones de integridad en la capa de Servicio y restricciones de unicidad en la base de datos.
  * DTOs en los módulos sensibles, para que el hash de la contraseña nunca salga en un JSON.
  * Errores uniformes: 400, 401 y 403.
  * CORS restringido a los orígenes configurados.

## 📂 Documentación Técnica
Para una comprensión profunda del diseño y uso del sistema, consultá los archivos en la carpeta `/docs`:

* 📜 **[Arquitectura Modular](./docs/ARCHITECTURE.md):** Explicación del diseño por contextos delimitados (Membresía, Asistencia, Seguridad, Pagos).
* 🗄️ **[Esquema de Base de Datos](./docs/database-schema.md):** Tablas, columnas, restricciones y relaciones.
* 🛠️ **[Deuda Técnica y Decisiones](./docs/DEBT.md):** Registro de compromisos técnicos y simplificaciones de diseño.
* 📡 **[Especificación de la API](./docs/api-spec.md):** Listado detallado de endpoints, autenticación, códigos de error, parámetros de entrada y modelos de respuesta.
* 📋 **[Backlog de Desarrollo](./docs/BACKLOG.md):** Listado de tareas pendientes, tickets de mejora y funcionalidades futuras.

## 🛠️ Stack Tecnológico
* **Lenguaje:** Java 21
* **Framework:** Spring Boot 4.0.3
* **Seguridad:** Spring Security 7 + OAuth2 Resource Server (JWT HS256), BCrypt
* **Persistencia:** Spring Data JPA + Hibernate
* **Base de Datos:** PostgreSQL
* **Documentación de API:** springdoc-openapi 3.0.3 (Swagger UI, perfil `dev`)
* **Archivos:** Apache POI (Excel) y Apache Commons CSV
* **Librerías:** Lombok (Boilerplate reduction), Jakarta Persistence (ORM).

## 📊 Modelo de Datos (Flujo de Dominio)
El sistema sigue una jerarquía de dependencia estricta para garantizar la integridad:
`Actividad` ➡️ `Comisión` ➡️ `Inscripción` ⬅️ `Persona` ➡️ `Asistencia` (vía Inscripción)

`Usuario` (quién opera el sistema) es independiente de `Persona` (socios y alumnos del club).

---

## 🏁 Cómo ejecutar el proyecto
1. Clonar el repositorio.
2. Copiar `src/main/resources/application.properties.example` como `application.properties`. Este archivo **no se versiona**, porque contiene secretos. Después completar:
   * las credenciales de PostgreSQL (`spring.datasource.*`);
   * `app.jwt.secret`: una clave aleatoria de 32 o más caracteres, por ejemplo `openssl rand -base64 48`;
   * `app.admin.email` y `app.admin.password`: el **ADMIN inicial**, que se crea solo cuando la tabla `usuarios` está vacía;
   * `app.cors.allowed-origins`: la URL del front (por defecto `http://localhost:5173`).
3. Ejecutar `./mvnw spring-boot:run`. Para tener Swagger, usar `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.
4. El sistema gestionará automáticamente el esquema de tablas mediante Hibernate.
5. Iniciar sesión con `POST /api/auth/login` usando el ADMIN inicial, y mandar el token en cada request como `Authorization: Bearer <token>`.

### 🧪 Probar la API
* **Swagger UI** (perfil `dev`): `http://localhost:8080/swagger-ui/index.html`. Hacer login, tocar **Authorize** y pegar el token.
* **Archivos `.http`** en `/api-tests` (cliente HTTP de IntelliJ): ejecutar primero el login de `auth.http`, que guarda el token para todos los demás. `socios-ejemplo.csv` sirve para probar la importación.
