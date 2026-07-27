<?php
/**
 * Navigation Bar Include
 * Displays consistent navigation across all pages
 */

if (!isset($_SESSION['user_id'])) {
    header("Location: index.php");
    exit();
}

if (!isset($_SESSION['role']) && isset($conn)) {
    $role_stmt = $conn->prepare("SELECT role FROM Users WHERE user_id = ?");
    $role_stmt->bind_param("i", $_SESSION['user_id']);
    $role_stmt->execute();
    $role_result = $role_stmt->get_result();
    $_SESSION['role'] = $role_result->num_rows === 1 ? $role_result->fetch_assoc()['role'] : 'Student';
    $role_stmt->close();
}
?>

<nav class="navbar">
    <div class="navbar-container">
        <div class="navbar-brand">
            <h1>Smart Reminder System</h1>
            <p class="college-name">Intuthuko College Staff Portal</p>
        </div>

        <button class="hamburger" onclick="toggleMobileMenu()" aria-label="Toggle navigation menu">
            <span></span>
            <span></span>
            <span></span>
        </button>
        
        <ul class="navbar-menu" id="navbarMenu">
            <?php if (($_SESSION['role'] ?? '') === 'Admin'): ?>
                <li><a href="admin.php" class="nav-link">Admin</a></li>
            <?php else: ?>
                <li><a href="dashboard.php" class="nav-link">Dashboard</a></li>
                <?php if (($_SESSION['role'] ?? '') === 'Student'): ?>
                    <li><a href="subjects.php" class="nav-link">Subjects</a></li>
                <?php endif; ?>
                <li><a href="tasks.php" class="nav-link">Tasks</a></li>
                <li><a href="calendar.php" class="nav-link">Calendar</a></li>
                <li><a href="reminders.php" class="nav-link">Reminders</a></li>
            <?php endif; ?>
        </ul>
        
        <div class="navbar-user">
            <span class="welcome-text">Welcome, <?php echo htmlspecialchars($_SESSION['username']); ?> (<?php echo htmlspecialchars($_SESSION['role'] ?? 'Student'); ?>)</span>
            <a href="logout.php" class="logout-btn">Logout</a>
        </div>
    </div>
</nav>
