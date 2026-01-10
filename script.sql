CREATE DATABASE IF NOT EXISTS library_db;

USE library_db;

CREATE TABLE IF NOT EXISTS books (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    year INT NOT NULL
);

INSERT INTO books (title, author, year) VALUES
('El Quijote', 'Miguel de Cervantes', 1605),
('Cien años de soledad', 'Gabriel García Márquez', 1967),
('1984', 'George Orwell', 1949);