<?php
class DBConnection {
    private static $instance = null;
    private $conn;

    private function __construct() {
        $host = self::env('PSM_DB_HOST', '127.0.0.1');
        $user = self::env('PSM_DB_USER', 'root');
        $pass = self::env('PSM_DB_PASS', '');
        $db   = self::env('PSM_DB_NAME', 'psm2');
        $port = (int) self::env('PSM_DB_PORT', '3306');

        mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);
        try {
            $this->conn = new mysqli($host, $user, $pass, $db, $port);
            if (!$this->conn) {
                throw new Exception('Conexión nula');
            }
            $this->conn->set_charset('utf8mb4');
        } catch (Throwable $e) {
            // No fugamos credenciales; lanzamos excepción genérica
            throw new Exception('Error de conexión a la base de datos');
        }
    }

    public static function getInstance() {
        if (self::$instance === null) {
            self::$instance = new DBConnection();
        }
        return self::$instance;
    }

    public function getConnection() {
        return $this->conn;
    }

    private static function env($key, $default = null) {
        $v = getenv($key);
        if ($v === false && isset($_ENV[$key])) { $v = $_ENV[$key]; }
        return ($v === false || $v === null || $v === '') ? $default : $v;
    }
}
