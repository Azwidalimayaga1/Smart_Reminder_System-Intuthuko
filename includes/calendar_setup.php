<?php
$conn->query("
    CREATE TABLE IF NOT EXISTS CalendarEvents (
        event_id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        title VARCHAR(200) NOT NULL,
        description TEXT,
        event_type ENUM('meeting', 'exam', 'workshop', 'deadline') NOT NULL DEFAULT 'meeting',
        event_date DATE NOT NULL,
        event_time TIME,
        end_date DATE,
        end_time TIME,
        color VARCHAR(7) DEFAULT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
    )
");
