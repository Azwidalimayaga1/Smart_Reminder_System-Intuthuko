# CampusSync role dashboards

Implemented 18 October 2026.

## Student

Teal study overview with personal progress, pending and overdue assignments, reward points, assignments/notes/GPA shortcuts, next steps and campus news. Submitted assignments now count towards finished work in the progress ring.

Bottom navigation: Home, Tasks, Calendar, Notes, More.

## Tutor

Violet teaching workspace with a publishing action, personal agenda, event and personal-task counts, private teaching notes and follow-ups. Agenda entries are the tutor's own calendar events; the app does not infer assigned classes or expose other accounts' work.

Bottom navigation: Workspace, Planner, Updates, Notes, More.

## Admin

Navy-blue campus overview with shared subject and visible-update counts, catalogue management, campus communications and personal planning. No invented student totals or access to private student work. Staff roles continue to be provisioned through the trusted Firebase project owner.

Bottom navigation: Overview, Manage, Updates, More.

## Shared behavior

- Dashboard selection follows the authenticated account's role.
- Changing an account or role rebuilds navigation so the previous workspace is discarded.
- Bottom navigation remains available on secondary screens, including personal tools outside staff tabs.
- Staff personal tools remain available under More; GPA/enrolment shortcuts appear for Students.
- Campus updates are readable by signed-in users. Publishing is offered to Tutor/Admin roles; existing Firebase rules continue to enforce permissions.
- Demo publishing explains that a real staff account is required; it does not claim to publish to the cloud.
- App data and existing Firebase settings are preserved. No billing or backup services were activated.

## Validation

- Debug APK built successfully.
- 22 unit tests passed (19 existing tests and 3 role-navigation tests).
- Android lint: 0 errors, 27 warnings.
- Updated APK installed on the connected Samsung using an in-place update.
- Visually inspected Student, Tutor and Admin dashboards on the Samsung SM-A526B.
- Verified Student Tasks, Calendar, Notes and GPA navigation and return Home; campus news hides publishing for Students.
- Verified Tutor planner, private notes and updates navigation and return to Workspace. Demo publishing requests a real staff account.
- Verified the real Admin's management screen, Add Subject dialog (cancelled), update composer (closed without publishing) and saved cloud note.
- Found and fixed a bottom-tab restoration bug that could reopen a child screen when returning to a dashboard. Rebuilt, reran the 22 unit tests and lint, and reinstalled the fixed APK.
- Verified the fixed Admin navigation from management, campus updates, notifications, profile and private notes back to Overview using the local Admin demo. Tested Tutor preparation-note and personal-follow-up shortcuts and the read-updates shortcut.
- No crashes observed during these checks. The cloud publishing composer was inspected but no update was published; no shared subjects were created or deleted.
- Real Student/Tutor cloud-account testing remains pending account creation. These role-layout checks used existing local demo profiles for Student/Tutor.

These checks do not establish production readiness or replace a full device and account test.
