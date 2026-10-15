<#
.SYNOPSIS
  Teste le bootstrap en local, sans rien publier et sans toucher à l'installation réelle.

.DESCRIPTION
  Construit de vrais payloads du launcher avec des versions de test (0.0.1, 0.0.2, 0.0.3), les
  signe avec une clé jetable, les sert depuis un petit serveur local et lance le bootstrap en le
  pointant dessus. Le bootstrap n'accepte ce détournement que pour une adresse localhost : ça ne
  peut pas servir à le faire parler à un autre serveur, et tes joueurs ne voient jamais ces versions.

  Tout vit dans %TEMP%\deitycube-bootstrap-test (données du bootstrap isolées de ta vraie
  installation : %APPDATA%\DeityCube\launcher n'est pas touché).

  Les versions de test ne peuvent pas contenir de lettres ("1.0.test" serait refusé) : une version
  sert de nom de dossier et se compare numériquement. 0.0.x se reconnaît dans le titre du launcher.

  Le bootstrap lancé est, dans l'ordre : -Exe, sinon l'installation réelle
  (%LOCALAPPDATA%\DeityCube Launcher), sinon les jars compilés du dépôt.

.PARAMETER Step
  1 = première installation (0.0.1)
  2 = mise à jour vers 0.0.2 (l'ancienne version n'est pas touchée)
  3 = "mise à jour" vers 0.0.3 qui plante : retour automatique à 0.0.2

.PARAMETER Auto
  Sans fenêtres (tout est accepté, attente antivirus de 1 s). Pour automatiser ; sans ce
  paramètre tu vois les vraies boîtes de dialogue.

.PARAMETER Clean
  Arrête le serveur local et efface tout.

.EXAMPLE
  .\tools\test-bootstrap.ps1 -Step 1
  .\tools\test-bootstrap.ps1 -Step 2
  .\tools\test-bootstrap.ps1 -Step 3
  .\tools\test-bootstrap.ps1 -Clean
#>
param(
    [ValidateSet(0, 1, 2, 3)]
    [int]$Step = 0,
    [switch]$Clean,
    [switch]$Auto,
    [string]$Exe
)

# Les programmes natifs (gradle, java) écrivent sur stderr sans que ce soit une erreur : on teste
# $LASTEXITCODE nous-mêmes.
$ErrorActionPreference = 'Continue'

$Repo = Split-Path -Parent $PSScriptRoot
$Work = Join-Path $env:TEMP 'deitycube-bootstrap-test'
$Www = Join-Path $Work 'www'
$Root = Join-Path $Work 'root'
$KeyFile = Join-Path $Work 'test.key'
$PubFile = Join-Path $Work 'test.pub'
$PidFile = Join-Path $Work 'server.pid'
$Port = 8123
$Utf8 = New-Object System.Text.UTF8Encoding($false)   # sans BOM : un BOM ferait échouer la lecture du JSON

function Find-Java {

    $candidates = @()

    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }

    $onPath = Get-Command java -ErrorAction SilentlyContinue
    if ($onPath) { $candidates += $onPath.Source }

    $jdks = Join-Path $env:USERPROFILE '.jdks'
    if (Test-Path $jdks) {
        $candidates += Get-ChildItem $jdks -Directory | Sort-Object Name -Descending |
                ForEach-Object { Join-Path $_.FullName 'bin\java.exe' }
    }

    foreach ($candidate in $candidates) {

        if (-not (Test-Path $candidate)) { continue }

        # Il faut Java 21 ou plus (le bootstrap et le serveur local en ont besoin).
        $release = Join-Path (Split-Path -Parent (Split-Path -Parent $candidate)) 'release'
        $major = 0

        if (Test-Path $release) {
            $line = Get-Content $release | Where-Object { $_ -like 'JAVA_VERSION=*' } | Select-Object -First 1
            if ($line -match '"(\d+)') { $major = [int]$Matches[1] }
        }

        if ($major -ge 21) { return $candidate }
    }

    throw "Java 21 ou plus introuvable (JAVA_HOME, PATH ou ~\.jdks)."
}

function Invoke-Gradle([string[]]$GradleArgs) {

    Push-Location $Repo

    try {
        $output = & "$Repo\gradlew.bat" @GradleArgs --console=plain 2>&1 | Out-String

        if ($LASTEXITCODE -ne 0) {
            Write-Host $output
            throw "Gradle a échoué : $($GradleArgs -join ' ')"
        }
    }
    finally {
        Pop-Location
    }
}

function Initialize-Work {

    New-Item -ItemType Directory -Force $Www | Out-Null

    if ((Test-Path $KeyFile) -and (Test-Path $PubFile)) { return }

    Remove-Item $KeyFile -ErrorAction SilentlyContinue

    $output = & $script:Java (Join-Path $Repo 'tools\GenerateSigningKey.java') $KeyFile
    $publicKey = $output | Where-Object { $_ -match '^MC[A-Za-z0-9+/=]{40,}$' } | Select-Object -First 1

    if (-not $publicKey) { throw "La clé de test n'a pas pu être générée." }

    [IO.File]::WriteAllText($PubFile, $publicKey.Trim(), $Utf8)
}

