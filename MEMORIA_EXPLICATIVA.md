# MEMORIA TÉCNICA
## Implementación de API HTTP con Sockets y MySQL en Java

---

**Proyecto:** Servidor HTTP RESTful con gestión de libros  
**Tecnologías:** Java SE, Sockets, JDBC, MySQL  
**Fecha:** Enero 2026  
**Versión:** 1.0

---

## 1. INTRODUCCIÓN

### 1.1 Objetivo del Proyecto

El objetivo de este proyecto es desarrollar un servidor HTTP básico utilizando sockets nativos de Java, sin frameworks externos como Spring Boot, que sea capaz de gestionar un recurso `/books` con operaciones CRUD completas (Create, Read, Update, Delete) y almacenar los datos de forma persistente en una base de datos MySQL.

### 1.2 Arquitectura del Sistema

El sistema está compuesto por tres capas principales:

```
┌─────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Cliente   │  HTTP   │   HttpServer     │  JDBC   │    MySQL     │
│  (curl/     │ ──────> │  (Sockets)       │ ──────> │   Database   │
│  Postman)   │ <────── │                  │ <────── │  library_db  │
└─────────────┘         └──────────────────┘         └──────────────┘
                               │
                               │ usa
                               ▼
                        ┌──────────────────┐
                        │ DatabaseManager  │
                        │  (Capa de        │
                        │   Datos)         │
                        └──────────────────┘
                               │
                               │ manipula
                               ▼
                        ┌──────────────────┐
                        │     Book         │
                        │   (Modelo)       │
                        └──────────────────┘
```

**Capa de Presentación (Cliente):** Herramientas como cURL o Postman que envían solicitudes HTTP.

**Capa de Aplicación (HttpServer):** Servidor que maneja las conexiones mediante sockets, parsea las solicitudes HTTP y genera respuestas apropiadas.

**Capa de Datos (DatabaseManager):** Gestiona todas las operaciones con la base de datos MySQL mediante JDBC.

**Capa de Modelo (Book):** Representa la entidad de negocio con sus atributos y métodos de serialización.

### 1.3 Componentes Principales

- **HttpServer.java:** Servidor principal que escucha conexiones en el puerto 8080 y enruta las solicitudes.
- **DatabaseManager.java:** Capa de acceso a datos que encapsula todas las operaciones CRUD.
- **Book.java:** Modelo de datos que representa un libro con capacidad de serialización a JSON.
- **MySQL:** Base de datos relacional para la persistencia de información.

---

## 2. CONFIGURACIÓN DE LA BASE DE DATOS

### 2.1 Esquema de la Base de Datos

Se creó una base de datos llamada `library_db` con una única tabla `books`:

```sql
CREATE DATABASE IF NOT EXISTS library_db;

USE library_db;

CREATE TABLE IF NOT EXISTS books (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    year INT NOT NULL
);
```

### 2.2 Estructura de la Tabla

| Campo  | Tipo         | Restricciones         | Descripción                    |
|--------|--------------|----------------------|--------------------------------|
| id     | INT          | PRIMARY KEY, AUTO_INCREMENT | Identificador único autogenerado |
| title  | VARCHAR(255) | NOT NULL            | Título del libro               |
| author | VARCHAR(255) | NOT NULL            | Autor del libro                |
| year   | INT          | NOT NULL            | Año de publicación             |

### 2.3 Datos de Ejemplo

Para facilitar las pruebas, se insertaron tres libros iniciales:

```sql
INSERT INTO books (title, author, year) VALUES
('El Quijote', 'Miguel de Cervantes', 1605),
('Cien años de soledad', 'Gabriel García Márquez', 1967),
('1984', 'George Orwell', 1949);
```

---

## 3. IMPLEMENTACIÓN DE LOS ENDPOINTS

### 3.1 Resumen de Endpoints

| Método | Endpoint      | Descripción                | Código Respuesta       |
|--------|---------------|----------------------------|------------------------|
| GET    | /books        | Obtener todos los libros   | 200 OK                 |
| GET    | /books/{id}   | Obtener un libro por ID    | 200 OK / 404 Not Found |
| POST   | /books        | Crear un nuevo libro       | 201 Created            |
| PUT    | /books/{id}   | Actualizar un libro        | 200 OK / 404 Not Found |
| DELETE | /books/{id}   | Eliminar un libro          | 200 OK / 404 Not Found |

