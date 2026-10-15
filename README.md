<p align="center">
  <img src="src/main/resources/fr/deitycube/launcher/ui/assets/logo-title.png" alt="DeityCube Launcher" width="360">
</p>

# DeityCube Launcher

Official launcher for the **DeityCube** Minecraft server. Players sign in with their
**DeityCompte** (their deitycube.fr account, which is also their in-game account); the launcher
shows their DeityCompte profile, automatically downloads and installs Minecraft, NeoForge and
the modpack, then launches the game — no manual file handling required.

> **A legitimate copy of Minecraft: Java Edition is required.** The game is only ever launched
> with the player's official Minecraft account; there is no offline/cracked mode.

## Features

- **DeityCompte** sign-in: the player signs in with their deitycube.fr account (email +
  password, exchanged once for a revocable session token). The Microsoft account is linked on
  the website when the DeityCompte is created; its refresh token stays on the server. At each
  game launch the website provides a short-lived Microsoft access token, and the launcher runs
  Xbox Live → XSTS → Minecraft Services itself, from the player's own IP address.
- DeityCompte **profile** on the dashboard (photo, pseudo, role, linked Microsoft account,
  in-game identity).
- Local testing against the website in development: `-Ddeitycube.api=http://localhost:8000/api/launcher/`.
- Automatic install and repair of Minecraft, NeoForge and the DeityCube modpack (every
  downloaded file is verified against a SHA-1/SHA-256 checksum).
