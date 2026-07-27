<?php
/**
 * Smart Reminder System - Public Landing Page
 * Intuthuko College
 */

session_start();

if (isset($_SESSION['user_id'])) {
    header("Location: dashboard.php");
    exit();
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Smart Reminder System - Intuthuko College</title>
    <link rel="icon" href="images/logo.png" type="image/png">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
</head>
<body class="landing-page">
    <header class="landing-header">
        <nav class="landing-nav" aria-label="Main navigation">
            <a class="landing-brand" href="index.php" aria-label="Intuthuko College home">
                <img class="brand-logo" src="images/logo.png" alt="Intuthuko College logo">
                <span>
                    <strong>Intuthuko College</strong>
                    <small>Smart Reminder System</small>
                </span>
            </a>

            <div class="landing-links" id="landingLinks">
                <a href="index.php">Home</a>
                <a href="#notice">Notice</a>
                <a href="#subjects">Subjects</a>
            </div>

            <div class="landing-actions" id="landingActions">
                <a href="login.php" class="btn btn-ghost">Sign In</a>
                <a href="register.php" class="btn btn-primary">Get Started</a>
            </div>

            <button class="hamburger landing-hamburger" onclick="document.getElementById('landingLinks').classList.toggle('active'); document.getElementById('landingActions').classList.toggle('active'); this.classList.toggle('active');" aria-label="Toggle navigation menu">
                <span></span>
                <span></span>
                <span></span>
            </button>
        </nav>
    </header>

    <main>
        <section class="hero-section" id="home">
            <div class="hero-content">
                <p class="hero-kicker">College of BOL learning support</p>
                <h1>Gateway to Unlimited Educational Resources</h1>
                <p class="hero-description">
                    A focused web application that helps Intuthuko College staff and tutors manage tasks,
                    schedules, due dates, reminders, and rewards without relying on manual notebooks.
                </p>
                <div class="hero-buttons">
                    <a href="register.php" class="btn btn-primary btn-large">Get Started</a>
                    <a href="#overview" class="btn btn-outline btn-large">Learn More</a>
                </div>
            </div>

            <div class="hero-media" aria-label="Academic learning environment">
                <img
                    class="hero-img hero-img-top"
                    src="images/learner in class.jpg"
                    alt="Learner focused on class work"
                >
                <img
                    class="hero-img hero-img-tall"
                    src="images/learners  of a high school  in the.jpg"
                    alt="Smiling high school learners at Intuthuko College"
                >
                <img
                    class="hero-img hero-img-bottom"
                    src="images/children in class 3.jpg"
                    alt="Students learning together during a tutoring session"
                >

                <div class="stat-card stat-card-students">
                    <span>Active Students</span>
                    <strong>250+</strong>
                </div>
                <div class="stat-card stat-card-courses">
                    <span>Available Subjects</span>
                    <strong>12</strong>
                </div>
            </div>
        </section>

        <section class="landing-section" id="overview">
            <div class="section-copy">
                <p class="section-kicker">Overview</p>
                <h2>Simple reminders for busy academic teams</h2>
                <p>
                    The Smart Reminder System gives tutors and staff one place to track pending tasks,
                    upcoming deadlines, urgent work, completed responsibilities, and earned reward points.
                </p>
            </div>
            <div class="feature-grid">
                <article class="feature-item">
                    <h3>Task Tracking</h3>
                    <p>Create and manage academic or administrative tasks with priority levels and due dates.</p>
                </article>
                <article class="feature-item">
                    <h3>Due-Date Reminders</h3>
                    <p>View urgent and overdue items quickly so important college work is easier to follow up.</p>
                </article>
                <article class="feature-item">
                    <h3>Reward Points</h3>
                    <p>Earn points for finished tasks so progress feels visible, motivating, and easy to celebrate.</p>
                </article>
                <article class="feature-item">
                    <h3>Staff Access</h3>
                    <p>Secure sign-in keeps each user's tasks connected to their own dashboard session.</p>
                </article>
            </div>
        </section>

        <section class="landing-band" id="notice">
            <div>
                <p class="section-kicker">Notice</p>
                <h2>Designed for Intuthuko College staff and tutors</h2>
            </div>
            <p>
                Sign in to manage your reminders, or create an account to start organizing class preparation,
                learner support, assessment dates, and daily administration.
            </p>
            <a href="login.php" class="btn btn-secondary">Sign In</a>
        </section>

        <section class="landing-section" id="subjects">
            <div class="section-copy">
                <p class="section-kicker">Subjects</p>
                <h2>Select subjects before completing sign up</h2>
                <p>
                    Learners can choose the tutoring subjects they need, review the monthly subject fees,
                    and complete registration with a clear academic plan.
                </p>
            </div>
        </section>

        <section class="landing-section team-section" id="specialists">
            <div class="section-copy">
                <p class="section-kicker">Meet the Team</p>
                <h2>Meet Our Specialists</h2>
                <p>
                    Intuthuko College is supported by tutors who help learners stay prepared,
                    confident, and on schedule.
                </p>
            </div>

            <div class="team-grid">
                <article class="team-member">
                    <img src="images/mr Rambo.jpg" alt="Mr. Rambo - Mathematics Specialist">
                    <h3>Mr. Rambo</h3>
                    <p>Mathematics Specialist</p>
                </article>
                <article class="team-member">
                    <img src="images/mr tutombo.jpg" alt="Mr. Tutombo - Science Specialist">
                    <h3>Mr. Tutombo</h3>
                    <p>Science Specialist</p>
                </article>
                <article class="team-member">
                    <img src="images/mrs  tumi.jpg" alt="Mrs. Tumi - English Specialist">
                    <h3>Mrs. Tumi</h3>
                    <p>English Specialist</p>
                </article>
                <article class="team-member">
                    <img src="images/mrs lesego.jpg" alt="Mrs. Lesego - Learner Support Specialist">
                    <h3>Mrs. Lesego</h3>
                    <p>Learner Support Specialist</p>
                </article>
            </div>
        </section>
    </main>

    <footer class="landing-footer">
        <p><strong>Intuthuko College</strong> &mdash; Smart Reminder System</p>
        <p>Empowering staff and tutors with organized task management.</p>
        <p style="margin-top:8px;font-size:12px;opacity:0.7">&copy; <?php echo date('Y'); ?> Intuthuko College. All rights reserved.</p>
    </footer>
</body>
</html>
