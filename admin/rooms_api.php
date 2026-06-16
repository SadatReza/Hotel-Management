<?php
session_start();
require_once('../includes/database.php');
header('Content-Type: application/json');

if (!isset($_SESSION['logged_in']) || !in_array($_SESSION['role'], ['admin', 'staff'])) {
    echo json_encode(['success' => false, 'message' => 'Unauthorized.']); exit();
}

$action = $_POST['action'] ?? $_GET['action'] ?? 'get';

if ($action === 'add') {
    if ($_SESSION['role'] !== 'admin') {
        echo json_encode(['success' => false, 'message' => 'Unauthorized.']); exit();
    }
    $room_number = trim($_POST['room_number'] ?? '');
    $type = $_POST['type'] ?? 'Deluxe';
    $floor = (int)($_POST['floor'] ?? 1);
    $price = (float)($_POST['price'] ?? 150);
    $capacity = (int)($_POST['capacity'] ?? 2);
    $amenities = trim($_POST['amenities'] ?? '');
    
    if (empty($room_number)) {
        echo json_encode(['success' => false, 'message' => 'Room number is required.']); exit();
    }
    
    // Check if room number already exists
    $stmt = $conn->prepare("SELECT id FROM rooms WHERE room_number = ?");
    $stmt->bind_param("s", $room_number);
    $stmt->execute();
    $stmt->store_result();
    if ($stmt->num_rows > 0) {
        echo json_encode(['success' => false, 'message' => 'Room number already exists.']); exit();
    }
    $stmt->close();
    
    $stmt = $conn->prepare("INSERT INTO rooms (room_number, type, floor, price, capacity, amenities, status) VALUES (?, ?, ?, ?, ?, ?, 'available')");
    $stmt->bind_param("ssidis", $room_number, $type, $floor, $price, $capacity, $amenities);
    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Room added successfully!']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Failed to add room.']);
    }
    $stmt->close();
}
elseif ($action === 'delete') {
    if ($_SESSION['role'] !== 'admin') {
        echo json_encode(['success' => false, 'message' => 'Unauthorized.']); exit();
    }
    $id = (int)($_POST['id'] ?? 0);
    if (!$id) { echo json_encode(['success' => false, 'message' => 'Room ID is required.']); exit(); }
    
    $stmt = $conn->prepare("DELETE FROM rooms WHERE id = ?");
    $stmt->bind_param("i", $id);
    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Room removed.']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Failed to remove room. Maybe it has active bookings.']);
    }
    $stmt->close();
}
elseif ($action === 'update') {
    $id = (int)($_POST['id'] ?? 0);
    $status = $_POST['status'] ?? '';
    $allowed = ['available','occupied','cleaning','maintenance'];
    if (!$id || !in_array($status, $allowed)) {
        echo json_encode(['success' => false, 'message' => 'Invalid parameters.']); exit();
    }
    
    $stmt = $conn->prepare("UPDATE rooms SET status = ? WHERE id = ?");
    $stmt->bind_param("si", $status, $id);
    if ($stmt->execute()) {
        echo json_encode(['success' => true, 'message' => 'Room updated.']);
    } else {
        echo json_encode(['success' => false, 'message' => 'Failed to update room.']);
    }
    $stmt->close();
}
$conn->close();
?>
