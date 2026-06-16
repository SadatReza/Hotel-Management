<?php
session_start();
require_once('../includes/database.php');
header('Content-Type: application/json');
if (!isset($_SESSION['logged_in']) || !in_array($_SESSION['role'], ['admin','staff'])) {
    echo json_encode(['success' => false, 'message' => 'Unauthorized.']); exit();
}
$action = $_POST['action'] ?? $_GET['action'] ?? 'get';
$role = $_SESSION['role'];
if ($action === 'get') {
    $result = $conn->query("SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.is_active, sp.department, sp.zone, sp.shift_start, sp.shift_end, sp.on_duty FROM users u LEFT JOIN staff_profiles sp ON sp.user_id=u.id WHERE u.role='staff' AND u.is_active=1 ORDER BY u.first_name");
    $staff = []; while ($row = $result->fetch_assoc()) $staff[] = $row;
    echo json_encode(['success' => true, 'data' => $staff]);
} elseif ($action === 'add' && $role === 'admin') {
    $fn = trim($_POST['first_name'] ?? ''); $ln = trim($_POST['last_name'] ?? '');
    $email = trim($_POST['email'] ?? ''); $pass = $_POST['password'] ?? 'Staff@1234';
    $dept = $_POST['department'] ?? ''; $zone = $_POST['zone'] ?? 'All Floors';
    $ss = $_POST['shift_start'] ?? '08:00'; $se = $_POST['shift_end'] ?? '16:00';
    $hash = password_hash($pass, PASSWORD_BCRYPT);
    $stmt = $conn->prepare("INSERT INTO users (first_name,last_name,email,password,role) VALUES (?,?,?,?,'staff')");
    $stmt->bind_param("ssss", $fn, $ln, $email, $hash);
    if ($stmt->execute()) {
        $uid = $stmt->insert_id;
        $s2 = $conn->prepare("INSERT INTO staff_profiles (user_id,department,zone,shift_start,shift_end) VALUES (?,?,?,?,?)");
        $s2->bind_param("issss", $uid, $dept, $zone, $ss, $se); $s2->execute();
        echo json_encode(['success' => true, 'message' => 'Staff added!']);
    } else { echo json_encode(['success' => false, 'message' => 'Email already exists.']); }
} elseif ($action === 'update' && $role === 'admin') {
    $uid = (int)($_POST['user_id'] ?? 0);
    $dept = $_POST['department'] ?? '';
    $zone = $_POST['zone'] ?? '';
    $ss = $_POST['shift_start'] ?? '08:00';
    $se = $_POST['shift_end'] ?? '16:00';
    if (!$uid || empty($dept) || empty($zone)) {
        echo json_encode(['success' => false, 'message' => 'Invalid parameters.']); exit();
    }
    
    // Check if staff profile exists
    $check = $conn->prepare("SELECT id FROM staff_profiles WHERE user_id = ?");
    $check->bind_param("i", $uid);
    $check->execute();
    $check->store_result();
    $exists = ($check->num_rows > 0);
    $check->close();
    
    if (!$exists) {
        $stmt = $conn->prepare("INSERT INTO staff_profiles (user_id, department, zone, shift_start, shift_end) VALUES (?, ?, ?, ?, ?)");
        $stmt->bind_param("issss", $uid, $dept, $zone, $ss, $se);
    } else {
        $stmt = $conn->prepare("UPDATE staff_profiles SET department = ?, zone = ?, shift_start = ?, shift_end = ? WHERE user_id = ?");
        $stmt->bind_param("ssssi", $dept, $zone, $ss, $se, $uid);
    }
    
    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Staff profile updated.']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Failed to update staff profile.']);
    }
    $stmt->close();
} elseif ($action === 'toggle_duty') {
    $uid = (int)($_POST['user_id'] ?? 0); $on = (int)($_POST['on_duty'] ?? 0);
    
    // Check if staff profile exists
    $check = $conn->prepare("SELECT id FROM staff_profiles WHERE user_id = ?");
    $check->bind_param("i", $uid);
    $check->execute();
    $check->store_result();
    $exists = ($check->num_rows > 0);
    $check->close();
    
    if (!$exists) {
        $stmt = $conn->prepare("INSERT INTO staff_profiles (user_id, department, zone, shift_start, shift_end, on_duty) VALUES (?, 'Housekeeper', 'All Floors', '08:00:00', '16:00:00', ?)");
        $stmt->bind_param("ii", $uid, $on);
        $stmt->execute();
        $stmt->close();
    } else {
        $conn->query("UPDATE staff_profiles SET on_duty=$on WHERE user_id=$uid");
    }
    echo json_encode(['success' => true]);
} elseif ($action === 'deactivate' && $role === 'admin') {
    $uid = (int)($_POST['user_id'] ?? 0);
    $conn->query("UPDATE users SET is_active=0 WHERE id=$uid AND role='staff'");
    echo json_encode(['success' => true]);
}
$conn->close();
?>
