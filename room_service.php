<?php
// room_service.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if (!isset($_SESSION['logged_in']) || !$_SESSION['logged_in']) {
    echo json_encode(['success' => false, 'message' => 'Not logged in.']); exit();
}

$action  = $_POST['action'] ?? 'place';
$user_id = $_SESSION['user_id'];
$role    = $_SESSION['role'];

// PLACE ORDER (guest)
if ($action === 'place') {
    $booking_id = (int)($_POST['booking_id'] ?? 0);
    $items_raw  = $_POST['items'] ?? '';       // JSON string from frontend
    $notes      = trim($_POST['notes'] ?? '');

    $items = json_decode($items_raw, true);
    if (!$booking_id || empty($items)) {
        echo json_encode(['success' => false, 'message' => 'Booking and items are required.']); exit();
    }

    // Verify booking belongs to user
    $stmt = $conn->prepare("SELECT room_id FROM bookings WHERE id = ? AND user_id = ?");
    $stmt->bind_param("ii", $booking_id, $user_id);
    $stmt->execute();
    $bk = $stmt->get_result()->fetch_assoc();
    $stmt->close();
    if (!$bk) { echo json_encode(['success' => false, 'message' => 'Booking not found.']); exit(); }

    $room_id = $bk['room_id'];
    $total   = array_reduce($items, fn($carry, $i) => $carry + ($i['qty'] * $i['price']), 0);
    $items_json = json_encode($items);

    $stmt = $conn->prepare("INSERT INTO service_orders (order_ref, booking_id, user_id, room_id, items, total, notes) VALUES ('TMP',?,?,?,?,?,?)");
    $stmt->bind_param("iiisds", $booking_id, $user_id, $room_id, $items_json, $total, $notes);
    $stmt->execute();
    $oid = $stmt->insert_id; $stmt->close();
    $ref = 'ORD' . str_pad($oid, 4, '0', STR_PAD_LEFT);
    $conn->query("UPDATE service_orders SET order_ref='$ref' WHERE id=$oid");
    echo json_encode(['success' => true, 'order_ref' => $ref, 'total' => $total]);
}

// UPDATE STATUS (staff/admin)
elseif ($action === 'update_status') {
    if (!in_array($role, ['admin','staff'])) {
        echo json_encode(['success' => false, 'message' => 'Unauthorized.']); exit();
    }
    $order_id  = (int)($_POST['order_id'] ?? 0);
    $new_status = $_POST['status'] ?? '';
    $allowed   = ['pending','preparing','on-the-way','delivered','cancelled'];
    if (!$order_id || !in_array($new_status, $allowed)) {
        echo json_encode(['success' => false, 'message' => 'Invalid data.']); exit();
    }
    if ($new_status === 'preparing') {
        $stmt = $conn->prepare("UPDATE service_orders SET status=?, assigned_to=? WHERE id=?");
        $stmt->bind_param("sii", $new_status, $user_id, $order_id);
    } else {
        $stmt = $conn->prepare("UPDATE service_orders SET status=? WHERE id=?");
        $stmt->bind_param("si", $new_status, $order_id);
    }
    echo json_encode(['success' => $stmt->execute()]);
}

// GET ORDERS
elseif ($action === 'get') {
    if (in_array($role, ['admin','staff'])) {
        $result = $conn->query("SELECT o.*, u.first_name, u.last_name, r.room_number
            FROM service_orders o JOIN users u ON u.id=o.user_id JOIN rooms r ON r.id=o.room_id
            ORDER BY o.ordered_at DESC");
    } else {
        $stmt = $conn->prepare("SELECT o.*, r.room_number FROM service_orders o JOIN rooms r ON r.id=o.room_id WHERE o.user_id=? ORDER BY o.ordered_at DESC");
        $stmt->bind_param("i", $user_id); $stmt->execute();
        $result = $stmt->get_result();
    }
    $orders = [];
    while ($row = $result->fetch_assoc()) {
        $row['items_array'] = json_decode($row['items'], true);
        $orders[] = $row;
    }
    echo json_encode(['success' => true, 'data' => $orders]);
}
$conn->close();
?>
