<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr){ echo json_encode($arr); exit(); }

$raw = file_get_contents('php://input');
$json = json_decode($raw, true);
$isJson = is_array($json);
$data = $isJson ? $json : $_POST;

$userId = isset($data['user_id']) ? (int)$data['user_id'] : 0;

if ($userId <= 0) {
  respond(['success'=>false,'message'=>'user_id requerido']);
}

// Optional fields to update
$nameuser = isset($data['nameuser']) ? trim((string)$data['nameuser']) : null;
$lastnames = isset($data['lastnames']) ? trim((string)$data['lastnames']) : null;
$username = isset($data['username']) ? trim((string)$data['username']) : null;
$email = isset($data['email']) ? trim((string)$data['email']) : null;
$phone = isset($data['phone']) ? trim((string)$data['phone']) : null;
$direccion = isset($data['direccion']) ? trim((string)$data['direccion']) : null;

// Build dynamic update
$updates = [];
$types = '';
$values = [];

if ($nameuser !== null && $nameuser !== '') {
  $updates[] = 'nameuser = ?';
  $types .= 's';
  $values[] = $nameuser;
}

if ($lastnames !== null && $lastnames !== '') {
  $updates[] = 'lastnames = ?';
  $types .= 's';
  $values[] = $lastnames;
}

if ($username !== null && $username !== '') {
  if (!preg_match('/^[A-Za-z0-9_]{3,50}$/', $username)) {
    respond(['success'=>false,'message'=>'Alias inválido (3-50, letras/números/_)']);
  }
  $updates[] = 'username = ?';
  $types .= 's';
  $values[] = $username;
}

if ($email !== null && $email !== '') {
  if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    respond(['success'=>false,'message'=>'Correo electrónico inválido']);
  }
  $updates[] = 'email = ?';
  $types .= 's';
  $values[] = $email;
}

if ($phone !== null) {
  $updates[] = 'phone = ?';
  $types .= 's';
  $values[] = $phone;
}

if ($direccion !== null) {
  $updates[] = 'direccion = ?';
  $types .= 's';
  $values[] = $direccion;
}

if (empty($updates)) {
  respond(['success'=>false,'message'=>'No hay campos para actualizar']);
}

try {
  $conn = DBConnection::getInstance()->getConnection();

  // Check uniqueness for username and email if provided
  if ($username !== null && $username !== '') {
    $chk = $conn->prepare('SELECT 1 FROM users WHERE username = ? AND user_id != ? LIMIT 1');
    $chk->bind_param('si', $username, $userId);
    $chk->execute();
    $r = $chk->get_result();
    if ($r && $r->num_rows > 0) {
      respond(['success'=>false,'message'=>'El alias ya está en uso']);
    }
    $chk->close();
  }

  if ($email !== null && $email !== '') {
    $chk = $conn->prepare('SELECT 1 FROM users WHERE email = ? AND user_id != ? LIMIT 1');
    $chk->bind_param('si', $email, $userId);
    $chk->execute();
    $r = $chk->get_result();
    if ($r && $r->num_rows > 0) {
      respond(['success'=>false,'message'=>'El correo ya está registrado']);
    }
    $chk->close();
  }

  // Build and execute update
  $sql = 'UPDATE users SET ' . implode(', ', $updates) . ' WHERE user_id = ?';
  $stmt = $conn->prepare($sql);
  
  $types .= 'i';
  $values[] = $userId;
  
  $stmt->bind_param($types, ...$values);
  $ok = $stmt->execute();
  $affected = $stmt->affected_rows;
  $stmt->close();

  if (!$ok || $affected <= 0) {
    respond(['success'=>false,'message'=>'No se pudo actualizar el perfil']);
  }

  respond(['success'=>true,'message'=>'Perfil actualizado exitosamente']);
} catch (Throwable $e) {
  // Handle duplicate key
  if (isset($conn) && isset($conn->errno) && $conn->errno === 1062) {
    $msg = strpos($conn->error, 'username') !== false ? 'El alias ya está en uso' : (strpos($conn->error, 'email') !== false ? 'El correo ya está registrado' : 'Duplicado');
    respond(['success'=>false,'message'=>$msg]);
  }
  respond(['success'=>false,'message'=>'Error interno']);
}
?>
