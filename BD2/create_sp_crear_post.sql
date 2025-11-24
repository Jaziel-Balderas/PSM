USE psm2;

-- =====================
-- Stored Procedure: sp_crear_post
-- =====================
DELIMITER $$

DROP PROCEDURE IF EXISTS sp_crear_post$$

CREATE PROCEDURE sp_crear_post(
  IN p_user_id INT,
  IN p_title VARCHAR(255),
  IN p_content TEXT,
  IN p_location VARCHAR(255),
  IN p_is_public TINYINT,
  OUT p_post_id INT,
  OUT p_success BOOLEAN,
  OUT p_message VARCHAR(255)
)
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    SET p_success = FALSE;
    SET p_message = 'Error al crear la publicación';
    SET p_post_id = 0;
  END;
  
  -- Validar datos requeridos
  IF p_user_id IS NULL OR p_user_id <= 0 THEN
    SET p_success = FALSE;
    SET p_message = 'user_id es requerido y debe ser válido';
    SET p_post_id = 0;
  ELSEIF p_content IS NULL OR TRIM(p_content) = '' THEN
    SET p_success = FALSE;
    SET p_message = 'content es requerido';
    SET p_post_id = 0;
  ELSE
    START TRANSACTION;
    
    -- Insertar el post
    INSERT INTO posts (user_id, title, content, location, is_public)
    VALUES (p_user_id, p_title, p_content, p_location, IFNULL(p_is_public, 1));
    
    -- Obtener el ID generado
    SET p_post_id = LAST_INSERT_ID();
    
    COMMIT;
    
    SET p_success = TRUE;
    SET p_message = 'Publicación creada exitosamente';
  END IF;
END$$

DELIMITER ;

-- Verificar que se creó correctamente
SHOW PROCEDURE STATUS WHERE Name = 'sp_crear_post';