---

### 3.2 GET /books - Obtener Todos los Libros

**Descripción:** Este endpoint devuelve un array JSON con todos los libros almacenados en la base de datos.

**Solicitud HTTP:**
```
GET /books HTTP/1.1
Host: localhost:8080
```

**Respuesta Exitosa (200 OK):**
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 189

[
  {
    "id": 1,
    "title": "El Quijote",
    "author": "Miguel de Cervantes",
    "year": 1605
  },
  {
    "id": 2,
    "title": "Cien años de soledad",
    "author": "Gabriel García Márquez",
    "year": 1967
  }
]
```

**Ejemplo con cURL:**
```bash
curl -X GET http://localhost:8080/books
```

**Implementación en Java:**

El método `handleGetAllBooks()` realiza los siguientes pasos:

1. Invoca `dbManager.getAllBooks()` que ejecuta `SELECT * FROM books`
2. Convierte la lista de objetos `Book` a formato JSON
3. Envía la respuesta con código 200 y Content-Type: application/json

**Nota importante:** Si no hay libros en la base de datos, este endpoint devuelve un array vacío `[]` con código 200, no un error 404.

---

### 3.3 GET /books/{id} - Obtener un Libro Específico

**Descripción:** Recupera un libro individual identificado por su ID.

**Solicitud HTTP:**
```
GET /books/1 HTTP/1.1
Host: localhost:8080
```

**Respuesta Exitosa (200 OK):**
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 89

{
  "id": 1,
  "title": "El Quijote",
  "author": "Miguel de Cervantes",
  "year": 1605
}
```

**Respuesta de Error (404 Not Found):**
```
HTTP/1.1 404 Not Found
Content-Type: application/json
Content-Length: 28

{"error":"Book not found"}
```

**Ejemplos con cURL:**
```bash
# Libro existente
curl -X GET http://localhost:8080/books/1

# Libro no encontrado
curl -X GET http://localhost:8080/books/999
```

**Implementación:**

1. Se extrae el ID de la URL usando el método `extractId(path)`
2. Se ejecuta la consulta: `SELECT * FROM books WHERE id = ?`
3. Si se encuentra el libro, se serializa a JSON y se devuelve con código 200
4. Si no existe, se devuelve un mensaje de error con código 404

**Flujo de ejecución:**
- Cliente → HttpServer: GET /books/1
- HttpServer → DatabaseManager: getBookById(1)
- DatabaseManager → MySQL: SELECT * FROM books WHERE id = 1
- MySQL → DatabaseManager: ResultSet con datos
- DatabaseManager → HttpServer: Objeto Book
- HttpServer → Cliente: JSON con código 200

---

### 3.4 POST /books - Crear un Nuevo Libro

**Descripción:** Crea un nuevo libro en la base de datos. El ID se genera automáticamente.

**Solicitud HTTP:**
```
POST /books HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 88

{
  "title": "La sombra del viento",
  "author": "Carlos Ruiz Zafón",
  "year": 2001
}
```

**Respuesta Exitosa (201 Created):**
```
HTTP/1.1 201 Created
Content-Type: application/json
Content-Length: 94

{
  "id": 4,
  "title": "La sombra del viento",
  "author": "Carlos Ruiz Zafón",
  "year": 2001
}
```

**Respuesta de Error (400 Bad Request):**
```
HTTP/1.1 400 Bad Request
Content-Type: application/json

{"error":"Invalid JSON format: Missing required fields"}
```

**Ejemplo con cURL:**
```bash
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "La sombra del viento",
    "author": "Carlos Ruiz Zafón",
    "year": 2001
  }'
```

**Implementación:**

1. Se lee el cuerpo de la solicitud HTTP
2. Se parsea el JSON para extraer title, author y year
3. Se validan los campos requeridos
4. Se ejecuta: `INSERT INTO books (title, author, year) VALUES (?, ?, ?)`
5. Se recupera el ID generado automáticamente
6. Se devuelve el libro creado con código 201

**Validaciones realizadas:**
- Verificación de que el cuerpo no esté vacío
- Comprobación de que todos los campos requeridos estén presentes
- Validación de que 'year' sea un número entero

---

### 3.5 PUT /books/{id} - Actualizar un Libro

