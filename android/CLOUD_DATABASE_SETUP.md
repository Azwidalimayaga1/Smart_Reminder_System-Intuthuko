# CampusSync cloud data and permissions

Project: `campussync-intuthuko`. Cloud Firestore Standard, `(default)` database, `africa-south1` (Johannesburg), Spark plan. No billing upgrade or paid backups enabled.

## Data and access

- `/profiles/{Firebase UID}` contains the person's editable profile. Only that person can read or change it. Email must match the signed-in provider. Passwords, password hashes and roles are excluded.
- `/accounts/{UID}/items/{record ID}` contains private assignments, calendar events, notes, notifications, enrolments and GPA entries. Each record has its own stable cloud document ID, a record kind and a JSON payload. Only its owner can access it, including when the caller is a Tutor or Admin.
- `/accounts/{UID}` is a permanent one-time migration marker. Empty cloud accounts do not reimport previously deleted local records.
- `/roles/{UID}` is trusted role configuration. No app client, including an app Admin, can write it. Missing documents mean Student. `disabled: true` blocks cloud access and signs the account out when observed by the app. App Admin privileges do not grant Firebase project access.
- `/subjects` and `/timetable` are shared campus content. Signed-in, enabled accounts can read them; only Admins can write them.
- `/announcements` is shared campus news. Tutors and Admins can publish; Tutors can modify their own updates, while Admins can manage all updates. Individual dismissal stays on the device.

The project owner approved and provisioned the Google account `<project-owner-email>` as an app Admin. Its UID is the same one verified during Google sign-in. Future staff must first sign in; then the trusted project owner can add their exact UID under `roles` with `role` set to `Tutor` or `Admin`. Do not match authority by an email typed into the app, and do not enable client writes to roles.

## Migration and device behaviour

Only records explicitly owned by the signed-in local profile migrate. Demo users and their passwords are never uploaded. Existing enrolled subject names/fees are preserved as personal snapshots, even if not in the new shared catalogue. Unowned demo GPA entries, announcements and timetable data stay in the original local demo dataset; a cloud account's GPA starts empty. New campus content must be entered by authorised staff.

Cloud and demo workspaces are separated in memory. Logout removes cloud listeners and restores the original demo workspace. Firebase caches private records on this device for queued writes. The first/opening cloud session requires a network connection to verify its role and fetch its workspace. While a session is open, offline edits queue for reconnection. Editing the same record on two devices follows Firestore's last-write-wins behaviour; changes to independent records are not whole-account replacements.

The top status strip shows sync state and reports failures. App appearance and notification preferences remain device settings. Phone reminders now use local Android schedules; see PHONE_REMINDERS.md. The PHP website database remains separate. Authentication-account deletion and staff-role provisioning remain trusted project-owner operations.

## Validation

- Android debug build and existing five unit tests pass.
- Twelve Firestore emulator tests pass: unauthenticated denial, owner CRUD, cross-account denial, staff privacy, role escalation denial, profile validation, Admin-only subject management, Tutor publishing, disabled accounts, invalid/oversized records, migration marker protection, and independent record preservation.
- The emulator uses `demo-campussync`; no live student data is used in the tests.
- Live permissions were published in the Firebase Console after the user's explicit confirmation.
- Live phone-to-cloud save verified: the user completed the phone check, and the note appeared under the authenticated account in Firestore. The user subsequently confirmed that the note was still present after signing out and signing back in. A second physical device has not been tested.

Run the rules tests from `firebase-tests`: install dependencies using `npm ci`, then run `npx firebase emulators:exec --config ../firebase.json --project demo-campussync --only firestore "node --test rules.test.mjs"`. A current Java runtime is required by the emulator. Do not run connected Android tests on a phone containing user data: that Gradle workflow can remove the app and its storage.

Official references: https://firebase.google.com/docs/firestore/manage-data/enable-offline and https://firebase.google.com/docs/firestore/solutions/role-based-access.



