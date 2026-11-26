# Cloud Backup & Sync — New Maa Durga Billing

## Status
Development branch: `cloud-sync-development`
Base: `main` (current production source is unchanged).

## Verified current storage
The current WebView app stores `maaItems`, `maaBills`, `maaCustomers`, and `maaSettings` in browser `localStorage`. The current Shop ID/PIN check is hard-coded in JavaScript and is only a local app lock. There is no server-side sign-in or Firestore read/write in this source.

## Required design
- Keep the current production branch and APK untouched until a tested replacement is approved.
- Use Firebase Authentication for a real shop identity, and Cloud Firestore for shop-scoped data.
- Store each shop's data under an authenticated UID (not a user-supplied Shop ID path).
- Sync bills, catalog/stock, customers/balances, transactions, and settings.
- Keep local-first billing: write locally immediately, queue cloud writes while offline, retry when online.
- On a second device, authenticate first, then download and merge that shop's cloud data.
- Use stable document IDs and timestamps/revisions to avoid duplicate bills and accidental overwrites.
- Do not let logout, app reinstall, or a changed device erase cloud records.
- Keep manual JSON Backup/Restore as an additional safety option.

## Security prerequisite
The existing four-digit PIN is embedded in the client source, so it cannot safely authorize cloud access by itself. A secure server-side authentication flow is required (for example, a rate-limited HTTPS Cloud Function that verifies a hashed shop PIN and issues a Firebase custom token), plus Firestore Security Rules that restrict every read/write to the authenticated shop UID. Do not deploy permissive rules or put a privileged service-account key in the APK.

## Before enabling cloud writes
1. Confirm Firebase project owner access for project `new-maa-durga-billing`.
2. Enable/configure Firebase Authentication and Cloud Firestore.
3. Deploy the server-side PIN verification/custom-token endpoint and strict Firestore rules.
4. Implement and test the Android native Firebase bridge and local-to-cloud migration.
5. Test with disposable test bills on two devices, including offline/reconnect and reinstall scenarios.
6. Only then build a new APK; never uninstall the existing app as a test step.

## Data migration safety
The first authenticated sync must merge the current device's local records into the shop's cloud namespace without deleting or replacing existing cloud data. Conflicts must be resolved by stable IDs and timestamps, with a recoverable export before migration.