**Descripción:** Actualiza todos los campos de un libro existente identificado por su ID.

**Solicitud HTTP:**
```
PUT /books/1 HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 105

{
  "title": "Don Quijote de la Mancha",
  "author": "Miguel de Cervantes Saavedra",
  "year": 1605
}
```

**Respuesta Exitosa (200 OK):**
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 111

{
  "id": 1,
  "title": "Don Quijote de la Mancha",
  "author": "Miguel de Cervantes Saavedra",
  "year": 1605
}
```

**Respuesta de Error (404 Not Found):**
```
HTTP/1.1 404 Not Found
Content-Type: application/json
Content-Length: 28

{"error":"Book not found"}
```

**Ejemplo con cURL:**
```bash
curl -X PUT http://localhost:8080/books/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Don Quijote de la Mancha",
    "author": "Miguel de Cervantes Saavedra",
    "year": 1605
  }'
```

**Implementación:**

1. Se extrae el ID de la URL
2. Se verifica que el libro exista en la base de datos
3. Si no existe, se devuelve error 404
4. Se parsea el JSON del cuerpo
5. Se ejecuta: `UPDATE books SET title = ?, author = ?, year = ? WHERE id = ?`
6. Se devuelve el libro actualizado con código 200

**Consideración importante:** Este es un PUT completo (no PATCH), por lo que se deben enviar todos los campos, incluso los que no cambien.

---

### 3.6 DELETE /books/{id} - Eliminar un Libro

**Descripción:** Elimina permanentemente un libro de la base de datos.

**Solicitud HTTP:**
```
DELETE /books/3 HTTP/1.1
Host: localhost:8080
```

**Respuesta Exitosa (200 OK):**
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 43

{"message":"Book deleted successfully"}
```

**Respuesta de Error (404 Not Found):**
```
HTTP/1.1 404 Not Found
Content-Type: application/json
Content-Length: 28

{"error":"Book not found"}
```

**Ejemplos con cURL:**
```bash
# Eliminar libro existente
curl -X DELETE http://localhost:8080/books/3

# Intentar eliminar libro inexistente
curl -X DELETE http://localhost:8080/books/999
```

**Implementación:**

1. Se extrae el ID de la URL
2. Se ejecuta: `DELETE FROM books WHERE id = ?`
3. Se verifica el número de filas afectadas
4. Si es mayor que 0, se devuelve mensaje de éxito con código 200
5. Si es 0, significa que el libro no existía y se devuelve error 404

**Nota de seguridad:** En un sistema real, se debería considerar implementar "soft delete" (eliminación lógica) en lugar de eliminación física para mantener un historial.

---

## 4. DETALLES TÉCNICOS DE IMPLEMENTACIÓN

### 4.1 Manejo de Sockets

El servidor utiliza la clase `ServerSocket` de Java para escuchar conexiones entrantes:

```java
ServerSocket serverSocket = new ServerSocket(8080);
System.out.println("Servidor iniciado en puerto 8080...");

while (true) {
    Socket clientSocket = serverSocket.accept();
    handleClient(clientSocket);
}
```

**Características:**
- Escucha en el puerto 8080
- Bucle infinito para aceptar múltiples conexiones
- Procesamiento síncrono (una conexión a la vez)

### 4.2 Parsing de Solicitudes HTTP

El proceso de análisis de solicitudes HTTP se realiza manualmente:

**Paso 1: Leer la línea de solicitud**
```java
String requestLine = in.readLine();
// Ejemplo: "GET /books HTTP/1.1"
```

**Paso 2: Extraer método y ruta**
```java
String[] requestParts = requestLine.split(" ");
String method = requestParts[0];  // "GET"
String path = requestParts[1];    // "/books"
```

**Paso 3: Leer cabeceras**
```java
int contentLength = 0;
while ((line = in.readLine()) != null && !line.isEmpty()) {
    if (line.toLowerCase().startsWith("content-length:")) {
        contentLength = Integer.parseInt(line.substring(15).trim());
    }
}
```

**Paso 4: Leer el cuerpo (si existe)**
```java
if (contentLength > 0) {
    char[] bodyChars = new char[contentLength];
    in.read(bodyChars, 0, contentLength);
    body = new String(bodyChars);
}
```

### 4.3 Enrutamiento de Solicitudes

Se utiliza un sistema de enrutamiento basado en expresiones regulares:

