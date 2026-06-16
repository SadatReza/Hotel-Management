<?php
session_start();
require_once('../includes/database.php');
header('Content-Type: application/json');
if (!isset($_SESSION['logged_in']) || !in_array($_SESSION['role'], ['admin','staff'])) {
    echo json_encode(['success' => false, 'message' => 'Unauthorized.']); exit();
}
$action = $_POST['action'] ?? $_GET['action'] ?? 'get';
$role = $_SESSION['role']; $user_id = $_SESSION['user_id'];
if ($action === 'get') {
    if ($role === 'admin') { $result = $conn->query("SELECT t.*, u.first_name, u.last_name FROM admin_tasks t LEFT JOIN users u ON u.id=t.assigned_to ORDER BY t.created_at DESC"); }
    else { $stmt = $conn->prepare("SELECT t.*, u.first_name, u.last_name FROM admin_tasks t LEFT JOIN users u ON u.id=t.assigned_to WHERE t.assigned_to=? ORDER BY t.created_at DESC"); $stmt->bind_param("i", $user_id); $stmt->execute(); $result = $stmt->get_result(); }
    $tasks = []; while ($row = $result->fetch_assoc()) $tasks[] = $row;
    echo json_encode(['success' => true, 'data' => $tasks]);
} elseif ($action === 'add' && $role === 'admin') {
    $title = trim($_POST['title'] ?? ''); $desc = trim($_POST['description'] ?? '');
    $asgn = (int)($_POST['assigned_to'] ?? 0) ?: null; $pri = $_POST['priority'] ?? 'medium'; $due = $_POST['due_date'] ?? null;
    if (empty($due)) $due = null;
    $stmt = $conn->prepare("INSERT INTO admin_tasks (title,description,assigned_to,created_by,priority,due_date) VALUES (?,?,?,?,?,?)");
    $stmt->bind_param("ssiiss", $title, $desc, $asgn, $user_id, $pri, $due);
    echo json_encode(['success' => $stmt->execute()]);
} elseif ($action === 'update_status') {
    $tid = (int)($_POST['id'] ?? 0); $status = $_POST['status'] ?? '';
    if (!in_array($status, ['todo','in-progress','done','cancelled'])) { echo json_encode(['success' => false]); exit(); }
    $stmt = $conn->prepare("UPDATE admin_tasks SET status=? WHERE id=?");
    $stmt->bind_param("si", $status, $tid); echo json_encode(['success' => $stmt->execute()]);
} elseif ($action === 'delete' && $role === 'admin') {
    $tid = (int)($_POST['id'] ?? 0); $conn->query("DELETE FROM admin_tasks WHERE id=$tid");
    echo json_encode(['success' => true]);
}
$conn->close();
?>
