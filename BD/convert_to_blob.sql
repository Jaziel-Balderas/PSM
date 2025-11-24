-- Script SQL para convertir tablas a usar BLOB
-- Ejecuta este script completo en phpMyAdmin

-- 1. Modificar tabla users - Cambiar profile_image_url de VARCHAR a MEDIUMBLOB
ALTER TABLE `users` 
MODIFY COLUMN `profile_image_url` MEDIUMBLOB DEFAULT NULL;

-- 2. Modificar tabla posts - Cambiar image_urls de TEXT a LONGBLOB
ALTER TABLE `posts` 
MODIFY COLUMN `image_urls` LONGBLOB DEFAULT NULL;

-- 3. Verificar los cambios
-- Descomenta las siguientes líneas para ver la estructura actualizada:
-- DESCRIBE users;
-- DESCRIBE posts;

-- NOTAS:
-- - MEDIUMBLOB: hasta 16MB (perfecto para fotos de perfil)
-- - LONGBLOB: hasta 4GB (perfecto para múltiples imágenes de posts)
-- - Los datos existentes se mantendrán, pero ahora las nuevas imágenes se guardarán como BLOB
