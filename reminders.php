<?php
/**
 * Smart Reminder System - Notifications & Reminders
 * Automated reminders for academic deadlines
 */

session_start();
require_once 'includes/db_connection.php';

// Check if user is logged in
if (!isset($_SESSION['user_id'])) {
    header("Location: index.php");
    exit();
}

$user_id = $_SESSION['user_id'];

if (!isset($_SESSION['role'])) {
    $role_stmt = $conn->prepare("SELECT role FROM Users WHERE user_id = ?");
    $role_stmt->bind_param("i", $user_id);
    $role_stmt->execute();
    $role_result = $role_stmt->get_result();
    $_SESSION['role'] = $role_result->num_rows === 1 ? $role_result->fetch_assoc()['role'] : 'Student';
    $role_stmt->close();
}

if (($_SESSION['role'] ?? '') === 'Admin') {
    header("Location: admin.php");
    exit();
}

// Get reminders (tasks due soon)
$reminders_query = "
    SELECT task_id, title, due_date, priority, status,
    CASE 
        WHEN DATE(due_date) = CURDATE() THEN 'Today'
        WHEN DATE(due_date) = DATE_ADD(CURDATE(), INTERVAL 1 DAY) THEN 'Tomorrow'
        WHEN DATE(due_date) BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 3 DAY) THEN 'Within 3 Days'
        WHEN DATE(due_date) BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 'Within 7 Days'
        WHEN DATE(due_date) < CURDATE() THEN 'Overdue'
    END AS reminder_type
    FROM Tasks 
    WHERE user_id = $user_id 
    AND status = 'Pending'
    AND due_date >= DATE_SUB(CURDATE(), INTERVAL 1 DAY)
    ORDER BY due_date ASC
";

$reminders_result = $conn->query($reminders_query);

// Get overdue tasks
$overdue_query = "
    SELECT task_id, title, due_date, priority 
    FROM Tasks 
    WHERE user_id = $user_id 
    AND status = 'Pending'
    AND due_date < CURDATE()
    ORDER BY due_date ASC
";

$overdue_result = $conn->query($overdue_query);
$overdue_count = $overdue_result->num_rows;

// Get 24-hour urgent reminders
$urgent_query = "
    SELECT task_id, title, due_date, priority, status
    FROM Tasks 
    WHERE user_id = $user_id 
    AND status = 'Pending'
    AND due_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 1 DAY)
    ORDER BY due_date ASC
";

$urgent_result = $conn->query($urgent_query);
$urgent_count = $urgent_result->num_rows;

