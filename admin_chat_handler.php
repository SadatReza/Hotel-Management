<?php
// admin_chat_handler.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

if (!isset($_SESSION['logged_in']) || $_SESSION['role'] !== 'admin') {
    echo json_encode(['success' => false, 'error' => 'Unauthorized']); exit();
}

$action = $_POST['action'] ?? '';

// 1. GET ALL CHAT USERS (inbox list)
if ($action === 'get_inbox') {
    $result = $conn->query("SELECT user_email, user_name, MAX(created_at) AS last_message,
        SUM(CASE WHEN message_type='customer' AND is_read=0 THEN 1 ELSE 0 END) AS unread
        FROM chat_messages GROUP BY user_email, user_name ORDER BY last_message DESC");
    $users = [];
    while ($row = $result->fetch_assoc()) $users[] = $row;
    echo json_encode(['success' => true, 'users' => $users]);
}

// 2. FETCH CONVERSATION
elseif ($action === 'fetch_conversation') {
    $email = $_POST['email'] ?? '';
    $conn->query("UPDATE chat_messages SET is_read=1 WHERE user_email='".mysqli_real_escape_string($conn,$email)."' AND message_type='customer'");
    $stmt = $conn->prepare("SELECT * FROM chat_messages WHERE user_email = ? ORDER BY created_at ASC");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();
    $messages = [];
    while ($row = $result->fetch_assoc()) $messages[] = $row;
    echo json_encode(['success' => true, 'messages' => $messages]);
}

// 3. SEND REPLY
elseif ($action === 'send_reply') {
    $email     = $_POST['email']     ?? '';
    $user_name = $_POST['user_name'] ?? '';
    $message   = trim($_POST['message'] ?? '');
    if (empty($message) || empty($email)) { echo json_encode(['success' => false]); exit(); }
    $stmt = $conn->prepare("INSERT INTO chat_messages (message, message_type, user_name, user_email, is_read, created_at) VALUES (?, 'admin', ?, ?, 1, NOW())");
    $stmt->bind_param("sss", $message, $user_name, $email);
    echo json_encode(['success' => $stmt->execute()]);
}
?>
