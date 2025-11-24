<?php
ob_start();
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
require_once 'DBConnection.php';

function respond($arr) { ob_end_clean(); echo json_encode($arr); exit(); }

$userId = isset($_GET['userId']) ? (int)$_GET['userId'] : 0;
$query = isset($_GET['query']) ? trim($_GET['query']) : '';
$orderBy = isset($_GET['orderBy']) ? trim($_GET['orderBy']) : 'date'; // date, title, username
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
$offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

if ($userId <= 0) {
    respond(['success' => false, 'message' => 'userId es requerido']);
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    
    // Determinar el ORDER BY según el parámetro
    $orderClause = 'f.created_at DESC'; // Por defecto ordenar por fecha de favorito
    switch ($orderBy) {
        case 'title':
            $orderClause = 'p.title ASC';
            break;
        case 'username':
            $orderClause = 'u.username ASC';
            break;
        case 'date':
        default:
            $orderClause = 'f.created_at DESC';
            break;
    }
    
    // Construir la consulta base
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
        f.created_at AS favorited_at
    FROM post_favorites f
    INNER JOIN posts p ON f.post_id = p.post_id
    LEFT JOIN users u ON p.user_id = u.user_id
    WHERE f.user_id = ?';
    
    // Agregar filtro de búsqueda si existe
    $params = [$userId, $userId];
    $types = 'ii';
    
    if ($query !== '') {
        $searchParam = "%{$query}%";
        $sql .= ' AND (
            p.title LIKE ? 
            OR p.content LIKE ? 
            OR u.username LIKE ?
            OR u.nameuser LIKE ?
            OR CONCAT(u.nameuser, " ", u.lastnames) LIKE ?
        )';
        $params[] = $searchParam;
        $params[] = $searchParam;
        $params[] = $searchParam;
        $params[] = $searchParam;
        $params[] = $searchParam;
        $types .= 'sssss';
    }
    
    $sql .= " ORDER BY {$orderClause} LIMIT ? OFFSET ?";
    $params[] = $limit;
    $params[] = $offset;
    $types .= 'ii';
    
    $stmt = $conn->prepare($sql);
    if (!$stmt) {
        error_log("Error preparando consulta: " . $conn->error);
        respond(['success' => false, 'message' => 'Error al obtener favoritos']);
    }
    
    // Bind dinámico de parámetros
    $stmt->bind_param($types, ...$params);
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
        
        // Mapear a camelCase
        $post = [
            'postId' => (string)$row['post_id'],
            'userId' => (string)$row['user_id'],
            'title' => $row['title'] ?? '',
            'description' => $row['content'] ?? '',
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
            'isFavorite' => true, // Siempre true porque vienen de favoritos
            'favoritedAt' => $row['favorited_at'],
            'images' => $images
        ];
        $posts[] = $post;
    }
    
    $stmt->close();
    
    error_log("get_favorites.php: Usuario $userId tiene " . count($posts) . " favoritos (query='$query', orderBy='$orderBy')");
    
    respond([
        'success' => true,
        'count' => count($posts),
        'posts' => $posts,
        'orderBy' => $orderBy,
        'query' => $query
    ]);
    
} catch (Throwable $e) {
    error_log("Error en get_favorites.php: " . $e->getMessage());
    respond(['success' => false, 'message' => 'Error al obtener favoritos']);
}
?>
