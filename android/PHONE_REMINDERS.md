# CampusSync phone reminders

Phone alerts use Android's local scheduling and notification services. No paid Firebase service, billing upgrade or paid cloud backup is required.

## Turn on alerts

1. Sign in and choose **Enable reminders** when asked, then allow Android notifications.
2. Go to **More → Settings → Phone reminders**.
3. Keep **Keep me on track** enabled. On Android 12 or later, use **Allow precise timing** and permit alarms/reminders if you want timely alerts. Without that access Android can delay delivery.
4. Tap **Test a phone reminder**, go to the phone's home screen, and wait about 20 seconds when precise timing is enabled. Tap the notification to return to CampusSync.

## What is scheduled

- Pending assignments: 9:00 AM on the due date. A task added later that day gets one catch-up alert.
- Calendar events: 30 minutes before their start. Events without a time use a 9:00 AM start. Events entered inside the reminder window get a catch-up alert, until 15 minutes after the start.
- Times follow the phone's local time zone. Schedules are refreshed after a time or time-zone change.
- Completing/submitting/deleting a task removes its reminder. Removing an event removes its reminder. Category switches control both the inbox and relevant scheduled alerts.
- Signing out cancels scheduled and displayed phone alerts. Signing back in rebuilds reminders for that person's data. Notification taps only navigate after the matching account is signed in.
- Task and event IDs cannot overwrite each other's alarms. Cloud record identity stays stable when local IDs change after another login.
- A delivered-occurrence ledger prevents repeated alerts for the same deadline. Editing a deadline or event start creates a new occurrence.
- The next 32 alarms are scheduled, and the window is refilled when an alert fires. This avoids device alarm-count limits.

## Closed app, reboot and offline behaviour

Scheduled alerts survive ordinary process termination or removal from Recents. Receivers restore schedules after reboot and app updates. Previously loaded reminders work offline. Open the app to load changes made on another device; this implementation does not add server push delivery.

Android's **Force stop** stops the app's alarms until it is opened again. Turning off notifications or restricting the app through Android settings can prevent delivery. Revoking exact-alarm access can stop existing alarms; reopening CampusSync restores them using Android's approximate scheduling. No exact-alarm exemption or notification permission is granted silently.

Notification previews use Android's private lock-screen visibility, with a generic public version. Reminder payloads stay in private app storage and do not include passwords. Receivers are not exported to other apps. Notification content opens the relevant Tasks, Calendar or Home screen, retaining sign-in checks.

## Verification

- Fourteen reminder planner regression tests cover timing, ownership, completed/submitted tasks, expired reminders, catch-up occurrence identity, malformed input, category settings, task/event ID collisions, stable cloud IDs, changed deadlines and time zones.
- Existing five app unit tests are retained.
- Android lint checks the manifest, receiver, notification and alarm API usage.
- Live background delivery verified on the Samsung: the user confirmed that the test notification arrived after leaving CampusSync for the home screen. The user also confirmed that tapping the notification opens CampusSync. Actual device reboot delivery has not been tested.

Official Android references: https://developer.android.com/develop/background-work/services/alarms and https://developer.android.com/develop/ui/compose/notifications/notification-permission.


