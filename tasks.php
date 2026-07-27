<?php
/**
 * Smart Reminder System - Task Management
 * Tutors assign tasks; users can create their own reminder tasks.
 */

session_start();
require_once 'includes/db_connection.php';
require_once 'includes/tasks_setup.php';

if (!isset($_SESSION['user_id'])) {
    header("Location: index.php");
    exit();
}

$user_id = (int) $_SESSION['user_id'];

if (!isset($_SESSION['role'])) {
    $role_stmt = $conn->prepare("SELECT role FROM Users WHERE user_id = ?");
    $role_stmt->bind_param("i", $user_id);
    $role_stmt->execute();
    $role_result = $role_stmt->get_result();
    $_SESSION['role'] = $role_result->num_rows === 1 ? $role_result->fetch_assoc()['role'] : 'Student';
    $role_stmt->close();
}

$user_role = $_SESSION['role'] ?? 'Student';

if ($user_role === 'Admin') {
    header("Location: admin.php");
    exit();
}

$message = '';
$message_type = '';

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

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['create_task']) && in_array($user_role, ['Student', 'Tutor'], true)) {
    $recipient = $_POST['recipient'] ?? 'self';
    $target_user_id = $user_id;
    $is_self_task = true;
    $title = trim($_POST['title'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $due_date = trim($_POST['due_date'] ?? '');
    $priority_values = ['Low', 'Medium', 'High'];
    $priority = in_array($_POST['priority'] ?? 'Medium', $priority_values, true) ? $_POST['priority'] : 'Medium';

    if ($user_role === 'Tutor' && $recipient !== 'self') {
        $target_user_id = intval($recipient);
        $is_self_task = false;

        $student_stmt = $conn->prepare("SELECT user_id FROM Users WHERE user_id = ? AND role = 'Student'");
        $student_stmt->bind_param("i", $target_user_id);
        $student_stmt->execute();
        $student_result = $student_stmt->get_result();
    }

    if ($title === '' || $due_date === '') {
        $message = "Please fill in the task title and due date.";
        $message_type = 'error';
    } elseif (isset($student_result) && $student_result->num_rows !== 1) {
        $message = "Please choose a valid learner.";
        $message_type = 'error';
    } else {
        $stmt = $conn->prepare("
            INSERT INTO Tasks (user_id, assigned_by, title, description, due_date, priority, status)
            VALUES (?, ?, ?, ?, ?, ?, 'Pending')
        ");
        $stmt->bind_param("iissss", $target_user_id, $user_id, $title, $description, $due_date, $priority);

        if ($stmt->execute()) {
            $message = $is_self_task ? "Reminder task created successfully." : "Task assigned to learner successfully.";
            $message_type = 'success';
        } else {
            $message = "Error creating task.";
            $message_type = 'error';
        }

        $stmt->close();
    }

    if (isset($student_stmt)) {
        $student_stmt->close();
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['submit_task']) && $user_role === 'Student') {
    $task_id = intval($_POST['task_id'] ?? 0);
    $submission_note = trim($_POST['submission_note'] ?? '');
    $pdf_path = null;
    $pdf_name = null;
    $existing_pdf_path = null;

    $task_check_stmt = $conn->prepare("
        SELECT submission_pdf_path
        FROM Tasks
        WHERE task_id = ? AND user_id = ? AND status <> 'Completed'
    ");
    $task_check_stmt->bind_param("ii", $task_id, $user_id);
    $task_check_stmt->execute();
    $task_check_result = $task_check_stmt->get_result();

    if ($task_check_result->num_rows !== 1) {
        $message = "Task could not be submitted.";
        $message_type = 'error';
    } else {
        $existing_pdf_path = $task_check_result->fetch_assoc()['submission_pdf_path'] ?? null;

        if (isset($_FILES['submission_pdf']) && $_FILES['submission_pdf']['error'] !== UPLOAD_ERR_NO_FILE) {
            $file = $_FILES['submission_pdf'];
            $max_file_size = 10 * 1024 * 1024;

            if ($file['error'] !== UPLOAD_ERR_OK) {
                $message = "PDF upload failed. Please choose the file again.";
                $message_type = 'error';
            } elseif ($file['size'] > $max_file_size) {
                $message = "PDF is too large. Please upload a file smaller than 10MB.";
                $message_type = 'error';
            } else {
                $original_name = basename($file['name']);
                $extension = strtolower(pathinfo($original_name, PATHINFO_EXTENSION));
                $mime_type = mime_content_type($file['tmp_name']);
                $pdf_signature = file_get_contents($file['tmp_name'], false, null, 0, 4);

                if ($extension !== 'pdf' || ($mime_type !== 'application/pdf' && $pdf_signature !== '%PDF')) {
                    $message = "Only PDF files are allowed.";
                    $message_type = 'error';
                } else {
                    $upload_dir = __DIR__ . DIRECTORY_SEPARATOR . 'uploads' . DIRECTORY_SEPARATOR . 'submissions';

                    if (!is_dir($upload_dir)) {
                        mkdir($upload_dir, 0755, true);
                    }

                    $stored_name = 'task_' . $task_id . '_student_' . $user_id . '_' . bin2hex(random_bytes(8)) . '.pdf';
                    $destination = $upload_dir . DIRECTORY_SEPARATOR . $stored_name;

                    if (move_uploaded_file($file['tmp_name'], $destination)) {
                        $pdf_path = 'uploads/submissions/' . $stored_name;
                        $pdf_name = $original_name;
                    } else {
                        $message = "PDF could not be saved. Please try again.";
                        $message_type = 'error';
                    }
                }
            }
        }
    }

    $task_check_stmt->close();

    if ($message_type !== 'error') {
        if ($pdf_path !== null) {
            $stmt = $conn->prepare("
                UPDATE Tasks
                SET status = 'Submitted', submission_note = ?, submission_pdf_path = ?, submission_pdf_name = ?, submitted_at = NOW()
                WHERE task_id = ? AND user_id = ? AND status <> 'Completed'
            ");
            $stmt->bind_param("sssii", $submission_note, $pdf_path, $pdf_name, $task_id, $user_id);
        } else {
            $stmt = $conn->prepare("
                UPDATE Tasks
                SET status = 'Submitted', submission_note = ?, submitted_at = NOW()
                WHERE task_id = ? AND user_id = ? AND status <> 'Completed'
            ");
            $stmt->bind_param("sii", $submission_note, $task_id, $user_id);
        }

        if ($stmt->execute() && $stmt->affected_rows > 0) {
            if ($pdf_path !== null) {
                delete_submission_pdf($existing_pdf_path);
            }

            $message = "Task submitted successfully. Your tutor can now review it.";
            $message_type = 'success';
        } else {
            delete_submission_pdf($pdf_path);
            $message = "Task could not be submitted.";
            $message_type = 'error';
        }

        $stmt->close();
    }
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['update_status']) && in_array($user_role, ['Student', 'Tutor'], true)) {
    $task_id = intval($_POST['task_id'] ?? 0);
    $status_values = ['Pending', 'Submitted', 'Completed'];
    $status = in_array($_POST['status'] ?? 'Pending', $status_values, true) ? $_POST['status'] : 'Pending';

    $stmt = $conn->prepare("
        UPDATE Tasks
        SET status = ?
        WHERE task_id = ?
        AND assigned_by = ?
        AND (assigned_by = user_id OR ? = 'Tutor')
    ");
    $stmt->bind_param("siis", $status, $task_id, $user_id, $user_role);

    if ($stmt->execute() && $stmt->affected_rows > 0) {
        $message = $status === 'Completed'
            ? "Task marked as completed."
            : "Task status updated successfully.";
        $message_type = 'success';
    } else {
        $message = "Task status could not be updated.";
        $message_type = 'error';
    }

    $stmt->close();
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['delete_task']) && in_array($user_role, ['Student', 'Tutor'], true)) {
    $task_id = intval($_POST['task_id'] ?? 0);
    $pdf_path = null;

    $file_stmt = $conn->prepare("
        SELECT submission_pdf_path
        FROM Tasks
        WHERE task_id = ?
        AND assigned_by = ?
        AND (assigned_by = user_id OR ? = 'Tutor')
    ");
    $file_stmt->bind_param("iis", $task_id, $user_id, $user_role);
    $file_stmt->execute();
    $file_result = $file_stmt->get_result();
    if ($file_result->num_rows === 1) {
        $pdf_path = $file_result->fetch_assoc()['submission_pdf_path'] ?? null;
    }
    $file_stmt->close();

    $stmt = $conn->prepare("
        DELETE FROM Tasks
        WHERE task_id = ?
        AND assigned_by = ?
        AND (assigned_by = user_id OR ? = 'Tutor')
    ");
    $stmt->bind_param("iis", $task_id, $user_id, $user_role);

    if ($stmt->execute() && $stmt->affected_rows > 0) {
        delete_submission_pdf($pdf_path);
        $message = "Task deleted successfully.";
        $message_type = 'success';
    } else {
        $message = "Error deleting task.";
        $message_type = 'error';
    }

    $stmt->close();
}

if ($user_role === 'Tutor') {
    $students_result = $conn->query("SELECT user_id, username, email FROM Users WHERE role = 'Student' ORDER BY username ASC");

    $self_tasks_stmt = $conn->prepare("
        SELECT t.*
        FROM Tasks t
        WHERE t.user_id = ? AND t.assigned_by = t.user_id
        ORDER BY t.due_date ASC, FIELD(t.priority, 'High', 'Medium', 'Low')
    ");
    $self_tasks_stmt->bind_param("i", $user_id);

    $shared_tasks_stmt = $conn->prepare("
        SELECT t.*, student.username AS student_name, student.email AS student_email
        FROM Tasks t
        INNER JOIN Users student ON t.user_id = student.user_id
        WHERE t.assigned_by = ? AND t.user_id <> t.assigned_by
        ORDER BY t.due_date ASC, FIELD(t.priority, 'High', 'Medium', 'Low')
    ");
    $shared_tasks_stmt->bind_param("i", $user_id);
} else {
    $self_tasks_stmt = $conn->prepare("
        SELECT t.*
        FROM Tasks t
        WHERE t.user_id = ? AND t.assigned_by = t.user_id
        ORDER BY t.due_date ASC, FIELD(t.priority, 'High', 'Medium', 'Low')
    ");
    $self_tasks_stmt->bind_param("i", $user_id);

    $shared_tasks_stmt = $conn->prepare("
        SELECT t.*, tutor.username AS tutor_name, tutor.email AS tutor_email
        FROM Tasks t
        LEFT JOIN Users tutor ON t.assigned_by = tutor.user_id
        WHERE t.user_id = ? AND (t.assigned_by IS NULL OR t.assigned_by <> t.user_id)
        ORDER BY t.due_date ASC, FIELD(t.priority, 'High', 'Medium', 'Low')
    ");
    $shared_tasks_stmt->bind_param("i", $user_id);
}

$self_tasks_stmt->execute();
$self_tasks_result = $self_tasks_stmt->get_result();
$self_tasks = [];
while ($task = $self_tasks_result->fetch_assoc()) {
    $self_tasks[] = $task;
}

$shared_tasks_stmt->execute();
$shared_tasks_result = $shared_tasks_stmt->get_result();
$shared_tasks = [];
while ($task = $shared_tasks_result->fetch_assoc()) {
    $shared_tasks[] = $task;
}

function render_tasks_section(string $title, string $empty_message, array $tasks, string $user_role, int $user_id, ?string $person_header, bool $show_submission): void
{
    ?>
    <section class="section">
        <div class="section-header">
            <h3><?php echo htmlspecialchars($title); ?></h3>
        </div>

        <?php if (count($tasks) > 0): ?>
            <div class="table-responsive">
                <table class="tasks-table">
                    <thead>
                        <tr>
                            <th>Task</th>
                            <?php if ($person_header): ?>
                                <th><?php echo htmlspecialchars($person_header); ?></th>
                            <?php endif; ?>
                            <th>Due Date</th>
                            <th>Priority</th>
                            <th>Status</th>
                            <?php if ($show_submission): ?>
                                <th>Submission</th>
                            <?php endif; ?>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php foreach ($tasks as $task): ?>
                            <tr class="task-row status-<?php echo strtolower($task['status']); ?>">
                                <td>
                                    <strong class="task-title"><?php echo htmlspecialchars($task['title']); ?></strong>
                                    <p class="task-desc"><?php echo htmlspecialchars($task['description'] ?: 'No details added.'); ?></p>
                                </td>
                                <?php if ($person_header): ?>
                                    <td>
                                        <?php if ($user_role === 'Tutor'): ?>
                                            <?php echo htmlspecialchars($task['student_name']); ?><br>
                                            <small><?php echo htmlspecialchars($task['student_email']); ?></small>
                                        <?php else: ?>
                                            <?php echo htmlspecialchars($task['tutor_name'] ?: 'Tutor'); ?><br>
                                            <small><?php echo htmlspecialchars($task['tutor_email'] ?: ''); ?></small>
                                        <?php endif; ?>
                                    </td>
                                <?php endif; ?>
                                <td class="task-due"><?php echo date('M j, Y - g:i A', strtotime($task['due_date'])); ?></td>
                                <td><span class="badge badge-<?php echo strtolower($task['priority']); ?>"><?php echo $task['priority']; ?></span></td>
                                <td><span class="badge badge-<?php echo strtolower($task['status']); ?>"><?php echo $task['status']; ?></span></td>
                                <?php if ($show_submission): ?>
                                    <td>
                                        <?php if ($task['submission_note'] || $task['submission_pdf_path']): ?>
                                            <?php if ($task['submission_note']): ?>
                                                <p class="task-submission"><?php echo htmlspecialchars($task['submission_note']); ?></p>
                                            <?php endif; ?>
                                            <?php if ($task['submission_pdf_path']): ?>
                                                <a class="submission-file-link" href="download_submission.php?task_id=<?php echo (int) $task['task_id']; ?>" target="_blank" rel="noopener">
                                                    View PDF: <?php echo htmlspecialchars($task['submission_pdf_name'] ?: 'Submitted PDF'); ?>
                                                </a>
                                            <?php endif; ?>
                                            <small>Submitted: <?php echo date('M j, Y - g:i A', strtotime($task['submitted_at'])); ?></small>
                                        <?php else: ?>
                                            <span class="task-desc">No submission yet.</span>
                                        <?php endif; ?>
                                    </td>
                                <?php endif; ?>
                                <td>
                                    <?php $is_self_task_row = (int) $task['user_id'] === (int) $task['assigned_by'] && (int) $task['user_id'] === $user_id; ?>
                                    <?php if ($user_role === 'Tutor' || $is_self_task_row): ?>
                                        <form method="POST" class="admin-inline-form">
                                            <input type="hidden" name="task_id" value="<?php echo $task['task_id']; ?>">
                                            <select name="status" class="status-select">
                                                <option value="Pending" <?php echo $task['status'] === 'Pending' ? 'selected' : ''; ?>>Pending</option>
                                                <option value="Submitted" <?php echo $task['status'] === 'Submitted' ? 'selected' : ''; ?>>Submitted</option>
                                                <option value="Completed" <?php echo $task['status'] === 'Completed' ? 'selected' : ''; ?>>Completed</option>
                                            </select>
                                            <button type="submit" name="update_status" class="btn btn-sm btn-primary">Save</button>
                                        </form>
                                        <form method="POST" class="task-delete-form" onsubmit="return confirm('Delete this task?');">
                                            <input type="hidden" name="task_id" value="<?php echo $task['task_id']; ?>">
                                            <button type="submit" name="delete_task" class="btn btn-sm btn-danger">Delete</button>
                                        </form>
                                    <?php elseif ($task['status'] !== 'Completed'): ?>
                                        <form method="POST" class="submission-form" enctype="multipart/form-data">
                                            <input type="hidden" name="task_id" value="<?php echo $task['task_id']; ?>">
                                            <textarea name="submission_note" rows="3" placeholder="Write what you completed or add a short note"><?php echo htmlspecialchars($task['submission_note'] ?? ''); ?></textarea>
                                            <input type="file" name="submission_pdf" accept="application/pdf,.pdf">
                                            <?php if ($task['submission_pdf_path']): ?>
                                                <small>Current PDF: <?php echo htmlspecialchars($task['submission_pdf_name'] ?: 'Submitted PDF'); ?></small>
                                            <?php endif; ?>
                                            <button type="submit" name="submit_task" class="btn btn-sm btn-primary">Submit Work</button>
                                        </form>
                                    <?php else: ?>
                                        <span class="status-completed">Reviewed</span>
                                    <?php endif; ?>
                                </td>
                            </tr>
                        <?php endforeach; ?>
                    </tbody>
                </table>
            </div>
        <?php else: ?>
            <div class="empty-state">
                <p><?php echo htmlspecialchars($empty_message); ?></p>
            </div>
        <?php endif; ?>
    </section>
    <?php
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Tasks - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body>
    <?php include 'includes/navbar.php'; ?>

    <div class="container">
        <div class="page-header">
            <div>
                <h2><?php echo $user_role === 'Tutor' ? 'Assign and Track Tasks' : 'My Tasks and Reminders'; ?></h2>
                <p><?php echo $user_role === 'Tutor' ? 'Create learner tasks or personal reminders and review submitted work.' : 'Create reminders, view tutor tasks, and submit completed work.'; ?></p>
            </div>
            <button class="btn btn-primary" onclick="openModal('createTaskModal')">+ <?php echo $user_role === 'Tutor' ? 'Create Task' : 'Create Reminder'; ?></button>
        </div>

        <?php if ($message): ?>
            <div class="alert alert-<?php echo $message_type; ?>">
                <?php echo htmlspecialchars($message); ?>
            </div>
        <?php endif; ?>

        <div id="createTaskModal" class="modal">
            <div class="modal-content">
                <span class="close-modal" onclick="closeModal('createTaskModal')">&times;</span>
                <h3><?php echo $user_role === 'Tutor' ? 'Create Task' : 'Create Reminder'; ?></h3>
                <form method="POST" action="">
                    <?php if ($user_role === 'Tutor'): ?>
                        <div class="form-group">
                            <label for="recipient">For</label>
                            <select id="recipient" name="recipient">
                                <option value="self">Myself</option>
                                <?php while ($student = $students_result->fetch_assoc()): ?>
                                    <option value="<?php echo $student['user_id']; ?>">
                                        <?php echo htmlspecialchars($student['username'] . ' - ' . $student['email']); ?>
                                    </option>
                                <?php endwhile; ?>
                            </select>
                        </div>
                    <?php endif; ?>
                    <div class="form-group">
                        <label for="title">Task Title *</label>
                        <input type="text" id="title" name="title" required placeholder="Enter task title">
                    </div>
                    <div class="form-group">
                        <label for="description">Details</label>
                        <textarea id="description" name="description" rows="4" placeholder="Add instructions or reminder notes"></textarea>
                    </div>
                    <div class="form-group">
                        <label for="due_date">Due Date *</label>
                        <input type="datetime-local" id="due_date" name="due_date" required>
                    </div>
                    <div class="form-group">
                        <label for="priority">Priority</label>
                        <select id="priority" name="priority">
                            <option value="Low">Low</option>
                            <option value="Medium" selected>Medium</option>
                            <option value="High">High</option>
                        </select>
                    </div>
                    <button type="submit" name="create_task" class="btn btn-primary btn-loading-submit"><?php echo $user_role === 'Tutor' ? 'Create Task' : 'Create Reminder'; ?></button>
                </form>
            </div>
        </div>

        <div class="page-content">
            <?php
            render_tasks_section(
                'My Own Reminders',
                'No personal reminders yet. Create one to track your own deadlines.',
                $self_tasks,
                $user_role,
                $user_id,
                null,
                false
            );

            render_tasks_section(
                $user_role === 'Tutor' ? 'Learner Tasks' : 'Tasks From Tutors',
                $user_role === 'Tutor' ? 'No learner tasks assigned yet.' : 'No assigned tasks yet. Your tutor has not assigned anything.',
                $shared_tasks,
                $user_role,
                $user_id,
                $user_role === 'Tutor' ? 'Learner' : 'Tutor',
                true
            );
            ?>
        </div>
    </div>

    <script src="js/script.js"></script>
</body>
</html>
