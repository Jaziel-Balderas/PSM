<?php
header("Content-Type: text/html; charset=utf-8");
require_once 'DBConnection.php';

echo "<h2>Configuración de tabla LIKES</h2>";

$db = DBConnection::getInstance()->getConnection();

if (!$db) {
    die("<p style='color:red'>Error: No se pudo conectar a la base de datos</p>");
}

echo "<p style='color:green'>Conexión exitosa a la base de datos</p>";

// Leer el archivo SQL
$sqlFile = __DIR__ . '/create_likes_table.sql';
if (!file_exists($sqlFile)) {
    die("<p style='color:red'>Error: No se encontró el archivo create_likes_table.sql</p>");
}

$sql = file_get_contents($sqlFile);

echo "<h3>Ejecutando SQL:</h3>";
echo "<pre style='background:#f5f5f5; padding:10px; border:1px solid #ddd;'>$sql</pre>";

// Ejecutar la consulta
if ($db->query($sql)) {
    echo "<p style='color:green; font-weight:bold'> Tabla 'likes' creada exitosamente</p>";
    
    // Verificar que existe
    $result = $db->query("SHOW TABLES LIKE 'likes'");
    if ($result && $result->num_rows > 0) {
        echo "<p style='color:green'>Confirmado: La tabla 'likes' existe en la base de datos</p>";
        
        // Mostrar estructura
        $result = $db->query("DESCRIBE likes");
        if ($result) {
            echo "<h3>Estructura de la tabla:</h3>";
            echo "<table border='1' cellpadding='5' style='border-collapse:collapse;'>";
            echo "<tr><th>Campo</th><th>Tipo</th><th>Null</th><th>Key</th><th>Default</th><th>Extra</th></tr>";
            while ($row = $result->fetch_assoc()) {
                echo "<tr>";
                echo "<td>{$row['Field']}</td>";
                echo "<td>{$row['Type']}</td>";
                echo "<td>{$row['Null']}</td>";
                echo "<td>{$row['Key']}</td>";
                echo "<td>{$row['Default']}</td>";
                echo "<td>{$row['Extra']}</td>";
                echo "</tr>";
            }
            echo "</table>";
        }
    }
} else {
    echo "<p style='color:red'>Error al crear la tabla: " . $db->error . "</p>";
}

echo "<hr>";
echo "<p><a href='test_get_posts.php'>Probar get_posts.php</a></p>";
echo "<p><a href='get_posts.php?current_user_id=0'>Ver posts (JSON)</a></p>";
?>
