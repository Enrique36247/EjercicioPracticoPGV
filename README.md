# API HTTP con Sockets y MySQL - Guía de Instalación y Uso

## 📋 Requisitos Previos

- **Java JDK 8 o superior** instalado
- **MySQL Server** instalado y ejecutándose
- **MySQL Connector/J** (Driver JDBC para MySQL)
- **curl** o **Postman** para realizar pruebas

## 🔧 Configuración del Proyecto

### 1. Estructura del Proyecto

```
proyecto/
│
├── Book.java
├── DatabaseManager.java
├── HttpServer.java
├── setup.sql
└── lib/
    └── mysql-connector-java-8.0.33.jar
```

### 2. Descargar el Driver de MySQL

1. Descarga el MySQL Connector/J desde: https://dev.mysql.com/downloads/connector/j/
2. Extrae el archivo `.jar` y colócalo en la carpeta `lib/` del proyecto

### 3. Configurar la Base de Datos

```bash
# Conectar a MySQL
mysql -u root -p

# Ejecutar el script SQL
source setup.sql

# O copiar y pegar el contenido del archivo setup.sql
```

### 4. Configurar Credenciales de Base de Datos

Edita el archivo `DatabaseManager.java` y modifica las credenciales:

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db";
private static final String DB_USER = "root";        // Tu usuario
private static final String DB_PASSWORD = "tu_password"; // Tu contraseña
```

## 🚀 Compilación y Ejecución

### Compilar el Proyecto

```bash
# Windows
javac -cp ".;lib/mysql-connector-java-8.0.33.jar" *.java

# Linux/Mac
javac -cp ".:lib/mysql-connector-java-8.0.33.jar" *.java
```

### Ejecutar el Servidor

```bash
# Windows
java -cp ".;lib/mysql-connector-java-8.0.33.jar" HttpServer

# Linux/Mac
java -cp ".:lib/mysql-connector-java-8.0.33.jar" HttpServer
```

El servidor se iniciará en el puerto **8080**.

## 📡 Endpoints Disponibles

| Método | Endpoint | Descripción | Código Respuesta |
|--------|----------|-------------|------------------|
| GET | /books | Obtener todos los libros | 200 OK |
| GET | /books/{id} | Obtener un libro por ID | 200 OK / 404 Not Found |
| POST | /books | Crear un nuevo libro | 201 Created |
| PUT | /books/{id} | Actualizar un libro | 200 OK / 404 Not Found |
| DELETE | /books/{id} | Eliminar un libro | 200 OK / 404 Not Found |

## 🧪 Ejemplos de Uso

### 1. Obtener todos los libros

```bash
curl -X GET http://localhost:8080/books
```

**Respuesta:**
```json
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

### 2. Obtener un libro específico

```bash
curl -X GET http://localhost:8080/books/1
```

**Respuesta:**
```json
{
  "id": 1,
  "title": "El Quijote",
  "author": "Miguel de Cervantes",
  "year": 1605
}
```

### 3. Crear un nuevo libro

```bash
curl -X POST http://localhost:8080/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "La sombra del viento",
    "author": "Carlos Ruiz Zafón",
    "year": 2001
  }'
```

**Respuesta:**
```json
{
  "id": 4,
  "title": "La sombra del viento",
  "author": "Carlos Ruiz Zafón",
  "year": 2001
}
```

### 4. Actualizar un libro

```bash
curl -X PUT http://localhost:8080/books/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Don Quijote de la Mancha",
    "author": "Miguel de Cervantes Saavedra",
    "year": 1605
  }'
```

**Respuesta:**
```json
{
  "id": 1,
  "title": "Don Quijote de la Mancha",
  "author": "Miguel de Cervantes Saavedra",
  "year": 1605
}
```

### 5. Eliminar un libro

```bash
curl -X DELETE http://localhost:8080/books/3
```

**Respuesta:**
```json
{
  "message": "Book deleted successfully"
}
```

## 🔍 Solución de Problemas

### Error: "Driver MySQL no encontrado"
- Verifica que el archivo `.jar` del conector MySQL esté en la carpeta `lib/`
- Asegúrate de incluir el classpath al compilar y ejecutar

### Error: "Connection refused"
- Verifica que MySQL esté ejecutándose: `systemctl status mysql` (Linux) o desde los servicios (Windows)
- Comprueba las credenciales en `DatabaseManager.java`

### Error: "Unknown database 'library_db'"
- Ejecuta el script SQL para crear la base de datos

### Puerto 8080 en uso
- Cambia el puerto en `HttpServer.java` modificando la constante `PORT`

## 📚 Características Técnicas

- **Manejo de conexiones concurrentes**: El servidor acepta múltiples conexiones secuencialmente
- **Gestión de recursos**: Uso de `try-with-resources` para cerrar automáticamente conexiones
- **Parsing HTTP manual**: Implementación sin frameworks para entender el protocolo
- **Gestión de errores**: Manejo apropiado de excepciones SQL y de I/O
- **Respuestas HTTP estándar**: Códigos de estado y cabeceras correctas

## 📝 Notas Adicionales

- Este servidor es **síncrono** y maneja una conexión a la vez
- Para producción, se recomienda usar frameworks como Spring Boot
- El parser JSON es básico; para proyectos reales usar librerías como Jackson o Gson
- No hay autenticación implementada (no apto para producción)