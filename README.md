# XISD Smart Reminder System

A PHP and MariaDB/MySQL reminder system for Intuthuko College. It supports user registration and login, subject selection, tasks, calendar events, reminders, and administrative features.

## Requirements

- PHP 8.2 or later with the `mysqli` extension
- MariaDB or MySQL

## Local setup

1. Create a database named `smart_reminder_db`.
2. Import `database_schema.sql`.
3. Update the local credentials in `includes/db_connection.php` if your database does not use the default local `root` account with an empty password.
4. From the project directory, start PHP:

   ```powershell
   php -S 127.0.0.1:8000
   ```

5. Open <http://127.0.0.1:8000/>.

## Repository contents

This repository contains the application source code, database schema, and site images. Runtime uploads and duplicate coursework archives are intentionally excluded.

