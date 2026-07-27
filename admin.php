<?php
/**
 * Smart Reminder System - Admin Panel
 * Manage users, tasks, and platform progress.
 */

session_start();
require_once 'includes/db_connection.php';
require_once 'includes/subjects_setup.php';
require_once 'includes/tasks_setup.php';
require_once 'includes/calendar_setup.php';

if (!isset($_SESSION['user_id'])) {
    header("Location: index.php");
    exit();
}

if (!isset($_SESSION['role'])) {
    $role_stmt = $conn->prepare("SELECT role FROM Users WHERE user_id = ?");
    $role_stmt->bind_param("i", $_SESSION['user_id']);
    $role_stmt->execute();
    $role_result = $role_stmt->get_result();
    $_SESSION['role'] = $role_result->num_rows === 1 ? $role_result->fetch_assoc()['role'] : 'Student';
    $role_stmt->close();
}

$is_admin = ($_SESSION['role'] ?? '') === 'Admin';

if (!$is_admin) {
    header("Location: dashboard.php");
    exit();
}

$message = '';
$message_type = '';

$active_tab = $_GET['tab'] ?? 'dashboard';

function delete_submission_pdf(?string $relative_path): void
{
    if (!$relative_path) {
        return;
    }

    $base_dir = realpath(__DIR__ . DIRECTORY_SEPARATOR . 'uploads' . DIRECTORY_SEPARATOR . 'submissions');
    $file_path = realpath(__DIR__ . DIRECTORY_SEPARATOR . $relative_path);

    if ($base_dir && $file_path && strpos($file_path, $base_dir) === 0 && is_file($file_path)) {
        unlink($file_path);
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['update_task'])) {
    $task_id = intval($_POST['task_id']);
    $status_values = ['Pending', 'Submitted', 'Completed'];
    $status = in_array($_POST['status'] ?? 'Pending', $status_values, true) ? $_POST['status'] : 'Pending';
    $priority_values = ['Low', 'Medium', 'High'];
    $priority = in_array($_POST['priority'], $priority_values, true) ? $_POST['priority'] : 'Medium';

    $stmt = $conn->prepare("UPDATE Tasks SET status = ?, priority = ? WHERE task_id = ?");
    $stmt->bind_param("ssi", $status, $priority, $task_id);

    if ($stmt->execute()) {
        $message = "Task updated successfully.";
        $message_type = 'success';
    } else {
        $message = "Task update failed.";
        $message_type = 'error';
    }

    $stmt->close();
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['delete_task'])) {
    $task_id = intval($_POST['task_id']);
    $pdf_path = null;

    $file_stmt = $conn->prepare("SELECT submission_pdf_path FROM Tasks WHERE task_id = ?");
    $file_stmt->bind_param("i", $task_id);
    $file_stmt->execute();
    $file_result = $file_stmt->get_result();
    if ($file_result->num_rows === 1) {
        $pdf_path = $file_result->fetch_assoc()['submission_pdf_path'] ?? null;
    }
    $file_stmt->close();

    $stmt = $conn->prepare("DELETE FROM Tasks WHERE task_id = ?");
    $stmt->bind_param("i", $task_id);

    if ($stmt->execute()) {
        delete_submission_pdf($pdf_path);
        $message = "Task deleted successfully.";
        $message_type = 'success';
    } else {
        $message = "Task delete failed.";
        $message_type = 'error';
    }

    $stmt->close();
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['delete_user'])) {
    $delete_user_id = intval($_POST['user_id']);

    if ($delete_user_id === (int) $_SESSION['user_id']) {
        $message = "You cannot delete your own admin account while signed in.";
        $message_type = 'error';
    } else {
        $stmt = $conn->prepare("DELETE FROM Users WHERE user_id = ?");
        $stmt->bind_param("i", $delete_user_id);

        if ($stmt->execute()) {
            $message = "User and related tasks deleted successfully.";
            $message_type = 'success';
        } else {
            $message = "User delete failed.";
            $message_type = 'error';
        }

        $stmt->close();
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['add_subject'])) {
    $subject_name = trim($_POST['subject_name'] ?? '');
    $monthly_fee = (float) ($_POST['monthly_fee'] ?? 0);

    if ($subject_name === '') {
        $message = "Subject name is required.";
        $message_type = 'error';
    } else {
        $stmt = $conn->prepare("INSERT INTO Subjects (subject_name, monthly_fee) VALUES (?, ?)");
        $stmt->bind_param("sd", $subject_name, $monthly_fee);

        try {
            $stmt->execute();
            $message = "Subject added successfully.";
            $message_type = 'success';
        } catch (mysqli_sql_exception $exception) {
            $message = "Subject already exists or could not be added.";
            $message_type = 'error';
        }

        $stmt->close();
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['delete_subject'])) {
    $subject_id = intval($_POST['subject_id']);

    $stmt = $conn->prepare("DELETE FROM Subjects WHERE subject_id = ?");
    $stmt->bind_param("i", $subject_id);

    if ($stmt->execute()) {
        $message = "Subject deleted successfully.";
        $message_type = 'success';
    } else {
        $message = "Subject delete failed.";
        $message_type = 'error';
    }

    $stmt->close();
}

