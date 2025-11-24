<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) { echo json_encode($arr); exit(); }

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) {
    $data = $_POST;
}

$commentId = isset($data['comment_id']) ? (int)$data['comment_id'] : 0;
$userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;
$replyText = isset($data['reply_text']) ? trim($data['reply_text']) : '';

if ($commentId <= 0 || $userId <= 0 || $replyText === '') {
    respond(['success' => false, 'message' => 'comment_id, user_id y reply_text son requeridos']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    
    // Verificar que el comentario existe
    $checkComment = $conn->prepare('SELECT comment_id FROM post_comments WHERE comment_id = ?');
    $checkComment->bind_param('i', $commentId);
    $checkComment->execute();
    $resultComment = $checkComment->get_result();
    
    if ($resultComment->num_rows === 0) {
        $checkComment->close();
        respond(['success' => false, 'message' => 'El comentario no existe']);
    }
    $checkComment->close();
    
    // Insertar respuesta
    $stmt = $conn->prepare('INSERT INTO comment_replies (comment_id, user_id, reply_text, created_at) VALUES (?, ?, ?, NOW())');
    if (!$stmt) {
        respond(['success' => false, 'message' => 'Error preparando consulta: ' . $conn->error]);
    }
    
    $stmt->bind_param('iis', $commentId, $userId, $replyText);
    
    if ($stmt->execute()) {
        $replyId = $conn->insert_id;
        
        // Obtener la respuesta recién creada con información del usuario
        $getReply = $conn->prepare('SELECT 
            r.reply_id,
            r.comment_id,
            r.user_id,
            r.reply_text,
            r.created_at,
            u.username,
            u.nameuser,
            u.lastnames,
            CASE WHEN u.profile_image_url IS NOT NULL THEN TO_BASE64(u.profile_image_url) ELSE NULL END AS profile_image_base64
        FROM comment_replies r
        LEFT JOIN users u ON r.user_id = u.user_id
        WHERE r.reply_id = ?');
        
        $getReply->bind_param('i', $replyId);
        $getReply->execute();
        $result = $getReply->get_result();
        $row = $result->fetch_assoc();
        $getReply->close();
        
        // Mapear a camelCase
        $reply = [
            'replyId' => (int)$row['reply_id'],
            'commentId' => (int)$row['comment_id'],
            'userId' => (int)$row['user_id'],
            'replyText' => $row['reply_text'],
            'createdAt' => $row['created_at'],
            'username' => $row['username'],
            'nameuser' => $row['nameuser'],
            'lastnames' => $row['lastnames'] ?? '',
            'profileImageBase64' => $row['profile_image_base64']
        ];
        
        respond([
            'success' => true,
            'message' => 'Respuesta creada exitosamente',
            'reply' => $reply
        ]);
    } else {
        respond(['success' => false, 'message' => 'Error al crear respuesta: ' . $stmt->error]);
    }
    
    $stmt->close();
    
} catch (Throwable $e) {
    respond(['success' => false, 'message' => 'Error creando respuesta']);
}
?>
