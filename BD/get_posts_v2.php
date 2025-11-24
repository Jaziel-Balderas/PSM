<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) { echo json_encode($arr); exit(); }

// Accept params via GET or JSON body
$raw = file_get_contents('php://input');
$json = json_decode($raw, true);

$userId = isset($_GET['userId']) ? (int)$_GET['userId'] : 0;
if ($userId === 0 && isset($json['userId'])) $userId = (int)$json['userId'];

$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 20;
if (isset($json['limit'])) $limit = (int)$json['limit'];
if ($limit <= 0) $limit = 20;

$offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;
if (isset($json['offset'])) $offset = (int)$json['offset'];
if ($offset < 0) $offset = 0;

$conn = DBConnection::getInstance()->getConnection();

// Get posts with user vote information
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
    COALESCE(v.vote, 0) AS user_vote
  FROM posts p
  LEFT JOIN post_votes v ON p.post_id = v.post_id AND v.user_id = ?
  ORDER BY p.created_at DESC
  LIMIT ? OFFSET ?';

$stmt = $conn->prepare($sql);
if (!$stmt) {
    respond(['success'=>false,'message'=>'Error preparando consulta: '.$conn->error]);
}
$stmt->bind_param('iii', $userId, $limit, $offset);
$stmt->execute();
$result = $stmt->get_result();
$posts = [];
while ($row = $result->fetch_assoc()) {
    $posts[] = $row;
}
$stmt->close();

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
    $p['images'] = $images;
}
if ($imgStmt) $imgStmt->close();

respond([
    'success' => true,
    'count' => count($posts),
    'limit' => $limit,
    'offset' => $offset,
    'posts' => $posts
]);
?>
