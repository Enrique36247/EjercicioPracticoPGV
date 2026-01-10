import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;

public class HttpServer {
    private static final int PORT = 8080;
    private final DatabaseManager dbManager;

    public HttpServer() {
        this.dbManager = new DatabaseManager();
    }

    public void start() {
        System.out.println("Iniciando servidor HTTP en el puerto " + PORT + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor iniciado correctamente. Esperando conexiones...");
            
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error al manejar cliente: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream()
        ) {
            // Leer la línea de solicitud HTTP
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }

            System.out.println("Solicitud: " + requestLine);

            // Parsear método, ruta y versión HTTP
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) {
                sendResponse(out, 400, "Bad Request", "text/plain");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            // Leer las cabeceras
            String line;
            int contentLength = 0;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                }
            }

            // Leer el cuerpo si existe
            String body = null;
            if (contentLength > 0) {
                char[] bodyChars = new char[contentLength];
                in.read(bodyChars, 0, contentLength);
                body = new String(bodyChars);
            }

            // Procesar la solicitud
            handleRequest(out, method, path, body);

        } catch (IOException e) {
            System.err.println("Error de I/O: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar socket: " + e.getMessage());
            }
        }
    }

    private void handleRequest(OutputStream out, String method, String path, String body) throws IOException {
        try {
            if (path.equals("/books") && method.equals("GET")) {
                handleGetAllBooks(out);
            } else if (path.matches("/books/\\d+") && method.equals("GET")) {
                int id = extractId(path);
                handleGetBookById(out, id);
            } else if (path.equals("/books") && method.equals("POST")) {
                handleCreateBook(out, body);
            } else if (path.matches("/books/\\d+") && method.equals("PUT")) {
                int id = extractId(path);
                handleUpdateBook(out, id, body);
            } else if (path.matches("/books/\\d+") && method.equals("DELETE")) {
                int id = extractId(path);
                handleDeleteBook(out, id);
            } else {
                sendResponse(out, 404, "Not Found", "text/plain");
            }
        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
            sendResponse(out, 500, "Internal Server Error: " + e.getMessage(), "text/plain");
        }
    }

    private void handleGetAllBooks(OutputStream out) throws SQLException, IOException {
        List<Book> books = dbManager.getAllBooks();
        String json = booksToJson(books);
        sendResponse(out, 200, json, "application/json");
    }

    private void handleGetBookById(OutputStream out, int id) throws SQLException, IOException {
        Book book = dbManager.getBookById(id);
        if (book != null) {
            sendResponse(out, 200, book.toJson(), "application/json");
        } else {
            sendResponse(out, 404, "{\"error\":\"Book not found\"}", "application/json");
        }
    }

    private void handleCreateBook(OutputStream out, String body) throws SQLException, IOException {
        if (body == null || body.isEmpty()) {
            sendResponse(out, 400, "{\"error\":\"Request body is required\"}", "application/json");
            return;
        }

        try {
            Book book = parseBookFromJson(body);
            Book createdBook = dbManager.createBook(book);
            sendResponse(out, 201, createdBook.toJson(), "application/json");
        } catch (IllegalArgumentException e) {
            sendResponse(out, 400, "{\"error\":\"Invalid JSON format: " + e.getMessage() + "\"}", "application/json");
        }
    }

    private void handleUpdateBook(OutputStream out, int id, String body) throws SQLException, IOException {
        if (body == null || body.isEmpty()) {
            sendResponse(out, 400, "{\"error\":\"Request body is required\"}", "application/json");
            return;
        }

        if (!dbManager.bookExists(id)) {
            sendResponse(out, 404, "{\"error\":\"Book not found\"}", "application/json");
            return;
        }

        try {
            Book book = parseBookFromJson(body);
            dbManager.updateBook(id, book);
            book.setId(id);
            sendResponse(out, 200, book.toJson(), "application/json");
        } catch (IllegalArgumentException e) {
            sendResponse(out, 400, "{\"error\":\"Invalid JSON format: " + e.getMessage() + "\"}", "application/json");
        }
    }

    private void handleDeleteBook(OutputStream out, int id) throws SQLException, IOException {
        boolean deleted = dbManager.deleteBook(id);
        if (deleted) {
            sendResponse(out, 200, "{\"message\":\"Book deleted successfully\"}", "application/json");
        } else {
            sendResponse(out, 404, "{\"error\":\"Book not found\"}", "application/json");
        }
    }

    private void sendResponse(OutputStream out, int statusCode, String body, String contentType) throws IOException {
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

    private int extractId(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private String booksToJson(List<Book> books) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < books.size(); i++) {
            json.append(books.get(i).toJson());
            if (i < books.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    private Book parseBookFromJson(String json) {
        // Parser JSON simple (sin bibliotecas externas)
        String title = extractJsonValue(json, "title");
        String author = extractJsonValue(json, "author");
        String yearStr = extractJsonValue(json, "year");

        if (title == null || author == null || yearStr == null) {
            throw new IllegalArgumentException("Missing required fields");
        }

        try {
            int year = Integer.parseInt(yearStr);
            return new Book(title, author, year);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Year must be a number");
        }
    }

    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"?([^\",}]+)\"?";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.start();
    }
}