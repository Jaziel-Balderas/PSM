<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

function respond($arr){ echo json_encode($arr); exit(); }

// Accept JSON or multipart/form-data
$raw = file_get_contents('php://input');
$json = json_decode($raw, true);
$isJson = is_array($json);
$data = $isJson ? $json : $_POST;

// Map inputs
$nameuser  = isset($data['nameuser']) ? trim((string)$data['nameuser']) : '';
$lastnames = isset($data['lastnames']) ? trim((string)$data['lastnames']) : '';
$email     = isset($data['email']) ? trim((string)$data['email']) : '';
$username  = isset($data['username']) ? trim((string)$data['username']) : '';
$password  = isset($data['password']) ? (string)$data['password'] : '';
$phone     = isset($data['phone']) ? trim((string)$data['phone']) : null;
$direccion = isset($data['direccion']) ? trim((string)$data['direccion']) : null;

// Avatar can be provided as multipart file `avatar` or JSON base64 under common keys
$avatarBin = null;
if (!$isJson && isset($_FILES['avatar']) && is_uploaded_file($_FILES['avatar']['tmp_name'])) {
    $avatarBin = file_get_contents($_FILES['avatar']['tmp_name']);
} elseif ($isJson) {
    // Accept multiple possible keys from clients
    $keys = ['avatar_base64','avatar','profile_image_base64','profile_image','profile_image_url','image','imagen'];
    $b64 = null;
    foreach ($keys as $k) {
        if (isset($json[$k]) && is_string($json[$k]) && $json[$k] !== '') { $b64 = $json[$k]; break; }
    }
    if ($b64 !== null) {
        if (strpos($b64, ',') !== false) { $parts = explode(',', $b64, 2); $b64 = $parts[1]; }
        // Remove whitespace/newlines then decode
        $b64 = preg_replace('/\s+/', '', $b64);
        $decoded = base64_decode($b64); // non-strict to allow newlines
        if ($decoded !== false) { $avatarBin = $decoded; }
    }
}

// Validation helpers
$errors = [];
if ($nameuser === '') { $errors[] = 'El nombre es requerido'; }
if ($lastnames === '') { $errors[] = 'Los apellidos son requeridos'; }
if ($email === '' || !filter_var($email, FILTER_VALIDATE_EMAIL)) { $errors[] = 'Correo electrónico inválido'; }
if ($username === '' || !preg_match('/^[A-Za-z0-9_]{3,50}$/', $username)) { $errors[] = 'Alias inválido (3-50, letras/números/_ )'; }
if (strlen($password) < 10 ||
    !preg_match('/[A-Z]/', $password) ||
    !preg_match('/[a-z]/', $password) ||
    !preg_match('/[0-9]/', $password)) {
  $errors[] = 'La contraseña debe tener mínimo 10 caracteres, mayúscula, minúscula y número';
}
if ($avatarBin === null) { $errors[] = 'Avatar requerido'; }

if (!empty($errors)) { respond(['success'=>false,'message'=>'Validación fallida','errors'=>$errors]); }

