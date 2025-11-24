<?php
// Script de prueba para verificar get_posts.php
require_once 'DBConnection.php';

echo "<h2>Test de get_posts.php</h2>";

// Probar la conexión
$db = DBConnection::getInstance()->getConnection();
if (!$db) {
    die("<p style='color:red'>❌ Error de conexión a la base de datos</p>");
}
echo "<p style='color:green'>✅ Conexión exitosa a la base de datos</p>";

// Verificar si existen posts
$queryCount = "SELECT COUNT(*) as total FROM posts";
$result = $db->query($queryCount);
$row = $result->fetch_assoc();
echo "<p>📊 Total de posts en BD: <strong>{$row['total']}</strong></p>";

// Verificar si existen usuarios
$queryUsers = "SELECT COUNT(*) as total FROM users";
$result = $db->query($queryUsers);
$row = $result->fetch_assoc();
echo "<p>👥 Total de usuarios en BD: <strong>{$row['total']}</strong></p>";

// Probar la consulta de posts
echo "<h3>Probando consulta de posts:</h3>";
$query = "SELECT 
            p.id,
            p.user_id,
            p.title,
            p.description,
            p.location,
            p.image_urls,
            p.is_public,
            p.created_at,
            u.nameuser,
            u.lastnames,
            u.username,
            u.profile_image_url,
            COUNT(DISTINCT l.id) as like_count,
            MAX(CASE WHEN l.user_id = 0 THEN 1 ELSE 0 END) as is_liked
          FROM posts p
          INNER JOIN users u ON p.user_id = u.user_id
          LEFT JOIN likes l ON p.id = l.post_id
          WHERE p.is_public = 1
          GROUP BY p.id 
          ORDER BY p.created_at DESC 
          LIMIT 10";

$result = $db->query($query);

if (!$result) {
    echo "<p style='color:red'>❌ Error en la consulta: " . $db->error . "</p>";
} else {
    $posts = [];
    while ($row = $result->fetch_assoc()) {
        $posts[] = $row;
    }
    
    echo "<p>📝 Posts encontrados: <strong>" . count($posts) . "</strong></p>";
    
    if (count($posts) > 0) {
        echo "<pre style='background:#f5f5f5; padding:10px; border:1px solid #ddd; overflow:auto;'>";
        print_r($posts);
        echo "</pre>";
    }
}

// Probar get_posts.php directamente
echo "<h3>Probando get_posts.php:</h3>";
$url = "http://localhost:8080/PSM/BD/get_posts.php?current_user_id=0";
echo "<p>URL: <a href='$url' target='_blank'>$url</a></p>";

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

echo "<p>HTTP Code: <strong>$httpCode</strong></p>";
echo "<h4>Respuesta JSON:</h4>";
echo "<pre style='background:#f5f5f5; padding:10px; border:1px solid #ddd; overflow:auto;'>";
echo htmlspecialchars($response);
echo "</pre>";

$data = json_decode($response, true);
if ($data) {
    echo "<h4>Respuesta decodificada:</h4>";
    echo "<pre style='background:#f5f5f5; padding:10px; border:1px solid #ddd; overflow:auto;'>";
    print_r($data);
    echo "</pre>";
}
?>