```java
if (path.equals("/books") && method.equals("GET")) {
    handleGetAllBooks(out);
} else if (path.matches("/books/\\d+") && method.equals("GET")) {
    int id = extractId(path);
    handleGetBookById(out, id);
} else if (path.equals("/books") && method.equals("POST")) {
    handleCreateBook(out, body);
} 
// ... más rutas
```

**Extracción de parámetros de URL:**
```java
private int extractId(String path) {
    String[] parts = path.split("/");
    return Integer.parseInt(parts[parts.length - 1]);
}
```

### 4.4 Gestión de Conexiones a la Base de Datos

**Patrón try-with-resources:**

Este patrón garantiza el cierre automático de recursos incluso si ocurre una excepción:

```java
try (Connection conn = getConnection();
     PreparedStatement pstmt = conn.prepareStatement(query)) {
    
    pstmt.setInt(1, id);
    ResultSet rs = pstmt.executeQuery();
    // Procesamiento
    
} // Cierre automático de Connection y PreparedStatement
```

**Prevención de SQL Injection:**

Se utilizan `PreparedStatement` con parámetros en lugar de concatenación de strings:

```java
// ❌ INCORRECTO (vulnerable a SQL injection)
String query = "SELECT * FROM books WHERE id = " + id;

// ✅ CORRECTO
String query = "SELECT * FROM books WHERE id = ?";
PreparedStatement pstmt = conn.prepareStatement(query);
pstmt.setInt(1, id);
```

### 4.5 Serialización y Deserialización JSON

**Serialización (Java → JSON):**

```java
public String toJson() {
    return String.format(
        "{\"id\":%d,\"title\":\"%s\",\"author\":\"%s\",\"year\":%d}",
        id, escapeJson(title), escapeJson(author), year
    );
}

private String escapeJson(String str) {
    return str.replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
              .replace("\t", "\\t");
}
```

**Deserialización (JSON → Java):**

```java
private Book parseBookFromJson(String json) {
    String title = extractJsonValue(json, "title");
    String author = extractJsonValue(json, "author");
    String yearStr = extractJsonValue(json, "year");
    
    int year = Integer.parseInt(yearStr);
    return new Book(title, author, year);
}

private String extractJsonValue(String json, String key) {
    String pattern = "\"" + key + "\"\\s*:\\s*\"?([^\",}]+)\"?";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(json);
    return m.find() ? m.group(1).trim() : null;
}
```

### 4.6 Generación de Respuestas HTTP

**Método principal para enviar respuestas:**

```java
private void sendResponse(OutputStream out, int statusCode, 
                         String body, String contentType) throws IOException {
    String statusText = getStatusText(statusCode);
    String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                     "Content-Type: " + contentType + "\r\n" +
                     "Content-Length: " + body.getBytes().length + "\r\n" +
                     "Connection: close\r\n" +
                     "\r\n" +
                     body;
    
    out.write(response.getBytes());
    out.flush();
}
```

**Mapeo de códigos de estado:**

```java
private String getStatusText(int statusCode) {
    switch (statusCode) {
        case 200: return "OK";
        case 201: return "Created";
        case 400: return "Bad Request";
        case 404: return "Not Found";
        case 500: return "Internal Server Error";
        default: return "Unknown";
    }
}
```

---

## 5. CÓDIGOS DE ESTADO HTTP UTILIZADOS

### 5.1 Tabla de Códigos

| Código | Nombre                  | Descripción                                    | Uso en el Proyecto                    |
|--------|-------------------------|------------------------------------------------|---------------------------------------|
| 200    | OK                      | Solicitud procesada exitosamente               | GET, PUT, DELETE exitosos             |
| 201    | Created                 | Recurso creado exitosamente                    | POST exitoso                          |
| 400    | Bad Request             | Solicitud mal formada                          | JSON inválido, campos faltantes       |
| 404    | Not Found               | Recurso no encontrado                          | GET/PUT/DELETE de libro inexistente   |
| 500    | Internal Server Error   | Error del servidor                             | Errores de base de datos              |

### 5.2 Criterios de Uso

**200 OK:**
- GET exitoso de uno o varios libros
- PUT exitoso al actualizar un libro
- DELETE exitoso al eliminar un libro

**201 Created:**
- POST exitoso al crear un nuevo libro
- Se devuelve el objeto creado con su ID asignado

