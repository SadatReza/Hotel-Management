<?php
// get_rooms.php — Returns rooms as JSON (public)
require_once('includes/database.php');
header('Content-Type: application/json');

$where = "1=1";
$params = []; $types = "";

if (!empty($_GET['status'])) {
    $allowed = ['available','occupied','cleaning','maintenance'];
    if (in_array($_GET['status'], $allowed)) {
        $where .= " AND status = ?"; $params[] = $_GET['status']; $types .= "s";
    }
}
if (!empty($_GET['type'])) {
    $where .= " AND type = ?"; $params[] = $_GET['type']; $types .= "s";
}
if (!empty($_GET['min_price'])) {
    $where .= " AND price >= ?"; $params[] = (float)$_GET['min_price']; $types .= "d";
}
if (!empty($_GET['max_price'])) {
    $where .= " AND price <= ?"; $params[] = (float)$_GET['max_price']; $types .= "d";
}

$sql  = "SELECT * FROM rooms WHERE $where ORDER BY floor, room_number";
$stmt = $conn->prepare($sql);
if (!empty($params)) { $stmt->bind_param($types, ...$params); }
$stmt->execute();
$result = $stmt->get_result();

$rooms = [];
while ($row = $result->fetch_assoc()) {
    $row['amenities_array'] = array_map('trim', explode(',', $row['amenities'] ?? ''));
    $rooms[] = $row;
}
echo json_encode(['success' => true, 'data' => $rooms]);
$stmt->close(); $conn->close();
?>
