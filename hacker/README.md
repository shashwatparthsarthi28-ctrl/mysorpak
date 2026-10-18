# Cyber Prank — safe Android starter

This is a two-app Kotlin starter:
- `friend-app`: an opt-in full-screen red prank simulation with a local PIN exit.
- `admin-app`: a paired-device command panel to request Activate/Stop through Firestore.

**Safety and Android limits**
- This project does not use Device Admin, Accessibility abuse, kiosk/lock-task mode, overlays, or power-button interception.
- Android may prevent an app from opening an Activity from the background. Remote Activate is therefore a *request*; the friend app displays it when running/next opened, and an FCM notification is the recommended production fallback.
- Stop is also a request and takes effect when the friend app receives it.
- Pair devices only with the owner's consent. Use a test Firebase project.
- The starter uses Firebase Anonymous Authentication for a quick demo. Before real use, replace it with stronger owner authentication and enforce the pairing workflow server-side.

## Requirements
- Android Studio (current stable)
- JDK 17
- Android SDK 35
- A Firebase project with **Authentication → Anonymous** enabled and **Cloud Firestore** created.
- Add each app's Android package to Firebase and download its `google-services.json` into that app module.

## Firebase setup
1. Create a Firebase project.
2. Register these Android package IDs:
   - `com.example.cyberprank.friend`
   - `com.example.cyberprank.admin`
3. Download `google-services.json` for each app and place it at:
   - `friend-app/app/google-services.json`
   - `admin-app/app/google-services.json`
4. Enable **Authentication → Sign-in method → Anonymous**.
5. Create a Firestore database.
6. Apply `firestore.rules` from this folder as your initial demo rules.
7. Open the root folder in Android Studio, allow Gradle sync, then run either app on a test phone.

## Demo pairing flow
1. Install and open Friend app on the consenting test phone. It signs in anonymously and creates a pairing code in Firestore.
2. The friend app shows its six-character pairing code on screen.
3. Open Admin app and enter that code. The admin app attaches to the paired device record and can request Activate or Stop.
4. Keep the Friend app open during the demo for reliable live updates. Android background launch restrictions mean the project intentionally does not force-open a screen from the background.
5. Set a PIN in the Friend app's Settings field (default demo PIN: `2468`; change it before testing).

## Build APKs
- Android Studio: select `friend-app` or `admin-app` configuration and Build → Build APK(s).
- Or from the project root:
  - `./gradlew :friend-app:assembleDebug`
  - `./gradlew :admin-app:assembleDebug`

A Gradle wrapper is not bundled in this starter; Android Studio can generate it, or use an installed Gradle 8.9+.

## Important production hardening
The included Firestore rules are intentionally restrictive and demo-oriented but are not a substitute for a production pairing backend. A production version should use a trusted backend/Cloud Function to issue short-lived pairing codes, bind admin UIDs to friend device IDs, validate every command, rate-limit commands, and revoke pairings. Never expose service-account keys in either APK. Firebase client config is not a secret; Firestore rules and backend authorization are the security boundary.
