# CampusSync Android app

Native Android companion to the Intuthuko Smart Reminder website, built with Kotlin and Jetpack Compose.

## Open and run

1. Open this `android` folder in Android Studio. Use its bundled JDK and Android SDK 34. The Gradle 8.7 wrapper is included.
2. Sync Gradle, connect an Android 8.0+ phone and run the app.
3. Debug builds support local demo accounts. For real accounts, obtain `google-services.json` from the authorised Firebase project owner and place it in `app/`. That local configuration is intentionally excluded from Git.

### Debug demo sign-in

- Student: `student1@example.invalid`
- Tutor: `tutor1@example.invalid`
- Admin: `admin@example.invalid`
- Password for these **local debug demos only**: `pass123`

Demo identities are not Firebase users. Release builds require online authentication. New real accounts start as Students; staff roles must be provisioned through the trusted Firebase project owner. Selecting a role on the login screen does not grant it.

## Features

- Separate Student study dashboard, Tutor teaching workspace and Admin campus overview, each with role-specific navigation.
- Assignments, notes, calendar, subject enrolment and GPA tools.
- Google SSO and email/password authentication when Firebase is configured.
- Private cloud records per account, with shared subject catalogue and staff campus updates protected by Firestore rules.
- Android phone reminders, notification permission controls and an on-device reminder test.
- Read-only Google Calendar integration: connect your Google account, view upcoming events, refresh, and disconnect.

The PHP website and Android app currently use separate databases; they do not share login sessions or records.

## Build and test

From this folder on Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

On macOS/Linux use `./gradlew` with the same tasks. The debug APK is generated in `app/build/outputs/apk/debug/`.

The latest local checks passed 25 unit tests and Android lint with 0 errors (28 warnings). The three role dashboards were visually inspected on a Samsung SM-A526B. These are development checks, not a production-readiness certification. Real Student/Tutor account tests and release signing remain outstanding.

Run connected instrumentation tests only on a disposable emulator/test device: the Gradle workflow may uninstall the app and remove its local data.

## Firebase and reminders

- [Google sign-in setup](GOOGLE_SIGN_IN_SETUP.md)
- [Google Calendar API setup and phone test results](GOOGLE_CALENDAR_INTEGRATION.md)
- [Cloud schema, roles and emulator tests](CLOUD_DATABASE_SETUP.md)
- [Phone reminders and limitations](PHONE_REMINDERS.md)
- [Role dashboards and device checks](ROLE_DASHBOARDS.md)

Do not publish service-account credentials, signing keys or user data. The app does not require a paid Firebase plan for the configured features. Register the signing certificate used by each developer/device build for Google sign-in. Release signing and release certificate registration are separate steps before production distribution.

## Public sample data

All bundled profiles are fictional development fixtures using reserved `example.invalid` addresses. They contain no phone numbers or student numbers. Their published debug-only passwords grant no Firebase access. The installed APK used for device checks retained the original demo labels; the public source replaces those labels and addresses for privacy.
