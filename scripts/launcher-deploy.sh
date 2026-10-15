#!/usr/bin/env bash
#
# Télécharge les fichiers d'une release GitHub de DeityCubeLauncher et régénère
# launcher_manifest.json en conséquence.
#
# Deux familles de fichiers, lues par deux générations de launcher :
#   - payload (DeityCubeLauncher-payload-<v>-<os>.zip + .sig) : le launcher lui-même, que le
#     bootstrap télécharge pour s'installer et se mettre à jour. Bloc "payload" du manifeste.
#   - installeurs (.exe / .deb / .rpm) : pour les nouveaux joueurs, et pour faire migrer UNE FOIS
#     les anciens launchers (sans bootstrap), qui lisent encore les clés "windows" / "linux_deb" /
#     "linux_rpm". À conserver tant qu'il reste de tels launchers.
#
# Installation sur le VPS :
#   sudo cp scripts/launcher-deploy.sh /usr/local/bin/launcher-deploy
#   sudo chmod +x /usr/local/bin/launcher-deploy
#
# Utilisation :
#   launcher-deploy <version>          # ex: launcher-deploy 1.1.0
#
# Configuration (valeurs par défaut ci-dessous, surchargeables dans
# /etc/deitycube/launcher-deploy.conf) :
#   GITHUB_REPO   dépôt GitHub (owner/repo)
#   DOWNLOADS_DIR dossier local servi en tant que https://deitycube.fr/downloads/
#   BASE_URL      URL publique correspondant à DOWNLOADS_DIR
#
# La clé de signature des payloads n'a rien à faire ici : elle reste dans les secrets GitHub, le
# VPS ne fait que recopier les signatures produites par la CI. Même compromis, il ne peut donc pas
# faire accepter un payload de son cru par les bootstraps.

set -euo pipefail

GITHUB_REPO="Xcore56164/DeityCubeLauncher"
DOWNLOADS_DIR="/var/www/deitycube-downloads"
BASE_URL="https://deitycube.fr/downloads"

# Version du contrat bootstrap/payload (voir BootstrapMain.PROTOCOL) : à n'augmenter que si un
# payload exige un bootstrap plus récent.
BOOTSTRAP_PROTOCOL=1

CONFIG_FILE="/etc/deitycube/launcher-deploy.conf"
if [ -f "$CONFIG_FILE" ]; then
    # shellcheck disable=SC1090
    source "$CONFIG_FILE"
fi

usage() {
    echo "Usage: launcher-deploy <version>" >&2
    echo "Exemple : launcher-deploy 1.1.0" >&2
    exit 1
}

