<?php
// Archivo: register.php
header("Content-Type: application/json");
require_once 'DBConnection.php';

$db = DBConnection::getInstance()->getConnection();
$conn = $db;

$data = json_decode(file_get_contents("php://input"));

if (
    !isset($data->nameuser) ||
    !isset($data->username) ||
    !isset($data->password) ||
    !isset($data->email)
) {

    echo json_encode(["success" => false, "message" => "Datos incompletos. Faltan campos esenciales."]);
    exit();
}

$nameuser = $conn->real_escape_string($data->nameuser);
$lastnames = $conn->real_escape_string($data->lastnames ?? '');
$username = $conn->real_escape_string($data->username);
$email = $conn->real_escape_string($data->email);
$phone = $conn->real_escape_string($data->phone ?? '0');
$password = $data->password;
$direccion = $conn->real_escape_string($data->direccion ?? null);


$image_data_base64 = $data->profile_image_url ?? null;
$profile_image_blob = null;

if ($image_data_base64) {
    // Decodificar Base64 a datos binarios 
    $profile_image_blob = base64_decode($image_data_base64);
}

$sql_call = "CALL sp_registrar_usuario(?, ?, ?, ?, ?, ?, ?, ?, @p_mensaje)";

$stmt = $conn->prepare($sql_call);

if (!$stmt) {
    echo json_encode(["success" => false, "message" => "Error de preparación SQL: " . $conn->error]);
    exit();
}

$stmt->bind_param(
    "ssssssbs",
    $nameuser,
    $lastnames,
    $password,
    $email,
    $phone,
    $direccion,
    $profile_image_blob,
    $username
);
$stmt->execute();
$stmt->close();

$result = $conn->query("SELECT @p_mensaje AS mensaje");
$row = $result->fetch_assoc();
$mensaje_salida = $row['mensaje'];

$success = (strpos($mensaje_salida, 'ERROR') === false);

echo json_encode([
    "success" => $success,
    "message" => $mensaje_salida
]);
exit();
?>