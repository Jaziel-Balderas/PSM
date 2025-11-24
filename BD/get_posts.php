<?php
// Evitar que se muestren warnings o notices que rompan el JSON
error_reporting(E_ERROR | E_PARSE);
ini_set('display_errors', 0);

header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET");

require_once 'DBConnection.php';

$response = array("success" => false, "message" => "Error desconocido", "posts" => array(), "count" => 0);

try {
    if ($_SERVER['REQUEST_METHOD'] == 'GET') {
        $db = DBConnection::getInstance()->getConnection();
        
        if (!$db) {
            $response["message"] = "No se pudo conectar a la base de datos";
            echo json_encode($response);
            exit;
        }

        // Obtener parámetros opcionales
        $currentUserId = isset($_GET['current_user_id']) ? (int)$_GET['current_user_id'] : 0;
        $userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : null;
        $limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
        $offset = isset($_GET['offset']) ? (int)$_GET['offset'] : 0;

        // Query para obtener posts con BLOB (image_urls y profile_image_url son BLOB ahora)
        $query = "SELECT 
                    p.id,
                    p.user_id,
                    p.title,
                    p.description,
                    p.location,
                    p.image_urls,
                    p.is_public,
                    p.created_at,
                    u.nameuser,
                    u.lastnames,
                    u.username,
                    u.profile_image_url,
                    COUNT(DISTINCT l.id) as like_count,
                    MAX(CASE WHEN l.user_id = ? THEN 1 ELSE 0 END) as is_liked
                  FROM posts p
                  INNER JOIN users u ON p.user_id = u.user_id
                  LEFT JOIN likes l ON p.id = l.post_id
                  WHERE p.is_public = 1";
        
        // Si se especifica un user_id, filtrar por ese usuario
        if ($userId) {
            $query .= " AND p.user_id = ?";
        }
        
        // Agrupar por post y ordenar por más reciente primero
        $query .= " GROUP BY p.id, p.user_id, p.title, p.description, p.location, p.image_urls, p.is_public, p.created_at, u.nameuser, u.lastnames, u.username, u.profile_image_url 
                    ORDER BY p.created_at DESC 
                    LIMIT ? OFFSET ?";

        $stmt = $db->prepare($query);
        
        if (!$stmt) {
            $response["message"] = "Error al preparar consulta: " . $db->error;
            echo json_encode($response);
            exit;
        }
        
        // Bind de parámetros según si hay userId o no
        if ($userId) {
            $stmt->bind_param("iiii", $currentUserId, $userId, $limit, $offset);
        } else {
            $stmt->bind_param("iii", $currentUserId, $limit, $offset);
        }
        
        if (!$stmt->execute()) {
            $response["message"] = "Error al ejecutar consulta: " . $stmt->error;
            echo json_encode($response);
            $stmt->close();
            exit;
        }
        
        $result = $stmt->get_result();
        $posts = array();

        while ($row = $result->fetch_assoc()) {
            // Convertir BLOB de image_urls a array de base64
            $imageUrls = array();
            if ($row['image_urls']) {
                // El BLOB contiene JSON con array de base64
                $decoded = json_decode($row['image_urls'], true);
                if (is_array($decoded)) {
                    $imageUrls = $decoded;
                }
            }
            
            // Convertir BLOB de perfil a base64
            $profileImageBase64 = null;
            if ($row['profile_image_url']) {
                $profileImageBase64 = base64_encode($row['profile_image_url']);
            }
            
            $post = array(
                "postId" => (string)$row['id'],
                "userId" => (string)$row['user_id'],
                "username" => $row['username'] ? $row['username'] : "Usuario",
                "title" => $row['title'],
                "description" => $row['description'],
                "location" => $row['location'],
                "imageUrls" => $imageUrls,
                "isPublic" => (int)$row['is_public'] == 1,
                "createdAt" => $row['created_at'],
                "profileResId" => null,
                "profileImageBase64" => $profileImageBase64,
                "likeCount" => (int)$row['like_count'],
                "isLiked" => (int)$row['is_liked'] == 1
            );
            
            $posts[] = $post;
        }

        $response["success"] = true;
        $response["message"] = "Posts obtenidos exitosamente";
        $response["posts"] = $posts;
        $response["count"] = count($posts);
        
        $stmt->close();
        
    } else {
        $response["message"] = "Método no permitido (Usa GET).";
    }
} catch (Exception $e) {
    $response["success"] = false;
    $response["message"] = "Excepción: " . $e->getMessage();
}

echo json_encode($response);
?>
