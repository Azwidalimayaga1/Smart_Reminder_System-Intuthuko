# CampusSync Google Calendar API integration

Implemented 19 October 2026. This is a read-only external API integration alongside the existing Firebase login and Firestore records.

## What users can do

1. Sign in to CampusSync using their real account.
2. Open Calendar (Admin: More → Personal calendar).
3. Select the Google tab and tap Connect Google Calendar.
4. Approve Google’s separate read-only event permission for the Google account matching their CampusSync email.
5. View up to 20 upcoming primary-calendar events, including recurring event occurrences, all-day events, event times and optional locations.
6. Refresh the list or Disconnect to clear the in-app view.

Disconnect clears the local view and keeps the CampusSync login active. It does not revoke the Google account grant or delete Google events. The confirmation dialog links to Google Account permissions for users who want to remove the grant. Revocation is kept separate because Google may revoke other scopes granted to the same app, including sign-in permissions.

## API and access

- Service: Google Calendar API v3.
- Request: `GET https://www.googleapis.com/calendar/v3/calendars/primary/events`.
- Scope: `https://www.googleapis.com/auth/calendar.events.readonly`.
- Parameters: `timeMin` (current instant), `maxResults=20`, `singleEvents=true`, `orderBy=startTime`, and a restricted response field selection.
- Authorization: Google Identity Services `AuthorizationClient`, separate from Firebase Authentication. The request is bound to the CampusSync account’s email.
- The Android client sends a short-lived Google access token in the HTTPS Authorization header. No backend, service-account key or client secret is embedded.
- Google events are held in memory; the integration does not upload them to Firestore, add them to personal CampusSync records, schedule duplicate reminders or write to Google Calendar.
- It does not copy events from secondary/shared calendars. It displays the next 20 results from the primary calendar and does not claim to perform a complete calendar import.

## Google Cloud setup

1. Use the Google Cloud project corresponding to the existing Firebase project, `campussync-intuthuko`.
2. Enable Google Calendar API (`calendar-json.googleapis.com`). This was confirmed enabled in the Console on 19 October 2026.
3. The Android OAuth client must match package `com.intuthuko.smartreminder` and the certificate signing the APK. Each developer’s debug signing certificate may differ.
4. Keep the existing Firebase `app/google-services.json` configuration and Google/email login.
5. Check Google Auth Platform → Audience and Data access. Add the requested event-read scope to the declared data-access configuration if required by your OAuth setup. If Audience is Testing, add the authorised demo users. The inspected project’s Audience was External / In production, with unapproved sensitive-scope limits still applicable.
6. Before public distribution, complete the required consent branding/privacy disclosures and any Google verification for the requested scope. OAuth publishing status alone does not establish production readiness.

The event-read scope declaration and its usage justification were saved in Google Auth Platform after the user explicitly approved read-only access on 19 October 2026. Google still marks the sensitive scope as unverified; no verification submission was made. The permission itself allows reading events on accessible calendars, while this app deliberately requests only the primary calendar’s events from the API.

No billing upgrade or paid backup service was enabled by this integration.

## Validation

- Debug APK builds.
- 25 unit tests pass: 22 existing tests plus 3 Google Calendar parser tests.
- Tests cover timed events in Africa/Johannesburg, all-day dates, untitled events, cancelled/malformed entries, duplicate IDs and empty calendars.
- Android lint: 0 errors, 28 warnings.
- Updated APK installed on the Samsung using an in-place update.
- Corrected an existing Calendar tab-indicator drawing issue that hid all tab labels. The corrected build passed the same tests and lint; the user confirmed Month, Timetable and Google are visible on the phone.
- Live Connect launched Google’s native Calendar consent flow. The phone displayed “Google hasn’t verified this app”, consistent with the newly declared unverified sensitive scope. The user must personally decide whether to continue their own development test; no security warning was bypassed by automation.
- The user personally continued through Google’s unverified-app notice, granted access and confirmed upcoming events appeared. The app UI also showed the connected controls and a nonempty event list.
- Live Refresh passed: the event list remained available with no connection-error message, and the Disconnect control became usable after the request.
- Live Disconnect passed: the event list disappeared, Connect returned, and the campus account remained signed in.
- Live reconnect passed: upcoming events returned using the existing grant.
- Month and Timetable both opened, and switching back to Google preserved the connected view.
- Empty-calendar behavior and malformed/date handling are covered by parser tests. Live offline refresh and cancellation were not exercised, because doing so would require changing phone connectivity or revoking the existing grant. The app implements those error/cancellation paths, but they are not marked as live-tested.

## Demonstration checklist

- Connect and allow event-read access; show a recognisable upcoming event.
- If no events exist, create one manually in Google Calendar (not through CampusSync), return to CampusSync and Refresh.
- Show the same date/time in the app, including the phone timezone.
- Cancel the consent prompt and confirm CampusSync stays signed in.
- Test a failed/offline refresh and the retry path.
- Disconnect and verify events disappear while the CampusSync account stays signed in.
- Reconnect and verify the list returns.
- Do not expose passwords, tokens or private event details in a public demonstration.

This feature demonstrates an external REST API call. It does not implement the separate Task 1 proposal for a shared website/Android REST backend or Supabase migration.

## Official references

- https://developer.android.com/identity/authorization
- https://developers.google.com/workspace/calendar/api/auth
- https://developers.google.com/workspace/calendar/api/v3/reference/events/list
