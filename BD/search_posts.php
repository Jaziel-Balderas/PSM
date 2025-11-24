<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) { echo json_encode($arr); exit(); }

$userId = isset($_GET['userId']) ? (int)$_GET['userId'] : 0;
$query = isset($_GET['query']) ? trim($_GET['query']) : '';
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
$offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

if ($userId <= 0) {
    respond(['success' => false, 'message' => 'userId es requerido']);
}

if ($query === '') {
    respond(['success' => false, 'message' => 'query es requerido']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    
    // Búsqueda por título, contenido (descripción) o nombre de usuario
    $searchParam = "%{$query}%";
    
    $sql = 'SELECT 
        p.post_id,
        p.user_id,
        p.title,
        p.content,
        p.location,
        p.is_public,
        p.created_at,
        p.updated_at,
        u.username,
        u.nameuser,
        u.lastnames,
        CASE WHEN u.profile_image_url IS NOT NULL 
            THEN TO_BASE64(u.profile_image_url) 
            ELSE NULL 
        END AS profile_image_base64,
        (SELECT COUNT(*) FROM post_votes WHERE post_id = p.post_id AND vote = 1) AS likes_count,
        (SELECT COUNT(*) FROM post_votes WHERE post_id = p.post_id AND vote = -1) AS dislikes_count,
        (SELECT COUNT(*) FROM post_comments WHERE post_id = p.post_id) AS comments_count,
        (SELECT vote FROM post_votes WHERE post_id = p.post_id AND user_id = ?) AS user_vote,
        (SELECT COUNT(*) FROM post_favorites WHERE post_id = p.post_id AND user_id = ?) AS is_favorite
    FROM posts p
    LEFT JOIN users u ON p.user_id = u.user_id
    WHERE p.is_public = 1
    AND (
        p.title LIKE ? 
        OR p.content LIKE ? 
        OR u.username LIKE ?
        OR u.nameuser LIKE ?
        OR CONCAT(u.nameuser, " ", u.lastnames) LIKE ?
    )
    ORDER BY p.created_at DESC
    LIMIT ? OFFSET ?';
    
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        error_log("Error preparando consulta: " . $conn->error);
        respond(['success' => false, 'message' => 'Error en la búsqueda']);
    }
    
    // 2 userId (ii) + 5 searchParam (sssss) + 2 limit/offset (ii) = 9 parámetros
    $stmt->bind_param('iisssssii', $userId, $userId, $searchParam, $searchParam, $searchParam, $searchParam, $searchParam, $limit, $offset);
    $stmt->execute();
    $result = $stmt->get_result();
    
    $posts = [];
    while ($row = $result->fetch_assoc()) {
        // Obtener imágenes del post
        $postId = (int)$row['post_id'];
        $imgStmt = $conn->prepare('SELECT image_id, TO_BASE64(image_data) AS base64 FROM post_images WHERE post_id = ?');
        $imgStmt->bind_param('i', $postId);
        $imgStmt->execute();
        $imgResult = $imgStmt->get_result();
        
        $images = [];
        while ($imgRow = $imgResult->fetch_assoc()) {
            $images[] = [
                'imageId' => (int)$imgRow['image_id'],
                'base64' => $imgRow['base64']
            ];
        }
        $imgStmt->close();
        
        // Mapear a camelCase para Kotlin (convertir post_id a string, content a description)
        $post = [
            'postId' => (string)$row['post_id'],
            'userId' => (string)$row['user_id'],
            'title' => $row['title'] ?? '',
            'description' => $row['content'] ?? '',  // Mapear content -> description
            'location' => $row['location'],
            'isPublic' => (bool)$row['is_public'],
            'createdAt' => $row['created_at'],
            'updatedAt' => $row['updated_at'],
            'username' => $row['username'] ?? 'Usuario',
            'nameuser' => $row['nameuser'] ?? '',
            'lastnames' => $row['lastnames'] ?? '',
            'profileImageBase64' => $row['profile_image_base64'],
            'likesCount' => (int)$row['likes_count'],
            'dislikesCount' => (int)$row['dislikes_count'],
            'commentsCount' => (int)$row['comments_count'],
            'userVote' => $row['user_vote'] !== null ? (int)$row['user_vote'] : null,
            'isFavorite' => (int)($row['is_favorite'] ?? 0) > 0,
            'images' => $images
        ];
        $posts[] = $post;
    }
    
    $stmt->close();
    
    error_log("search_posts.php: Búsqueda '$query' encontró " . count($posts) . " resultados");
    
    respond([
        'success' => true,
        'count' => count($posts),
        'posts' => $posts,
        'query' => $query
    ]);
    
} catch (Throwable $e) {
    error_log("Error en search_posts.php: " . $e->getMessage());
    respond(['success' => false, 'message' => 'Error en la búsqueda']);
}
?>