$total_users = $conn->query("SELECT COUNT(*) AS total FROM Users")->fetch_assoc()['total'];
$total_tasks = $conn->query("SELECT COUNT(*) AS total FROM Tasks")->fetch_assoc()['total'];
$completed_tasks = $conn->query("SELECT COUNT(*) AS total FROM Tasks WHERE status = 'Completed'")->fetch_assoc()['total'];
$pending_tasks = $conn->query("SELECT COUNT(*) AS total FROM Tasks WHERE status = 'Pending'")->fetch_assoc()['total'];
$total_subjects = $conn->query("SELECT COUNT(*) AS total FROM Subjects")->fetch_assoc()['total'];
$total_points = $completed_tasks * 10;

$users_result = $conn->query("
    SELECT u.user_id, u.username, u.email, u.created_at,
        u.role,
        COUNT(DISTINCT t.task_id) AS task_count,
        COUNT(DISTINCT CASE WHEN t.status = 'Completed' THEN t.task_id END) AS completed_count,
        GROUP_CONCAT(DISTINCT s.subject_name ORDER BY s.subject_name SEPARATOR ', ') AS subjects
    FROM Users u
    LEFT JOIN Tasks t ON u.user_id = t.user_id
    LEFT JOIN UserSubjects us ON u.user_id = us.user_id
    LEFT JOIN Subjects s ON us.subject_id = s.subject_id
    GROUP BY u.user_id, u.username, u.email, u.role, u.created_at
    ORDER BY u.created_at DESC
");

$tasks_result = $conn->query("
    SELECT t.task_id, t.title, t.due_date, t.priority, t.status, t.submission_note,
        t.submission_pdf_path, t.submission_pdf_name, t.submitted_at,
        u.username, tutor.username AS tutor_name
    FROM Tasks t
    INNER JOIN Users u ON t.user_id = u.user_id
    LEFT JOIN Users tutor ON t.assigned_by = tutor.user_id
    ORDER BY t.due_date ASC
");

$subjects_result = $conn->query("
    SELECT s.subject_id, s.subject_name, s.monthly_fee, COUNT(us.user_subject_id) AS learner_count
    FROM Subjects s
    LEFT JOIN UserSubjects us ON s.subject_id = us.subject_id
    GROUP BY s.subject_id, s.subject_name, s.monthly_fee
    ORDER BY s.subject_name ASC
");

// ── Admin Calendar (all users) ──
$cal_view = $_GET['cal_view'] ?? 'month';
$cal_year = intval($_GET['cal_year'] ?? date('Y'));
$cal_month = intval($_GET['cal_month'] ?? date('m'));
$cal_today = date('Y-m-d');
$cal_week_start = $_GET['cal_week_start'] ?? date('Y-m-d', strtotime('sunday this week'));
$cal_week_ts = strtotime($cal_week_start);

$cal_first_day = mktime(0, 0, 0, $cal_month, 1, $cal_year);
$cal_days_in_month = date('t', $cal_first_day);
$cal_start_dow = date('w', $cal_first_day);
$cal_prev_month_ts = mktime(0, 0, 0, $cal_month - 1, 1, $cal_year);
$cal_prev_days = date('t', $cal_prev_month_ts);
$cal_month_name = date('F Y', $cal_first_day);

$cal_prev_month_link = $cal_month == 1 ? 12 : $cal_month - 1;
$cal_prev_year_link = $cal_month == 1 ? $cal_year - 1 : $cal_year;
$cal_next_month_link = $cal_month == 12 ? 1 : $cal_month + 1;
$cal_next_year_link = $cal_month == 12 ? $cal_year + 1 : $cal_year;

$cal_month_start = "$cal_year-$cal_month-01";
$cal_month_end = date('Y-m-t', $cal_first_day);

// Fetch ALL calendar events with username
$cal_events_result = $conn->query("
    SELECT ce.event_id, ce.title, ce.description, ce.event_type,
           ce.event_date, ce.event_time, ce.end_date, ce.end_time, ce.color,
           u.username
    FROM CalendarEvents ce
    INNER JOIN Users u ON ce.user_id = u.user_id
    WHERE (ce.event_date BETWEEN '$cal_month_start' AND '$cal_month_end'
           OR (ce.end_date IS NOT NULL AND ce.end_date >= '$cal_month_start' AND ce.event_date <= '$cal_month_end'))
    ORDER BY ce.event_date ASC, ce.event_time ASC
");
$cal_events_by_date = [];
while ($ev = $cal_events_result->fetch_assoc()) {
    $d = $ev['event_date'];
    if (!isset($cal_events_by_date[$d])) $cal_events_by_date[$d] = [];
    $cal_events_by_date[$d][] = $ev;
}

// Fetch ALL pending tasks with username
$cal_tasks_result = $conn->query("
    SELECT t.task_id, t.title, t.due_date, t.priority, u.username
    FROM Tasks t
    INNER JOIN Users u ON t.user_id = u.user_id
    WHERE t.status = 'Pending'
    AND DATE(t.due_date) BETWEEN '$cal_month_start' AND '$cal_month_end'
    ORDER BY t.due_date ASC
");
while ($tk = $cal_tasks_result->fetch_assoc()) {
    $d = date('Y-m-d', strtotime($tk['due_date']));
    if (!isset($cal_events_by_date[$d])) $cal_events_by_date[$d] = [];
    $cal_events_by_date[$d][] = [
        'event_id' => null,
        'title' => $tk['title'],
        'event_type' => 'task-deadline',
        'event_time' => date('H:i:s', strtotime($tk['due_date'])),
        'color' => null,
        'username' => $tk['username'],
    ];
}

function cal_get_week_days($ts) {
    $sunday = strtotime('sunday', $ts);
    $days = [];
    for ($i = 0; $i < 7; $i++) {
        $days[] = date('Y-m-d', strtotime("+$i day", $sunday));
    }
    return $days;
}
$cal_week_days = cal_get_week_days($cal_week_ts);
$cal_week_prev = date('Y-m-d', strtotime('-7 days', $cal_week_ts));
$cal_week_next = date('Y-m-d', strtotime('+7 days', $cal_week_ts));

$cal_day_names = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
$cal_event_icons = [
    'meeting' => 'bi-people-fill',
    'exam' => 'bi-pencil-fill',
    'workshop' => 'bi-tools',
    'deadline' => 'bi-clock-fill',
    'task-deadline' => 'bi-check2-square',
];
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Panel - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body>
    <?php include 'includes/navbar.php'; ?>

    <div class="container">
        <div class="page-header">
            <div>
                <h2>Admin Panel</h2>
                <p>Manage Intuthuko College users, tasks, and reward progress.</p>
            </div>
        </div>

        <?php if ($message): ?>
            <div class="alert alert-<?php echo $message_type; ?>">
                <?php echo htmlspecialchars($message); ?>
            </div>
        <?php endif; ?>

        <!-- Tab Navigation -->
        <div class="admin-tabs">
            <a href="?tab=dashboard" class="admin-tab <?php echo $active_tab === 'dashboard' ? 'active' : ''; ?>">
                <i class="bi bi-speedometer2"></i> Dashboard
            </a>
            <a href="?tab=subjects" class="admin-tab <?php echo $active_tab === 'subjects' ? 'active' : ''; ?>">
                <i class="bi bi-book-fill"></i> Subjects <span class="tab-badge"><?php echo $total_subjects; ?></span>
            </a>
            <a href="?tab=users" class="admin-tab <?php echo $active_tab === 'users' ? 'active' : ''; ?>">
                <i class="bi bi-people-fill"></i> Users <span class="tab-badge"><?php echo $total_users; ?></span>
            </a>
            <a href="?tab=tasks" class="admin-tab <?php echo $active_tab === 'tasks' ? 'active' : ''; ?>">
                <i class="bi bi-list-task"></i> Tasks <span class="tab-badge"><?php echo $pending_tasks; ?></span>
            </a>
            <a href="?tab=calendar" class="admin-tab <?php echo $active_tab === 'calendar' ? 'active' : ''; ?>">
                <i class="bi bi-calendar3"></i> Calendar
            </a>
        </div>

        <div class="page-content">

            <!-- ── Dashboard Tab ── -->
            <div class="admin-section <?php echo $active_tab === 'dashboard' ? 'active' : ''; ?>" id="tab-dashboard">
                <section class="section">
                    <div class="section-header">
                        <h3>Platform Overview</h3>
                        <p>Real-time summary of all activity</p>
                    </div>
                    <div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:16px">
                        <div style="background:var(--gray-100);border-radius:var(--radius-sm);padding:18px;text-align:center">
                            <span style="font-size:32px;color:var(--accent)"><i class="bi bi-people-fill"></i></span>
                            <h4 style="font-size:28px;margin:8px 0 2px;color:var(--dark)"><?php echo $total_users; ?></h4>
                            <p style="color:var(--gray-600);font-size:13px;font-weight:600">Total Users</p>
                        </div>
                        <div style="background:var(--gray-100);border-radius:var(--radius-sm);padding:18px;text-align:center">
                            <span style="font-size:32px;color:#d97706"><i class="bi bi-list-task"></i></span>
                            <h4 style="font-size:28px;margin:8px 0 2px;color:var(--dark)"><?php echo $total_tasks; ?></h4>
                            <p style="color:var(--gray-600);font-size:13px;font-weight:600">Total Tasks</p>
                        </div>
                        <div style="background:var(--gray-100);border-radius:var(--radius-sm);padding:18px;text-align:center">
                            <span style="font-size:32px;color:var(--success)"><i class="bi bi-check-circle-fill"></i></span>
                            <h4 style="font-size:28px;margin:8px 0 2px;color:var(--dark)"><?php echo $completed_tasks; ?></h4>
                            <p style="color:var(--gray-600);font-size:13px;font-weight:600">Completed Tasks</p>
                        </div>
                        <div style="background:var(--gray-100);border-radius:var(--radius-sm);padding:18px;text-align:center">
                            <span style="font-size:32px;color:#b8860b"><i class="bi bi-trophy-fill"></i></span>
                            <h4 style="font-size:28px;margin:8px 0 2px;color:var(--dark)"><?php echo $total_points; ?></h4>
                            <p style="color:var(--gray-600);font-size:13px;font-weight:600">Reward Points</p>
                        </div>
                        <div style="background:var(--gray-100);border-radius:var(--radius-sm);padding:18px;text-align:center">
                            <span style="font-size:32px;color:var(--accent)"><i class="bi bi-book-fill"></i></span>
                            <h4 style="font-size:28px;margin:8px 0 2px;color:var(--dark)"><?php echo $total_subjects; ?></h4>
                            <p style="color:var(--gray-600);font-size:13px;font-weight:600">Subjects</p>
                        </div>
                    </div>
                </section>

                <section class="section">
                    <div class="section-header">
                        <h3>Quick Actions</h3>
                    </div>
                    <div style="display:flex;gap:12px;flex-wrap:wrap">
                        <a href="?tab=subjects" class="btn btn-primary"><i class="bi bi-plus-lg"></i> Manage Subjects</a>
                        <a href="?tab=users" class="btn btn-outline"><i class="bi bi-people-fill"></i> View Users</a>
                        <a href="?tab=tasks" class="btn btn-outline"><i class="bi bi-list-task"></i> View Tasks</a>
                        <a href="?tab=calendar" class="btn btn-outline"><i class="bi bi-calendar3"></i> View Calendar</a>
                    </div>
                </section>
            </div>

            <!-- ── Subjects Tab ── -->
            <div class="admin-section <?php echo $active_tab === 'subjects' ? 'active' : ''; ?>" id="tab-subjects">
                <section class="section">
                    <div class="section-header">
                        <h3>Subjects</h3>
                        <p>Manage subject offerings and fees</p>
                    </div>
                    <form method="POST" class="admin-subject-form">
                        <div class="form-group">
                            <label for="subject_name">Subject Name</label>
                            <input type="text" id="subject_name" name="subject_name" placeholder="Example: Geography">
                        </div>
                        <div class="form-group">
                            <label for="monthly_fee">Monthly Fee (ZAR)</label>
                            <input type="number" id="monthly_fee" name="monthly_fee" min="0" step="0.01" placeholder="0.00">
                        </div>
                        <button type="submit" name="add_subject" class="btn btn-primary">Add Subject</button>
                    </form>

                    <div class="table-responsive">
                        <table class="tasks-table">
                            <thead>
                                <tr>
                                    <th>Subject</th>
                                    <th>Monthly Fee</th>
                                    <th>Learners</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <?php while ($subject = $subjects_result->fetch_assoc()): ?>
                                    <tr>
                                        <td class="task-title"><?php echo htmlspecialchars($subject['subject_name']); ?></td>
                                        <td>R<?php echo number_format($subject['monthly_fee'], 2); ?></td>
                                        <td><?php echo (int) $subject['learner_count']; ?></td>
                                        <td>
                                            <button type="button" class="btn btn-sm btn-danger" onclick="confirmAction('Delete this subject from the sign-up options?', function() { document.getElementById('delete_subject_<?php echo $subject['subject_id']; ?>').submit(); })">Delete</button>
                                            <form method="POST" id="delete_subject_<?php echo $subject['subject_id']; ?>" style="display:none">
                                                <input type="hidden" name="subject_id" value="<?php echo $subject['subject_id']; ?>">
                                                <input type="hidden" name="delete_subject" value="1">
                                            </form>
                                        </td>
                                    </tr>
                                <?php endwhile; ?>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>

            <!-- ── Users Tab ── -->
            <div class="admin-section <?php echo $active_tab === 'users' ? 'active' : ''; ?>" id="tab-users">
                <section class="section">
                    <div class="section-header">
                        <h3>Users</h3>
                        <p><?php echo $total_users; ?> registered users</p>
                    </div>
                    <div class="admin-search">
                        <i class="bi bi-search"></i>
                        <input type="text" id="userSearch" placeholder="Search by name, email, or role..." oninput="filterTable('userSearch', 'usersTableBody')">
                    </div>
                    <div class="table-responsive">
                        <table class="tasks-table">
                            <thead>
                                <tr>
                                    <th>Username</th>
                                    <th>Email</th>
                                    <th>Role</th>
                                    <th>Subjects</th>
                                    <th>Tasks</th>
                                    <th>Reward Points</th>
                                    <th>Joined</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="usersTableBody">
                                <?php while ($user = $users_result->fetch_assoc()): ?>
                                    <tr>
                                        <td class="task-title"><?php echo htmlspecialchars($user['username']); ?></td>
                                        <td><?php echo htmlspecialchars($user['email']); ?></td>
                                        <td>
                                            <span class="badge <?php echo $user['role'] === 'Admin' ? 'badge-high' : ($user['role'] === 'Tutor' ? 'badge-medium' : 'badge-low'); ?>">
                                                <?php echo htmlspecialchars($user['role']); ?>
                                            </span>
                                        </td>
                                        <td class="task-desc"><?php echo htmlspecialchars($user['subjects'] ?: 'None selected'); ?></td>
                                        <td><?php echo (int) $user['task_count']; ?></td>
                                        <td><?php echo ((int) $user['completed_count']) * 10; ?></td>
                                        <td style="font-size:13px;color:var(--gray-600)"><?php echo date('M j, Y', strtotime($user['created_at'])); ?></td>
                                        <td>
                                            <?php if ((int) $user['user_id'] !== (int) $_SESSION['user_id']): ?>
                                                <button type="button" class="btn btn-sm btn-danger" onclick="confirmAction('Delete this user and all related tasks?', function() { document.getElementById('delete_user_<?php echo $user['user_id']; ?>').submit(); })">Delete</button>
                                                <form method="POST" id="delete_user_<?php echo $user['user_id']; ?>" style="display:none">
                                                    <input type="hidden" name="user_id" value="<?php echo $user['user_id']; ?>">
                                                    <input type="hidden" name="delete_user" value="1">
                                                </form>
                                            <?php else: ?>
                                                <span style="color:var(--gray-500);font-size:13px;font-weight:600"><i class="bi bi-shield-check"></i> You</span>
                                            <?php endif; ?>
                                        </td>
                                    </tr>
                                <?php endwhile; ?>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>

            <!-- ── Tasks Tab ── -->
            <div class="admin-section <?php echo $active_tab === 'tasks' ? 'active' : ''; ?>" id="tab-tasks">
                <section class="section">
                    <div class="section-header">
                        <h3>All Tasks</h3>
                        <p><?php echo $pending_tasks; ?> pending across the platform</p>
                    </div>
                    <div class="admin-search">
                        <i class="bi bi-search"></i>
                        <input type="text" id="taskSearch" placeholder="Search by title, user, tutor..." oninput="filterTable('taskSearch', 'tasksTableBody')">
                    </div>
                    <div class="table-responsive">
                        <table class="tasks-table">
                            <thead>
                                <tr>
                                    <th>Task</th>
                                    <th>User</th>
                                    <th>Tutor</th>
                                    <th>Due Date</th>
                                    <th>Priority</th>
                                    <th>Status</th>
                                    <th>Submission</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="tasksTableBody">
                                <?php while ($task = $tasks_result->fetch_assoc()): ?>
                                    <tr class="<?php echo $task['priority'] === 'High' && $task['status'] !== 'Completed' ? 'priority-high' : ''; ?>">
                                        <td class="task-title"><?php echo htmlspecialchars($task['title']); ?></td>
                                        <td><?php echo htmlspecialchars($task['username']); ?></td>
                                        <td><?php echo htmlspecialchars($task['tutor_name'] ?: '—'); ?></td>
                                        <td style="font-size:13px"><?php echo date('M j, Y - g:i A', strtotime($task['due_date'])); ?></td>
                                        <td>
                                            <span class="badge <?php echo $task['priority'] === 'High' ? 'badge-high' : ($task['priority'] === 'Medium' ? 'badge-medium' : 'badge-low'); ?>">
                                                <?php echo $task['priority']; ?>
                                            </span>
                                        </td>
                                        <td>
                                            <span class="badge <?php echo $task['status'] === 'Completed' ? 'badge-completed' : ($task['status'] === 'Submitted' ? 'badge-submitted' : 'badge-pending'); ?>">
                                                <?php echo $task['status']; ?>
                                            </span>
                                        </td>
                                        <td>
                                            <?php if ($task['submission_note'] || $task['submission_pdf_path']): ?>
                                                <?php if ($task['submission_note']): ?>
                                                    <p class="task-submission"><?php echo htmlspecialchars($task['submission_note']); ?></p>
                                                <?php endif; ?>
                                                <?php if ($task['submission_pdf_path']): ?>
                                                    <a class="submission-file-link" href="download_submission.php?task_id=<?php echo (int) $task['task_id']; ?>" target="_blank" rel="noopener">
                                                        <i class="bi bi-file-pdf"></i> View PDF
                                                    </a>
                                                <?php endif; ?>
                                            <?php else: ?>
                                                <span class="task-desc">No submission</span>
                                            <?php endif; ?>
                                        </td>
                                        <td>
                                            <button type="button" class="btn btn-sm btn-danger" onclick="confirmAction('Delete this task?', function() { document.getElementById('delete_task_<?php echo $task['task_id']; ?>').submit(); })">Delete</button>
                                            <form method="POST" id="delete_task_<?php echo $task['task_id']; ?>" style="display:none">
                                                <input type="hidden" name="task_id" value="<?php echo $task['task_id']; ?>">
                                                <input type="hidden" name="delete_task" value="1">
                                            </form>
                                        </td>
                                    </tr>
                                <?php endwhile; ?>
                            </tbody>
                        </table>
                    </div>
                </section>

                <section class="section">
                    <div class="section-header">
                        <h3>Quick Update</h3>
                        <p>Change priority and status inline</p>
                    </div>
                    <div class="table-responsive">
                        <table class="tasks-table">
                            <thead>
                                <tr>
                                    <th>Task</th>
                                    <th>User</th>
                                    <th>Tutor</th>
                                    <th>Due Date</th>
                                    <th colspan="2">Priority / Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <?php
                                $tasks_result2 = $conn->query("
                                    SELECT t.task_id, t.title, t.due_date, t.priority, t.status,
                                        u.username, tutor.username AS tutor_name
                                    FROM Tasks t
                                    INNER JOIN Users u ON t.user_id = u.user_id
                                    LEFT JOIN Users tutor ON t.assigned_by = tutor.user_id
                                    ORDER BY t.due_date ASC
                                ");
                                while ($task = $tasks_result2->fetch_assoc()):
                                ?>
                                    <tr>
                                        <td class="task-title"><?php echo htmlspecialchars($task['title']); ?></td>
                                        <td><?php echo htmlspecialchars($task['username']); ?></td>
                                        <td><?php echo htmlspecialchars($task['tutor_name'] ?: '—'); ?></td>
                                        <td style="font-size:13px"><?php echo date('M j, Y', strtotime($task['due_date'])); ?></td>
                                        <td colspan="2">
                                            <form method="POST" class="admin-inline-form" style="display:flex;gap:6px;align-items:center;flex-wrap:wrap">
                                                <input type="hidden" name="task_id" value="<?php echo $task['task_id']; ?>">
                                                <select name="priority" class="status-select">
                                                    <option value="Low" <?php echo $task['priority'] === 'Low' ? 'selected' : ''; ?>>Low</option>
                                                    <option value="Medium" <?php echo $task['priority'] === 'Medium' ? 'selected' : ''; ?>>Medium</option>
                                                    <option value="High" <?php echo $task['priority'] === 'High' ? 'selected' : ''; ?>>High</option>
                                                </select>
                                                <select name="status" class="status-select">
                                                    <option value="Pending" <?php echo $task['status'] === 'Pending' ? 'selected' : ''; ?>>Pending</option>
                                                    <option value="Submitted" <?php echo $task['status'] === 'Submitted' ? 'selected' : ''; ?>>Submitted</option>
                                                    <option value="Completed" <?php echo $task['status'] === 'Completed' ? 'selected' : ''; ?>>Completed</option>
                                                </select>
                                                <button type="submit" name="update_task" class="btn btn-sm btn-primary">Save</button>
                                            </form>
                                        </td>
                                    </tr>
                                <?php endwhile; ?>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>

            <!-- ── Calendar Tab ── -->
            <div class="admin-section <?php echo $active_tab === 'calendar' ? 'active' : ''; ?>" id="tab-calendar">
                <section class="section">
                    <div class="section-header">
                        <h3>All Users Calendar</h3>
                        <p>Events and deadlines across all users</p>
                    </div>

                    <!-- Legend -->
                    <div class="calendar-legend">
                        <span class="legend-item"><span class="legend-dot" style="background:#1e40af"></span> Meeting</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#c62828"></span> Exam</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#2e7d32"></span> Workshop</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#e65100"></span> Deadline</span>
                        <span class="legend-item"><span class="legend-dot" style="background:#6a1b9a"></span> Task Due</span>
                    </div>

                    <!-- Navigation -->
                    <div class="calendar-nav">
                        <div class="calendar-nav-actions">
                            <?php if ($cal_view === 'month'): ?>
                                <a href="?tab=calendar&cal_view=month&cal_month=<?php echo $cal_prev_month_link; ?>&cal_year=<?php echo $cal_prev_year_link; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-left"></i></a>
                                <h2><?php echo $cal_month_name; ?></h2>
                                <a href="?tab=calendar&cal_view=month&cal_month=<?php echo $cal_next_month_link; ?>&cal_year=<?php echo $cal_next_year_link; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-right"></i></a>
                            <?php else: ?>
                                <a href="?tab=calendar&cal_view=week&cal_week_start=<?php echo $cal_week_prev; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-left"></i></a>
                                <h2><?php echo date('M j', $cal_week_ts) . ' - ' . date('M j, Y', strtotime($cal_week_days[6])); ?></h2>
                                <a href="?tab=calendar&cal_view=week&cal_week_start=<?php echo $cal_week_next; ?>" class="btn btn-ghost btn-sm"><i class="bi bi-chevron-right"></i></a>
                            <?php endif; ?>
                        </div>
                        <div class="calendar-nav-actions">
                            <a href="?tab=calendar&cal_view=month" class="btn btn-ghost btn-sm"><i class="bi bi-calendar3"></i> Today</a>
                            <div class="view-toggle">
                                <a href="?tab=calendar&cal_view=month&cal_year=<?php echo $cal_year; ?>&cal_month=<?php echo $cal_month; ?>" class="view-btn <?php echo $cal_view === 'month' ? 'active' : ''; ?>">Month</a>
                                <a href="?tab=calendar&cal_view=week&cal_week_start=<?php echo $cal_today; ?>" class="view-btn <?php echo $cal_view === 'week' ? 'active' : ''; ?>">Week</a>
                            </div>
                        </div>
                    </div>

                    <!-- Month View -->
                    <?php if ($cal_view === 'month'): ?>
                    <div class="calendar-grid">
                    <?php foreach ($cal_day_names as $dn): ?>
                        <div class="calendar-header"><?php echo $dn; ?></div>
                    <?php endforeach; ?>

                    <?php
                    $cal_day = 1;
                    $cal_cell_count = $cal_start_dow + $cal_days_in_month;
                    $cal_rows = ceil($cal_cell_count / 7);
                    $cal_total_cells = $cal_rows * 7;

                    for ($i = 0; $i < $cal_total_cells; $i++):
                        $cal_is_filler = $i < $cal_start_dow || $cal_day > $cal_days_in_month;
                        if ($cal_is_filler && $i < $cal_start_dow) {
                            $pd = $cal_prev_days - $cal_start_dow + $i + 1;
                            $cal_display = $pd;
                            $cal_date_str = date('Y-m-d', mktime(0,0,0,$cal_month-1,$pd,$cal_year));
                        } elseif ($cal_is_filler) {
                            $fd = $i - $cal_days_in_month - $cal_start_dow + 1;
                            $cal_display = $fd;
                            $cal_date_str = date('Y-m-d', mktime(0,0,0,$cal_month+1,$fd,$cal_year));
                        } else {
                            $cal_display = $cal_day;
                            $cal_date_str = date('Y-m-d', mktime(0,0,0,$cal_month,$cal_day,$cal_year));
                            $cal_day++;
                        }
                        $cal_cls = $cal_is_filler ? 'calendar-day other-month' : 'calendar-day';
                        if ($cal_date_str === $cal_today) $cal_cls .= ' today';
                    ?>
                        <div class="<?php echo $cal_cls; ?>">
                            <div class="day-number"><?php echo $cal_display; ?></div>
                            <?php if (isset($cal_events_by_date[$cal_date_str])): ?>
                                <?php
                                $cal_shown = 0;
                                $cal_max_visible = 3;
                                foreach ($cal_events_by_date[$cal_date_str] as $ev):
                                    if ($cal_shown >= $cal_max_visible) break;
                                    $cal_shown++;
                                    $et = $ev['event_type'];
                                    $cal_icon = $cal_event_icons[$et] ?? 'bi-calendar-event';
                                ?>
                                    <div class="calendar-event event-<?php echo $et; ?>"
                                         onclick="event.stopPropagation(); showAdminEventDetail(<?php echo htmlspecialchars(json_encode($ev)); ?>)">
                                        <i class="bi <?php echo $cal_icon; ?>"></i>
                                        <?php echo htmlspecialchars(mb_strimwidth($ev['title'], 0, 14, '…')); ?>
                                        <span style="font-weight:400;opacity:0.8">(<?php echo htmlspecialchars($ev['username'] ?? '?'); ?>)</span>
                                    </div>
                                <?php endforeach; ?>
                                <?php if (count($cal_events_by_date[$cal_date_str]) > $cal_max_visible): ?>
                                    <div class="event-more">+<?php echo count($cal_events_by_date[$cal_date_str]) - $cal_max_visible; ?> more</div>
                                <?php endif; ?>
                            <?php endif; ?>
                        </div>
                    <?php endfor; ?>
                </div>

                <!-- Week View -->
                <?php else: ?>
                <div class="week-grid">
                    <div class="week-header">Time</div>
                    <?php foreach ($cal_week_days as $wd): ?>
                        <div class="week-header">
                            <?php echo $cal_day_names[date('w', strtotime($wd))]; ?>
                            <span class="date-label"><?php echo date('M j', strtotime($wd)); ?></span>
                        </div>
                    <?php endforeach; ?>

                    <?php
                    $cal_slots = [
                        '08:00', '09:00', '10:00', '11:00', '12:00',
                        '13:00', '14:00', '15:00', '16:00', '17:00', '18:00'
                    ];
                    foreach ($cal_slots as $slot):
                    ?>
                        <div class="week-time"><?php echo $slot; ?></div>
                        <?php foreach ($cal_week_days as $wd):
                            $cal_day_events = $cal_events_by_date[$wd] ?? [];
                            $cal_slot_events = array_filter($cal_day_events, function($e) use ($slot) {
                                $et = $e['event_time'] ?? '';
                                return $et !== '' && substr($et, 0, 5) === $slot;
                            });
                        ?>
                            <div class="week-cell">
                                <?php foreach ($cal_slot_events as $ev):
                                    $et = $ev['event_type'];
                                    $cal_icon = $cal_event_icons[$et] ?? 'bi-calendar-event';
                                ?>
                                    <div class="calendar-event event-<?php echo $et; ?>"
                                         onclick="event.stopPropagation(); showAdminEventDetail(<?php echo htmlspecialchars(json_encode($ev)); ?>)">
                                        <i class="bi <?php echo $cal_icon; ?>"></i>
                                        <?php echo htmlspecialchars(mb_strimwidth($ev['title'], 0, 12, '…')); ?>
                                    </div>
                                <?php endforeach; ?>
                            </div>
                        <?php endforeach; ?>
                    <?php endforeach; ?>
                </div>
                <?php endif; ?>
                </section>
            </div>

        </div>
    </div>

    <!-- Admin Event Detail Modal -->
    <div id="adminEventDetailModal" class="modal">
        <div class="modal-content" style="max-width:440px">
            <span class="close-modal" onclick="closeModal('adminEventDetailModal')">&times;</span>
            <div id="adminEventDetailContent"></div>
        </div>
    </div>

    <!-- Confirm Action Modal -->
    <div id="confirmModal" class="modal">
        <div class="modal-content" style="max-width:400px">
            <span class="close-modal" onclick="closeModal('confirmModal')">&times;</span>
            <div id="confirmContent"></div>
        </div>
    </div>

    <script>
    var confirmCallback = null;

    function confirmAction(message, callback) {
        confirmCallback = callback;
        var html = '<div class="confirm-icon"><i class="bi bi-exclamation-triangle-fill" style="color:var(--danger)"></i></div>';
        html += '<h3 style="text-align:center;margin-bottom:8px">Are you sure?</h3>';
        html += '<p style="text-align:center;color:var(--gray-600);margin-bottom:8px">' + escapeHtml(message) + '</p>';
        html += '<div class="confirm-actions">';
        html += '<button class="btn btn-secondary" onclick="closeModal(\'confirmModal\')">Cancel</button>';
        html += '<button class="btn btn-danger" onclick="executeConfirm()">Delete</button>';
        html += '</div>';
        document.getElementById('confirmContent').innerHTML = html;
        openModal('confirmModal');
    }

    function executeConfirm() {
        closeModal('confirmModal');
        if (confirmCallback) {
            confirmCallback();
            confirmCallback = null;
        }
    }

    function filterTable(searchId, tableBodyId) {
        var input = document.getElementById(searchId);
        var filter = input.value.toLowerCase();
        var rows = document.getElementById(tableBodyId).getElementsByTagName('tr');
        for (var i = 0; i < rows.length; i++) {
            var text = rows[i].textContent.toLowerCase();
            rows[i].style.display = text.indexOf(filter) > -1 ? '' : 'none';
        }
    }

    function showAdminEventDetail(event) {
        const types = { meeting: 'Meeting', exam: 'Exam', workshop: 'Workshop', deadline: 'Deadline', 'task-deadline': 'Task Due' };
        const icons = { meeting: 'bi-people-fill', exam: 'bi-pencil-fill', workshop: 'bi-tools', deadline: 'bi-clock-fill', 'task-deadline': 'bi-check2-square' };
        const icon = icons[event.event_type] || 'bi-calendar-event';
        const typeLabel = types[event.event_type] || event.event_type;
        const time = event.event_time ? new Date('1970-01-01T' + event.event_time).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'}) : '—';
        const desc = event.description || 'No description.';
        const user = event.username || 'Unknown';

        let html = '<div style="text-align:center;margin-bottom:16px">';
        html += '<span style="font-size:40px;color:var(--primary)"><i class="bi ' + icon + '"></i></span>';
        html += '</div>';
        html += '<h3 style="text-align:center;margin-bottom:4px">' + escapeHtml(event.title) + '</h3>';
        html += '<p style="text-align:center;color:var(--gray-600);font-size:13px;margin-bottom:16px">' + typeLabel + '</p>';
        html += '<div style="display:grid;gap:10px">';
        html += '<p><strong>User:</strong> ' + escapeHtml(user) + '</p>';
        html += '<p><strong>Date:</strong> ' + event.event_date + '</p>';
        html += '<p><strong>Time:</strong> ' + time + '</p>';
        if (event.end_date) html += '<p><strong>End Date:</strong> ' + event.end_date + '</p>';
        html += '<p><strong>Description:</strong> ' + escapeHtml(desc) + '</p>';
        html += '</div>';

        document.getElementById('adminEventDetailContent').innerHTML = html;
        openModal('adminEventDetailModal');
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }
    </script>

    <script src="js/script.js"></script>
</body>
</html>