**400 Bad Request:**
- Cuerpo de solicitud vacío en POST/PUT
- JSON mal formado
- Campos requeridos ausentes
- Tipo de dato incorrecto (ej: year no numérico)

**404 Not Found:**
- GET de un libro con ID inexistente
- PUT intentando actualizar un libro inexistente
- DELETE intentando eliminar un libro inexistente
- Ruta no definida en el servidor

**500 Internal Server Error:**
- Errores de conexión con la base de datos
- SQLException no manejadas
- Errores inesperados del servidor

---

## 6. PRUEBAS REALIZADAS

### 6.1 Suite de Pruebas Completa

Se realizaron las siguientes pruebas exhaustivas:

**Prueba 1: GET /books - Listado completo**
- **Objetivo:** Verificar que se devuelven todos los libros
- **Resultado:** ✓ Exitoso - Array JSON con 3 libros
- **Código:** 200 OK

**Prueba 2: GET /books/1 - Libro específico**
- **Objetivo:** Obtener un libro por ID
- **Resultado:** ✓ Exitoso - Objeto JSON del libro
- **Código:** 200 OK

**Prueba 3: GET /books/999 - Libro inexistente**
- **Objetivo:** Verificar manejo de error
- **Resultado:** ✓ Exitoso - Mensaje de error
- **Código:** 404 Not Found

**Prueba 4: POST /books - Creación exitosa**
- **Objetivo:** Crear un nuevo libro
- **Entrada:**
```json
{
  "title": "La sombra del viento",
  "author": "Carlos Ruiz Zafón",
  "year": 2001
}
```
- **Resultado:** ✓ Exitoso - Libro creado con ID 4
- **Código:** 201 Created

**Prueba 5: POST /books - Sin cuerpo**
- **Objetivo:** Verificar validación de entrada
- **Resultado:** ✓ Exitoso - Error de validación
- **Código:** 400 Bad Request

**Prueba 6: POST /books - JSON inválido**
- **Objetivo:** Verificar parser JSON
- **Entrada:** `{invalid json}`
- **Resultado:** ✓ Exitoso - Error de formato
- **Código:** 400 Bad Request

**Prueba 7: PUT /books/1 - Actualización exitosa**
- **Objetivo:** Actualizar un libro existente
- **Entrada:**
```json
{
  "title": "Don Quijote de la Mancha",
  "author": "Miguel de Cervantes Saavedra",
  "year": 1605
}
```
- **Resultado:** ✓ Exitoso - Libro actualizado
- **Código:** 200 OK

**Prueba 8: PUT /books/999 - Libro inexistente**
- **Objetivo:** Verificar validación de existencia
- **Resultado:** ✓ Exitoso - Mensaje de error
- **Código:** 404 Not Found

**Prueba 9: DELETE /books/3 - Eliminación exitosa**
- **Objetivo:** Eliminar un libro
- **Resultado:** ✓ Exitoso - Libro eliminado
- **Código:** 200 OK

**Prueba 10: DELETE /books/999 - Libro inexistente**
- **Objetivo:** Verificar manejo de error
- **Resultado:** ✓ Exitoso - Mensaje de error
- **Código:** 404 Not Found

**Prueba 11: Ruta no existente**
- **Solicitud:** GET /authors
- **Resultado:** ✓ Exitoso - Error 404
- **Código:** 404 Not Found

### 6.2 Herramientas Utilizadas

**cURL:**
- Pruebas desde línea de comandos
- Verificación rápida de endpoints
- Automatización de tests

**Postman:**
- Interfaz gráfica para pruebas
- Colección de solicitudes organizadas
- Visualización de respuestas JSON

**MySQL Workbench:**
- Verificación directa de datos
- Comprobación de inserciones y actualizaciones
- Análisis de la estructura de la base de datos

### 6.3 Resultados de las Pruebas

Todas las pruebas se ejecutaron exitosamente. El servidor respondió correctamente a:
- ✓ Solicitudes válidas con datos correctos
- ✓ Solicitudes con IDs inexistentes
- ✓ Solicitudes mal formadas
- ✓ JSON inválido
- ✓ Rutas no definidas

---

## 7. MANEJO DE ERRORES

### 7.1 Estrategia de Manejo de Errores

El proyecto implementa un manejo de errores robusto en tres niveles:

