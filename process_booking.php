<?php
// process_booking.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if (!isset($_SESSION['logged_in']) || !$_SESSION['logged_in']) {
    echo json_encode(['success' => false, 'message' => 'You must be logged in to book.']); exit();
}

$room_id          = (int)($_POST['room_id']          ?? 0);
$check_in         = trim($_POST['check_in']           ?? '');
$check_out        = trim($_POST['check_out']          ?? '');
$guests_count     = (int)($_POST['guests_count']      ?? 1);
$special_requests = trim($_POST['special_requests']   ?? '');
$user_id          = $_SESSION['user_id'];

// Validate
$errors = [];
if (!$room_id)           $errors[] = 'Room is required.';
if (empty($check_in))    $errors[] = 'Check-in date is required.';
if (empty($check_out))   $errors[] = 'Check-out date is required.';

$in  = new DateTime($check_in);
$out = new DateTime($check_out);
$today = new DateTime('today');

if ($in < $today)  $errors[] = 'Check-in cannot be in the past.';
if ($out <= $in)   $errors[] = 'Check-out must be after check-in.';

if (!empty($errors)) { echo json_encode(['success' => false, 'errors' => $errors]); exit(); }

$nights = $in->diff($out)->days;

// Get room & verify available
$stmt = $conn->prepare("SELECT * FROM rooms WHERE id = ?");
$stmt->bind_param("i", $room_id);
$stmt->execute();
$room = $stmt->get_result()->fetch_assoc();
$stmt->close();

if (!$room) { echo json_encode(['success' => false, 'message' => 'Room not found.']); exit(); }
if ($room['status'] !== 'available') {
    echo json_encode(['success' => false, 'message' => 'Room is currently ' . $room['status'] . '.']); exit();
}

// Overlap check
$stmt = $conn->prepare("SELECT id FROM bookings WHERE room_id=? AND status NOT IN ('cancelled','checked_out') AND check_in < ? AND check_out > ?");
$stmt->bind_param("iss", $room_id, $check_out, $check_in);
$stmt->execute(); $stmt->store_result();
if ($stmt->num_rows > 0) {
    echo json_encode(['success' => false, 'message' => 'Room is already booked for those dates.']); exit();
}
$stmt->close();

$total = $room['price'] * $nights;

// Insert booking with temp ref
$stmt = $conn->prepare("INSERT INTO bookings (booking_ref, user_id, room_id, check_in, check_out, nights, guests_count, status, total_amount, special_requests) VALUES ('TMP', ?, ?, ?, ?, ?, ?, 'pending', ?, ?)");
$stmt->bind_param("iisssiis", $user_id, $room_id, $check_in, $check_out, $nights, $guests_count, $total, $special_requests);
$stmt->execute();
$booking_id = $stmt->insert_id;
$stmt->close();

// Set real booking_ref
$booking_ref = 'BK' . str_pad($booking_id, 4, '0', STR_PAD_LEFT);
$conn->query("UPDATE bookings SET booking_ref='$booking_ref' WHERE id=$booking_id");

// Auto-create invoice (15% tax)
$tax        = round($total * 0.15, 2);
$inv_total  = $total + $tax;
$inv_ref    = 'INV' . str_pad($booking_id, 4, '0', STR_PAD_LEFT);
$stmt = $conn->prepare("INSERT INTO invoices (invoice_ref, booking_id, user_id, room_charges, tax_amount, total_amount, status, issued_at) VALUES (?, ?, ?, ?, ?, ?, 'issued', NOW())");
$stmt->bind_param("siiddd", $inv_ref, $booking_id, $user_id, $total, $tax, $inv_total);
$stmt->execute(); $stmt->close();

echo json_encode(['success' => true, 'message' => 'Booking confirmed!',
    'booking_ref' => $booking_ref, 'total' => $total, 'nights' => $nights]);

$conn->close();
?>
