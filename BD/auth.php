<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr) { echo json_encode($arr); exit(); }

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) { $data = $_POST; }

$identifier = isset($data['username']) ? trim((string)$data['username']) : (isset($data['email']) ? trim((string)$data['email']) : '');
$password   = isset($data['password']) ? (string)$data['password'] : '';

if ($identifier === '' || $password === '') {
  respond(['success'=>false,'message'=>'username/email y password requeridos']);
}

try {
  $conn = DBConnection::getInstance()->getConnection();

  $stmt = $conn->prepare('SELECT user_id, nameuser, lastnames, username, email, password, phone, direccion FROM users WHERE username = ? OR email = ? LIMIT 1');
  if (!$stmt) { respond(['success'=>false,'message'=>'Error preparando consulta']); }
  $stmt->bind_param('ss', $identifier, $identifier);
  $stmt->execute();
  $res = $stmt->get_result();
  if (!$res || !$res->num_rows) {
    respond(['success'=>false,'message'=>'Usuario no encontrado']);
  }
  $u = $res->fetch_assoc();
  $stmt->close();

  $hash = $u['password'];
  $ok = password_verify($password, $hash);
  // Fallback legacy: comparar en texto plano si no es hash
  if (!$ok) {
    // si el hash no parece bcrypt, probamos igualdad directa
    if (strpos($hash, '$2y$') !== 0 && $hash === $password) {
      $ok = true;
    }
  }

  if (!$ok) {
    respond(['success'=>false,'message'=>'Credenciales inválidas']);
  }

  // Agregar log para debug
  error_log("LOGIN EXITOSO - user_id: " . $u['user_id'] . ", username: " . $u['username']);
  
  $user = [
    'userId' => (int)$u['user_id'],  // Cambiar a camelCase para Kotlin
    'nameuser' => $u['nameuser'],
    'lastnames' => $u['lastnames'],
    'username' => $u['username'],
    'email' => $u['email'],
    'phone' => $u['phone'] ?? '',
    'direccion' => $u['direccion'] ?? '',
    'profile_image_url' => null  // Agregar campo faltante
  ];

  error_log("USER DATA ENVIADO: " . json_encode($user));
  respond(['success'=>true,'message'=>'OK','user'=>$user]);
} catch (Throwable $e) {
  respond(['success'=>false,'message'=>'Error interno']);
}
?>

