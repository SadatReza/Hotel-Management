<?php
// update_booking_status.php  (staff / admin)
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if (!isset($_SESSION['logged_in']) || !in_array($_SESSION['role'], ['admin','staff'])) {
    echo json_encode(['success' => false, 'message' => 'Unauthorized.']); exit();
}

$booking_id = (int)($_POST['booking_id'] ?? 0);
$new_status = $_POST['status'] ?? '';
$allowed    = ['pending','approved','confirmed','checked_in','checked_out','cancelled'];

if (!$booking_id || !in_array($new_status, $allowed)) {
    echo json_encode(['success' => false, 'message' => 'Invalid data.']); exit();
}

// Get room_id for syncing room status
$stmt = $conn->prepare("SELECT room_id FROM bookings WHERE id = ?");
$stmt->bind_param("i", $booking_id);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$row) { echo json_encode(['success' => false, 'message' => 'Booking not found.']); exit(); }
$room_id = $row['room_id'];

$stmt = $conn->prepare("UPDATE bookings SET status = ? WHERE id = ?");
$stmt->bind_param("si", $new_status, $booking_id);
$stmt->execute(); $stmt->close();

// Sync room status
if (in_array($new_status, ['checked_out','cancelled'])) {
    $conn->query("UPDATE rooms SET status='available' WHERE id=$room_id");
} elseif ($new_status === 'checked_in') {
    $conn->query("UPDATE rooms SET status='occupied' WHERE id=$room_id");
}

echo json_encode(['success' => true, 'message' => 'Status updated to ' . $new_status . '.']);
$conn->close();
?>