- Multiple modpack profiles to choose from (server-defined via the DeityCube manifest).
- Adjustable allocated RAM, option to keep the launcher open while the game is running.
- **Safe self-update** through a small, never-changing *bootstrap* (see [Updating](#updating)):
  signed downloads, installed next to the current version, automatic rollback — on both Windows
  and Linux, with no administrator rights.
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

The installer only contains the *bootstrap* and the Java runtime. On first launch the bootstrap
downloads the launcher itself (about 20 MB), so an internet connection is needed once.

## Updating

What you install is a tiny program, the **bootstrap**, that never changes from one release to
the next. Each time you start DeityCube Launcher it checks for a newer launcher, then starts it:

1. If a new version exists, it asks you whether to update (you can say "later").
2. The new version is downloaded and its **Ed25519 signature is verified** — a payload that was
   not published by DeityCube is rejected, even if the download server were compromised.
3. It is extracted **next to** the current version, which is never modified.
4. It waits a few seconds so your antivirus can scan the new files, then re-checks every file.
   If one disappeared or changed (typically an antivirus quarantine), it tells you which folder
   to add to your antivirus exclusions and offers to retry; nothing has been replaced yet.
5. Only then does the new version become the active one. If it fails to start, the bootstrap
   automatically goes back to the previous version and stops proposing the broken one.

There is no uninstall/reinstall step, no installer to run, and no administrator password, on
Windows and Linux alike. The previous major-upgrade installer flow could leave a player with
nothing installed when an antivirus killed the installer halfway.

If a future launcher needs a newer bootstrap, the bootstrap says so and sends you to the
download page to reinstall (rare by design).

> **Migrating from an older launcher** (before the bootstrap): the old launcher updates itself
> one last time through the previous mechanism, by running the new installer.

## Data and logs

All launcher data is stored in a single per-user folder:

| OS      | Location                              |
|---------|----------------------------------------|
| Windows | `%APPDATA%\DeityCube`                  |
| Linux   | `$XDG_DATA_HOME/DeityCube` (defaults to `~/.local/share/DeityCube`) |

| Folder/file      | Contents                                                        |
|-------------------|------------------------------------------------------------------|
| `launcher/`       | The launcher itself: `versions/<x.y.z>/` (kept: current + previous), `current`, `previous` |
| `game/`           | Minecraft, NeoForge, libraries, assets, modpack                  |
| `cache/`          | Temporary files (manifests)                                       |
| `logs/`           | `bootstrap.log`, `launcher-*.log` (launcher activity) and `game-*.log` (game output) |
| `settings.json`   | User preferences (profile, RAM, account...)                       |

If your antivirus complains about files under `launcher/`, adding that folder to its exclusions
is enough.

The "View logs" button in the launcher settings opens this `logs/` folder directly.

## Development

Requirements: JDK 21 (resolved automatically via the Gradle toolchain).

```bash
./gradlew run      # run the launcher in development mode
./gradlew build    # compile and run the tests
```

### Layout

| Part | What it is |
|------|------------|
| `src/main/…` | The launcher (JavaFX). Shipped as the **payload**, downloaded by the bootstrap. |
| `src/bootstrap/…` | The **bootstrap**: installed once, then frozen. Depends on nothing from the launcher (no JavaFX). |
| `src/test/…` | Unit tests of the bootstrap (signature, download, extraction, rollback, antivirus retry…). |

Anything the bootstrap and a payload must agree on (data folder layout, manifest keys read by
the bootstrap, `payload.json` schema, signed message, embedded public key) is marked
`CONTRAT FIGÉ` in the code: changing it breaks every installed bootstrap. Everything else can
evolve freely, because it ships in the payload.

### One-time setup: payload signing key

Payloads are signed with Ed25519. The private key lives only in GitHub secrets (never on the
VPS); the public key is compiled into the bootstrap, so **do this before building the first
installer** — it cannot be added later without reinstalling everywhere.

```bash
java tools/GenerateSigningKey.java
```

1. Paste the printed public key into `PayloadSignature.EMBEDDED_PUBLIC_KEY` and commit it.
2. Put the content of the generated private-key file in the GitHub secret
   `PAYLOAD_SIGNING_KEY` (Settings → Secrets and variables → Actions), keep an offline backup,
   then delete the file. Losing the key means reinstalling the bootstrap for every player.

Until the public key is set, a bootstrap refuses to install anything.

### Building installers and payloads

```bash
./gradlew createInstaller -PreleaseVersion=1.1.0    # bootstrap + runtime (.exe, or .deb/.rpm)
./gradlew createPayload   -PreleaseVersion=1.1.0    # the launcher, as build/payload/*.zip(+.sig)
```

`createPayload` signs with the key from the `PAYLOAD_SIGNING_KEY` environment variable (or
`-PpayloadSigningKeyFile=path`); add `-PrequireSigning=true` to fail instead of producing an
unsigned payload (the CI does). Both produce the artifacts for whichever OS they run on
(`jpackage` cannot cross-compile and the payload bundles OS-specific JavaFX). Requirements:

- **Windows**: [WiX Toolset](https://wixtoolset.org/) v3.x (`candle`/`light`) on the `PATH`.
- **Linux**: `rpm` and `fakeroot` packages installed (`dpkg-deb` for `.deb` is normally already
  present on Debian-based systems).

[.github/workflows/build-installers.yml](.github/workflows/build-installers.yml) builds both
automatically on GitHub Actions (Windows + Linux runners) and attaches everything to a GitHub
Release whenever a `v*.*.*` tag is pushed. The version always comes from the tag:

```bash
git tag v1.1.0
git push --tags
```

(A manual run of the workflow asks for the version.) Most releases only change the launcher:
players receive them as a payload through the bootstrap, and the installers are only needed by
new players.

### Update manifest format

The bootstrap reads `launcher_manifest.json` (URL fixed in `BootstrapEnvironment`). The `payload`
block is what current bootstraps use; the `windows` / `linux_deb` / `linux_rpm` installer keys
are only read by older launchers (no bootstrap) to migrate once, and can be dropped when none
remain:

```json
{
  "version": "1.1.0",
  "notes": "What's new in this release.",
  "bootstrap_protocol": 1,
  "payload": {
    "windows": { "url": "https://deitycube.fr/downloads/DeityCubeLauncher-payload-1.1.0-windows.zip", "sha256": "...", "signature": "<base64>" },
    "linux":   { "url": "https://deitycube.fr/downloads/DeityCubeLauncher-payload-1.1.0-linux.zip",   "sha256": "...", "signature": "<base64>" }
  },
  "windows":   { "installer_url": "https://deitycube.fr/downloads/DeityCubeLauncher-1.1.0.exe", "sha256": "..." },
  "linux_deb": { "installer_url": "https://deitycube.fr/downloads/deitycubelauncher_1.1.0_amd64.deb", "sha256": "..." },
  "linux_rpm": { "installer_url": "https://deitycube.fr/downloads/deitycubelauncher-1.1.0.x86_64.rpm", "sha256": "..." }
}
```

A platform can be omitted while its build isn't ready: players on it just aren't offered the
update. `signature` is produced by the CI (a `.sig` file next to each payload) and binds the
payload to its version, platform and hash. `bootstrap_protocol` only needs raising if a payload
requires a newer bootstrap.

### Deploying a release to the server

[scripts/launcher-deploy.sh](scripts/launcher-deploy.sh) automates the manifest step: run on the
VPS after the release has finished building in CI, it downloads that release's payloads,
signatures and installers from GitHub and regenerates `launcher_manifest.json` (SHA-256 computed,
signatures copied as produced by the CI, release notes taken from the GitHub Release body). The
VPS never holds the signing key, so even a compromised server cannot get a payload of its own
accepted.

One-time setup on the VPS (copy the script again whenever it changes):

```bash
sudo cp scripts/launcher-deploy.sh /usr/local/bin/launcher-deploy
sudo chmod +x /usr/local/bin/launcher-deploy
sudo apt install -y jq   # required, curl/sha256sum are normally already present
```

`DOWNLOADS_DIR` (where `https://deitycube.fr/downloads/` is actually served from) defaults to
`/var/www/deitycube-downloads`; edit the defaults at the top of the script, or override them in
`/etc/deitycube/launcher-deploy.conf`.

```bash
launcher-deploy 1.1.0
```

Old payload archives are kept on the server; delete the ones you no longer need.

### Testing the bootstrap locally

`./gradlew test` covers the logic (including hostile cases: tampered archive, forged or replayed
signature, path traversal, files removed by an antivirus mid-install). For a full run, serve a
manifest and a payload on `localhost` and start the bootstrap with:
`DEITYCUBE_MANIFEST_URL=http://127.0.0.1:8123/launcher_manifest.json`,
`DEITYCUBE_PAYLOAD_PUBKEY=<test public key>`, `DEITYCUBE_BOOTSTRAP_ROOT=<throwaway folder>`,
`DEITYCUBE_BOOTSTRAP_AUTO=1` (accept everything, no window), `DEITYCUBE_AV_WAIT_SECONDS=1`.
These overrides are ignored unless the manifest URL is a loopback address.

## License

All rights reserved — see [LICENSE](LICENSE). This repository is visible for reference
purposes; reuse, modification, or redistribution is not permitted without prior agreement.

## Disclaimer

DeityCube Launcher is not affiliated with, nor endorsed by, Mojang Studios, Microsoft, or the
NeoForged project. Minecraft is a trademark of Mojang Studios / Microsoft.
