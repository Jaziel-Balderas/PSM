<?php
header("Content-Type: application/json");
error_reporting(E_ERROR | E_PARSE);
ini_set('display_errors', 0);

require_once 'DBConnection.php';

$response = array("success" => false, "message" => "");

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $db = DBConnection::getInstance()->getConnection();
    
    if (!$db) {
        $response["message"] = "No se pudo conectar a la base de datos";
        echo json_encode($response);
        exit;
    }

    // Obtener datos del formulario
    $userId = isset($_POST['user_id']) ? $_POST['user_id'] : null;
    $title = isset($_POST['title']) ? $_POST['title'] : null;
    $description = isset($_POST['description']) ? $_POST['description'] : "";
    $location = isset($_POST['location']) ? $_POST['location'] : "";
    $isPublic = isset($_POST['is_public']) ? (int)$_POST['is_public'] : 1;

    if (!$userId || !$title) {
        $response["message"] = "Faltan campos obligatorios (user_id, title)";
        echo json_encode($response);
        exit;
    }

    // Procesar imágenes como base64
    $imagesArray = array();
    
    if (isset($_FILES['images']) && count($_FILES['images']['tmp_name']) > 0) {
        foreach ($_FILES['images']['tmp_name'] as $key => $tmpName) {
            if ($_FILES['images']['error'][$key] == UPLOAD_ERR_OK) {
                $imageData = file_get_contents($tmpName);
                $base64Image = base64_encode($imageData);
                $imagesArray[] = $base64Image;
            }
        }
    }
    
    // Convertir array de imágenes a JSON para guardar en BLOB
    $imagesJson = json_encode($imagesArray);

    // Insertar en la base de datos
    $query = "INSERT INTO posts (user_id, title, description, location, images, is_public) 
              VALUES (?, ?, ?, ?, ?, ?)";
    
    $stmt = $db->prepare($query);
    
    if ($stmt) {
        $stmt->bind_param("issssi", $userId, $title, $description, $location, $imagesJson, $isPublic);
        
        if ($stmt->execute()) {
            $postId = $stmt->insert_id;
            $response["success"] = true;
            $response["message"] = "Publicación creada exitosamente";
            $response["postId"] = (string)$postId;
        } else {
            $response["message"] = "Error al crear publicación: " . $stmt->error;
        }
        
        $stmt->close();
    } else {
        $response["message"] = "Error en la consulta: " . $db->error;
    }
    
} else {
    $response["message"] = "Método no permitido (Usa POST)";
}

echo json_encode($response);
?>
