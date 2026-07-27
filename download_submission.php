<?php
/**
 * Secure PDF submission download.
 */

session_start();
require_once 'includes/db_connection.php';
require_once 'includes/tasks_setup.php';

if (!isset($_SESSION['user_id'])) {
    header("Location: index.php");
    exit();
}

$user_id = (int) $_SESSION['user_id'];
$user_role = $_SESSION['role'] ?? 'Student';
$task_id = intval($_GET['task_id'] ?? 0);

if (!isset($_SESSION['role'])) {
    $role_stmt = $conn->prepare("SELECT role FROM Users WHERE user_id = ?");
    $role_stmt->bind_param("i", $user_id);
    $role_stmt->execute();
    $role_result = $role_stmt->get_result();
    $_SESSION['role'] = $role_result->num_rows === 1 ? $role_result->fetch_assoc()['role'] : 'Student';
    $role_stmt->close();
    $user_role = $_SESSION['role'];
}

if ($task_id <= 0) {
    http_response_code(404);
    exit('Submission not found.');
}

$stmt = $conn->prepare("
    SELECT submission_pdf_path, submission_pdf_name, user_id, assigned_by
    FROM Tasks
    WHERE task_id = ?
");
$stmt->bind_param("i", $task_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows !== 1) {
    http_response_code(404);
    exit('Submission not found.');
}

$task = $result->fetch_assoc();
$stmt->close();

$can_download = $user_role === 'Admin'
    || (int) $task['user_id'] === $user_id
    || (int) $task['assigned_by'] === $user_id;

if (!$can_download || !$task['submission_pdf_path']) {
    http_response_code(403);
    exit('You do not have access to this submission.');
}

$base_dir = realpath(__DIR__ . DIRECTORY_SEPARATOR . 'uploads' . DIRECTORY_SEPARATOR . 'submissions');
$file_path = realpath(__DIR__ . DIRECTORY_SEPARATOR . $task['submission_pdf_path']);

if (!$base_dir || !$file_path || strpos($file_path, $base_dir) !== 0 || !is_file($file_path)) {
    http_response_code(404);
    exit('Submission file not found.');
}

$download_name = preg_replace('/[\r\n"]/', '', $task['submission_pdf_name'] ?: 'submitted-work.pdf');

header('Content-Type: application/pdf');
header('Content-Length: ' . filesize($file_path));
header('Content-Disposition: inline; filename="' . $download_name . '"');
header('X-Content-Type-Options: nosniff');

readfile($file_path);
exit();
