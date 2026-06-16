<?php
// login.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => 'Invalid request.']); exit();
}

$email    = trim($_POST['email']    ?? '');
$password = $_POST['password']      ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(['success' => false, 'message' => 'Email and password are required.']); exit();
}

$stmt = $conn->prepare("SELECT * FROM users WHERE email = ? AND is_active = 1");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode(['success' => false, 'message' => 'Invalid email or password.']); exit();
}

$user = $result->fetch_assoc();

if (!password_verify($password, $user['password'])) {
    echo json_encode(['success' => false, 'message' => 'Invalid email or password.']); exit();
}

// Set session
$_SESSION['logged_in'] = true;
$_SESSION['user_id']   = $user['id'];
$_SESSION['user_name'] = $user['first_name'] . ' ' . $user['last_name'];
$_SESSION['email']     = $user['email'];
$_SESSION['role']      = $user['role'];

echo json_encode(['success' => true, 'message' => 'Login successful.',
    'user' => ['id' => $user['id'], 'name' => $user['first_name'].' '.$user['last_name'],
               'email' => $user['email'], 'role' => $user['role']]]);

$stmt->close(); $conn->close();
?>
