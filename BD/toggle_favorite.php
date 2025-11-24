<?php
ob_start();
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
require_once 'DBConnection.php';

function respond($arr) { ob_end_clean(); echo json_encode($arr); exit(); }

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) {
    $data = $_POST;
}

$postId = isset($data['post_id']) ? (int)$data['post_id'] : 0;
$userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;

if ($postId <= 0 || $userId <= 0) {
    respond(['success' => false, 'message' => 'post_id y user_id son requeridos']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    $conn->begin_transaction();
    
    // Verificar si ya está en favoritos
    $checkFav = $conn->prepare('SELECT favorite_id FROM post_favorites WHERE post_id = ? AND user_id = ?');
    $checkFav->bind_param('ii', $postId, $userId);
    $checkFav->execute();
    $resultFav = $checkFav->get_result();
    $alreadyFavorited = $resultFav->num_rows > 0;
    $checkFav->close();
    
    if ($alreadyFavorited) {
        // Quitar de favoritos
        $deleteFav = $conn->prepare('DELETE FROM post_favorites WHERE post_id = ? AND user_id = ?');
        $deleteFav->bind_param('ii', $postId, $userId);
        $deleteFav->execute();
        $deleteFav->close();
        $message = 'Eliminado de favoritos';
        $isFavorite = false;
    } else {
        // Agregar a favoritos
        $insertFav = $conn->prepare('INSERT INTO post_favorites (post_id, user_id, created_at) VALUES (?, ?, NOW())');
        $insertFav->bind_param('ii', $postId, $userId);
        $insertFav->execute();
        $insertFav->close();
        $message = 'Agregado a favoritos';
        $isFavorite = true;
    }
    
    $conn->commit();
    
    respond([
        'success' => true,
        'message' => $message,
        'isFavorite' => $isFavorite,
        'postId' => $postId
    ]);
    
} catch (Throwable $e) {
    if (isset($conn)) {
        $conn->rollback();
    }
    error_log("Error en toggle_favorite.php: " . $e->getMessage());
    respond(['success' => false, 'message' => 'Error al procesar favorito']);
}
?>
