<?php
/**
 * Subject setup helpers.
 * Creates subject tables for existing installs and seeds common tutoring subjects.
 */

$role_check = $conn->query("SHOW COLUMNS FROM Users LIKE 'role'");

if ($role_check->num_rows === 0) {
    $conn->query("ALTER TABLE Users ADD role ENUM('Student', 'Tutor', 'Admin') NOT NULL DEFAULT 'Student' AFTER email");
}

$conn->query("UPDATE Users SET role = 'Admin' WHERE LOWER(username) = 'admin'");

$conn->query("
    CREATE TABLE IF NOT EXISTS Subjects (
        subject_id INT AUTO_INCREMENT PRIMARY KEY,
        subject_name VARCHAR(100) NOT NULL UNIQUE,
        monthly_fee DECIMAL(10, 2) DEFAULT 0.00,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
");

$conn->query("
    CREATE TABLE IF NOT EXISTS UserSubjects (
        user_subject_id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        subject_id INT NOT NULL,
        selected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE KEY unique_user_subject (user_id, subject_id),
        FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
        FOREIGN KEY (subject_id) REFERENCES Subjects(subject_id) ON DELETE CASCADE
    )
");

$default_subjects = [
    ['Mathematics', 250.00],
    ['Physical Sciences', 250.00],
    ['Life Sciences', 220.00],
    ['English', 200.00],
    ['Accounting', 230.00],
    ['Business Studies', 220.00],
];

$subject_stmt = $conn->prepare("INSERT IGNORE INTO Subjects (subject_name, monthly_fee) VALUES (?, ?)");

foreach ($default_subjects as $subject) {
    $subject_stmt->bind_param("sd", $subject[0], $subject[1]);
    $subject_stmt->execute();
}

$subject_stmt->close();
