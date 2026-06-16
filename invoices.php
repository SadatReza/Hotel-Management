<?php
// invoices.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if (!isset($_SESSION['logged_in']) || !$_SESSION['logged_in']) {
    echo json_encode(['success' => false, 'message' => 'Not logged in.']); exit();
}

$user_id = $_SESSION['user_id'];
$role    = $_SESSION['role'];
$action  = $_GET['action'] ?? 'get';

if ($action === 'get') {
    if (in_array($role, ['admin','staff'])) {
        $result = $conn->query("SELECT inv.*, u.first_name, u.last_name, u.email AS user_email,
            r.room_number, b.check_in, b.check_out, b.nights, b.booking_ref
            FROM invoices inv JOIN bookings b ON b.id=inv.booking_id
            JOIN users u ON u.id=inv.user_id JOIN rooms r ON r.id=b.room_id
            ORDER BY inv.issued_at DESC");
    } else {
        $stmt = $conn->prepare("SELECT inv.*, r.room_number, r.type AS room_type,
            b.check_in, b.check_out, b.nights, b.booking_ref
            FROM invoices inv JOIN bookings b ON b.id=inv.booking_id JOIN rooms r ON r.id=b.room_id
            WHERE inv.user_id=? ORDER BY inv.issued_at DESC");
        $stmt->bind_param("i", $user_id); $stmt->execute();
        $result = $stmt->get_result();
    }
    $list = [];
    while ($row = $result->fetch_assoc()) $list[] = $row;
    echo json_encode(['success' => true, 'data' => $list]);

} elseif ($action === 'mark_paid' && $role === 'admin') {
    $inv_id = (int)($_GET['id'] ?? 0);
    $conn->query("UPDATE invoices SET status='paid', paid_at=NOW() WHERE id=$inv_id");
    echo json_encode(['success' => true]);
}
$conn->close();
?>
