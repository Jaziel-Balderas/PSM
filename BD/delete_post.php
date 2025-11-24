<?php
// Evitar cualquier salida antes del JSON
ob_start();

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'DBConnection.php';

$response = ['success' => false, 'message' => ''];

try {
    // Verificar método POST
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
        throw new Exception('Método no permitido');
    }
    
    // Obtener datos
    $post_id = isset($_POST['post_id']) ? intval($_POST['post_id']) : 0;
    $user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
    
    // Log para debugging
    error_log("delete_post.php - Received: post_id=$post_id, user_id=$user_id");
    
    // Validaciones
    if ($post_id <= 0) {
        throw new Exception('ID de publicación inválido');
    }
    
    if ($user_id <= 0) {
        throw new Exception('Usuario no autenticado');
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
        throw new Exception('No tienes permisos para eliminar esta publicación');
    }
    
    $checkStmt->close();
    
    // Eliminar la publicación (las imágenes se eliminan por CASCADE)
    $deleteSql = "DELETE FROM posts WHERE post_id = ? AND user_id = ?";
    $deleteStmt = $conn->prepare($deleteSql);
    
    if (!$deleteStmt) {
        throw new Exception('Error preparando consulta: ' . $conn->error);
    }
    
    $deleteStmt->bind_param('ii', $post_id, $user_id);
    
    if (!$deleteStmt->execute()) {
        throw new Exception('Error al eliminar la publicación: ' . $deleteStmt->error);
    }
    
    if ($deleteStmt->affected_rows === 0) {
        throw new Exception('No se pudo eliminar la publicación. Verifica que exista y tengas permisos.');
    }
    
    error_log("delete_post.php - Post deleted successfully: post_id=$post_id");
    
    $deleteStmt->close();
    $conn->close();
    
    $response['success'] = true;
    $response['message'] = 'Publicación eliminada exitosamente';
    
} catch (Exception $e) {
    $response['message'] = $e->getMessage();
    error_log("Error en delete_post.php: " . $e->getMessage());
}

// Limpiar buffer y enviar JSON
ob_end_clean();
echo json_encode($response);
exit;
?>
