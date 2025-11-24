<?php
header('Content-Type: application/json');
require_once 'DBConnection.php';

$result = [
  'success' => false,
  'message' => '',
  'env' => [
    'host' => getenv('PSM_DB_HOST') ?: '127.0.0.1',
    'user' => getenv('PSM_DB_USER') ?: 'root',
    'db'   => getenv('PSM_DB_NAME') ?: 'psm2',
    'port' => getenv('PSM_DB_PORT') ?: '3306',
  ]
];

try {
  $conn = DBConnection::getInstance()->getConnection();
  $res = $conn->query('SELECT DATABASE() AS db, VERSION() AS version');
  $row = $res ? $res->fetch_assoc() : null;
  $result['success'] = true;
  $result['message'] = 'OK';
  $result['db_info'] = $row;
} catch (Throwable $e) {
  $result['success'] = false;
  $result['message'] = 'Error conectando a la base de datos';
}

echo json_encode($result);
?>