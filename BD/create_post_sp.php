<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr){ echo json_encode($arr); exit(); }

// Support JSON and multipart form
$raw = file_get_contents('php://input');
$json = json_decode($raw, true);
$isJson = is_array($json);
$data = $isJson ? $json : $_POST;

// Debug logging
error_log("==================== NEW REQUEST (SP VERSION) ====================");
error_log("DEBUG create_post_sp.php - isJson: " . ($isJson ? 'true' : 'false'));
error_log("DEBUG create_post_sp.php - Content-Type: " . ($_SERVER['CONTENT_TYPE'] ?? 'not set'));
error_log("DEBUG create_post_sp.php - POST keys: " . implode(', ', array_keys($_POST)));
if (!empty($_POST)) {
    foreach ($_POST as $key => $value) {
        error_log("  POST[$key] = " . (is_string($value) ? "'$value'" : json_encode($value)));
    }
}
error_log("====================================================");

$userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;
$title = isset($data['title']) ? trim((string)$data['title']) : null;
$content = isset($data['content']) ? trim((string)$data['content']) : '';
$location = isset($data['location']) ? trim((string)$data['location']) : null;
$isPublic = isset($data['is_public']) ? (int)$data['is_public'] : 1;

error_log("VALORES EXTRAÍDOS:");
error_log("  user_id: $userId (isset: " . (isset($data['user_id']) ? 'true' : 'false') . ", raw: '" . ($data['user_id'] ?? 'NULL') . "')");
error_log("  title: " . ($title ?? 'NULL'));
error_log("  content: '$content' (length: " . strlen($content) . ")");
error_log("  location: " . ($location ?? 'NULL'));
error_log("  is_public: $isPublic");

try {
  $conn = DBConnection::getInstance()->getConnection();
  $conn->begin_transaction();

  // Llamar al procedimiento almacenado
  $stmt = $conn->prepare('CALL sp_crear_post(?, ?, ?, ?, ?, @post_id, @success, @message)');
  if (!$stmt) { 
    throw new Exception('No se pudo preparar CALL sp_crear_post'); 
  }
  
  $stmt->bind_param('isssi', $userId, $title, $content, $location, $isPublic);
  $stmt->execute();
  $stmt->close();
  
  // Obtener los valores de salida
  $result = $conn->query('SELECT @post_id as post_id, @success as success, @message as message');
  $row = $result->fetch_assoc();
  
  $postId = (int)$row['post_id'];
  $success = (bool)$row['success'];
  $message = $row['message'];
  
  error_log("RESULTADO PROCEDIMIENTO: success=$success, post_id=$postId, message=$message");
  
  if (!$success || $postId <= 0) {
    $conn->rollback();
    error_log("❌ VALIDACIÓN FALLÓ: $message");
    respond(['success'=>false, 'message'=>$message]);
  }

  $imagesInserted = 0;

  // Handle images from multipart
  if (!$isJson && isset($_FILES['images']) && is_array($_FILES['images']['tmp_name'])) {
    $count = count($_FILES['images']['tmp_name']);
    for ($i=0; $i<$count; $i++) {
      if (is_uploaded_file($_FILES['images']['tmp_name'][$i])) {
        $bin = file_get_contents($_FILES['images']['tmp_name'][$i]);
        if ($bin !== false) {
          $img = $conn->prepare('INSERT INTO post_images (post_id, image_data) VALUES (?,?)');
          $null = null;
          $img->bind_param('ib', $postId, $null);
          $img->send_long_data(1, $bin);
          $img->execute();
          $img->close();
          $imagesInserted++;
        }
      }
    }
  }

  // Handle images from JSON base64 array
  if ($isJson && isset($json['images_base64']) && is_array($json['images_base64'])) {
    foreach ($json['images_base64'] as $b64) {
      if (!is_string($b64) || $b64 === '') continue;
      if (strpos($b64, ',') !== false) {
        $parts = explode(',', $b64, 2);
        $b64 = $parts[1];
      }
      $bin = base64_decode($b64, true);
      if ($bin === false) continue;
      $img = $conn->prepare('INSERT INTO post_images (post_id, image_data) VALUES (?,?)');
      $null = null;
      $img->bind_param('ib', $postId, $null);
      $img->send_long_data(1, $bin);
      $img->execute();
      $img->close();
      $imagesInserted++;
    }
  }

  $conn->commit();
  error_log("✅ PUBLICACIÓN CREADA: post_id=$postId, images=$imagesInserted");
  respond(['success'=>true,'message'=>'Publicación creada','post_id'=>$postId,'images'=>$imagesInserted]);
} catch (Throwable $e) {
  if (isset($conn)) { $conn->rollback(); }
  error_log("❌ ERROR CRÍTICO: " . $e->getMessage());
  respond(['success'=>false,'message'=>'Error creando publicación: ' . $e->getMessage()]);
}
?>
