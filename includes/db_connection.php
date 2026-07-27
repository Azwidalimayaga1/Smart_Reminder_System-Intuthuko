<?php
/**
 * Database Connection File
 * Handles connection to MySQL database for Smart Reminder System
 */

// Database credentials
$db_host = 'localhost';
$db_user = 'root';
$db_pass = ''; // Leave empty if no password
$db_name = 'smart_reminder_db';

// Create connection
$conn = new mysqli($db_host, $db_user, $db_pass, $db_name);

// Check connection
if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}

// Set charset to utf8
$conn->set_charset("utf8");

// Enable error reporting for development (disable in production)
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

?>
