-- Modificar tabla posts para usar BLOB en image_urls
-- Primero agregar nueva columna
ALTER TABLE posts 
ADD COLUMN images LONGBLOB DEFAULT NULL;

-- Para migrar datos existentes, necesitarás un script PHP
-- La columna image_urls se mantendrá temporalmente para compatibilidad
