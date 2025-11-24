<?php
require_once 'DBConnection.php';

function detect_content_type($bin) {
    if (strlen($bin) >= 8 && substr($bin, 0, 8) === "\x89PNG\r\n\x1a\n") return 'image/png';
    if (strlen($bin) >= 3 && substr($bin, 0, 3) === "\xFF\xD8\xFF") return 'image/jpeg';
    if (strlen($bin) >= 6 && substr($bin, 0, 6) === "GIF87a") return 'image/gif';
    if (strlen($bin) >= 6 && substr($bin, 0, 6) === "GIF89a") return 'image/gif';
    if (strlen($bin) >= 12 && substr($bin, 0, 4) === 'RIFF' && substr($bin, 8, 4) === 'WEBP') return 'image/webp';
    return 'application/octet-stream';
}

$userId = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
$username = isset($_GET['username']) ? trim((string)$_GET['username']) : '';
$asBase64 = isset($_GET['base64']) ? (int)$_GET['base64'] === 1 : false;

if ($userId <= 0 && $username === '') {
    if ($asBase64) {
        header('Content-Type: application/json');
        echo json_encode(['success'=>false,'message'=>'user_id o username requerido']);
    } else {
        http_response_code(400);
        header('Content-Type: text/plain');
        echo 'user_id o username requerido';
    }
    exit();
}

try {
    $conn = DBConnection::getInstance()->getConnection();
    if ($userId > 0) {
        $stmt = $conn->prepare('SELECT profile_image_url FROM users WHERE user_id = ? LIMIT 1');
        $stmt->bind_param('i', $userId);
    } else {
        $stmt = $conn->prepare('SELECT profile_image_url FROM users WHERE username = ? LIMIT 1');
        $stmt->bind_param('s', $username);
    }
    $stmt->execute();
    $res = $stmt->get_result();
    if (!$res || !$res->num_rows) {
        if ($asBase64) {
            header('Content-Type: application/json');
            echo json_encode(['success'=>false,'message'=>'Usuario no encontrado']);
        } else {
            http_response_code(404);
            header('Content-Type: text/plain');
            echo 'No encontrado';
        }
        exit();
    }
    $row = $res->fetch_assoc();
    $stmt->close();

    $bin = $row['profile_image_url'];
    if ($bin === null || $bin === '' || strlen($bin) === 0) {
        if ($asBase64) {
            header('Content-Type: application/json');
            echo json_encode(['success'=>false,'message'=>'Sin avatar']);
        } else {
            http_response_code(404);
            header('Content-Type: text/plain');
            echo 'Sin avatar';
        }
        exit();
    }

    if ($asBase64) {
        header('Content-Type: application/json');
        echo json_encode(['success'=>true,'avatar_base64'=>base64_encode($bin)]);
        exit();
    }

    $ctype = detect_content_type($bin);
    header('Content-Type: '.$ctype);
    header('Cache-Control: public, max-age=86400');
    header('Content-Length: '.strlen($bin));
    echo $bin;
    exit();
} catch (Throwable $e) {
    if ($asBase64) {
        header('Content-Type: application/json');
        echo json_encode(['success'=>false,'message'=>'Error interno']);
    } else {
        http_response_code(500);
        header('Content-Type: text/plain');
        echo 'Error interno';
    }
    exit();
}
?>
