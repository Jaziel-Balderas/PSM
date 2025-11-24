USE psm_db;

-- Tabla de borradores de publicaciones
CREATE TABLE IF NOT EXISTS drafts (
    draft_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title TEXT,
    content TEXT,
    image_uris TEXT,
    location VARCHAR(255),
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índice para búsquedas rápidas por usuario
CREATE INDEX IF NOT EXISTS idx_user_drafts ON drafts(user_id, updated_at DESC);

SELECT 'Tabla drafts creada exitosamente' AS resultado;
SHOW TABLES;
DESCRIBE drafts;
