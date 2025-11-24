-- Modificar tabla users para usar BLOB en profile_image
ALTER TABLE users 
MODIFY COLUMN profile_image_url MEDIUMBLOB DEFAULT NULL;

-- Renombrar columna para mejor claridad
ALTER TABLE users 
CHANGE COLUMN profile_image_url profile_image MEDIUMBLOB DEFAULT NULL;
