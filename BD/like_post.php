<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'DBConnection.php';

// Obtener datos
$postId = isset($_POST['post_id']) ? intval($_POST['post_id']) : 0;
$userId = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;

// Validar datos
if ($postId <= 0 || $userId <= 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Datos inválidos'
    ]);
    exit;
}

try {
    $db = new DBConnection();
    $conn = $db->connect();
    
    if (!$conn) {
        throw new Exception('Error de conexión a la base de datos');
    }
    
    // Verificar si ya existe el like
    $checkSql = "SELECT id FROM likes WHERE post_id = ? AND user_id = ?";
    $checkStmt = $conn->prepare($checkSql);
    $checkStmt->bind_param("ii", $postId, $userId);
    $checkStmt->execute();
    $result = $checkStmt->get_result();
    
    if ($result->num_rows > 0) {
        // El like ya existe, eliminarlo (unlike)
        $deleteSql = "DELETE FROM likes WHERE post_id = ? AND user_id = ?";
        $deleteStmt = $conn->prepare($deleteSql);
        $deleteStmt->bind_param("ii", $postId, $userId);
        
        if ($deleteStmt->execute()) {
            // Obtener nuevo conteo
            $countSql = "SELECT COUNT(*) as count FROM likes WHERE post_id = ?";
            $countStmt = $conn->prepare($countSql);
            $countStmt->bind_param("i", $postId);
            $countStmt->execute();
            $countResult = $countStmt->get_result();
            $countRow = $countResult->fetch_assoc();
            
            echo json_encode([
                'success' => true,
                'message' => 'Like eliminado',
                'liked' => false,
                'likeCount' => intval($countRow['count'])
            ]);
        } else {
            throw new Exception('Error al eliminar el like');
        }
        
        $deleteStmt->close();
    } else {
        // El like no existe, agregarlo
        $insertSql = "INSERT INTO likes (post_id, user_id) VALUES (?, ?)";
        $insertStmt = $conn->prepare($insertSql);
        $insertStmt->bind_param("ii", $postId, $userId);
        
        if ($insertStmt->execute()) {
            // Obtener nuevo conteo
            $countSql = "SELECT COUNT(*) as count FROM likes WHERE post_id = ?";
            $countStmt = $conn->prepare($countSql);
            $countStmt->bind_param("i", $postId);
            $countStmt->execute();
            $countResult = $countStmt->get_result();
            $countRow = $countResult->fetch_assoc();
            
            echo json_encode([
                'success' => true,
                'message' => 'Like agregado',
                'liked' => true,
                'likeCount' => intval($countRow['count'])
            ]);
        } else {
            throw new Exception('Error al agregar el like');
        }
        
        $insertStmt->close();
    }
    
    $checkStmt->close();
    $conn->close();
    
} catch (Exception $e) {
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?>