try {
  $conn = DBConnection::getInstance()->getConnection();
  // Pre-check uniqueness (also rely on unique constraints)
  $q1 = $conn->prepare('SELECT 1 FROM users WHERE email = ? LIMIT 1');
  $q1->bind_param('s', $email); $q1->execute(); $r1 = $q1->get_result(); $existsEmail = ($r1 && $r1->num_rows>0); $q1->close();
  if ($existsEmail) { respond(['success'=>false,'message'=>'El correo ya está registrado']); }
  $q2 = $conn->prepare('SELECT 1 FROM users WHERE username = ? LIMIT 1');
  $q2->bind_param('s', $username); $q2->execute(); $r2 = $q2->get_result(); $existsUser = ($r2 && $r2->num_rows>0); $q2->close();
  if ($existsUser) { respond(['success'=>false,'message'=>'El alias ya está en uso']); }

  $hash = password_hash($password, PASSWORD_BCRYPT);

  $stmt = $conn->prepare('INSERT INTO users (nameuser, lastnames, username, email, password, phone, direccion, profile_image_url) VALUES (?,?,?,?,?,?,?,?)');
  if (!$stmt) { respond(['success'=>false,'message'=>'Error preparando registro']); }

  // For blob binding, use send_long_data
  $null = null;
  $stmt->bind_param('sssssssb', $nameuser, $lastnames, $username, $email, $hash, $phone, $direccion, $null);
  $stmt->send_long_data(7, $avatarBin);
  $ok = $stmt->execute();
  $newId = $conn->insert_id;
  $stmt->close();

  if (!$ok) { respond(['success'=>false,'message'=>'No se pudo registrar']); }

  respond([
    'success'=>true,
    'message'=>'Registro exitoso',
    'user' => [
      'user_id' => (int)$newId,
      'nameuser' => $nameuser,
      'lastnames' => $lastnames,
      'username' => $username,
      'email' => $email,
      'phone' => $phone ?? '',
      'direccion' => $direccion ?? '',
      'avatar_saved' => $avatarBin !== null
    ]
  ]);
} catch (Throwable $e) {
  // Handle duplicate key
  if (isset($conn) && isset($conn->errno) && $conn->errno === 1062) {
    $msg = strpos($conn->error, 'username') !== false ? 'El alias ya está en uso' : (strpos($conn->error, 'email') !== false ? 'El correo ya está registrado' : 'Duplicado');
    respond(['success'=>false,'message'=>$msg]);
  }
  respond(['success'=>false,'message'=>'Error interno durante el registro']);
}
?>
<?php
// Archivo: register.php
header("Content-Type: application/json");
require_once 'DBConnection.php';

function jsonError($msg) {
    // Siempre 200 para que Retrofit pueda leer el cuerpo
    http_response_code(200);
    echo json_encode(["success" => false, "message" => $msg]);
    exit();
}

function validatePassword($pwd) {
    if (strlen($pwd) < 10) {
        return "La contraseña debe tener al menos 10 caracteres";
    }
    if (!preg_match('/[A-Z]/', $pwd)) {
        return "La contraseña debe incluir al menos una mayúscula";
    }
    if (!preg_match('/[a-z]/', $pwd)) {
        return "La contraseña debe incluir al menos una minúscula";
    }
    if (!preg_match('/[0-9]/', $pwd)) {
        return "La contraseña debe incluir al menos un número";
    }
    return null; // OK
}

$db = DBConnection::getInstance()->getConnection();
$conn = $db;

// Permitir JSON POST o multipart (avatar como base64)
$rawBody = file_get_contents("php://input");
error_log("[register] rawBody length=" . strlen($rawBody));
$data = json_decode($rawBody);
if (!$data) {
    // Intentar leer variables POST clásicas si no vino JSON
    if (!empty($_POST)) {
        $data = (object) $_POST;
    }
}

if (!$data) {
    error_log("[register] No JSON decode, intentando POST tradicionales. POST keys=" . implode(',', array_keys($_POST)));
    jsonError("Formato inválido. Enviar JSON con campos requeridos.");
}

if (!isset($data->nameuser) || !isset($data->lastnames) || !isset($data->username) || !isset($data->password) || !isset($data->email)) {
    jsonError("Datos incompletos. Se requieren: nameuser, lastnames, username, password, email.");
}

// Normalizar y limpiar
$nameuser = trim($data->nameuser);
$lastnames = trim($data->lastnames ?? '');
$username = trim($data->username);
$email = trim($data->email);
$phone = trim($data->phone ?? '');
$passwordPlain = $data->password;
$direccionRaw = trim($data->direccion ?? '');

// Validaciones de formato
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    jsonError("Correo electrónico inválido");
}

$pwdError = validatePassword($passwordPlain);
if ($pwdError) {
    jsonError($pwdError);
}

// Phone opcional: aceptar vacío, validar formato si viene
if ($phone !== '' && !preg_match('/^[0-9+\- ]{7,20}$/', $phone)) {
    jsonError("Formato de teléfono inválido (solo dígitos, +, -, espacio)");
}

// Dirección opcional
$direccion = ($direccionRaw === '') ? null : $direccionRaw;

