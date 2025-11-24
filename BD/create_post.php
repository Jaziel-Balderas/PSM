<?php
header("Content-Type: application/json");
error_reporting(E_ERROR | E_PARSE);
ini_set('display_errors', 0);

require_once 'DBConnection.php';

$response = array("success" => false, "message" => "Error desconocido");

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $db = DBConnection::getInstance()->getConnection();

    // 1. Recibir datos de texto
    $userId = isset($_POST['user_id']) ? $_POST['user_id'] : null;
    $title = isset($_POST['title']) ? $_POST['title'] : "";
    $description = isset($_POST['description']) ? $_POST['description'] : "";
    $location = isset($_POST['location']) ? $_POST['location'] : "";
    $isPublic = isset($_POST['is_public']) ? (int)$_POST['is_public'] : 1;

    if ($userId && !empty($title)) {
        // 2. Convertir imágenes a base64 para guardar en BLOB
        $imagesBase64 = array();
        
        // Verificar si se enviaron múltiples archivos
        if (isset($_FILES['images']) && is_array($_FILES['images']['name'])) {
            $fileCount = count($_FILES['images']['name']);
            
            for ($i = 0; $i < $fileCount; $i++) {
                // Verificar si este archivo específico no tiene errores
                if ($_FILES['images']['error'][$i] === UPLOAD_ERR_OK) {
                    // Leer el contenido del archivo
                    $imageData = file_get_contents($_FILES['images']['tmp_name'][$i]);
                    
                    // Convertir a base64
                    $base64Image = base64_encode($imageData);
                    
                    // Agregar al array
                    $imagesBase64[] = $base64Image;
                }
            }
            
            if (empty($imagesBase64)) {
                $response["message"] = "Error al procesar las imágenes.";
                echo json_encode($response);
                exit();
            }
        } else {
            $response["message"] = "No se recibieron archivos.";
            echo json_encode($response);
            exit();
        }

        // 3. Convertir array de base64 a JSON para guardar en BLOB
        $imagesJsonBlob = json_encode($imagesBase64);

        // 4. Insertar en Base de Datos con BLOB
        $stmt = $db->prepare("INSERT INTO posts (user_id, title, description, location, image_urls, is_public, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())");
        
        if ($stmt) {
            $stmt->bind_param("issssi", $userId, $title, $description, $location, $imagesJsonBlob, $isPublic);

            if ($stmt->execute()) {
                $postId = $stmt->insert_id;
                $response["success"] = true;
                $response["message"] = "Publicación creada exitosamente con BLOB";
                $response["postId"] = (string)$postId;
            } else {
                $response["message"] = "Error al guardar en BD: " . $stmt->error;
            }
            $stmt->close();
        } else {
            $response["message"] = "Error en la consulta SQL: " . $db->error;
        }

    } else {
        $response["message"] = "Faltan datos obligatorios (user_id o título).";
    }

} else {
    $response["message"] = "Método no permitido (Usa POST).";
}

echo json_encode($response);
?>