**Nivel 1: Validación de Entrada**
```java
if (body == null || body.isEmpty()) {
    sendResponse(out, 400, 
        "{\"error\":\"Request body is required\"}", 
        "application/json");
    return;
}
```

**Nivel 2: Validación de Negocio**
```java
if (!dbManager.bookExists(id)) {
    sendResponse(out, 404, 
        "{\"error\":\"Book not found\"}", 
        "application/json");
    return;
}
```

**Nivel 3: Errores de Base de Datos**
```java
try {
    // Operaciones de base de datos
} catch (SQLException e) {
    System.err.println("Error de BD: " + e.getMessage());
    sendResponse(out, 500, 
        "Internal Server Error: " + e.getMessage(), 
        "text/plain");
}
```

### 7.2 Mensajes de Error

Todos los mensajes de error se devuelven en formato JSON:

```json
{"error": "Descripción del error"}
```

Excepto los errores 500 que pueden incluir información técnica adicional para debugging.

---

## 8. LIMITACIONES Y CONSIDERACIONES

### 8.1 Limitaciones del Proyecto

**1. Servidor Síncrono:**
- Procesa una solicitud a la vez
- No adecuado para alta concurrencia
- Bloquea otras conexiones mientras procesa

**2. Sin Autenticación:**
- Cualquiera puede acceder a todos los endpoints
- No hay control de permisos
- No apto para datos sensibles

**3. Parser JSON Básico:**
- Solo maneja JSON simple
- No soporta arrays anidados ni objetos complejos
- Sin validación de esquema

**4. Sin Pool de Conexiones:**
- Abre y cierra conexión en cada operación
- Menor rendimiento
- Mayor carga en la base de datos

**5. HTTP Simple:**
- No soporta HTTPS
- Sin compresión de respuestas
- Sin cache

**6. Manejo de Errores Básico:**
- Información de error limitada
- Sin logging estructurado
- Sin rastreo de errores

### 8.2 Casos de Uso Apropiados

Este servidor es apropiado para:
- ✓ Proyectos educativos
- ✓ Prototipos rápidos
- ✓ Pruebas de concepto
- ✓ Entornos de desarrollo local

No es apropiado para:
- ✗ Producción
- ✗ Alta concurrencia
- ✗ Datos sensibles
- ✗ Aplicaciones críticas

---

## 9. POSIBLES MEJORAS

### 9.1 Mejoras de Rendimiento

**1. Pool de Conexiones:**
```java
// Usar HikariCP o similar
HikariConfig config = new HikariConfig();
config.setJdbcUrl(DB_URL);
config.setUsername(DB_USER);
config.setPassword(DB_PASSWORD);
HikariDataSource ds = new HikariDataSource(config);
```

