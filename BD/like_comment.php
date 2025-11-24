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

if ($commentId <= 0 || $userId <= 0) {
    respond(['success' => false, 'message' => 'comment_id y user_id son requeridos']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    $conn->begin_transaction();
    
    // Verificar si ya dio like
    $checkLike = $conn->prepare('SELECT comment_id FROM comment_likes WHERE comment_id = ? AND user_id = ?');
    $checkLike->bind_param('ii', $commentId, $userId);
    $checkLike->execute();
    $resultLike = $checkLike->get_result();
    $alreadyLiked = $resultLike->num_rows > 0;
    $checkLike->close();
    
    if ($alreadyLiked) {
        // Quitar like (toggle)
        $deleteLike = $conn->prepare('DELETE FROM comment_likes WHERE comment_id = ? AND user_id = ?');
        $deleteLike->bind_param('ii', $commentId, $userId);
        $deleteLike->execute();
        $deleteLike->close();
        $message = 'Like removido';
    } else {
        // Agregar like
        $insertLike = $conn->prepare('INSERT INTO comment_likes (comment_id, user_id, created_at) VALUES (?, ?, NOW())');
        $insertLike->bind_param('ii', $commentId, $userId);
        $insertLike->execute();
        $insertLike->close();
        $message = 'Like agregado';
    }
    
    // Recalcular likes_count
    $countLikes = $conn->prepare('SELECT COUNT(*) AS total FROM comment_likes WHERE comment_id = ?');
    $countLikes->bind_param('i', $commentId);
    $countLikes->execute();
    $resultCount = $countLikes->get_result();
    $likesCount = $resultCount->fetch_assoc()['total'];
    $countLikes->close();
    
    // Actualizar likes_count en post_comments
    $updateCount = $conn->prepare('UPDATE post_comments SET likes_count = ? WHERE comment_id = ?');
    $updateCount->bind_param('ii', $likesCount, $commentId);
    $updateCount->execute();
    $updateCount->close();
    
    $conn->commit();
    
    respond([
        'success' => true,
        'message' => $message,
        'liked' => !$alreadyLiked,
        'likesCount' => (int)$likesCount,
        'commentId' => $commentId
    ]);
    
} catch (Throwable $e) {
    if (isset($conn)) {
        $conn->rollback();
    }
    respond(['success' => false, 'message' => 'Error procesando like']);
}
?>
