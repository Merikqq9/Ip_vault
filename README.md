# IP VAULT

Minimal Android starter built with Kotlin and Jetpack Compose.

## Included in this first scaffold
- Minimal black and emerald interface
- First-run trust setup that adds the current public IP to the global allowlist
- Public IP lookup on launch and local allowlist persistence with DataStore
- Unknown IP or unavailable IP check blocks the app and finishes the Activity
- Settings screen to add and remove allowed IPs
- File picker UI placeholder
- Screenshot blocking with FLAG_SECURE
- GitHub Actions workflow to build a debug APK and upload it as an artifact

## Security status — not production-ready
This is a project foundation, not yet a secure file vault. The file picker currently displays selected file names only; it does not copy or encrypt file contents. PIN authentication, AES-GCM file encryption with Android Keystore, emergency network recovery, and continuous network monitoring remain to be implemented and tested before storing sensitive files.

The initial trust setup bootstraps the first allowed IP. Do not treat the current scaffold as protection for confidential data.

## Build
The GitHub Actions workflow builds the debug APK on every push to main and on manual dispatch. Open the project in Android Studio with JDK 17 and Android SDK 35 to build locally.
