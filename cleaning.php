<?php
// cleaning.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if (!isset($_SESSION['logged_in']) || !$_SESSION['logged_in']) {
    echo json_encode(['success' => false, 'message' => 'Not logged in.']); exit();
}

$action  = $_POST['action'] ?? $_GET['action'] ?? 'get';
$user_id = $_SESSION['user_id'];
$role    = $_SESSION['role'];

if ($action === 'request') {
    $room_id  = (int)($_POST['room_id']  ?? 0);
    $priority = $_POST['priority'] ?? 'normal';
    $notes    = trim($_POST['notes'] ?? '');
    $allowed  = ['low','normal','high','urgent'];
    if (!in_array($priority, $allowed)) $priority = 'normal';
    if (!$room_id) { echo json_encode(['success' => false, 'message' => 'Room is required.']); exit(); }

    $stmt = $conn->prepare("INSERT INTO cleaning_requests (request_ref, room_id, requested_by, priority, notes) VALUES ('TMP',?,?,?,?)");
    $stmt->bind_param("iiss", $room_id, $user_id, $priority, $notes);
    $stmt->execute();
    $cid = $stmt->insert_id; $stmt->close();
    $ref = 'CR' . str_pad($cid, 4, '0', STR_PAD_LEFT);
    $conn->query("UPDATE cleaning_requests SET request_ref='$ref' WHERE id=$cid");
    $conn->query("UPDATE rooms SET status='cleaning' WHERE id=$room_id");
    echo json_encode(['success' => true, 'request_ref' => $ref]);

} elseif ($action === 'update' && in_array($role, ['admin','staff'])) {
    $cid        = (int)($_POST['id'] ?? 0);
    $new_status = $_POST['status'] ?? '';
    $allowed    = ['pending','in-progress','done','cancelled'];
    if (!in_array($new_status, $allowed)) { echo json_encode(['success' => false]); exit(); }
    if ($new_status === 'done') {
        $row = $conn->query("SELECT room_id FROM cleaning_requests WHERE id=$cid")->fetch_assoc();
        if ($row) $conn->query("UPDATE rooms SET status='available' WHERE id=".$row['room_id']);
        $conn->query("UPDATE cleaning_requests SET status='done', completed_at=NOW() WHERE id=$cid");
    } else {
        if ($new_status === 'in-progress') {
            $stmt = $conn->prepare("UPDATE cleaning_requests SET status=?, assigned_to=? WHERE id=?");
            $stmt->bind_param("sii", $new_status, $user_id, $cid);
            $stmt->execute();
            $stmt->close();
        } else {
            $stmt = $conn->prepare("UPDATE cleaning_requests SET status=? WHERE id=?");
            $stmt->bind_param("si", $new_status, $cid);
            $stmt->execute();
            $stmt->close();
        }
    }
    echo json_encode(['success' => true]);

} elseif ($action === 'get') {
    if (in_array($role, ['admin','staff'])) {
        $result = $conn->query("SELECT cr.*, r.room_number,
            u1.first_name AS req_first, u1.last_name AS req_last,
            u2.first_name AS asgn_first, u2.last_name AS asgn_last
            FROM cleaning_requests cr
            JOIN rooms r ON r.id=cr.room_id
            JOIN users u1 ON u1.id=cr.requested_by
            LEFT JOIN users u2 ON u2.id=cr.assigned_to
            ORDER BY cr.requested_at DESC");
    } else {
        $stmt = $conn->prepare("SELECT cr.*, r.room_number FROM cleaning_requests cr JOIN rooms r ON r.id=cr.room_id WHERE cr.requested_by=? ORDER BY cr.requested_at DESC");
        $stmt->bind_param("i", $user_id); $stmt->execute();
        $result = $stmt->get_result();
    }
    $list = [];
    while ($row = $result->fetch_assoc()) $list[] = $row;
    echo json_encode(['success' => true, 'data' => $list]);
}
$conn->close();
?>
