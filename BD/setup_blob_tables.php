<?php
header("Content-Type: text/html; charset=utf-8");
require_once 'DBConnection.php';

echo "<h2>Configuración de BLOB en tablas</h2>";

$db = DBConnection::getInstance()->getConnection();

if (!$db) {
    die("<p style='color:red'>❌ Error: No se pudo conectar a la base de datos</p>");
}

echo "<p style='color:green'>✅ Conexión exitosa a la base de datos</p>";

// 1. Modificar tabla users - cambiar profile_image_url a MEDIUMBLOB
echo "<h3>1. Modificando tabla USERS:</h3>";
$sql1 = "ALTER TABLE users MODIFY COLUMN profile_image_url MEDIUMBLOB DEFAULT NULL";
if ($db->query($sql1)) {
    echo "<p style='color:green'>✅ Columna profile_image_url modificada a MEDIUMBLOB</p>";
} else {
    if (strpos($db->error, 'check that it exists') !== false) {
        echo "<p style='color:orange'>⚠️ Ya estaba como MEDIUMBLOB: " . $db->error . "</p>";
    } else {
        echo "<p style='color:red'>❌ Error: " . $db->error . "</p>";
    }
}

// 2. Modificar tabla posts - cambiar image_urls a LONGBLOB
echo "<h3>2. Modificando tabla POSTS:</h3>";
$sql2 = "ALTER TABLE posts MODIFY COLUMN image_urls LONGBLOB DEFAULT NULL";
if ($db->query($sql2)) {
    echo "<p style='color:green'>✅ Columna image_urls modificada a LONGBLOB</p>";
} else {
    if (strpos($db->error, 'check that it exists') !== false) {
        echo "<p style='color:orange'>⚠️ Ya estaba como LONGBLOB: " . $db->error . "</p>";
    } else {
        echo "<p style='color:red'>❌ Error: " . $db->error . "</p>";
    }
}

// Mostrar estructuras actualizadas
echo "<h3>Estructura tabla USERS:</h3>";
$result = $db->query("DESCRIBE users");
if ($result) {
    echo "<table border='1' cellpadding='5' style='border-collapse:collapse;'>";
    echo "<tr><th>Campo</th><th>Tipo</th><th>Null</th><th>Key</th></tr>";
    while ($row = $result->fetch_assoc()) {
        $highlight = ($row['Field'] == 'profile_image_url') ? "style='background-color:#ccffcc;'" : "";
        echo "<tr $highlight>";
        echo "<td>{$row['Field']}</td>";
        echo "<td><strong>{$row['Type']}</strong></td>";
        echo "<td>{$row['Null']}</td>";
        echo "<td>{$row['Key']}</td>";
        echo "</tr>";
    }
    echo "</table>";
}

echo "<h3>Estructura tabla POSTS:</h3>";
$result = $db->query("DESCRIBE posts");
if ($result) {
    echo "<table border='1' cellpadding='5' style='border-collapse:collapse;'>";
    echo "<tr><th>Campo</th><th>Tipo</th><th>Null</th><th>Key</th></tr>";
    while ($row = $result->fetch_assoc()) {
        $highlight = ($row['Field'] == 'image_urls') ? "style='background-color:#ccffcc;'" : "";
        echo "<tr $highlight>";
        echo "<td>{$row['Field']}</td>";
        echo "<td><strong>{$row['Type']}</strong></td>";
        echo "<td>{$row['Null']}</td>";
        echo "<td>{$row['Key']}</td>";
        echo "</tr>";
    }
    echo "</table>";
}

echo "<hr>";
echo "<p><strong>✅ Tablas configuradas para usar BLOB</strong></p>";
echo "<p><strong>Siguiente:</strong> Las imágenes ahora se guardarán como BLOB en la base de datos</p>";
echo "<p><a href='get_posts.php?current_user_id=1'>Probar obtener posts</a></p>";
?>

