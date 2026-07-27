<?php
session_start();
require_once 'includes/db_connection.php';
require_once 'includes/subjects_setup.php';

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

if ($user_role !== 'Student') {
    header("Location: dashboard.php");
    exit();
}

$message = '';
$message_type = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_POST['add_subject'])) {
        $subject_id = intval($_POST['subject_id']);

        $check = $conn->prepare("SELECT 1 FROM UserSubjects WHERE user_id = ? AND subject_id = ?");
        $check->bind_param("ii", $user_id, $subject_id);
        $check->execute();
        $exists = $check->get_result()->num_rows > 0;
        $check->close();

        if ($exists) {
            $message = "You are already enrolled in this subject.";
            $message_type = 'error';
        } else {
            $insert = $conn->prepare("INSERT INTO UserSubjects (user_id, subject_id) VALUES (?, ?)");
            $insert->bind_param("ii", $user_id, $subject_id);
            if ($insert->execute()) {
                $message = "Subject added successfully.";
                $message_type = 'success';
            } else {
                $message = "Could not add subject. Please try again.";
                $message_type = 'error';
            }
            $insert->close();
        }
    }

    if (isset($_POST['drop_subject'])) {
        $subject_id = intval($_POST['subject_id']);
        $delete = $conn->prepare("DELETE FROM UserSubjects WHERE user_id = ? AND subject_id = ?");
        $delete->bind_param("ii", $user_id, $subject_id);
        if ($delete->execute() && $delete->affected_rows > 0) {
            $message = "Subject dropped successfully.";
            $message_type = 'success';
        } else {
            $message = "Could not drop subject.";
            $message_type = 'error';
        }
        $delete->close();
    }
}

$enrolled_query = "
    SELECT s.subject_id, s.subject_name, s.monthly_fee
    FROM UserSubjects us
    INNER JOIN Subjects s ON us.subject_id = s.subject_id
    WHERE us.user_id = ?
    ORDER BY s.subject_name ASC
";
$enrolled_stmt = $conn->prepare($enrolled_query);
$enrolled_stmt->bind_param("i", $user_id);
$enrolled_stmt->execute();
$enrolled_result = $enrolled_stmt->get_result();
$enrolled_subjects = [];
$subject_total = 0;
while ($row = $enrolled_result->fetch_assoc()) {
    $enrolled_subjects[] = $row;
    $subject_total += (float) $row['monthly_fee'];
}
$enrolled_stmt->close();

$available_query = "
    SELECT subject_id, subject_name, monthly_fee
    FROM Subjects
    WHERE subject_id NOT IN (SELECT subject_id FROM UserSubjects WHERE user_id = ?)
    ORDER BY subject_name ASC
";
$available_stmt = $conn->prepare($available_query);
$available_stmt->bind_param("i", $user_id);
$available_stmt->execute();
$available_result = $available_stmt->get_result();
$available_stmt->close();
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Subjects - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body>
    <?php include 'includes/navbar.php'; ?>

    <div class="container">
        <div class="page-header">
            <div>
                <h2>My Subjects</h2>
                <p>Manage the tutoring subjects you are enrolled in</p>
            </div>
        </div>

        <?php if ($message): ?>
            <div class="alert alert-<?php echo $message_type; ?>">
                <span class="alert-icon"><i class="bi bi-<?php echo $message_type === 'success' ? 'check-circle-fill' : 'exclamation-triangle-fill'; ?>"></i></span>
                <div class="alert-content">
                    <p><?php echo htmlspecialchars($message); ?></p>
                </div>
            </div>
        <?php endif; ?>

        <section class="subject-summary">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:12px;flex-wrap:wrap;">
                <div>
                    <p class="reward-label">Your Subjects</p>
                    <h3>Currently enrolled subjects</h3>
                </div>
            </div>

            <?php if (count($enrolled_subjects) > 0): ?>
                <div class="subject-pill-list">
                    <?php foreach ($enrolled_subjects as $subject): ?>
                        <form method="POST" style="display:inline" onsubmit="return confirm('Drop <?php echo htmlspecialchars($subject['subject_name']); ?>?');">
                            <input type="hidden" name="subject_id" value="<?php echo $subject['subject_id']; ?>">
                            <span class="subject-pill" style="padding-right:4px">
                                <?php echo htmlspecialchars($subject['subject_name']); ?>
                                <small>R<?php echo number_format($subject['monthly_fee'], 2); ?></small>
                                <button type="submit" name="drop_subject" style="background:none;border:none;color:var(--danger);cursor:pointer;font-size:16px;padding:0 4px;line-height:1;" title="Drop subject">&times;</button>
                            </span>
                        </form>
                    <?php endforeach; ?>
                </div>
                <strong class="subject-total">Monthly total: R<?php echo number_format($subject_total, 2); ?></strong>
            <?php else: ?>
                <p>You are not enrolled in any subjects yet. Select from the list below.</p>
            <?php endif; ?>
        </section>

        <section class="section">
            <div class="section-header">
                <h3><i class="bi bi-plus-circle-fill" style="color:var(--accent);margin-right:6px"></i> Available Subjects</h3>
            </div>

            <?php if ($available_result->num_rows > 0): ?>
                <div style="display:grid;gap:10px;">
                    <?php while ($subject = $available_result->fetch_assoc()): ?>
                        <form method="POST" style="display:flex;justify-content:space-between;align-items:center;padding:14px 18px;background:var(--gray-100);border-radius:var(--radius-sm);border:1px solid var(--gray-300);">
                            <div>
                                <strong style="font-size:15px;"><?php echo htmlspecialchars($subject['subject_name']); ?></strong>
                                <small style="color:var(--gray-600);margin-left:8px;">R<?php echo number_format($subject['monthly_fee'], 2); ?> / month</small>
                            </div>
                            <input type="hidden" name="subject_id" value="<?php echo $subject['subject_id']; ?>">
                            <button type="submit" name="add_subject" class="btn btn-sm btn-primary">+ Add</button>
                        </form>
                    <?php endwhile; ?>
                </div>
            <?php else: ?>
                <div class="empty-state">
                    <p>You are enrolled in all available subjects.</p>
                </div>
            <?php endif; ?>
        </section>

        <div class="section-footer">
            <a href="dashboard.php" class="btn btn-primary">Back to Dashboard</a>
        </div>
    </div>

    <script src="js/script.js"></script>
</body>
</html>