**2. Threading:**
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
while (true) {
    Socket client = serverSocket.accept();
    executor.submit(() -> handleClient(client));
}
```

**3. Caché:**
- Implementar cache en memoria para lecturas frecuentes
- Usar Redis o similar para cache distribuido

### 9.2 Mejoras de Funcionalidad

**1. Paginación:**
```
GET /books?page=1&size=10
```

**2. Filtros y Búsqueda:**
```
GET /books?author=Cervantes&year=1605
```

**3. Ordenamiento:**
```
GET /books?sort=year&order=desc
```

**4. Validación Avanzada:**
- Bean Validation (JSR 303)
- Validación de formatos (ISBN, fechas)
- Validación de rangos

### 9.3 Mejoras de Seguridad

**1. Autenticación:**
- JWT (JSON Web Tokens)
- OAuth 2.0
- Basic Authentication

**2. HTTPS:**
- Certificados SSL/TLS
- Encriptación de datos en tránsito

**3. Rate Limiting:**
- Limitar solicitudes por IP
- Prevenir abuso del API

**4. CORS:**
- Configurar Cross-Origin Resource Sharing
- Permitir solo orígenes autorizados

### 9.4 Mejoras de Calidad

**1. Logging:**
```java
Logger logger = LoggerFactory.getLogger(HttpServer.class);
logger.info("Solicitud recibida: {} {}", method, path);
logger.error("Error procesando solicitud", exception);
```

**2. Testing:**
- JUnit para pruebas unitarias
- Integration tests
- Cobertura de código

**3. Documentación:**
- Swagger/OpenAPI
- Javadoc completo
- Ejemplos de uso

---

## 10. CONCLUSIONES

### 10.1 Objetivos Alcanzados

Se cumplieron todos los objetivos del proyecto:

✓ **Servidor HTTP funcional** sin frameworks, implementado desde cero con sockets Java

✓ **Operaciones CRUD completas** para el recurso `/books`:
  - CREATE: POST /books
  - READ: GET /books y GET /books/{id}
  - UPDATE: PUT /books/{id}
  - DELETE: DELETE /books/{id}

✓ **Integración con MySQL** mediante JDBC con gestión apropiada de recursos

✓ **Respuestas HTTP válidas** con códigos de estado apropiados (200, 201, 400, 404, 500)

✓ **Manejo de errores robusto** en tres niveles: validación de entrada, validación de negocio y errores de base de datos

✓ **Serialización JSON** implementada manualmente sin bibliotecas externas

✓ **Prevención de SQL Injection** usando PreparedStatement

### 10.2 Aprendizajes Principales

**1. Protocolo HTTP:**
- Estructura de solicitudes y respuestas HTTP
- Importancia de las cabeceras (Content-Type, Content-Length)
- Códigos de estado y su significado

**2. Arquitectura de Aplicaciones:**
- Separación en capas (Presentación, Aplicación, Datos, Modelo)
- Responsabilidad única de cada clase
- Desacoplamiento de componentes

**3. Gestión de Recursos:**
- Importancia de cerrar conexiones y streams
- Patrón try-with-resources
- Prevención de memory leaks

**4. Seguridad:**
- Vulnerabilidades de SQL Injection
- Validación de entrada
- Escapado de caracteres especiales

**5. JDBC:**
- Manejo de conexiones a bases de datos
- PreparedStatement vs Statement
- Recuperación de IDs autogenerados

### 10.3 Valor Educativo del Proyecto

Este proyecto demuestra que es posible construir un servidor HTTP funcional sin frameworks, lo cual:

- **Profundiza el entendimiento** de cómo funcionan las tecnologías web por debajo
- **Aprecia el valor de los frameworks** al ver la complejidad que abstraen
- **Desarrolla habilidades de debugging** al trabajar a bajo nivel
- **Mejora la comprensión de sockets** y programación de red
- **Fortalece conocimientos de SQL** y gestión de bases de datos

### 10.4 Diferencias con Frameworks Modernos

**Lo que este proyecto NO tiene (pero Spring Boot sí):**
- Inyección de dependencias
- Configuración automática
- Pool de conexiones integrado
- Serialización JSON automática con Jackson
- Manejo de excepciones centralizado
- Logging configurable
- Métricas y monitoreo
- Testing integrado
- Documentación automática de API

**Por qué es importante entenderlo:**
- Frameworks como Spring Boot construyen sobre estos conceptos
- Debugging es más fácil cuando entiendes lo que pasa por debajo
- Mejor toma de decisiones arquitectónicas
- Capacidad de optimizar cuando sea necesario

### 10.5 Reflexión Final

La implementación de un servidor HTTP desde cero con sockets Java ha sido un ejercicio revelador que demuestra la complejidad inherente de las comunicaciones web. Aunque frameworks como Spring Boot simplifican enormemente este proceso, comprender los fundamentos permite:

1. **Apreciar el trabajo** que realizan los frameworks
2. **Diagnosticar problemas** más eficientemente
3. **Tomar mejores decisiones** de arquitectura
4. **Optimizar rendimiento** cuando es crítico
5. **Entender limitaciones** de las tecnologías

Este proyecto es un punto de partida excelente para comprender cómo evolucionan las aplicaciones desde implementaciones básicas hasta arquitecturas empresariales sofisticadas.

---

## 11. REFERENCIAS Y RECURSOS

### 11.1 Documentación Oficial

**Java SE:**
- Oracle Java Documentation: https://docs.oracle.com/javase/8/docs/
- java.net.Socket API: https://docs.oracle.com/javase/8/docs/api/java/net/Socket.html
- java.net.ServerSocket: https://docs.oracle.com/javase/8/docs/api/java/net/ServerSocket.html

**JDBC:**
- JDBC Tutorial: https://docs.oracle.com/javase/tutorial/jdbc/
- java.sql Package: https://docs.oracle.com/javase/8/docs/api/java/sql/package-summary.html
- PreparedStatement: https://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html

**MySQL:**
- MySQL Connector/J: https://dev.mysql.com/doc/connector-j/8.0/en/
- MySQL Documentation: https://dev.mysql.com/doc/

**HTTP Protocol:**
- RFC 2616 - HTTP/1.1: https://www.ietf.org/rfc/rfc2616.txt
- HTTP Status Codes: https://developer.mozilla.org/es/docs/Web/HTTP/Status

### 11.2 Herramientas Utilizadas

**Desarrollo:**
- Java Development Kit (JDK) 8+
- MySQL Server 8.0
- MySQL Connector/J 9.4.0
- IntelliJ IDEA / Eclipse / VS Code

**Pruebas:**
- cURL
- Postman
- MySQL Workbench

**Control de Versiones:**
- Git (recomendado para proyectos futuros)

### 11.3 Recursos Adicionales

**Tutoriales:**
- Oracle Java Tutorials: https://docs.oracle.com/javase/tutorial/
- Baeldung Java: https://www.baeldung.com/
- Java Network Programming by O'Reilly

**Mejores Prácticas:**
- Effective Java by Joshua Bloch
- Clean Code by Robert C. Martin
- OWASP Security Guidelines

---

## ANEXOS

### Anexo A: Comandos de Compilación y Ejecución

**Compilación (Windows):**
```bash
javac -cp ".;lib/mysql-connector-java-9.4.0.jar" *.java
```

**Compilación (Linux/Mac):**
```bash
javac -cp ".:lib/mysql-connector-java-9.4.0.jar" *.java
```

**Ejecución (Windows):**
```bash
java -cp ".;lib/mysql-connector-java-9.4.0.jar" HttpServer
```

**Ejecución (Linux/Mac):**
```bash
java -cp ".:lib/mysql-connector-java-9.4.0.jar" HttpServer
```

### Anexo B: Configuración de MySQL

**Crear usuario y otorgar privilegios:**
```sql
CREATE USER 'bookapi'@'localhost' IDENTIFIED BY 'password123';
GRANT ALL PRIVILEGES ON library_db.* TO 'bookapi'@'localhost';
FLUSH PRIVILEGES;
```

**Verificar conexión:**
```bash
mysql -u bookapi -p library_db
```

### Anexo C: Script Completo de Pruebas

```bash
#!/bin/bash
# Script de pruebas automatizado

