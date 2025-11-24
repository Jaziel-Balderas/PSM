<?php
header("Content-Type: text/plain");

// Aquí solo verificamos que el servidor web (Apache) recibe la solicitud.
echo "Conexión HTTP Exitosa. El servidor Apache está funcionando.\n";

// Si deseas probar la conexión real a MySQL:
require_once 'DBConnection.php'; // Incluye tu Singleton de conexión
try {
    $db = DBConnection::getInstance()->getConnection();
    if ($db) {
        echo "Conexión MySQL Exitosa. El Singleton de PHP funciona.\n";
    } else {
        echo "Error de Conexión a MySQL: La DB no pudo ser contactada.\n";
    }
} catch (Exception $e) {
    echo "Error interno al conectar a la DB: " . $e->getMessage() . "\n";
}
?>