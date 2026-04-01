Parámetros de búsqueda permitidos: (DNI, Nombre, Apellido, Activo, Socio).

Comportamiento por defecto: Si pido /api/personas, ¿me trae a los 2000 socios o solo a los que no deben plata? (Para un club de barrio, lo mejor es que el default sea activo=true).

Códigos de Estado: Recordá que si el filtro no devuelve nada, el status sigue siendo 200 OK pero con una lista vacía []. No es un error 404.

# API Specification - Club Imperio Juniors v1.0

## Base URL
`http://localhost:8080/api/v1`

## Recurso: Personas (Socios y Administrativos)

### 1. Listar Personas
`GET /personas`
*   **Filtros (Query Params):**
    *   `activo` (boolean): Filtra por estado lógico.
    *   `esSocio` (boolean): Filtra por condición de socio.
*   **Response (200 OK):**
    ```text
    [
      { "id": "uuid", "nombre": "Uriel", "activo": true, ... }
    ]
    ```

### 2. Buscar por DNI
`GET /personas/{dni}`
*   **Response (200 OK):** Objeto Persona.
*   **Response (404 Not Found):** `{"message": "Persona no encontrada"}`

### 3. Alta de Persona
`POST /personas`
*   **Body:** Campos obligatorios (dni, nombre, apellido, email).
*   **Seguridad:** Validación de DNI único (Check Ciberdefensa: Previene duplicidad de identidad).
*   **Response (201 Created):** Objeto creado con su UUID.

Recurso: Inscripciones (El "Contrato" Socio-Actividad)

### 1. Alta de Inscripción
`POST /inscripciones?dni={dni}&comisionId={uuid}`

**Validaciones de Negocio (Ciberdefensa):**

* **Persona:** debe estar activa.
* **Comisión:** debe estar activa.
* **Socio:** no puede estar ya inscripto en esa comisión.
* **Validación de Cupo:** Si inscriptos >= cupoMaximo, rebota con **error 400**.
* **Response (201 Created):** Objeto Inscripción con fecha de alta.