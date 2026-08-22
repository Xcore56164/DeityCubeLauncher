<p align="center">
  <img src="src/main/resources/fr/deitycube/launcher/ui/assets/logo-title.png" alt="DeityCube Launcher" width="360">
</p>

# DeityCube Launcher

Official launcher for the **DeityCube** Minecraft server. It handles sign-in (Microsoft
account or offline), automatically downloads and installs Minecraft, NeoForge and the modpack,
then launches the game — no manual file handling required.

> **Note on offline sign-in**: offline authentication is a **temporary** measure only,
> tolerated while DeityCube awaits authorization from Minecraft Services to use their
> authentication API. It is not a permanent feature: it will be removed once that
> authorization is obtained, at which point Microsoft sign-in will become mandatory.

## Features

- **Microsoft** sign-in (OAuth/PKCE flow + Xbox Live/XSTS), or a **temporary offline** profile
  (see note above — pending Minecraft Services API authorization).
- Automatic install and repair of Minecraft, NeoForge and the DeityCube modpack (every
  downloaded file is verified against a SHA-1/SHA-256 checksum).
- Multiple modpack profiles to choose from (server-defined via the DeityCube manifest).
- Adjustable allocated RAM, option to keep the launcher open while the game is running.
- **Built-in self-update**: the launcher checks for a new version on startup and offers to
  install it (download verified by SHA-256). On Windows this is fully silent with an automatic
  restart; on Linux the downloaded `.deb`/`.rpm` is handed off to your system's package manager,
  which will ask for your password to finish the install.
- Dedicated **logs** for every run (launcher and game), viewable directly from the launcher
  settings.

## Installation

Download the latest installer for your platform from [deitycube.fr](https://deitycube.fr).

**Windows** — run `DeityCubeLauncher-x.y.z.exe` and follow the wizard (per-user install, no
administrator rights required). Launch from the Start menu or desktop shortcut.

**Linux** — download the `.deb` (Debian/Ubuntu-based) or `.rpm` (Fedora/RHEL-based) package
matching your distribution and install it with your package manager, e.g.:

```bash
sudo apt install ./deitycubelauncher_x.y.z_amd64.deb   # Debian/Ubuntu
sudo dnf install ./deitycubelauncher-x.y.z.x86_64.rpm  # Fedora/RHEL
```

A menu entry is created automatically. The required Java runtime is bundled with every
installer: no separate Java installation is needed on either platform.

## Updating

The launcher automatically checks whether a new version is available every time it starts. If
one is found, it asks for confirmation before downloading it.

- **Windows**: the update installs silently and the launcher restarts on its own.
- **Linux**: since installing a `.deb`/`.rpm` requires root privileges, the downloaded package
  is opened with your desktop's package manager (GNOME Software, Discover...) instead — approve
  the installation there, then relaunch DeityCube Launcher yourself.

## Data and logs

All launcher data is stored in a single per-user folder:

| OS      | Location                              |
|---------|----------------------------------------|
| Windows | `%APPDATA%\DeityCube`                  |
| Linux   | `$XDG_DATA_HOME/DeityCube` (defaults to `~/.local/share/DeityCube`) |

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

### Building installers

```bash
./gradlew createInstaller
```

This produces the installer(s) for whichever OS it runs on — `jpackage` cannot cross-compile,
so a `.exe` must be built on Windows and `.deb`/`.rpm` on Linux. Requirements:

- **Windows**: [WiX Toolset](https://wixtoolset.org/) v3.x (`candle`/`light`) on the `PATH`.
- **Linux**: `rpm` and `fakeroot` packages installed (`dpkg-deb` for `.deb` is normally already
  present on Debian-based systems).

[.github/workflows/build-installers.yml](.github/workflows/build-installers.yml) builds both
automatically on GitHub Actions (Windows + Linux runners) and attaches the artifacts to a
GitHub Release whenever a `v*.*.*` tag is pushed:

```bash
git tag v1.1.0
git push --tags
```

### Update manifest format

The self-update system (`LauncherUpdater`) reads a JSON manifest from the URL configured as
`LAUNCHER_UPDATE_MANIFEST_URL` in `LauncherConfig`, one platform key per supported target:

```json
{
  "version": "1.1.0",
  "notes": "What's new in this release.",
  "windows":   { "installer_url": "https://deitycube.fr/downloads/DeityCubeLauncher-1.1.0.exe", "sha256": "..." },
  "linux_deb": { "installer_url": "https://deitycube.fr/downloads/deitycubelauncher_1.1.0_amd64.deb", "sha256": "..." },
  "linux_rpm": { "installer_url": "https://deitycube.fr/downloads/deitycubelauncher-1.1.0.x86_64.rpm", "sha256": "..." }
}
```

A platform key can be omitted if that build isn't ready yet — users on that platform simply
won't be offered the update until it's added. `sha256` is the checksum of the corresponding
installer file (e.g. `sha256sum file` on Linux, `Get-FileHash file -Algorithm SHA256` on
Windows).

## License

All rights reserved — see [LICENSE](LICENSE). This repository is visible for reference
purposes; reuse, modification, or redistribution is not permitted without prior agreement.

## Disclaimer

DeityCube Launcher is not affiliated with, nor endorsed by, Mojang Studios, Microsoft, or the
NeoForged project. Minecraft is a trademark of Mojang Studios / Microsoft.
