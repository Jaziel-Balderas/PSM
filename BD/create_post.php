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
error_log("==================== NEW REQUEST ====================");
error_log("DEBUG create_post.php - isJson: " . ($isJson ? 'true' : 'false'));
error_log("DEBUG create_post.php - Raw input length: " . strlen($raw));
error_log("DEBUG create_post.php - Content-Type: " . ($_SERVER['CONTENT_TYPE'] ?? 'not set'));
error_log("DEBUG create_post.php - Request Method: " . $_SERVER['REQUEST_METHOD']);
error_log("DEBUG create_post.php - POST count: " . count($_POST));
error_log("DEBUG create_post.php - POST keys: " . implode(', ', array_keys($_POST)));
error_log("DEBUG create_post.php - POST full data: " . print_r($_POST, true));
error_log("DEBUG create_post.php - FILES count: " . count($_FILES));
error_log("DEBUG create_post.php - FILES keys: " . implode(', ', array_keys($_FILES)));
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

if ($userId <= 0 || $content === '') {
  respond(['success'=>false,'message'=>'user_id y content son requeridos']);
}

try {
  $conn = DBConnection::getInstance()->getConnection();
  $conn->begin_transaction();

  $stmt = $conn->prepare('INSERT INTO posts (user_id, title, content, location, is_public) VALUES (?,?,?,?,?)');
  if (!$stmt) { throw new Exception('No se pudo preparar INSERT posts'); }
  $stmt->bind_param('isssi', $userId, $title, $content, $location, $isPublic);
  $stmt->execute();
  $postId = $conn->insert_id;
  $stmt->close();

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
  respond(['success'=>true,'message'=>'Publicación creada','post_id'=>$postId,'images'=>$imagesInserted]);
} catch (Throwable $e) {
  if (isset($conn)) { $conn->rollback(); }
  respond(['success'=>false,'message'=>'Error creando publicación']);
}
?>
