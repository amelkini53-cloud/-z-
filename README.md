# Vortex Role Play Launcher (Android)

Android launcher prototype built with Kotlin and Jetpack Compose. The visual theme is dark with neon purple accents.

## Included in 0.1.0
- Launcher home screen and Vortex branding.
- Server status card (placeholder until server query endpoint is configured).
- Play button that attempts to open an installed CRMP client.
- Install/update button placeholder.
- News, profile, file-check and support section cards.

## Build
1. Install Android Studio.
2. Open this repository as a Gradle project.
3. Wait for Gradle sync.
4. Run the app on an Android device or emulator.
5. To create an APK, use **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Integration still required
- Replace `com.vortexrp.crmp` with the actual package name of the compatible CRMP Android client in `MainActivity.kt` and `AndroidManifest.xml`.
- Configure the real server IP/port and a status endpoint.
- Implement a trusted update manifest, HTTPS downloads and SHA-256 verification before enabling game-file updates.
- Connect account/profile and news APIs.

This repository does not bundle a game client or third-party game assets. Use only a client and assets you are authorized to distribute.
