<?php
// includes/database.php — Aurelia Grand HMS
// XAMPP default: root with no password

$servername = "localhost";
$username   = "root";
$password   = "";        // Leave empty for XAMPP default
$dbname     = "aurelia_grand";

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    if ($_SERVER['HTTP_HOST'] === 'localhost') {
        die(json_encode(['success' => false, 'message' => 'DB Error: ' . $conn->connect_error]));
    } else {
        die(json_encode(['success' => false, 'message' => 'Service unavailable.']));
    }
}

$conn->set_charset("utf8mb4");
?>
