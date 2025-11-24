<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) { echo json_encode($arr); exit(); }

$postId = isset($_GET['post_id']) ? (int)$_GET['post_id'] : 0;
$userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;

if ($postId <= 0) {
    respond(['success' => false, 'message' => 'post_id es requerido']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    
    // Obtener comentarios con información del usuario
    $sql = 'SELECT 
        c.comment_id,
        c.post_id,
        c.user_id,
        c.comment_text,
        c.likes_count,
        c.created_at,
        c.updated_at,
        u.username,
        u.nameuser,
        (SELECT COUNT(*) FROM comment_likes WHERE comment_id = c.comment_id AND user_id = ?) AS user_liked,
        (SELECT COUNT(*) FROM comment_replies WHERE comment_id = c.comment_id) AS replies_count
    FROM post_comments c
    LEFT JOIN users u ON c.user_id = u.user_id
    WHERE c.post_id = ?
    ORDER BY c.created_at ASC';
    
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        respond(['success' => false, 'message' => 'Error preparando consulta: ' . $conn->error]);
    }
    
    $stmt->bind_param('ii', $userId, $postId);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $comments = [];
    while ($row = $result->fetch_assoc()) {
        $row['user_liked'] = (int)$row['user_liked'] > 0;
        $row['replies_count'] = (int)$row['replies_count'];
        $comments[] = $row;
    }
    
    $stmt->close();
    
    respond([
        'success' => true,
        'count' => count($comments),
        'comments' => $comments
    ]);
    
} catch (Throwable $e) {
    respond(['success' => false, 'message' => 'Error obteniendo comentarios']);
}
?>
