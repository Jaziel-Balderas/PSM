-- =============================================
-- Crear tabla post_favorites
-- Ejecutar en la base de datos psm2
-- =============================================

USE psm2;

-- Eliminar tabla si existe (cuidado con datos existentes)
DROP TABLE IF EXISTS post_favorites;

-- Crear tabla post_favorites
CREATE TABLE post_favorites (
  favorite_id INT AUTO_INCREMENT PRIMARY KEY,
  post_id INT NOT NULL,
  user_id INT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY unique_favorite (post_id, user_id),
  CONSTRAINT fk_favorites_post FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
  CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Crear índices para optimizar consultas
CREATE INDEX idx_favorites_post ON post_favorites(post_id);
CREATE INDEX idx_favorites_user ON post_favorites(user_id);
CREATE INDEX idx_favorites_created ON post_favorites(created_at);

-- Verificar que la tabla se creó correctamente
SELECT 'Tabla post_favorites creada exitosamente' AS resultado;
SHOW CREATE TABLE post_favorites;
