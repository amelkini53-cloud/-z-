# Vortex Role Play Launcher (Android)

Android launcher prototype built with Kotlin and Jetpack Compose. Dark interface with neon-purple accents.

## Features
- Vortex launcher home screen.
- Remote server list loaded from a `servers.json` file.
- Dropbox shared links are converted to direct-read links by the launcher.
- Server configuration URL is saved locally in app preferences.
- Play button opens the installed CRMP client (the client package name must be configured).
- Install/update remains a placeholder until a trusted client distribution is selected.

## Configure servers.json

The repository includes an example `servers.json`:

```json
{
  "schemaVersion": 1,
  "servers": [
    {
      "id": "main",
      "name": "Vortex Role Play",
      "host": "YOUR_SERVER_IP",
      "port": 7777,
      "version": "0.1.0",
      "players": null
    }
  ]
}
```

Each server needs a non-empty `host` and a `port` from 1 to 65535. `players` is optional and can be `null` when player count is not available.

### Dropbox setup
1. Edit `servers.json` with your real server address and port.
2. Upload it to Dropbox.
3. Create a public shared link that allows anyone with the link to view the file.
4. In Vortex Launcher, tap the settings icon and paste the shared link.
5. Tap **СОХРАНИТЬ**. The launcher fetches the JSON and displays the server list.

The launcher stores the link on the device. When you edit the same Dropbox file later, use the refresh/reopen flow or restart the app to fetch the updated contents. The link must be HTTPS and publicly readable.

## Build
1. Install Android Studio.
2. Open this repository as a Gradle project.
3. Wait for Gradle sync.
4. Run on an Android device or emulator.
5. To create an APK, use **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Still required
- Replace `com.vortexrp.crmp` with the actual package name of the selected compatible CRMP client in `MainActivity.kt` and `AndroidManifest.xml`.
- Connect the Play button to the selected client's documented server-join intent/deep link, if supported. At present it only opens the client app; it does not pass the selected server address.
- Add a real server status/player-count API if live values are needed. The `players` field is currently static JSON data.
- Implement a trusted update manifest, HTTPS downloads and SHA-256 verification before enabling game-file updates.
- Connect account/profile and news APIs.

The launcher does not bundle a game client or third-party game assets. Distribute only clients and assets you are authorized to use.

## SA-MP VIP and Payday starter

A standalone Pawn starter gamemode is available at `gamemodes/vortex_vip_payday.pwn`.

- Payday runs every 60 minutes for connected players and grants $1,000 in-game salary.
- VIP lasts 30 days.
- Bronze VIP costs 500 VC and grants 2 VC per Payday.
- Silver VIP costs 1,200 VC and grants 5 VC per Payday.
- Gold VIP costs 2,500 VC and grants 10 VC per Payday.
- `/vip` opens the VIP shop; `/vc` shows the VC balance.
- RCON admins can use `/vcgive [playerid] [amount]` to grant test VC.
- Player VC and VIP expiry are stored in the built-in SQLite database `scriptfiles/vortex_vip.db`.

This is a standalone starter gamemode, not yet merged into a larger RP gamemode. Compile it with the SA-MP server's Pawn compiler and test it on a private server before use. It has not been compiled or runtime-tested in this repository. The admin VC command is for server testing; a secure player-facing VC top-up system would need a separate verified backend and must never trust a balance supplied by the client.