echo "=== Prueba 1: GET todos los libros ==="
curl -X GET http://localhost:8080/books
echo -e "\n"

echo "=== Prueba 2: GET libro por ID ==="
curl -X GET http://localhost:8080/books/1
echo -e "\n"

echo "=== Prueba 3: POST nuevo libro ==="
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{"title":"Test Book","author":"Test Author","year":2024}'
echo -e "\n"

echo "=== Prueba 4: PUT actualizar libro ==="
curl -X PUT http://localhost:8080/books/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Updated Title","author":"Updated Author","year":2024}'
echo -e "\n"

echo "=== Prueba 5: DELETE libro ==="
curl -X DELETE http://localhost:8080/books/4
echo -e "\n"

echo "=== Prueba 6: Error 404 ==="
curl -X GET http://localhost:8080/books/999
echo -e "\n"

echo "Pruebas completadas"
```

### Anexo D: Troubleshooting

**Problema: "Driver MySQL no encontrado"**
- Solución: Verificar que mysql-connector-java.jar esté en lib/
- Verificar el classpath al compilar y ejecutar

**Problema: "Connection refused"**
- Solución: Verificar que MySQL esté ejecutándose
- Comando: `systemctl status mysql` (Linux) o Servicios (Windows)

**Problema: "Unknown database 'library_db'"**
- Solución: Ejecutar el script SQL de configuración
- Comando: `mysql -u root -p < setup.sql`

**Problema: "Access denied for user"**
- Solución: Verificar credenciales en DatabaseManager.java
- Verificar permisos del usuario en MySQL

**Problema: "Port 8080 already in use"**
- Solución: Cambiar el puerto en HttpServer.java
- O cerrar la aplicación que está usando el puerto 8080

---

**Fin del Documento**

---

**Autor:** Enrique Pérez García  
**Fecha de Elaboración:** Enero 2026  
**Versión:** 1.0  
**Institución:** IES El Rincón