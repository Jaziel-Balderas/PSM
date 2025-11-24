<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) { echo json_encode($arr); exit(); }

$commentId = isset($_GET['comment_id']) ? (int)$_GET['comment_id'] : 0;

if ($commentId <= 0) {
    respond(['success' => false, 'message' => 'comment_id es requerido']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    
    // Obtener respuestas con información del usuario
    $sql = 'SELECT 
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
    WHERE r.comment_id = ?
    ORDER BY r.created_at ASC';
    
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        respond(['success' => false, 'message' => 'Error preparando consulta: ' . $conn->error]);
    }
    
    $stmt->bind_param('i', $commentId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $replies = [];
    while ($row = $result->fetch_assoc()) {
        // Mapear a camelCase para Kotlin
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
        $replies[] = $reply;
    }
    
    $stmt->close();
    
    respond([
        'success' => true,
        'count' => count($replies),
        'replies' => $replies
    ]);
    
} catch (Throwable $e) {
    respond(['success' => false, 'message' => 'Error obteniendo respuestas']);
}
?>
