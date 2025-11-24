<?php
// Versión simplificada sin likes para depuración
error_reporting(E_ERROR | E_PARSE);
ini_set('display_errors', 0);

header("Content-Type: application/json");
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

        // Query simple sin likes primero
        $query = "SELECT 
                    p.id,
                    p.user_id,
                    p.title,
                    p.description,
                    p.location,
                    p.image_urls,
                    p.is_public,
                    p.created_at,
                    u.username
                  FROM posts p
                  INNER JOIN users u ON p.user_id = u.user_id
                  WHERE p.is_public = 1
                  ORDER BY p.created_at DESC 
                  LIMIT 50";

        $result = $db->query($query);
        
        if (!$result) {
            $response["message"] = "Error en consulta: " . $db->error;
            echo json_encode($response);
            exit;
        }

        $posts = array();
        while ($row = $result->fetch_assoc()) {
            // Decodificar el JSON de image_urls
            $imageUrls = json_decode($row['image_urls'], true);
            if (!is_array($imageUrls)) {
                $imageUrls = array();
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
                "likeCount" => 0,
                "isLiked" => false
            );
            
            $posts[] = $post;
        }

        $response["success"] = true;
        $response["message"] = "Posts obtenidos exitosamente";
        $response["posts"] = $posts;
        $response["count"] = count($posts);
        
    } else {
        $response["message"] = "Método no permitido (Usa GET).";
    }
} catch (Exception $e) {
    $response["message"] = "Excepción: " . $e->getMessage();
}

echo json_encode($response);
?>