// Escapar para SQL
$nameuser = $conn->real_escape_string($nameuser);
$lastnames = $conn->real_escape_string($lastnames);
$username = $conn->real_escape_string($username);
$email = $conn->real_escape_string($email);
$phone = $conn->real_escape_string($phone === '' ? '' : $phone);
$direccion = $direccion === null ? null : $conn->real_escape_string($direccion);

// Checar duplicados antes de SP para error claro
$dupStmt = $conn->prepare("SELECT user_id FROM users WHERE email = ? OR username = ? LIMIT 1");
$dupStmt->bind_param("ss", $email, $username);
$dupStmt->execute();
$dupRes = $dupStmt->get_result();
if ($dupRes && $dupRes->num_rows > 0) {
    $dupStmt->close();
    jsonError("Email o username ya registrado");
}
$dupStmt->close();

// Hash de contraseña (bcrypt) - costo por defecto
$passwordHashed = password_hash($passwordPlain, PASSWORD_BCRYPT);
if (!$passwordHashed) {
    jsonError("Error interno generando hash de contraseña");
}


$image_data_base64 = $data->profile_image_url ?? null; // base64 puro sin encabezado data:
$profile_image_blob = null;

if ($image_data_base64 && !empty($image_data_base64)) {
    // Limpiar el Base64 de espacios y saltos de línea
    $image_data_base64 = preg_replace('/\s+/', '', $image_data_base64);
    // Decodificar Base64 a datos binarios 
    $profile_image_blob = base64_decode($image_data_base64);
    
    // Verificar que la decodificación fue exitosa
    if ($profile_image_blob === false) {
        $profile_image_blob = null;
    }
    // Validar tamaño máximo (por ejemplo 1MB)
    if ($profile_image_blob !== null && strlen($profile_image_blob) > 1024 * 1024) {
        jsonError("Avatar demasiado grande (máx 1MB)");
    }
}

$sql_call = "CALL sp_registrar_usuario(?, ?, ?, ?, ?, ?, ?, ?, @p_mensaje)";

$stmt = $conn->prepare($sql_call);

if (!$stmt) {
    echo json_encode(["success" => false, "message" => "Error de preparación SQL: " . $conn->error]);
    exit();
}

// Los parámetros del SP son: nameuser, lastnames, password, email, phone, direccion, photo(BLOB), username
// Para BLOB usamos 'b' y luego send_long_data
$null_param = null;
$stmt->bind_param(
    "ssssssbs",
    $nameuser,
    $lastnames,
    $passwordHashed,
    $email,
    $phone,
    $direccion,
    $null_param,
    $username
);

// Enviar el BLOB usando send_long_data (índice 6 es el 7mo parámetro)
if ($profile_image_blob !== null && !empty($profile_image_blob)) {
    $stmt->send_long_data(6, $profile_image_blob);
}

$stmt->execute();
$stmt->close();

$result = $conn->query("SELECT @p_mensaje AS mensaje");
$row = $result->fetch_assoc();
$mensaje_salida = $row['mensaje'];

$success = (strpos($mensaje_salida, 'ERROR') === false);

$response = [
    "success" => $success,
    "message" => $mensaje_salida
];

// Si el registro fue exitoso, obtener el usuario recién creado
if ($success) {
    $sql_user = "SELECT user_id, nameuser, lastnames, username, email, phone, direccion, 
                 CASE WHEN profile_image_url IS NOT NULL 
                 THEN TO_BASE64(profile_image_url) 
                 ELSE NULL END as profile_image_url
                 FROM users WHERE username = ? OR email = ? LIMIT 1";
    
    $stmt_user = $conn->prepare($sql_user);
    $stmt_user->bind_param("ss", $username, $email);
    $stmt_user->execute();
    $result_user = $stmt_user->get_result();
    
    if ($result_user->num_rows > 0) {
        $user_data = $result_user->fetch_assoc();
        $response["user"] = [
            "userId" => (int)$user_data['user_id'],
            "nameuser" => $user_data['nameuser'],
            "lastnames" => $user_data['lastnames'],
            "username" => $user_data['username'],
            "email" => $user_data['email'],
            "phone" => $user_data['phone'],
            "direccion" => $user_data['direccion'],
            "profile_image_url" => $user_data['profile_image_url']
        ];
    }
    $stmt_user->close();
}

echo json_encode($response);
exit();
?>