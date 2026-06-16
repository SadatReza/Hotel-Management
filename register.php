<?php
// register.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => 'Invalid request.']); exit();
}

$first_name = trim($_POST['first_name'] ?? '');
$last_name  = trim($_POST['last_name']  ?? '');
$email      = trim($_POST['email']      ?? '');
$password   = $_POST['password']        ?? '';
$role       = $_POST['role']            ?? 'guest';
$phone      = trim($_POST['phone']      ?? '');

$errors = [];
if (empty($first_name))  $errors[] = 'First name is required.';
if (empty($last_name))   $errors[] = 'Last name is required.';
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) $errors[] = 'Valid email is required.';
if (strlen($password) < 8) $errors[] = 'Password must be at least 8 characters.';
if (!in_array($role, ['guest','staff'])) $role = 'guest';

if (!empty($errors)) { echo json_encode(['success' => false, 'errors' => $errors]); exit(); }

$stmt = $conn->prepare("SELECT id FROM users WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute(); $stmt->store_result();
if ($stmt->num_rows > 0) {
    echo json_encode(['success' => false, 'message' => 'Email already registered.']); exit();
}
$stmt->close();

$hash = password_hash($password, PASSWORD_BCRYPT);
$stmt = $conn->prepare("INSERT INTO users (first_name, last_name, email, password, role, phone) VALUES (?, ?, ?, ?, ?, ?)");
$stmt->bind_param("ssssss", $first_name, $last_name, $email, $hash, $role, $phone);

if ($stmt->execute()) {
    $user_id = $stmt->insert_id;
    
    if ($role === 'staff') {
        $s2 = $conn->prepare("INSERT INTO staff_profiles (user_id, department, zone, shift_start, shift_end) VALUES (?, 'Housekeeper', 'All Floors', '08:00:00', '16:00:00')");
        $s2->bind_param("i", $user_id);
        $s2->execute();
        $s2->close();
    }
    
    $_SESSION['logged_in'] = true;
    $_SESSION['user_id']   = $user_id;
    $_SESSION['user_name'] = $first_name . ' ' . $last_name;
    $_SESSION['email']     = $email;
    $_SESSION['role']      = $role;
    echo json_encode(['success' => true, 'message' => 'Account created!',
        'user' => ['id' => $user_id, 'name' => $first_name.' '.$last_name,
                   'email' => $email, 'role' => $role]]);
} else {
    echo json_encode(['success' => false, 'message' => 'Registration failed.']);
}
$stmt->close(); $conn->close();
?>
