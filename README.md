<p align="center">
  <img src="src/main/resources/fr/deitycube/launcher/ui/assets/logo-title.png" alt="DeityCube Launcher" width="360">
</p>

# DeityCube Launcher

Official launcher for the **DeityCube** Minecraft modpack. It handles sign-in (Microsoft
account or offline), automatically downloads and installs Minecraft, NeoForge and the modpack,
then launches the game — no manual file handling required.

## Features

- **Microsoft** sign-in (OAuth/PKCE flow + Xbox Live/XSTS) or **offline** profile.
- Automatic install and repair of Minecraft, NeoForge and the DeityCube modpack (every
  downloaded file is verified against a SHA-1/SHA-256 checksum).
- Multiple modpack profiles to choose from (server-defined via the DeityCube manifest).
- Adjustable allocated RAM, option to keep the launcher open while the game is running.
- **Built-in self-update**: the launcher checks for a new version on startup and offers to
  install it (download verified by SHA-256, silent installation, automatic restart).
- Dedicated **logs** for every run (launcher and game), viewable directly from the launcher
  settings.

## Installation

1. Download the latest installer (`DeityCubeLauncher-x.y.z.exe`) from
   [deitycube.fr](https://deitycube.fr).
2. Run the installer and follow the wizard (per-user install, no administrator rights
   required).
3. Launch DeityCube Launcher from the Start menu or the desktop shortcut.

The required Java runtime is bundled with the installer: no separate Java installation is
needed.

## Updating

The launcher automatically checks whether a new version is available every time it starts. If
one is found, it asks for confirmation before downloading and installing the update; the
launcher then restarts on its own on the new version.

## Data and logs

All launcher data is stored under `%APPDATA%\DeityCube`:

| Folder/file      | Contents                                                        |
|-------------------|------------------------------------------------------------------|
| `game/`           | Minecraft, NeoForge, libraries, assets, modpack                  |
| `cache/`          | Temporary files (manifests, downloaded installers)                |
| `logs/`           | `launcher-*.log` (launcher activity) and `game-*.log` (game output) |
| `settings.json`   | User preferences (profile, RAM, account...)                       |

The "View logs" button in the launcher settings opens this `logs/` folder directly.

## Development

Requirements: JDK 21 (resolved automatically via the Gradle toolchain).

```bash
./gradlew run      # run the launcher in development mode
./gradlew build    # compile and run the tests
```

### Building the Windows installer

```bash
./gradlew createInstaller
```

Requires [WiX Toolset](https://wixtoolset.org/) (v3.x, `candle`/`light`) installed and on the
`PATH`. The generated `.exe` installer is placed in `build/jpackage/`.

> **Important**: the `--win-upgrade-uuid` GUID defined in `build.gradle` (the
> `createInstaller` task) must never change between versions — it is what allows a silent
> update to replace the existing installation instead of creating a new one alongside it.

## License

All rights reserved — see [LICENSE](LICENSE). This repository is visible for reference
purposes; reuse, modification, or redistribution is not permitted without prior agreement.

## Disclaimer

DeityCube Launcher is not affiliated with, nor endorsed by, Mojang Studios, Microsoft, or the
NeoForged project. Minecraft is a trademark of Mojang Studios / Microsoft.
