-- Script SQL para crear/actualizar la tabla de posts con soporte para múltiples imágenes

-- Si ya existe la tabla posts, puedes ejecutar estas modificaciones:
ALTER TABLE posts 
ADD COLUMN title VARCHAR(255) NOT NULL AFTER user_id,
ADD COLUMN location VARCHAR(255) DEFAULT NULL,
MODIFY COLUMN image_url TEXT,
RENAME COLUMN image_url TO image_urls,
ADD COLUMN is_public TINYINT(1) DEFAULT 1;

-- O si prefieres crear la tabla desde cero:
/*
DROP TABLE IF EXISTS posts;

CREATE TABLE posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255) DEFAULT NULL,
    image_urls TEXT NOT NULL,
    is_public TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para mejorar el rendimiento
CREATE INDEX idx_user_id ON posts(user_id);
CREATE INDEX idx_created_at ON posts(created_at);
CREATE INDEX idx_is_public ON posts(is_public);
*/
