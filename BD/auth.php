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

  $user = [
    'user_id' => (int)$u['user_id'],
    'nameuser' => $u['nameuser'],
    'lastnames' => $u['lastnames'],
    'username' => $u['username'],
    'email' => $u['email'],
    'phone' => $u['phone'] ?? '',
    'direccion' => $u['direccion'] ?? ''
  ];

  respond(['success'=>true,'message'=>'OK','user'=>$user]);
} catch (Throwable $e) {
  respond(['success'=>false,'message'=>'Error interno']);
}
?>
<?php

header("Content-Type: application/json");

require_once 'DBConnection.php';

// 2. Obtener la única instancia de la conexión
$db = DBConnection::getInstance()->getConnection();

$data = json_decode(file_get_contents("php://input"));

if (!isset($data->username) || !isset($data->password)) {
    echo json_encode(["success" => false, "message" => "Datos de login incompletos."]);
    exit();
}

$username = $db->real_escape_string($data->username);
$inputPassword = $data->password;

$sql = "SELECT user_id, nameuser, lastnames, username, email, phone, direccion, password, 
    CASE WHEN profile_image_url IS NOT NULL 
    THEN TO_BASE64(profile_image_url) 
    ELSE NULL END as profile_image_url 
    FROM users WHERE username = '$username' LIMIT 1";
$result = $db->query($sql);

if ($result->num_rows == 1) {
    $userRow = $result->fetch_assoc();
    $dbPassword = $userRow['password'];

    // Compatibilidad: si el password almacenado parece bcrypt (starts with $2y$ or $2a$), usar password_verify
    $isBcrypt = strpos($dbPassword, '$2y$') === 0 || strpos($dbPassword, '$2a$') === 0;
    $passwordOk = false;
    if ($isBcrypt) {
        $passwordOk = password_verify($inputPassword, $dbPassword);
    } else {
        // Fallback a comparación directa (legacy plano). Se puede eliminar tras migración completa.
        $passwordOk = ($inputPassword === $dbPassword);
    }

    if ($passwordOk) {
        // Normalizar campos opcionales para evitar null en cliente (SQLite NOT NULL / Kotlin non-null)
        $phone = isset($userRow['phone']) && $userRow['phone'] !== null ? $userRow['phone'] : "";
        $direccion = isset($userRow['direccion']) && $userRow['direccion'] !== null ? $userRow['direccion'] : "";
        $profileImage = isset($userRow['profile_image_url']) && $userRow['profile_image_url'] !== null ? $userRow['profile_image_url'] : null;

        $user_data = [
            "userId" => (int)$userRow['user_id'],
            "nameuser" => $userRow['nameuser'],
            "lastnames" => $userRow['lastnames'],
            "username" => $userRow['username'],
            "email" => $userRow['email'],
            "phone" => $phone,
            "direccion" => $direccion,
            "profile_image_url" => $profileImage
        ];

        echo json_encode([
            "success" => true,
            "message" => "Inicio de sesión exitoso.",
            "user" => $user_data
        ]);

    } else {
        echo json_encode(["success" => false, "message" => "Contraseña incorrecta"]);
    }
} else {
    // CORRECCIÓN: Mensaje más claro
    echo json_encode(["success" => false, "message" => "Nombre de usuario no encontrado."]);
}



