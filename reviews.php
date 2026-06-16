<?php
// reviews.php
session_start();
require_once('includes/database.php');
header('Content-Type: application/json');

$action = $_POST['action'] ?? $_GET['action'] ?? 'get';

// GET (public)
if ($action === 'get') {
    $result = $conn->query("SELECT rv.*, u.first_name, u.last_name FROM reviews rv
        JOIN users u ON u.id=rv.user_id WHERE rv.is_approved=1 ORDER BY rv.created_at DESC LIMIT 50");
    $reviews = [];
    while ($row = $result->fetch_assoc()) $reviews[] = $row;
    echo json_encode(['success' => true, 'data' => $reviews]);

// SUBMIT (guest)
} elseif ($action === 'submit') {
    if (!isset($_SESSION['logged_in']) || $_SESSION['role'] !== 'guest') {
        echo json_encode(['success' => false, 'message' => 'Login required.']); exit();
    }
    $rating    = (int)($_POST['rating']    ?? 0);
    $comment   = trim($_POST['comment']    ?? '');
    $booking_id = (int)($_POST['booking_id'] ?? 0);
    if ($rating < 1 || $rating > 5 || empty($comment)) {
        echo json_encode(['success' => false, 'message' => 'Rating (1-5) and comment are required.']); exit();
    }
    $user_id = $_SESSION['user_id'];
    $stmt = $conn->prepare("INSERT INTO reviews (user_id, booking_id, rating, comment) VALUES (?,?,?,?)");
    $bid  = $booking_id ?: null;
    $stmt->bind_param("iiis", $user_id, $bid, $rating, $comment);
    echo json_encode(['success' => $stmt->execute(), 'message' => 'Review submitted!']);
}
$conn->close();
?>
