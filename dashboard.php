<?php
/**
 * Smart Reminder System - Dashboard
 * At-a-glance view of academic responsibilities
 */

session_start();
require_once 'includes/db_connection.php';
require_once 'includes/subjects_setup.php';
require_once 'includes/tasks_setup.php';

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

$user_role = $_SESSION['role'] ?? 'Student';
$task_scope = $user_role === 'Tutor' ? "assigned_by = $user_id" : "user_id = $user_id";

// Handle add/drop subject from modal
if ($_SERVER['REQUEST_METHOD'] === 'POST' && $user_role === 'Student') {
    if (isset($_POST['add_subject_btn'])) {
        $sid = intval($_POST['subject_id']);
        $check = $conn->prepare("SELECT 1 FROM UserSubjects WHERE user_id = ? AND subject_id = ?");
        $check->bind_param("ii", $user_id, $sid);
        $check->execute();
        if ($check->get_result()->num_rows === 0) {
            $conn->prepare("INSERT INTO UserSubjects (user_id, subject_id) VALUES (?, ?)")->execute([$user_id, $sid]);
        }
        $check->close();
        header("Location: dashboard.php");
        exit();
    }
    if (isset($_POST['drop_subject_btn'])) {
        $sid = intval($_POST['subject_id']);
        $conn->prepare("DELETE FROM UserSubjects WHERE user_id = ? AND subject_id = ?")->execute([$user_id, $sid]);
        header("Location: dashboard.php");
        exit();
    }
}

// Get total tasks count
$total_query = "SELECT COUNT(*) as total FROM Tasks WHERE $task_scope";
$total_result = $conn->query($total_query);
$total_tasks = $total_result->fetch_assoc()['total'];

// Get pending tasks count
$pending_query = "SELECT COUNT(*) as pending FROM Tasks WHERE $task_scope AND status = 'Pending'";
$pending_result = $conn->query($pending_query);
$pending_tasks = $pending_result->fetch_assoc()['pending'];

// Get completed tasks count for rewards
$completed_query = "SELECT COUNT(*) as completed FROM Tasks WHERE $task_scope AND status = 'Completed'";
$completed_result = $conn->query($completed_query);
$completed_tasks = $completed_result->fetch_assoc()['completed'];
$reward_points = $completed_tasks * 10;

// Get tasks due today and tomorrow
$today_query = "SELECT COUNT(*) as today FROM Tasks WHERE $task_scope AND status = 'Pending' AND DATE(due_date) = CURDATE()";
$today_result = $conn->query($today_query);
$tasks_today = $today_result->fetch_assoc()['today'];

// Get high priority pending tasks
$high_priority_query = "SELECT task_id, title, due_date, priority FROM Tasks WHERE $task_scope AND priority = 'High' AND status = 'Pending' ORDER BY due_date ASC LIMIT 5";
$high_priority_result = $conn->query($high_priority_query);

// Get upcoming deadlines (next 7 days)
$upcoming_query = "SELECT task_id, title, due_date, priority, status FROM Tasks WHERE $task_scope AND due_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) ORDER BY due_date ASC LIMIT 10";
$upcoming_result = $conn->query($upcoming_query);

$subjects_query = "
    SELECT s.subject_id, s.subject_name, s.monthly_fee
    FROM UserSubjects us
    INNER JOIN Subjects s ON us.subject_id = s.subject_id
    WHERE us.user_id = $user_id
    ORDER BY s.subject_name ASC
";
$subjects_result = $conn->query($subjects_query);
$enrolled_subjects = [];
$subject_total = 0;
while ($row = $subjects_result->fetch_assoc()) {
    $enrolled_subjects[] = $row;
    $subject_total += (float) $row['monthly_fee'];
}

