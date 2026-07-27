<?php
/**
 * Smart Reminder System - Registration Page
 * Inserts new Intuthuko College users into MySQL.
 */

session_start();
require_once 'includes/db_connection.php';
require_once 'includes/subjects_setup.php';

if (isset($_SESSION['user_id'])) {
    header("Location: dashboard.php");
    exit();
}

$error = '';
$success = '';
$role = 'Student';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $username = trim($_POST['username'] ?? '');
    $email = trim($_POST['email'] ?? '');
    $password = $_POST['password'] ?? '';
    $confirm_password = $_POST['confirm_password'] ?? '';
    $allowed_roles = ['Student', 'Tutor', 'Admin'];
    $role = in_array($_POST['role'] ?? 'Student', $allowed_roles, true) ? $_POST['role'] : 'Student';

    if (strlen($username) < 3) {
        $error = "Username must be at least 3 characters long.";
    } elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $error = "Please enter a valid email address.";
    } elseif (strlen($password) < 6) {
        $error = "Password must be at least 6 characters long.";
    } elseif ($password !== $confirm_password) {
        $error = "Passwords do not match.";
    } else {
        $check_stmt = $conn->prepare("SELECT user_id FROM Users WHERE username = ? OR email = ?");
        $check_stmt->bind_param("ss", $username, $email);
        $check_stmt->execute();
        $check_result = $check_stmt->get_result();

        if ($check_result->num_rows > 0) {
            $error = "Username or email already exists.";
        } else {
            $hashed_password = password_hash($password, PASSWORD_DEFAULT);
            $conn->begin_transaction();

            try {
                $insert_stmt = $conn->prepare("INSERT INTO Users (username, email, password, role) VALUES (?, ?, ?, ?)");
                $insert_stmt->bind_param("ssss", $username, $email, $hashed_password, $role);
                $insert_stmt->execute();
                $new_user_id = $conn->insert_id;
                $insert_stmt->close();
                $conn->commit();

                $_SESSION['user_id'] = $new_user_id;
                $_SESSION['username'] = $username;
                $_SESSION['email'] = $email;
                $_SESSION['role'] = $role;

                if ($role === 'Student') {
                    header("Location: choose_subjects.php");
                } elseif ($role === 'Admin') {
                    header("Location: admin.php");
                } else {
                    header("Location: dashboard.php");
                }
                exit();
            } catch (mysqli_sql_exception $exception) {
                $conn->rollback();
                $error = "Registration failed. Please try again.";
            }
        }

        $check_stmt->close();
    }
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign Up - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body class="auth-page register-page">
    <div class="auth-container">
        <div class="auth-box">
            <div class="auth-header">
                <h1>Smart Reminder System</h1>
                <p>Create your Intuthuko College learning account</p>
            </div>

            <?php if ($error): ?>
                <div class="alert alert-error"><?php echo htmlspecialchars($error); ?></div>
            <?php endif; ?>

            <?php if ($success): ?>
                <div class="alert alert-success"><?php echo htmlspecialchars($success); ?></div>
            <?php endif; ?>

            <form method="POST" action="register.php" class="register-form">
                <div class="form-group role-select-group">
                    <label for="roleSelect">Select Role:</label>
                    <select id="roleSelect" name="role" required>
                        <option value="Student" <?php echo $role === 'Student' ? 'selected' : ''; ?>>Student</option>
                        <option value="Tutor" <?php echo $role === 'Tutor' ? 'selected' : ''; ?>>Tutor</option>
                        <option value="Admin" <?php echo $role === 'Admin' ? 'selected' : ''; ?>>Admin</option>
                    </select>
                </div>

                <div class="role-panel active" id="studentRolePanel">
                    <strong>Student registration</strong>
                    <p>After creating your account, you will choose your tutoring subjects on the next page.</p>
                </div>
                <div class="role-panel" id="tutorRolePanel">
                    <strong>Tutor registration</strong>
                    <p>Tutors use the system to manage teaching tasks, reminders, and reward progress.</p>
                </div>
                <div class="role-panel" id="adminRolePanel">
                    <strong>Admin registration</strong>
                    <p>Admin accounts open the admin panel for managing users, subjects, and task data.</p>
                </div>

                <div class="form-group">
                    <label for="username">Username:</label>
                    <input type="text" id="username" name="username" required placeholder="Choose a username">
                </div>
                <div class="form-group">
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email" required placeholder="Enter your email">
                </div>
                <div class="form-group">
                    <label for="password">Password:</label>
                    <div class="password-wrapper">
                        <input type="password" id="password" name="password" required placeholder="Create a password">
                        <button type="button" class="password-toggle" onclick="togglePassword('password', this)" aria-label="Show password">Show</button>
                    </div>
                    <div class="strength-meter">
                        <div class="strength-bar-fill" id="passwordStrength"></div>
                    </div>
                    <div class="strength-label" id="passwordStrengthText"></div>
                </div>
                <div class="form-group">
                    <label for="confirm_password">Confirm Password:</label>
                    <div class="password-wrapper">
                        <input type="password" id="confirm_password" name="confirm_password" required placeholder="Confirm your password">
                        <button type="button" class="password-toggle" onclick="togglePassword('confirm_password', this)" aria-label="Show password">Show</button>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-block btn-loading-submit">Create Account</button>
            </form>

            <p class="auth-switch">
                Already have an account? <a href="login.php">Sign in</a>
            </p>
            <p class="auth-switch">
                <a href="index.php">Back to home</a>
            </p>
        </div>
    </div>
    <script src="js/script.js"></script>
</body>
</html>
