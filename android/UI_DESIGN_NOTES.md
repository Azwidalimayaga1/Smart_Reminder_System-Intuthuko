# CampusSync UI refresh

Unified native Android design: ink navy, accessible teal, mint, muted coral, warm neutral surfaces and coordinated dark mode. Shared headers, larger touch targets, consistent cards/forms/dialogs, a branded launcher icon, clearer bottom navigation, a focus dashboard, polished sign-in, grouped settings and account menus.

The dashboard now separates pending/overdue work and removes the misleading expired exam countdown. Assignment filters scroll instead of compressing labels. Secondary screens provide explicit back navigation. Signed-in state survives activity rotation; passwords and tokens are not saved in UI state. Existing authentication, local data and editing actions are retained.

This UI refresh is not a production backend deployment. Firebase configuration, real server/database integration, trusted role management, background reminders and release/device/accessibility validation remain release prerequisites. Demo credentials and local-only storage remain clearly labelled until online configuration is supplied.
