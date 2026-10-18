# Button verification checklist

Implemented: local registration with validation and password confirmation; hashed local passwords; sign-in and role checking; Google sign-in with verified Firebase credentials; reset email in configured Firebase mode; password change with current-password verification or Firebase reauthentication; sign-out; profile save including email validation; assignment create/edit/delete/complete; notification read/read-all and category filters; persistent theme selection; offline save and truthful sync status; About; Admin navigation; existing notes, subjects and GPA actions persisted; form date/number validation; dashboard refresh after task changes; scrollable dialogs.

Local application records and settings are saved using Android preferences. This is not a database/API migration. Unsupported language and cloud-sync toggles were replaced by explanatory text rather than fake controls. Notification preferences filter the in-app inbox; phone background alerts are not implemented by this change. GPA and timetable remain based on the existing shared data design.

On a physical Android phone, test:
- Create a student, restart the app and log in. Reject duplicate email, invalid email, short password and mismatched confirmation.
- Edit profile and reopen it. Change password; old password must fail and new password must work.
- Create an assignment, edit its date/title, complete/uncomplete it, delete it and cancel deletion. Check dashboard totals and persistence after restart.
- Add/edit/pin/delete notes; enrol/drop subjects; add/remove GPA modules; reject invalid marks/dates/fees. Verify admin subject/user/task actions with a demo admin.
- Open each bottom tab and More link; open admin panel with admin; read individual/all notifications; change category filters; change theme and restart.
- Test all dialogs with keyboard visible and on a small screen.
- Follow GOOGLE_SIGN_IN_SETUP.md, then verify successful Google login and cancellation on a phone. Verify reset emails with a real Firebase email account.

Build and unit-test results do not substitute for these device checks. Google authentication cannot be tested end-to-end until the project owner supplies Firebase configuration.
