<?php
/**
 * Task setup helpers.
 * Adds tutor assignment and student submission fields for existing installs.
 */

$conn->query("ALTER TABLE Tasks MODIFY status ENUM('Pending', 'Submitted', 'Completed') DEFAULT 'Pending'");

$assigned_by_check = $conn->query("SHOW COLUMNS FROM Tasks LIKE 'assigned_by'");
if ($assigned_by_check->num_rows === 0) {
    $conn->query("ALTER TABLE Tasks ADD assigned_by INT NULL AFTER user_id");
    $conn->query("UPDATE Tasks SET assigned_by = user_id WHERE assigned_by IS NULL");
}

$submission_note_check = $conn->query("SHOW COLUMNS FROM Tasks LIKE 'submission_note'");
if ($submission_note_check->num_rows === 0) {
    $conn->query("ALTER TABLE Tasks ADD submission_note TEXT NULL AFTER status");
}

$submitted_at_check = $conn->query("SHOW COLUMNS FROM Tasks LIKE 'submitted_at'");
if ($submitted_at_check->num_rows === 0) {
    $conn->query("ALTER TABLE Tasks ADD submitted_at DATETIME NULL AFTER submission_note");
}

$submission_pdf_path_check = $conn->query("SHOW COLUMNS FROM Tasks LIKE 'submission_pdf_path'");
if ($submission_pdf_path_check->num_rows === 0) {
    $conn->query("ALTER TABLE Tasks ADD submission_pdf_path VARCHAR(255) NULL AFTER submission_note");
}

$submission_pdf_name_check = $conn->query("SHOW COLUMNS FROM Tasks LIKE 'submission_pdf_name'");
if ($submission_pdf_name_check->num_rows === 0) {
    $conn->query("ALTER TABLE Tasks ADD submission_pdf_name VARCHAR(255) NULL AFTER submission_pdf_path");
}