?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reminders & Notifications - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body>
    <?php include 'includes/navbar.php'; ?>

    <div class="container">
        <div class="page-header">
            <h2>Notifications & Reminders</h2>
            <p>Stay on top of your academic deadlines</p>
        </div>

        <!-- Alert Summary -->
        <div class="alert-summary">
            <?php if ($urgent_count > 0): ?>
                <div class="alert alert-urgent">
                    <span class="alert-icon"><i class="bi bi-megaphone-fill" style="color:var(--danger)"></i></span>
                    <div class="alert-content">
                        <strong><?php echo $urgent_count; ?> Urgent Reminder<?php echo $urgent_count != 1 ? 's' : ''; ?></strong>
                        <p>You have <?php echo $urgent_count; ?> task<?php echo $urgent_count != 1 ? 's' : ''; ?> due within the next 24 hours!</p>
                    </div>
                </div>
            <?php endif; ?>

            <?php if ($overdue_count > 0): ?>
                <div class="alert alert-overdue">
                    <span class="alert-icon"><i class="bi bi-alarm-fill" style="color:#d97706"></i></span>
                    <div class="alert-content">
                        <strong><?php echo $overdue_count; ?> Overdue Task<?php echo $overdue_count != 1 ? 's' : ''; ?></strong>
                        <p>You have <?php echo $overdue_count; ?> overdue task<?php echo $overdue_count != 1 ? 's' : ''; ?> that needs attention!</p>
                    </div>
                </div>
            <?php endif; ?>

            <?php if ($urgent_count == 0 && $overdue_count == 0): ?>
                <div class="alert alert-success">
                    <span class="alert-icon"><i class="bi bi-check-circle-fill" style="color:var(--success)"></i></span>
                    <div class="alert-content">
                        <strong>All Clear!</strong>
                        <p>No urgent reminders or overdue tasks. Keep up the great work!</p>
                    </div>
                </div>
            <?php endif; ?>
        </div>

        <!-- Urgent Reminders (24 hours) -->
        <?php if ($urgent_count > 0): ?>
            <section class="reminders-section urgent-section">
                <div class="section-header">
                    <h3><i class="bi bi-exclamation-triangle-fill" style="color:var(--danger);margin-right:6px"></i> Due Within 24 Hours</h3>
                </div>
                <div class="reminders-list">
                    <?php 
                    $urgent_result->data_seek(0);
                    while ($task = $urgent_result->fetch_assoc()): 
                    ?>
                        <div class="reminder-card urgent-reminder">
                            <div class="reminder-icon"><i class="bi bi-exclamation-triangle-fill" style="color:var(--danger)"></i></div>
                            <div class="reminder-content">
                                <h4><?php echo htmlspecialchars($task['title']); ?></h4>
                                <p class="reminder-time">
                                    Due: <strong><?php echo date('F j, Y \a\t g:i A', strtotime($task['due_date'])); ?></strong>
                                </p>
                                <p class="time-remaining">
                                    <?php 
                                    $due_time = strtotime($task['due_date']);
                                    $now = time();
                                    $diff = $due_time - $now;
                                    
                                    if ($diff > 0) {
                                        $hours = floor($diff / 3600);
                                        $minutes = floor(($diff % 3600) / 60);
                                        echo '<i class="bi bi-stopwatch" style="color:var(--danger)"></i> ' . $hours . 'h ' . $minutes . 'm remaining';
                                    } else {
                                        echo '<i class="bi bi-alarm-fill" style="color:#d97706"></i> Overdue by ' . abs(floor($diff / 3600)) . 'h';
                                    }
                                    ?>
                                </p>
                            </div>
                            <div class="reminder-priority">
                                <span class="badge badge-<?php echo strtolower($task['priority']); ?>">
                                    <?php echo $task['priority']; ?>
                                </span>
                            </div>
                        </div>
                    <?php endwhile; ?>
                </div>
            </section>
        <?php endif; ?>

        <!-- Overdue Tasks -->
        <?php if ($overdue_count > 0): ?>
            <section class="reminders-section overdue-section">
                <div class="section-header">
                    <h3><i class="bi bi-alarm-fill" style="color:#d97706;margin-right:6px"></i> Overdue Tasks</h3>
                </div>
                <div class="reminders-list">
                    <?php while ($task = $overdue_result->fetch_assoc()): ?>
                        <div class="reminder-card overdue-reminder">
                            <div class="reminder-icon"><i class="bi bi-x-circle-fill" style="color:#d97706"></i></div>
                            <div class="reminder-content">
                                <h4><?php echo htmlspecialchars($task['title']); ?></h4>
                                <p class="reminder-time">
                                    Was due: <strong><?php echo date('F j, Y \a\t g:i A', strtotime($task['due_date'])); ?></strong>
                                </p>
                                <p class="overdue-text">
                                    <?php 
                                    $due_time = strtotime($task['due_date']);
                                    $now = time();
                                    $diff = $now - $due_time;
                                    
                                    $days = floor($diff / 86400);
                                    $hours = floor(($diff % 86400) / 3600);
                                    
                                    echo '<i class="bi bi-pin-angle-fill" style="color:#d97706"></i> Overdue by ' . $days . 'd ' . $hours . 'h';
                                    ?>
                                </p>
                            </div>
                            <div class="reminder-priority">
                                <span class="badge badge-<?php echo strtolower($task['priority']); ?>">
                                    <?php echo $task['priority']; ?>
                                </span>
                            </div>
                        </div>
                    <?php endwhile; ?>
                </div>
            </section>
        <?php endif; ?>

        <!-- All Pending Reminders -->
        <section class="reminders-section">
            <div class="section-header">
                <h3><i class="bi bi-list-check" style="color:var(--accent);margin-right:6px"></i> All Pending Reminders</h3>
            </div>
            <?php if ($reminders_result->num_rows > 0): ?>
                <div class="reminders-list">
                    <?php while ($task = $reminders_result->fetch_assoc()): ?>
                        <div class="reminder-card status-<?php echo strtolower($task['status']); ?>">
                            <div class="reminder-type-badge">
                                <?php 
                                $badge_class = 'reminder-badge-default';
                                $icon = '<i class="bi bi-calendar3"></i>';
                                
                                if ($task['reminder_type'] == 'Today') {
                                    $badge_class = 'reminder-badge-today';
                                    $icon = '<i class="bi bi-exclamation-triangle-fill"></i>';
                                } elseif ($task['reminder_type'] == 'Tomorrow') {
                                    $badge_class = 'reminder-badge-tomorrow';
                                    $icon = '<i class="bi bi-clock-fill"></i>';
                                } elseif ($task['reminder_type'] == 'Overdue') {
                                    $badge_class = 'reminder-badge-overdue';
                                    $icon = '<i class="bi bi-x-circle-fill"></i>';
                                }
                                ?>
                                <span class="reminder-badge <?php echo $badge_class; ?>">
                                    <?php echo $icon; ?> <?php echo $task['reminder_type']; ?>
                                </span>
                            </div>
                            <div class="reminder-content">
                                <h4><?php echo htmlspecialchars($task['title']); ?></h4>
                                <p class="reminder-time">
                                    Due: <?php echo date('F j, Y \a\t g:i A', strtotime($task['due_date'])); ?>
                                </p>
                            </div>
                            <div class="reminder-priority">
                                <span class="badge badge-<?php echo strtolower($task['priority']); ?>">
                                    <?php echo $task['priority']; ?>
                                </span>
                            </div>
                        </div>
                    <?php endwhile; ?>
                </div>
            <?php else: ?>
                <div class="empty-state">
                    <p>No pending reminders. All tasks are on track!</p>
                </div>
            <?php endif; ?>
        </section>

        <div class="section-footer">
            <a href="tasks.php" class="btn btn-primary">Manage Tasks</a>
            <a href="dashboard.php" class="btn btn-secondary">Back to Dashboard</a>
        </div>
    </div>

    <script src="js/script.js"></script>
</body>
</html>
