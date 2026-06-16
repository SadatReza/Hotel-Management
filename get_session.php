<?php
// get_session.php — Frontend calls this on load to check login status
session_start();
header('Content-Type: application/json');
if (isset($_SESSION['logged_in']) && $_SESSION['logged_in'] === true) {
    echo json_encode(['logged_in' => true, 'user' => [
        'id'    => $_SESSION['user_id'],
        'name'  => $_SESSION['user_name'],
        'email' => $_SESSION['email'],
        'role'  => $_SESSION['role'],
    ]]);
} else {
    echo json_encode(['logged_in' => false]);
}
?>
