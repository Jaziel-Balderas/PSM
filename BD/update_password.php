<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr){ echo json_encode($arr); exit(); }

$raw = file_get_contents('php://input');
$json = json_decode($raw, true);
$isJson = is_array($json);
$data = $isJson ? $json : $_POST;

$userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;
$currentPassword = isset($data['current_password']) ? (string)$data['current_password'] : '';
$newPassword = isset($data['new_password']) ? (string)$data['new_password'] : '';

if ($userId <= 0) {
  respond(['success'=>false,'message'=>'user_id requerido']);
}

if ($currentPassword === '' || $newPassword === '') {
  respond(['success'=>false,'message'=>'current_password y new_password requeridos']);
}

// Validate new password
if (strlen($newPassword) < 10 ||
    !preg_match('/[A-Z]/', $newPassword) ||
    !preg_match('/[a-z]/', $newPassword) ||
    !preg_match('/[0-9]/', $newPassword)) {
  respond(['success'=>false,'message'=>'La nueva contraseña debe tener mínimo 10 caracteres, mayúscula, minúscula y número']);
}

try {
  $conn = DBConnection::getInstance()->getConnection();

  // Fetch current password hash
  $stmt = $conn->prepare('SELECT password FROM users WHERE user_id = ? LIMIT 1');
  $stmt->bind_param('i', $userId);
  $stmt->execute();
  $res = $stmt->get_result();
  if (!$res || !$res->num_rows) {
    respond(['success'=>false,'message'=>'Usuario no encontrado']);
  }
  $row = $res->fetch_assoc();
  $stmt->close();

  $hash = $row['password'];
  
  // Verify current password
  $ok = password_verify($currentPassword, $hash);
  // Legacy fallback
  if (!$ok && strpos($hash, '$2y$') !== 0 && $hash === $currentPassword) {
    $ok = true;
  }

  if (!$ok) {
    respond(['success'=>false,'message'=>'Contraseña actual incorrecta']);
  }

  // Hash new password
  $newHash = password_hash($newPassword, PASSWORD_BCRYPT);

  // Update
  $upd = $conn->prepare('UPDATE users SET password = ? WHERE user_id = ?');
  $upd->bind_param('si', $newHash, $userId);
  $success = $upd->execute();
  $upd->close();

  if (!$success) {
    respond(['success'=>false,'message'=>'No se pudo actualizar la contraseña']);
  }

  respond(['success'=>true,'message'=>'Contraseña actualizada exitosamente']);
} catch (Throwable $e) {
  respond(['success'=>false,'message'=>'Error interno']);
}
?>
