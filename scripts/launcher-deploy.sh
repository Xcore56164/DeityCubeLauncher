#!/usr/bin/env bash
#
# Télécharge les installeurs d'une release GitHub de DeityCubeLauncher et régénère
# launcher_manifest.json en conséquence (utilisé par le launcher pour l'auto-update).
#
# Installation sur le VPS :
#   sudo cp scripts/launcher-deploy.sh /usr/local/bin/launcher-deploy
#   sudo chmod +x /usr/local/bin/launcher-deploy
#
# Utilisation :
#   launcher-deploy <version>          # ex: launcher-deploy 1.0.1
#
# Configuration (valeurs par défaut ci-dessous, surchargeables dans
# /etc/deitycube/launcher-deploy.conf) :
#   GITHUB_REPO   dépôt GitHub (owner/repo)
#   DOWNLOADS_DIR dossier local servi en tant que https://deitycube.fr/downloads/
#   BASE_URL      URL publique correspondant à DOWNLOADS_DIR

set -euo pipefail

GITHUB_REPO="Xcore56164/DeityCubeLauncher"
DOWNLOADS_DIR="/var/www/deitycube-downloads"
BASE_URL="https://deitycube.fr/downloads"

CONFIG_FILE="/etc/deitycube/launcher-deploy.conf"
if [ -f "$CONFIG_FILE" ]; then
    # shellcheck disable=SC1090
    source "$CONFIG_FILE"
fi

usage() {
    echo "Usage: launcher-deploy <version>" >&2
    echo "Exemple : launcher-deploy 1.0.1" >&2
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

if [ -z "$WINDOWS_URL" ] && [ -z "$DEB_URL" ] && [ -z "$RPM_URL" ]; then
    echo "Erreur : aucun installeur (.exe/.deb/.rpm) trouvé dans la release ${TAG}." >&2
    exit 1
fi

WINDOWS_SHA="" WINDOWS_FILENAME=""
DEB_SHA="" DEB_FILENAME=""
RPM_SHA="" RPM_FILENAME=""

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

MANIFEST_PATH="${DOWNLOADS_DIR}/launcher_manifest.json"

if [ -f "$MANIFEST_PATH" ]; then
    cp "$MANIFEST_PATH" "${MANIFEST_PATH}.bak"
fi

echo "==> Écriture de ${MANIFEST_PATH}..."

jq -n \
    --arg version "$VERSION" \
    --arg notes "$RELEASE_NOTES" \
    --arg windows_url "${WINDOWS_FILENAME:+${BASE_URL}/${WINDOWS_FILENAME}}" \
    --arg windows_sha "$WINDOWS_SHA" \
    --arg deb_url "${DEB_FILENAME:+${BASE_URL}/${DEB_FILENAME}}" \
    --arg deb_sha "$DEB_SHA" \
    --arg rpm_url "${RPM_FILENAME:+${BASE_URL}/${RPM_FILENAME}}" \
    --arg rpm_sha "$RPM_SHA" \
    '
    {version: $version, notes: $notes}
    + (if $windows_url != "" then {windows: {installer_url: $windows_url, sha256: $windows_sha}} else {} end)
    + (if $deb_url != "" then {linux_deb: {installer_url: $deb_url, sha256: $deb_sha}} else {} end)
    + (if $rpm_url != "" then {linux_rpm: {installer_url: $rpm_url, sha256: $rpm_sha}} else {} end)
    ' > "${MANIFEST_PATH}.tmp"

mv "${MANIFEST_PATH}.tmp" "$MANIFEST_PATH"

echo "==> Terminé : ${MANIFEST_PATH}"
echo
cat "$MANIFEST_PATH"
