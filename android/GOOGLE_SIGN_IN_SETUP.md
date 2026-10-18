# Enable Google sign-in

The app contains a real Credential Manager -> Google ID token -> Firebase Authentication flow. A successful server verification is required before opening the app.

## Configured project (18 October 2026)

- Firebase project: `campussync-intuthuko` (CampusSync Intuthuko), Spark plan.
- Registered Android package: `com.intuthuko.smartreminder`.
- Google and Email/Password authentication enabled.
- Current debug signing certificate registered; Android configuration saved at `app/google-services.json` and validated to contain the Google Web OAuth client.
- Existing local demo logins remain available in debug builds. Release builds require online authentication. Local demo passwords do not become Firebase passwords.
- A real Google login was verified in Firebase Authentication > Users on 18 October 2026 after the user completed the phone sign-in.

## Reconfiguration and release

1. Create a project at https://console.firebase.google.com/ and add an Android app with package name `com.intuthuko.smartreminder`.
2. Add the SHA-1 and SHA-256 fingerprints of your Android debug signing certificate. Run the Gradle `:app:signingReport` task to get them. Register your release certificate fingerprints before distributing a release build.
3. In Authentication > Sign-in method, enable Google (choose your support email) and Email/Password.
4. Download the updated `google-services.json` and place it at `app/google-services.json`. It must include the Web OAuth client. The build applies the Google Services plugin when this file exists.
5. Rebuild and install the app. Tap Continue with Google, select an account and confirm that Firebase Authentication > Users shows the account. Test cancellation, no connection, sign-out and sign-in with another account too.
6. Test email registration, sign-in, reset email and password change. New registrations use Firebase; existing debug demo accounts remain local and are not automatically migrated.

New accounts are Students. Tutor/Admin roles come from trusted Firestore role documents, and selecting a role on the sign-in screen does not grant it. Firebase Authentication and Firestore now support private profile and record sync; see CLOUD_DATABASE_SETUP.md for permissions, migration and testing. The PHP website remains separate. Local email collisions are rejected rather than silently linking accounts.

Official integration guide: https://firebase.google.com/docs/auth/android/google-signin

## This computer's debug certificate

SHA-1: `09:70:63:2B:CB:B6:47:8A:2D:09:DD:A8:DC:B6:E7:3F:22:87:62:4D`

SHA-256: `49:76:A7:1B:2F:B4:17:C0:97:8B:49:83:3D:86:B4:BD:F7:1C:37:A5:40:29:78:45:E2:FD:4E:BF:DE:0F:D1:32`

These are public signing-certificate fingerprints, read from the current debug keystore. Use the matching certificate when testing the supplied debug APK.


