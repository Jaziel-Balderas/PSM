<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) { echo json_encode($arr); exit(); }

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) {
    $data = $_POST;
}

$postId = isset($data['post_id']) ? (int)$data['post_id'] : 0;
$userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;
$commentText = isset($data['comment_text']) ? trim($data['comment_text']) : '';

if ($postId <= 0 || $userId <= 0 || $commentText === '') {
    respond(['success' => false, 'message' => 'post_id, user_id y comment_text son requeridos']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    
    // Verificar que el post existe
    $checkPost = $conn->prepare('SELECT post_id FROM posts WHERE post_id = ?');
    $checkPost->bind_param('i', $postId);
    $checkPost->execute();
    $resultPost = $checkPost->get_result();
    
    if ($resultPost->num_rows === 0) {
        $checkPost->close();
        respond(['success' => false, 'message' => 'El post no existe']);
    }
    $checkPost->close();
    
    // Insertar comentario
    $stmt = $conn->prepare('INSERT INTO post_comments (post_id, user_id, comment_text, created_at) VALUES (?, ?, ?, NOW())');
    if (!$stmt) {
        respond(['success' => false, 'message' => 'Error preparando consulta: ' . $conn->error]);
    }
    
    $stmt->bind_param('iis', $postId, $userId, $commentText);
    
    if ($stmt->execute()) {
        $commentId = $conn->insert_id;
        
        // Obtener el comentario recién creado con información del usuario
        $getComment = $conn->prepare('SELECT 
            c.comment_id,
            c.post_id,
            c.user_id,
            c.comment_text,
            c.likes_count,
            c.created_at,
            c.updated_at,
            u.username,
            u.nameuser,
            u.lastnames,
            CASE WHEN u.profile_image_url IS NOT NULL THEN TO_BASE64(u.profile_image_url) ELSE NULL END AS profile_image_base64
        FROM post_comments c
        LEFT JOIN users u ON c.user_id = u.user_id
        WHERE c.comment_id = ?');
        
        $getComment->bind_param('i', $commentId);
        $getComment->execute();
        $result = $getComment->get_result();
        $row = $result->fetch_assoc();
        $getComment->close();
        
        // Mapear a camelCase
        $comment = [
            'commentId' => (int)$row['comment_id'],
            'postId' => (int)$row['post_id'],
            'userId' => (int)$row['user_id'],
            'commentText' => $row['comment_text'],
            'likesCount' => (int)$row['likes_count'],
            'createdAt' => $row['created_at'],
            'updatedAt' => $row['updated_at'],
            'username' => $row['username'],
            'nameuser' => $row['nameuser'],
            'lastnames' => $row['lastnames'] ?? '',
            'profileImageBase64' => $row['profile_image_base64']
        ];
        
        respond([
            'success' => true,
            'message' => 'Comentario creado exitosamente',
            'comment' => $comment
        ]);
    } else {
        respond(['success' => false, 'message' => 'Error al crear comentario: ' . $stmt->error]);
    }
    
    $stmt->close();
    
} catch (Throwable $e) {
    respond(['success' => false, 'message' => 'Error creando comentario']);
}
?>
