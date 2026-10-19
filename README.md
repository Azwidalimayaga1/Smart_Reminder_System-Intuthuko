# XISD Smart Reminder System


## 1. Introduction & Project Overview
The **XISD Smart Reminder System** is a professional multi-platform academic management solution designed for Intuthuko College. It digitizes administrative and student tasks by connecting a high-performance PHP/MariaDB web dashboard with a native Kotlin Android application. The system facilitates seamless task assignment, real-time cloud synchronization, and automated reminders to enhance overall academic efficiency.

**Local Setup & Execution:**
1. Create a database named `smart_reminder_db` and import `database_schema.sql`.
2. Configure credentials in `includes/db_connection.php`.
3. Start the PHP server from the root: `php -S 127.0.0.1:8000`.
4. Open the `/android` folder in Android Studio, add your `google-services.json`, and build the project.

## 2. Architecture and Technology Choices
The system utilizes a modern decoupled architecture:
- **Mobile Frontend**: Built with **Kotlin** and **Jetpack Compose** (Material 3), using the **MVVM** pattern for clean code separation.
- **Web Backend**: Developed in **PHP 8.2**, providing a robust administrative anchor.
- **Cloud Layer**: **Firebase** integration for real-time persistence and secure authentication.

## 3. Database Structure & Table Design
The project features a dual-database design:
- **Relational MariaDB**: A 3NF normalized schema featuring tables for `Users`, `Tasks`, `Subjects`, and `UserSubjects` with optimized indexing.
- **Cloud Firestore**: A NoSQL layer for mobile-first data handling and offline persistence.
- **Data Density**: All tables are pre-populated with **10+ records** per table to demonstrate system reliability.

## 4. API Integration & Backend Services
Connectivity is achieved through the **Firebase SDK**, which acts as the primary data API for the mobile application. This enables real-time listeners for instant task updates and secure asynchronous communication between the device and the cloud backend.

## 5. Authentication and Security
- **Secure Sign-In**: Multi-role support for Students, Tutors, and Admins via **Firebase Auth**.
- **SSO Integration**: Full support for **Google Single Sign-On** for a seamless, bug-free login experience.
- **Data Security**: Credentials are encrypted prior to storage using BCRYPT (Web) and Firebase security protocols.

## 6. User Guide & Interface Documentation
The application includes a comprehensive help system. Detailed labels for all interface elements—such as the GPA Tracker, Task Cards, and Reward System—are documented in the [USER_GUIDE.md](USER_GUIDE.md) to ensure a professional and seamless user experience.

## 7. Automated Testing (GitHub Actions)
Continuous Integration (CI) is implemented via **GitHub Actions**. Every push to the repository automatically triggers an automated workflow that compiles the Android project and executes verification tests to ensure code stability.

## 8. Demonstration Video Link & Verification
A professional video demonstration is provided, showing the application running on a physical Android device. It covers authentication, settings navigation, and a walkthrough of the Firebase console.
- **Video Link**: [Insert your Google Drive or YouTube Link Here]

## 9. GitHub Repository Link
The complete source code and version control history are available at:
- **Repository**: [https://github.com/OWENM/Smart_Reminder_System-Intuthuko-main](https://github.com/OWENM/Smart_Reminder_System-Intuthuko-main)

---
*Developed by Kamohelo Simata (ST10377205) for Intuthuko Collegvate cloud sync and phone reminders.
