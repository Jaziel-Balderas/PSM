<?php
// Evitar cualquier salida antes del JSON
ob_start();

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'DBConnection.php';

function respond($arr) { 
    ob_end_clean();
    echo json_encode($arr); 
    exit(); 
}

// Accept params via GET or JSON body
$raw = file_get_contents('php://input');
$json = json_decode($raw, true);

$userId = isset($_GET['userId']) ? (int)$_GET['userId'] : 0;
if ($userId === 0 && isset($json['userId'])) $userId = (int)$json['userId'];

$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 100;
if (isset($json['limit'])) $limit = (int)$json['limit'];
if ($limit <= 0) $limit = 100;

$offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
if (isset($json['offset'])) $offset = (int)$json['offset'];
if ($offset < 0) $offset = 0;

$conn = DBConnection::getInstance()->getConnection();

// Get posts with user vote information, comments count, favorites status, and user profile
$sql = 'SELECT 
    p.post_id,
    p.user_id,
    p.title,
    p.content,
    p.location,
    p.is_public,
    p.likes_count,
    p.dislikes_count,
    p.created_at,
    COALESCE(v.vote, 0) AS user_vote,
    (SELECT COUNT(*) FROM post_comments WHERE post_id = p.post_id) AS comments_count,
    (SELECT COUNT(*) FROM post_favorites WHERE post_id = p.post_id AND user_id = ?) AS is_favorite,
    u.username,
    u.nameuser,
    u.lastnames,
    CASE WHEN u.profile_image_url IS NOT NULL THEN TO_BASE64(u.profile_image_url) ELSE NULL END AS profile_image_base64
  FROM posts p
  LEFT JOIN post_votes v ON p.post_id = v.post_id AND v.user_id = ?
  LEFT JOIN users u ON p.user_id = u.user_id
  ORDER BY p.created_at DESC
  LIMIT ? OFFSET ?';

$stmt = $conn->prepare($sql);
if (!$stmt) {
    respond(['success'=>false,'message'=>'Error preparando consulta: '.$conn->error]);
}
$stmt->bind_param('iiii', $userId, $userId, $limit, $offset);
$stmt->execute();
$result = $stmt->get_result();
$posts = [];
while ($row = $result->fetch_assoc()) {
    $posts[] = $row;
}
$stmt->close();

error_log("get_posts_v2: userId=$userId, limit=$limit, offset=$offset, found " . count($posts) . " posts");

// For each post, fetch images
$imgStmt = $conn->prepare('SELECT image_id, description, TO_BASE64(image_data) AS image_base64 FROM post_images WHERE post_id = ? ORDER BY image_id');
foreach ($posts as &$p) {
    $pId = (int)$p['post_id'];
    $images = [];
    if ($imgStmt) {
        $imgStmt->bind_param('i', $pId);
        $imgStmt->execute();
        $imgRes = $imgStmt->get_result();
        while ($iRow = $imgRes->fetch_assoc()) {
            $images[] = [
                'imageId' => (int)$iRow['image_id'],
                'description' => $iRow['description'],
                'base64' => $iRow['image_base64']
            ];
        }
    }
    
    // Mapear campos a camelCase para Kotlin
    $p['postId'] = (string)$p['post_id'];
    $p['userId'] = (string)$p['user_id'];
    $p['username'] = $p['username'] ?? 'Usuario';
    $p['nameuser'] = $p['nameuser'] ?? '';
    $p['lastnames'] = $p['lastnames'] ?? '';
    $p['profileImageBase64'] = $p['profile_image_base64'];
    $p['description'] = $p['content'] ?? '';  // Mapear content -> description
    $p['isPublic'] = (bool)$p['is_public'];
    $p['likesCount'] = (int)$p['likes_count'];
    $p['dislikesCount'] = (int)$p['dislikes_count'];
    $p['commentsCount'] = (int)($p['comments_count'] ?? 0);
    $p['createdAt'] = $p['created_at'];
    $p['userVote'] = (int)$p['user_vote'] === 0 ? null : (int)$p['user_vote'];
    $p['isFavorite'] = (int)($p['is_favorite'] ?? 0) > 0;
    $p['images'] = $images;
    
    // Eliminar campos snake_case antiguos
    unset($p['post_id'], $p['user_id'], $p['content'], $p['is_public'], 
          $p['likes_count'], $p['dislikes_count'], $p['created_at'], $p['user_vote'], 
          $p['comments_count'], $p['profile_image_base64'], $p['is_favorite']);
}
if ($imgStmt) $imgStmt->close();

// Limpiar buffer y enviar respuesta
ob_end_clean();
echo json_encode([
    'success' => true,
    'count' => count($posts),
    'limit' => $limit,
    'offset' => $offset,
    'posts' => $posts
]);
exit;
?>