function New-Payload([string]$Version) {

    $zip = Join-Path $Www "DeityCubeLauncher-payload-$Version-windows.zip"

    if (Test-Path "$zip.sig") { return $zip }

    Write-Host "Construction du payload $Version (le vrai launcher, signé avec la clé de test)..."

    Invoke-Gradle @('createPayload', "-PreleaseVersion=$Version", "-PpayloadSigningKeyFile=$KeyFile", '-PrequireSigning=true')

    Copy-Item "$Repo\build\payload\*" $Www -Force

    return $zip
}

# Un payload authentique et correctement signé, mais dont la classe principale n'existe pas :
# le launcher plante immédiatement, ce qui déclenche le retour automatique à la version précédente.
function New-CrashPayload([string]$Version) {

    $zip = Join-Path $Www "DeityCubeLauncher-payload-$Version-windows.zip"

    if (Test-Path "$zip.sig") { return $zip }

    Write-Host "Construction du payload $Version (volontairement cassé)..."

    Invoke-Gradle @('bootstrapDist')

    $lib = Join-Path $Work 'crash-lib'
    New-Item -ItemType Directory -Force $lib | Out-Null
    [IO.File]::WriteAllText((Join-Path $lib 'dummy.jar'), 'pas un vrai jar', $Utf8)

    & $script:Java -cp "$Repo\build\classes\java\bootstrap;$Repo\build\bootstrap-dist\lib\*" `
        fr.deitycube.bootstrap.PayloadBuilder `
        --lib-dir $lib --out $zip --version $Version --platform windows `
        --main-class fr.nope.Missing --private-key-file $KeyFile | Out-Null

    if (-not (Test-Path "$zip.sig")) { throw "Le payload cassé n'a pas pu être construit." }

    return $zip
}

function Publish-Manifest([string]$Version, [string]$Zip) {

    $sha256 = (Get-FileHash $Zip -Algorithm SHA256).Hash.ToLower()
    $signature = ([IO.File]::ReadAllText("$Zip.sig")).Trim()

    $manifest = [ordered]@{
        version            = $Version
        notes              = "Version de test $Version (serveur local, rien n'est publié)."
        bootstrap_protocol = 1
        payload            = [ordered]@{
            windows = [ordered]@{
                url       = "http://127.0.0.1:$Port/$(Split-Path -Leaf $Zip)"
                sha256    = $sha256
                signature = $signature
            }
        }
    }

    [IO.File]::WriteAllText((Join-Path $Www 'launcher_manifest.json'), ($manifest | ConvertTo-Json -Depth 6), $Utf8)

    Write-Host "Manifeste de test publié en local : version $Version"
}

function Start-TestServer {

    if (Test-Path $PidFile) {
        $running = Get-Process -Id ([int](Get-Content $PidFile)) -ErrorAction SilentlyContinue
        if ($running) { return }
    }

    $server = Start-Process -FilePath $script:Java -WindowStyle Hidden -PassThru `
        -ArgumentList @('-m', 'jdk.httpserver', '-b', '127.0.0.1', '-p', "$Port", '-d', "`"$Www`"")

    Set-Content $PidFile $server.Id

    for ($i = 0; $i -lt 24; $i++) {
        try {
            Invoke-WebRequest "http://127.0.0.1:$Port/" -UseBasicParsing | Out-Null
            return
        }
        catch {
            Start-Sleep -Milliseconds 250
        }
    }

    throw "Le serveur local ne répond pas sur le port $Port (déjà utilisé ?)."
}

function Test-BootstrapRunning {

    $byName = Get-Process -Name 'DeityCube Launcher' -ErrorAction SilentlyContinue

    $byCommand = Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" |
            Where-Object { $_.CommandLine -like '*fr.deitycube.bootstrap.BootstrapMain*' }

    return [bool]($byName -or $byCommand)
}

function Start-Bootstrap {

    $exePath = $Exe

    if (-not $exePath) {
        $installed = Join-Path $env:LOCALAPPDATA 'DeityCube Launcher\DeityCube Launcher.exe'
        if (Test-Path $installed) { $exePath = $installed }
    }

    $env:DEITYCUBE_MANIFEST_URL = "http://127.0.0.1:$Port/launcher_manifest.json"
    $env:DEITYCUBE_PAYLOAD_PUBKEY = ([IO.File]::ReadAllText($PubFile)).Trim()
    $env:DEITYCUBE_BOOTSTRAP_ROOT = $Root

    if ($Auto) {
        $env:DEITYCUBE_BOOTSTRAP_AUTO = '1'
        $env:DEITYCUBE_AV_WAIT_SECONDS = '1'
    }

    try {
        if ($exePath) {
            Write-Host "Bootstrap lancé : $exePath"
            Start-Process $exePath
        }
        else {
            Write-Host "Aucune installation trouvée : bootstrap lancé depuis les jars du dépôt."

            Invoke-Gradle @('bootstrapDist')

            $javaw = Join-Path (Split-Path -Parent $script:Java) 'javaw.exe'
            $launcher = if (Test-Path $javaw) { $javaw } else { $script:Java }

            Start-Process $launcher -ArgumentList @('-cp', "`"$Repo\build\bootstrap-dist\lib\*`"", 'fr.deitycube.bootstrap.BootstrapMain')
        }
    }
    finally {
        # Les variables ne doivent pas rester dans ce terminal : elles détourneraient le bootstrap
        # de n'importe quel lancement ultérieur depuis cette fenêtre.
        Remove-Item 'Env:DEITYCUBE_*' -ErrorAction SilentlyContinue
    }
}

