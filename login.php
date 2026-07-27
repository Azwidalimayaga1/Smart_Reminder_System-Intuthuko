<?php
/**
 * Smart Reminder System - Login Page
 * Starts a staff session for Intuthuko College users.
 */

session_start();
require_once 'includes/db_connection.php';
require_once 'includes/subjects_setup.php';

if (isset($_SESSION['user_id'])) {
    header("Location: " . (($_SESSION['role'] ?? '') === 'Admin' ? "admin.php" : "dashboard.php"));
    exit();
}

$error = '';
$role = 'Student';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = trim($_POST['email'] ?? '');
    $password = $_POST['password'] ?? '';
    $allowed_roles = ['Student', 'Tutor', 'Admin'];
    $role = in_array($_POST['role'] ?? 'Student', $allowed_roles, true) ? $_POST['role'] : 'Student';

    if ($email === '' || $password === '') {
        $error = "Please enter your email and password.";
    } elseif (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        $error = "Please enter a valid email address.";
    } else {
        $stmt = $conn->prepare("SELECT user_id, username, password, email, role FROM Users WHERE email = ? AND role = ?");
        $stmt->bind_param("ss", $email, $role);
        $stmt->execute();
        $result = $stmt->get_result();

        if ($result->num_rows === 1) {
            $user = $result->fetch_assoc();

            if (password_verify($password, $user['password'])) {
                $_SESSION['user_id'] = $user['user_id'];
                $_SESSION['username'] = $user['username'];
                $_SESSION['email'] = $user['email'];
                $_SESSION['role'] = $user['role'];

                header("Location: " . ($user['role'] === 'Admin' ? "admin.php" : "dashboard.php"));
                exit();
            }
        }

        $error = "Invalid email, password, or selected role.";
        $stmt->close();
    }
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign In - Smart Reminder System</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body class="auth-page login-page">
    <div class="auth-container">
        <div class="auth-box">
            <div class="auth-header">
                <h1>Smart Reminder System</h1>
                <p>Use your email, password, and role to access your account</p>
            </div>

            <?php if ($error): ?>
                <div class="alert alert-error"><?php echo htmlspecialchars($error); ?></div>
            <?php endif; ?>

            <form method="POST" action="login.php">
                <div class="form-group role-select-group">
                    <label for="roleSelect">Sign In As:</label>
                    <select id="roleSelect" name="role" required>
                        <option value="Student" <?php echo $role === 'Student' ? 'selected' : ''; ?>>Student</option>
                        <option value="Tutor" <?php echo $role === 'Tutor' ? 'selected' : ''; ?>>Tutor</option>
                        <option value="Admin" <?php echo $role === 'Admin' ? 'selected' : ''; ?>>Admin</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="email">Email:</label>
                    <input type="email" id="email" name="email" required placeholder="Enter your email address">
                </div>
                <div class="form-group">
                    <label for="password">Password:</label>
                    <div class="password-wrapper">
                        <input type="password" id="password" name="password" required placeholder="Enter your password">
                        <button type="button" class="password-toggle" onclick="togglePassword('password', this)" aria-label="Show password">Show</button>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-block btn-loading-submit">Sign In</button>
            </form>

            <p class="auth-switch">
                New to the system? <a href="register.php">Create an account</a>
            </p>
            <p class="auth-switch">
                <a href="index.php">Back to home</a>
            </p>
        </div>
    </div>
</body>
</html>
