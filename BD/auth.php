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

$sql = "SELECT user_id, username, email, password FROM users WHERE username = '$username'";
$result = $db->query($sql);

if ($result->num_rows == 1) {
    $userRow = $result->fetch_assoc();
    $dbPassword = $userRow['password'];

    if ($inputPassword === $dbPassword) {

        $user_data = [
            "userId" => $userRow['user_id'],
            "username" => $userRow['username']
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



