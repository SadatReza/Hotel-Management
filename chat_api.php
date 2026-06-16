<?php
// chat_api.php — Same pattern as reference project
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

// Identify user (logged in or guest)
if (isset($_SESSION['email']) && !empty($_SESSION['email'])) {
    $current_email = $_SESSION['email'];
    $current_name  = $_SESSION['user_name'] ?? 'User';
} else {
    if (!isset($_SESSION['guest_chat_email'])) {
        $_SESSION['guest_chat_email'] = 'guest_' . substr(session_id(), 0, 8) . '@guest.local';
        $_SESSION['guest_chat_name']  = 'Guest User';
    }
    $current_email = $_SESSION['guest_chat_email'];
    $current_name  = $_SESSION['guest_chat_name'];
}

$action = $_POST['action'] ?? '';

// 1. SEND MESSAGE
if ($action === 'send_message') {
    $message = trim($_POST['message'] ?? '');
    if (empty($message)) { echo json_encode(['success' => false]); exit(); }
    
    // Support custom name/email from contact form
    $name  = trim($_POST['name'] ?? '');
    $email = trim($_POST['email'] ?? '');
    if (!empty($name))  $current_name  = $name;
    if (!empty($email)) $current_email = $email;
    
    $stmt = $conn->prepare("INSERT INTO chat_messages (message, message_type, user_name, user_email, is_read, created_at) VALUES (?, 'customer', ?, ?, 0, NOW())");
    $stmt->bind_param("sss", $message, $current_name, $current_email);
    $stmt->execute();
    echo json_encode(['success' => true]);
}

// 2. FETCH MESSAGES (guest sees their own thread)
elseif ($action === 'fetch_messages') {
    $stmt = $conn->prepare("SELECT * FROM chat_messages WHERE user_email = ? ORDER BY created_at ASC");
    $stmt->bind_param("s", $current_email);
    $stmt->execute();
    $result = $stmt->get_result();
    $messages = [];
    while ($row = $result->fetch_assoc()) {
        $messages[] = ['type' => $row['message_type'], 'message' => $row['message'],
                       'time' => date('H:i', strtotime($row['created_at']))];
    }
    echo json_encode(['success' => true, 'messages' => $messages]);
}
?>
