<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr){ echo json_encode($arr); exit(); }

// Accept JSON or multipart
$raw = file_get_contents('php://input');
$json = json_decode($raw, true);
$isJson = is_array($json);
$data = $isJson ? $json : $_POST;

$userId   = isset($data['user_id']) ? (int)$data['user_id'] : 0;
$username = isset($data['username']) ? trim((string)$data['username']) : '';

if ($userId <= 0 && $username === '') {
  respond(['success'=>false,'message'=>'user_id o username requerido']);
}

// Read avatar
$avatarBin = null;
if (!$isJson && isset($_FILES['avatar']) && is_uploaded_file($_FILES['avatar']['tmp_name'])) {
  $avatarBin = file_get_contents($_FILES['avatar']['tmp_name']);
} elseif ($isJson) {
  // Accept multiple possible keys and tolerate whitespace/newlines
  $keys = ['avatar_base64','avatar','profile_image_base64','profile_image','image','imagen'];
  $b64 = null;
  foreach ($keys as $k) {
    if (isset($json[$k]) && is_string($json[$k]) && $json[$k] !== '') { $b64 = $json[$k]; break; }
  }
  if ($b64 !== null) {
    if (strpos($b64, ',') !== false) { $parts = explode(',', $b64, 2); $b64 = $parts[1]; }
    $b64 = preg_replace('/\s+/', '', $b64);
    $decoded = base64_decode($b64);
    if ($decoded !== false) { $avatarBin = $decoded; }
  }
}

if ($avatarBin === null) {
  respond(['success'=>false,'message'=>'Avatar requerido (archivo avatar o avatar_base64)']);
}

// Optional size limit (5MB)
if (strlen($avatarBin) > 5 * 1024 * 1024) {
  respond(['success'=>false,'message'=>'El avatar excede 5MB']);
}

try {
  $conn = DBConnection::getInstance()->getConnection();

  if ($userId > 0) {
    $stmt = $conn->prepare('UPDATE users SET profile_image_url = ? WHERE user_id = ?');
    $null = null;
    $stmt->bind_param('bi', $null, $userId);
    $stmt->send_long_data(0, $avatarBin);
  } else {
    $stmt = $conn->prepare('UPDATE users SET profile_image_url = ? WHERE username = ?');
    $null = null;
    $stmt->bind_param('bs', $null, $username);
    $stmt->send_long_data(0, $avatarBin);
  }

  $ok = $stmt->execute();
  $affected = $stmt->affected_rows;
  $stmt->close();

  if (!$ok || $affected <= 0) {
    respond(['success'=>false,'message'=>'No se pudo actualizar el avatar (usuario inexistente o sin cambios)']);
  }

  respond(['success'=>true,'message'=>'Avatar actualizado','bytes'=>strlen($avatarBin)]);
} catch (Throwable $e) {
  respond(['success'=>false,'message'=>'Error interno al actualizar avatar']);
}
?>