$available_result = $conn->query("
    SELECT subject_id, subject_name, monthly_fee
    FROM Subjects
    WHERE subject_id NOT IN (SELECT subject_id FROM UserSubjects WHERE user_id = $user_id)
    ORDER BY subject_name ASC
");

?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body>
    <?php include 'includes/navbar.php'; ?>

    <div class="container">
        <div class="page-header">
            <h2>Welcome to Your Dashboard</h2>
            <p>Your academic responsibilities, reminders, and rewards at a glance</p>
        </div>

        <!-- Summary Cards -->
        <div class="summary-cards">
            <div class="card card-primary">
                <div class="card-icon"><i class="bi bi-bar-chart-fill"></i></div>
                <div class="card-content">
                    <h3>Total Tasks</h3>
                    <p class="card-number"><?php echo $total_tasks; ?></p>
                </div>
            </div>

            <div class="card card-warning">
                <div class="card-icon"><i class="bi bi-hourglass-split"></i></div>
                <div class="card-content">
                    <h3>Pending Tasks</h3>
                    <p class="card-number"><?php echo $pending_tasks; ?></p>
                </div>
            </div>

            <div class="card card-danger">
                <div class="card-icon"><i class="bi bi-calendar-event-fill"></i></div>
                <div class="card-content">
                    <h3>Due Today</h3>
                    <p class="card-number"><?php echo $tasks_today; ?></p>
                </div>
            </div>

            <div class="card card-success">
                <div class="card-icon"><i class="bi bi-check-circle-fill"></i></div>
                <div class="card-content">
                    <h3>Completion Rate</h3>
                    <p class="card-number"><?php echo $total_tasks > 0 ? round((($total_tasks - $pending_tasks) / $total_tasks) * 100) : 0; ?>%</p>
                </div>
            </div>

            <div class="card card-reward">
                <div class="card-icon"><i class="bi bi-trophy-fill"></i></div>
                <div class="card-content">
                    <h3>Reward Points</h3>
                    <p class="card-number"><?php echo $reward_points; ?></p>
                </div>
            </div>
        </div>

        <section class="reward-banner">
            <div>
                <p class="reward-label">Task Rewards</p>
                <h3>Earn 10 points for every finished task.</h3>
                <p>Complete your work, build consistency, and track your progress as you stay ahead of deadlines.</p>
            </div>
            <a href="tasks.php" class="btn btn-primary">Finish Tasks</a>
        </section>

        <?php if ($user_role === 'Student'): ?>
            <section class="subject-summary">
                <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap;">
                    <div>
                        <p class="reward-label">Selected Subjects</p>
                        <h3>Your tutoring plan</h3>
                    </div>
                    <?php if (count($enrolled_subjects) > 0): ?>
                        <button class="btn btn-outline btn-sm" onclick="openModal('manageSubjectsModal')">
                            <i class="bi bi-gear-fill" style="margin-right:4px"></i> Manage Subjects
                        </button>
                    <?php endif; ?>
                </div>
                <?php if (count($enrolled_subjects) > 0): ?>
                    <div class="subject-pill-list">
                        <?php foreach ($enrolled_subjects as $subject): ?>
                            <span class="subject-pill">
                                <?php echo htmlspecialchars($subject['subject_name']); ?>
                                <small>R<?php echo number_format($subject['monthly_fee'], 2); ?></small>
                            </span>
                        <?php endforeach; ?>
                    </div>
                    <strong class="subject-total">Monthly total: R<?php echo number_format($subject_total, 2); ?></strong>
                <?php else: ?>
                    <p>No subjects selected yet.</p>
                    <button class="btn btn-primary" onclick="openModal('manageSubjectsModal')">Choose Subjects</button>
                <?php endif; ?>
            </section>
        <?php elseif ($user_role === 'Tutor'): ?>
            <section class="subject-summary">
                <div>
                    <p class="reward-label">Tutor Workspace</p>
                    <h3>Manage lessons, reminders, and learner support tasks.</h3>
                </div>
                <p>Your tutor account focuses on academic planning, task completion, and deadline follow-up.</p>
            </section>
        <?php endif; ?>

        <div class="page-content">
            <!-- High Priority Tasks -->
            <section class="section">
                <div class="section-header">
                    <h3><i class="bi bi-exclamation-triangle-fill" style="color:var(--danger);margin-right:6px"></i> High Priority Tasks</h3>
                    <a href="tasks.php" class="view-all-link">View All</a>
                </div>
                <?php if ($high_priority_result->num_rows > 0): ?>
                    <div class="task-list">
                        <?php while ($task = $high_priority_result->fetch_assoc()): ?>
                            <div class="task-item priority-high">
                                <div class="task-info">
                                    <h4><?php echo htmlspecialchars($task['title']); ?></h4>
                                    <p class="task-due">Due: <?php echo date('F j, Y - g:i A', strtotime($task['due_date'])); ?></p>
                                </div>
                                <div class="task-priority">
                                    <span class="badge badge-high"><?php echo $task['priority']; ?></span>
                                </div>
                            </div>
                        <?php endwhile; ?>
                    </div>
                <?php else: ?>
                    <p class="empty-state">No high priority tasks. Great job!</p>
                <?php endif; ?>
            </section>

            <!-- Upcoming Deadlines -->
            <section class="section">
                <div class="section-header">
                    <h3><i class="bi bi-calendar-week-fill" style="color:var(--accent);margin-right:6px"></i> Upcoming Deadlines (Next 7 Days)</h3>
                    <a href="tasks.php" class="view-all-link">View All</a>
                </div>
                <?php if ($upcoming_result->num_rows > 0): ?>
                    <div class="task-list">
                        <?php while ($task = $upcoming_result->fetch_assoc()): ?>
                            <div class="task-item status-<?php echo strtolower($task['status']); ?>">
                                <div class="task-info">
                                    <h4><?php echo htmlspecialchars($task['title']); ?></h4>
                                    <p class="task-due">Due: <?php echo date('F j, Y - g:i A', strtotime($task['due_date'])); ?></p>
                                </div>
                                <div class="task-meta">
                                    <span class="badge badge-<?php echo strtolower($task['priority']); ?>"><?php echo $task['priority']; ?></span>
                                    <span class="status-<?php echo strtolower($task['status']); ?>"><?php echo $task['status']; ?></span>
                                </div>
                            </div>
                        <?php endwhile; ?>
                    </div>
                <?php else: ?>
                    <p class="empty-state">No upcoming deadlines. You're all set!</p>
                <?php endif; ?>
            </section>

            <div class="section-footer">
                <a href="tasks.php" class="btn btn-primary">Create New Task</a>
            </div>
        </div>
    </div>

    <!-- Manage Subjects Modal -->
    <div id="manageSubjectsModal" class="modal">
        <div class="modal-content" style="max-width:560px">
            <span class="close-modal" onclick="closeModal('manageSubjectsModal')">&times;</span>
            <h3>Manage Your Subjects</h3>

            <h4 style="color:var(--gray-600);font-size:13px;text-transform:uppercase;letter-spacing:0.3px;margin-bottom:10px;">
                <i class="bi bi-check-circle-fill" style="color:var(--success);margin-right:4px"></i> Currently Enrolled
            </h4>
            <?php if (count($enrolled_subjects) > 0): ?>
                <div style="display:flex;flex-wrap:wrap;gap:8px;margin-bottom:20px;">
                    <?php foreach ($enrolled_subjects as $subject): ?>
                        <form method="POST" style="display:inline" onsubmit="return confirm('Drop <?php echo htmlspecialchars($subject['subject_name']); ?>?');">
                            <input type="hidden" name="subject_id" value="<?php echo $subject['subject_id']; ?>">
                            <span class="subject-pill" style="padding-right:4px">
                                <?php echo htmlspecialchars($subject['subject_name']); ?>
                                <small>R<?php echo number_format($subject['monthly_fee'], 2); ?></small>
                                <button type="submit" name="drop_subject_btn" style="background:none;border:none;color:var(--danger);cursor:pointer;font-size:16px;padding:0 4px;line-height:1;" title="Drop subject">&times;</button>
                            </span>
                        </form>
                    <?php endforeach; ?>
                </div>
                <strong class="subject-total" style="display:block;margin-bottom:20px;">
                    Monthly total: R<?php echo number_format($subject_total, 2); ?>
                </strong>
            <?php else: ?>
                <p style="color:var(--gray-500);margin-bottom:16px;">You are not enrolled in any subjects yet.</p>
            <?php endif; ?>

            <hr style="border:none;border-top:2px solid var(--gray-300);margin-bottom:18px;">

            <h4 style="color:var(--gray-600);font-size:13px;text-transform:uppercase;letter-spacing:0.3px;margin-bottom:10px;">
                <i class="bi bi-plus-circle-fill" style="color:var(--accent);margin-right:4px"></i> Available Subjects
            </h4>
            <?php if ($available_result->num_rows > 0): ?>
                <div style="display:grid;gap:8px;">
                    <?php while ($subject = $available_result->fetch_assoc()): ?>
                        <form method="POST" style="display:flex;justify-content:space-between;align-items:center;padding:10px 14px;background:var(--gray-100);border-radius:var(--radius-sm);">
                            <span>
                                <strong style="font-size:14px;"><?php echo htmlspecialchars($subject['subject_name']); ?></strong>
                                <small style="color:var(--gray-600);margin-left:6px;">R<?php echo number_format($subject['monthly_fee'], 2); ?> / month</small>
                            </span>
                            <input type="hidden" name="subject_id" value="<?php echo $subject['subject_id']; ?>">
                            <button type="submit" name="add_subject_btn" class="btn btn-sm btn-primary">+ Add</button>
                        </form>
                    <?php endwhile; ?>
                </div>
            <?php else: ?>
                <p style="color:var(--gray-500)">You are enrolled in all available subjects.</p>
            <?php endif; ?>
        </div>
    </div>

    <script src="js/script.js"></script>
</body>
</html>
