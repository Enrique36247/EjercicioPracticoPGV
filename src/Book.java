public class Book {
    private int id;
    private String title;
    private String author;
    private int year;

    // Constructor completo
    public Book(int id, String title, String author, int year) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.year = year;
    }

    // Constructor sin ID (para creación)
    public Book(String title, String author, int year) {
        this.title = title;
        this.author = author;
        this.year = year;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    // Convertir a JSON manualmente
    public String toJson() {
        return String.format(
            "{\"id\":%d,\"title\":\"%s\",\"author\":\"%s\",\"year\":%d}",
            id, escapeJson(title), escapeJson(author), year
        );
    }

    // Escapar caracteres especiales en JSON
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    @Override
    public String toString() {
        return "Book{id=" + id + ", title='" + title + "', author='" + author + "', year=" + year + "}";
    }
}