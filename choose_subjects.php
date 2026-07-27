<?php
/**
 * Smart Reminder System - Student Subject Selection
 * Student-only step after registration.
 */

session_start();
require_once 'includes/db_connection.php';
require_once 'includes/subjects_setup.php';

if (!isset($_SESSION['user_id'])) {
    header("Location: login.php");
    exit();
}

if (($_SESSION['role'] ?? '') !== 'Student') {
    header("Location: dashboard.php");
    exit();
}

$user_id = (int) $_SESSION['user_id'];
$error = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $selected_subjects = array_map('intval', $_POST['subjects'] ?? []);

    if (count($selected_subjects) === 0) {
        $error = "Please select at least one subject to continue.";
    } else {
        $conn->begin_transaction();

        try {
            $delete_stmt = $conn->prepare("DELETE FROM UserSubjects WHERE user_id = ?");
            $delete_stmt->bind_param("i", $user_id);
            $delete_stmt->execute();
            $delete_stmt->close();

            $insert_stmt = $conn->prepare("INSERT INTO UserSubjects (user_id, subject_id) VALUES (?, ?)");

            foreach ($selected_subjects as $subject_id) {
                $insert_stmt->bind_param("ii", $user_id, $subject_id);
                $insert_stmt->execute();
            }

            $insert_stmt->close();
            $conn->commit();

            header("Location: dashboard.php");
            exit();
        } catch (mysqli_sql_exception $exception) {
            $conn->rollback();
            $error = "Subjects could not be saved. Please try again.";
        }
    }
}

$subjects_result = $conn->query("SELECT subject_id, subject_name, monthly_fee FROM Subjects ORDER BY subject_name ASC");
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Choose Subjects - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body class="auth-page register-page">
    <div class="auth-container">
        <div class="auth-box subject-step-box">
            <div class="auth-header">
                <h1>Choose Your Subjects</h1>
                <p>Step 2 of 2: Select the tutoring subjects you want to register for.</p>
            </div>

            <div style="display:flex;gap:8px;margin-bottom:24px;justify-content:center;">
                <span style="background:var(--success);color:white;padding:6px 16px;border-radius:20px;font-size:13px;font-weight:700;">✓ Account</span>
                <span style="background:var(--accent);color:white;padding:6px 16px;border-radius:20px;font-size:13px;font-weight:700;">2 Subjects</span>
            </div>

            <?php if ($error): ?>
                <div class="alert alert-error"><?php echo htmlspecialchars($error); ?></div>
            <?php endif; ?>

            <form method="POST" action="choose_subjects.php">
                <div class="subject-choice-grid subject-step-grid">
                    <?php while ($subject = $subjects_result->fetch_assoc()): ?>
                        <label class="subject-choice">
                            <input type="checkbox" name="subjects[]" value="<?php echo $subject['subject_id']; ?>">
                            <span>
                                <strong><?php echo htmlspecialchars($subject['subject_name']); ?></strong>
                                <small>R<?php echo number_format($subject['monthly_fee'], 2); ?> / month</small>
                            </span>
                        </label>
                    <?php endwhile; ?>
                </div>

                <button type="submit" class="btn btn-primary btn-block btn-loading-submit">Save Subjects</button>
            </form>

            <p class="auth-switch">
                <a href="logout.php">Sign out and choose later</a>
            </p>
        </div>
    </div>

    <script src="js/script.js"></script>
</body>
</html>