[ $# -eq 1 ] || usage

RAW_VERSION="$1"
VERSION="${RAW_VERSION#v}"
TAG="v${VERSION}"

for cmd in curl jq sha256sum; do
    command -v "$cmd" >/dev/null 2>&1 || {
        echo "Erreur : '$cmd' est requis mais introuvable (essaie : sudo apt install $cmd)." >&2
        exit 1
    }
done

mkdir -p "$DOWNLOADS_DIR"

echo "==> Répertoire cible : ${DOWNLOADS_DIR}"
echo "==> Récupération de la release ${TAG} (${GITHUB_REPO})..."

RELEASE_JSON=$(curl -sSfL -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${GITHUB_REPO}/releases/tags/${TAG}") || {
    echo "Erreur : impossible de récupérer la release '${TAG}' (vérifie qu'elle existe et que le dépôt est public)." >&2
    exit 1
}

RELEASE_NOTES=$(jq -r '.body // ""' <<< "$RELEASE_JSON")

# $1 = motif (regex) du nom de fichier à chercher parmi les assets de la release
find_asset_url() {
    jq -r --arg pattern "$1" \
        '[.assets[] | select(.name | test($pattern))] | .[0].browser_download_url // empty' \
        <<< "$RELEASE_JSON"
}

download() {
    local url="$1"
    local dest="$2"
    echo "==> Téléchargement de $(basename "$dest") vers ${dest}..."
    curl -sSfL -o "$dest" "$url"
}

WINDOWS_URL=$(find_asset_url '\.exe$')
DEB_URL=$(find_asset_url '\.deb$')
RPM_URL=$(find_asset_url '\.rpm$')
PAYLOAD_WINDOWS_URL=$(find_asset_url '-payload-.*-windows\.zip$')
PAYLOAD_LINUX_URL=$(find_asset_url '-payload-.*-linux\.zip$')
PAYLOAD_WINDOWS_SIG_URL=$(find_asset_url '-payload-.*-windows\.zip\.sig$')
PAYLOAD_LINUX_SIG_URL=$(find_asset_url '-payload-.*-linux\.zip\.sig$')

if [ -z "$WINDOWS_URL$DEB_URL$RPM_URL$PAYLOAD_WINDOWS_URL$PAYLOAD_LINUX_URL" ]; then
    echo "Erreur : aucun fichier de release (.exe/.deb/.rpm/payload) trouvé dans la release ${TAG}." >&2
    exit 1
fi

# Un payload sans sa signature est inutilisable (tout bootstrap le refuserait) : mieux vaut
# s'arrêter ici que publier un manifeste qui ferait échouer les mises à jour.
if [ -n "$PAYLOAD_WINDOWS_URL" ] && [ -z "$PAYLOAD_WINDOWS_SIG_URL" ]; then
    echo "Erreur : payload Windows trouvé sans fichier .sig dans la release ${TAG}." >&2
    exit 1
fi
if [ -n "$PAYLOAD_LINUX_URL" ] && [ -z "$PAYLOAD_LINUX_SIG_URL" ]; then
    echo "Erreur : payload Linux trouvé sans fichier .sig dans la release ${TAG}." >&2
    exit 1
fi

WINDOWS_SHA="" WINDOWS_FILENAME=""
DEB_SHA="" DEB_FILENAME=""
RPM_SHA="" RPM_FILENAME=""
PAYLOAD_WINDOWS_SHA="" PAYLOAD_WINDOWS_FILENAME="" PAYLOAD_WINDOWS_SIG=""
PAYLOAD_LINUX_SHA="" PAYLOAD_LINUX_FILENAME="" PAYLOAD_LINUX_SIG=""

if [ -n "$WINDOWS_URL" ]; then
    WINDOWS_FILENAME=$(basename "$WINDOWS_URL")
    download "$WINDOWS_URL" "${DOWNLOADS_DIR}/${WINDOWS_FILENAME}"
    WINDOWS_SHA=$(sha256sum "${DOWNLOADS_DIR}/${WINDOWS_FILENAME}" | awk '{print $1}')
fi

if [ -n "$DEB_URL" ]; then
    DEB_FILENAME=$(basename "$DEB_URL")
    download "$DEB_URL" "${DOWNLOADS_DIR}/${DEB_FILENAME}"
    DEB_SHA=$(sha256sum "${DOWNLOADS_DIR}/${DEB_FILENAME}" | awk '{print $1}')
fi

if [ -n "$RPM_URL" ]; then
    RPM_FILENAME=$(basename "$RPM_URL")
    download "$RPM_URL" "${DOWNLOADS_DIR}/${RPM_FILENAME}"
    RPM_SHA=$(sha256sum "${DOWNLOADS_DIR}/${RPM_FILENAME}" | awk '{print $1}')
fi

if [ -n "$PAYLOAD_WINDOWS_URL" ]; then
    PAYLOAD_WINDOWS_FILENAME=$(basename "$PAYLOAD_WINDOWS_URL")
    download "$PAYLOAD_WINDOWS_URL" "${DOWNLOADS_DIR}/${PAYLOAD_WINDOWS_FILENAME}"
    PAYLOAD_WINDOWS_SHA=$(sha256sum "${DOWNLOADS_DIR}/${PAYLOAD_WINDOWS_FILENAME}" | awk '{print $1}')
    PAYLOAD_WINDOWS_SIG=$(curl -sSfL "$PAYLOAD_WINDOWS_SIG_URL" | tr -d '\r\n ')
fi

if [ -n "$PAYLOAD_LINUX_URL" ]; then
    PAYLOAD_LINUX_FILENAME=$(basename "$PAYLOAD_LINUX_URL")
    download "$PAYLOAD_LINUX_URL" "${DOWNLOADS_DIR}/${PAYLOAD_LINUX_FILENAME}"
    PAYLOAD_LINUX_SHA=$(sha256sum "${DOWNLOADS_DIR}/${PAYLOAD_LINUX_FILENAME}" | awk '{print $1}')
    PAYLOAD_LINUX_SIG=$(curl -sSfL "$PAYLOAD_LINUX_SIG_URL" | tr -d '\r\n ')
fi

MANIFEST_PATH="${DOWNLOADS_DIR}/launcher_manifest.json"

if [ -f "$MANIFEST_PATH" ]; then
    cp "$MANIFEST_PATH" "${MANIFEST_PATH}.bak"
fi

echo "==> Écriture de ${MANIFEST_PATH}..."

jq -n \
    --arg version "$VERSION" \
    --arg notes "$RELEASE_NOTES" \
    --argjson protocol "$BOOTSTRAP_PROTOCOL" \
    --arg windows_url "${WINDOWS_FILENAME:+${BASE_URL}/${WINDOWS_FILENAME}}" \
    --arg windows_sha "$WINDOWS_SHA" \
    --arg deb_url "${DEB_FILENAME:+${BASE_URL}/${DEB_FILENAME}}" \
    --arg deb_sha "$DEB_SHA" \
    --arg rpm_url "${RPM_FILENAME:+${BASE_URL}/${RPM_FILENAME}}" \
    --arg rpm_sha "$RPM_SHA" \
    --arg pw_url "${PAYLOAD_WINDOWS_FILENAME:+${BASE_URL}/${PAYLOAD_WINDOWS_FILENAME}}" \
    --arg pw_sha "$PAYLOAD_WINDOWS_SHA" \
    --arg pw_sig "$PAYLOAD_WINDOWS_SIG" \
    --arg pl_url "${PAYLOAD_LINUX_FILENAME:+${BASE_URL}/${PAYLOAD_LINUX_FILENAME}}" \
    --arg pl_sha "$PAYLOAD_LINUX_SHA" \
    --arg pl_sig "$PAYLOAD_LINUX_SIG" \
    '
    {version: $version, notes: $notes, bootstrap_protocol: $protocol}
    + (
        (if $pw_url != "" then {windows: {url: $pw_url, sha256: $pw_sha, signature: $pw_sig}} else {} end)
        + (if $pl_url != "" then {linux: {url: $pl_url, sha256: $pl_sha, signature: $pl_sig}} else {} end)
        | if . == {} then {} else {payload: .} end
      )
    + (if $windows_url != "" then {windows: {installer_url: $windows_url, sha256: $windows_sha}} else {} end)
    + (if $deb_url != "" then {linux_deb: {installer_url: $deb_url, sha256: $deb_sha}} else {} end)
    + (if $rpm_url != "" then {linux_rpm: {installer_url: $rpm_url, sha256: $rpm_sha}} else {} end)
    ' > "${MANIFEST_PATH}.tmp"

mv "${MANIFEST_PATH}.tmp" "$MANIFEST_PATH"

echo "==> Terminé : ${MANIFEST_PATH}"
echo
cat "$MANIFEST_PATH"