function Show-Pointers {

    foreach ($name in 'current', 'previous', 'blocked') {
        $file = Join-Path $Root "launcher\$name"
        $value = if (Test-Path $file) { ([IO.File]::ReadAllText($file)).Trim() } else { '(aucune)' }
        Write-Host ("  {0,-9} = {1}" -f $name, $value)
    }
}

# --- Programme ------------------------------------------------------------------------------

if ($Clean) {

    if (Test-Path $PidFile) {
        Stop-Process -Id ([int](Get-Content $PidFile)) -Force -ErrorAction SilentlyContinue
    }

    Get-CimInstance Win32_Process |
            Where-Object { $_.CommandLine -and $_.CommandLine -like "*$Work*" } |
            ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }

    Start-Sleep -Seconds 1
    Remove-Item $Work -Recurse -Force -ErrorAction SilentlyContinue

    Write-Host "Nettoyé : serveur de test arrêté, $Work supprimé."
    return
}

if ($Step -eq 0) {
    Get-Help $PSCommandPath -Detailed
    return
}

$script:Java = Find-Java
Write-Host "Java : $script:Java"

if (Test-BootstrapRunning) {
    Write-Host ""
    Write-Host "Un bootstrap est encore actif (il se ferme ~20 s après avoir lancé le launcher)." -ForegroundColor Yellow
    Write-Host "Patiente un peu puis relance cette commande." -ForegroundColor Yellow
    return
}

Initialize-Work
Start-TestServer

switch ($Step) {

    1 {
        Publish-Manifest '0.0.1' (New-Payload '0.0.1')
        Start-Bootstrap

        Write-Host ""
        Write-Host "ÉTAPE 1 — première installation. À observer :" -ForegroundColor Cyan
        Write-Host "  1. une fenêtre « Installation » : clique sur Installer"
        Write-Host "  2. une barre de téléchargement, puis l'extraction"
        Write-Host "  3. un compte à rebours « Analyse antivirus en cours… » (10 s)"
        Write-Host "  4. le launcher s'ouvre, titre « DeityCube Launcher  v0.0.1 »"
        Write-Host ""
        Write-Host "  Astuce : pendant le compte à rebours, supprime un fichier .jar dans"
        Write-Host "  $Root\launcher\staging\extracted-0.0.1\lib"
        Write-Host "  pour simuler un antivirus : le dialogue « Fichiers bloqués » doit apparaître."
    }

    2 {
        if (-not (Test-Path (Join-Path $Root 'launcher\current'))) {
            Write-Host "Fais d'abord l'étape 1." -ForegroundColor Yellow
            return
        }

        Publish-Manifest '0.0.2' (New-Payload '0.0.2')
        Start-Bootstrap

        Write-Host ""
        Write-Host "ÉTAPE 2 — mise à jour. À observer :" -ForegroundColor Cyan
        Write-Host "  1. une fenêtre « Mise à jour » proposant la 0.0.2 (avec « Plus tard »)"
        Write-Host "  2. le même déroulé : téléchargement, compte à rebours, vérification"
        Write-Host "  3. le launcher s'ouvre en v0.0.2 ; la v0.0.1 n'a jamais été touchée"
        Write-Host "     (tu peux d'ailleurs laisser l'ancienne fenêtre ouverte pendant la mise à jour)"
    }

    3 {
        if (-not (Test-Path (Join-Path $Root 'launcher\current'))) {
            Write-Host "Fais d'abord les étapes 1 et 2." -ForegroundColor Yellow
            return
        }

        Publish-Manifest '0.0.3' (New-CrashPayload '0.0.3')
        Start-Bootstrap

        Write-Host ""
        Write-Host "ÉTAPE 3 — version qui plante. À observer :" -ForegroundColor Cyan
        Write-Host "  1. une proposition de mise à jour vers 0.0.3 : accepte"
        Write-Host "  2. la 0.0.3 est installée puis plante aussitôt (rien ne s'affiche)"
        Write-Host "  3. une alerte « Retour à la version précédente »"
        Write-Host "  4. le launcher s'ouvre en v0.0.2 ; relancer l'étape 3 ne reproposera plus la 0.0.3"
    }
}

Write-Host ""
Write-Host "Journal du bootstrap : $Root\logs\bootstrap.log"

if ($Auto) {
    Start-Sleep -Seconds 14
    Write-Host "Versions après ce lancement :"
    Show-Pointers
}
else {
    Write-Host "Version active (une fois l'installation terminée) : Get-Content `"$Root\launcher\current`""
}

Write-Host ""
Write-Host "Quand tu as fini : .\tools\test-bootstrap.ps1 -Clean"
