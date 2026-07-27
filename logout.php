<?php
/**
 * Smart Reminder System - Logout
 * Destroy session and redirect to login
 */

session_start();

// Destroy session
session_destroy();

// Redirect to login page
header("Location: index.php");
exit();
?>
