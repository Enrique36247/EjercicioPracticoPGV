import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // Configuración de la base de datos
    private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String DB_USER = "root"; 
    private static final String DB_PASSWORD = ""; 

    // Obtener conexión a la base de datos
    public Connection getConnection() throws SQLException {
        try {
            // Cargar el driver de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL no encontrado", e);
        }
    }

    // Obtener todos los libros
    public List<Book> getAllBooks() throws SQLException {
        List<Book> books = new ArrayList<>();
        String query = "SELECT * FROM books";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                books.add(new Book(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getInt("year")
                ));
            }
        }
        return books;
    }

    // Obtener un libro por ID
    public Book getBookById(int id) throws SQLException {
        String query = "SELECT * FROM books WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Book(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("year")
                    );
                }
            }
        }
        return null;
    }

    // Crear un nuevo libro
    public Book createBook(Book book) throws SQLException {
        String query = "INSERT INTO books (title, author, year) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setInt(3, book.getYear());

            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("No se pudo crear el libro");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    book.setId(generatedKeys.getInt(1));
                    return book;
                } else {
                    throw new SQLException("No se pudo obtener el ID generado");
                }
            }
        }
    }

    // Actualizar un libro existente
    public boolean updateBook(int id, Book book) throws SQLException {
        String query = "UPDATE books SET title = ?, author = ?, year = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, book.getTitle());
            pstmt.setString(2, book.getAuthor());
            pstmt.setInt(3, book.getYear());
            pstmt.setInt(4, id);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    // Eliminar un libro
    public boolean deleteBook(int id) throws SQLException {
        String query = "DELETE FROM books WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    // Verificar si un libro existe
    public boolean bookExists(int id) throws SQLException {
        return getBookById(id) != null;
    }
}