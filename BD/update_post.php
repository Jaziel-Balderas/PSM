<?php
// Evitar cualquier salida antes del JSON
ob_start();

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'DBConnection.php';

$response = array('success' => false, 'message' => '');

try {
    // Validar método POST
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        throw new Exception('Método no permitido');
    }
    
    // Obtener datos del POST
    $post_id = isset($_POST['post_id']) ? intval($_POST['post_id']) : 0;
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    $title = isset($_POST['title']) && $_POST['title'] !== '' ? trim($_POST['title']) : '';
    $content = isset($_POST['content']) ? trim($_POST['content']) : '';
    $location = isset($_POST['location']) && $_POST['location'] !== '' ? trim($_POST['location']) : '';
    $is_public = isset($_POST['is_public']) ? intval($_POST['is_public']) : 1;
    
    // Log para debugging
    error_log("update_post.php - Received: post_id=$post_id, user_id=$user_id, title='$title', content length=" . strlen($content) . ", location='$location', is_public=$is_public");
    
    // Validaciones
    if ($post_id <= 0) {
        throw new Exception('ID de publicación inválido');
    }
    
    if ($user_id <= 0) {
        throw new Exception('Usuario no autenticado');
    }
    
    if (empty($content)) {
        throw new Exception('El contenido es obligatorio');
    }
    
    // Conectar a la base de datos usando el patrón Singleton
    $conn = DBConnection::getInstance()->getConnection();
    
    // Verificar que el post pertenece al usuario
    $checkSql = "SELECT user_id FROM posts WHERE post_id = ? LIMIT 1";
    $checkStmt = $conn->prepare($checkSql);
    $checkStmt->bind_param('i', $post_id);
    $checkStmt->execute();
    $checkResult = $checkStmt->get_result();
    
    if ($checkResult->num_rows === 0) {
        throw new Exception('Publicación no encontrada');
    }
    
    $postData = $checkResult->fetch_assoc();
    if ($postData['user_id'] != $user_id) {
        throw new Exception('No tienes permisos para editar esta publicación');
    }
    
    $checkStmt->close();
    
    // Iniciar transacción
    $conn->begin_transaction();
    
    try {
        // Actualizar el post (solo texto, sin imágenes)
        $updateSql = "UPDATE posts 
                      SET title = ?, 
                          content = ?, 
                          location = ?, 
                          is_public = ?
                      WHERE post_id = ? AND user_id = ?";
        
        $updateStmt = $conn->prepare($updateSql);
        
        if (!$updateStmt) {
            throw new Exception('Error preparando consulta: ' . $conn->error);
        }
        
        $updateStmt->bind_param('sssiii', $title, $content, $location, $is_public, $post_id, $user_id);
        
        if (!$updateStmt->execute()) {
            throw new Exception('Error al actualizar la publicación: ' . $updateStmt->error);
        }
        
        if ($updateStmt->affected_rows === 0) {
            throw new Exception('No se pudo actualizar la publicación. Verifica que tengas permisos.');
        }
        
        // NO permitir actualizar imágenes (como solicitaste)
        // Las imágenes permanecen sin cambios
        
        // Commit transacción
        $conn->commit();
        
        $response['success'] = true;
        $response['message'] = 'Publicación actualizada exitosamente';
        $response['post_id'] = $post_id;
        
        $updateStmt->close();
        
    } catch (Exception $e) {
        $conn->rollback();
        throw $e;
    }
    
    $conn->close();
    
} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = $e->getMessage();
    error_log("Error en update_post.php: " . $e->getMessage());
}

// Limpiar buffer y enviar JSON
ob_end_clean();
echo json_encode($response);
exit;
?>
