<#
.SYNOPSIS
  Prepara o projeto Android "Painel Financeiro" para submissão ao F-Droid.

.DESCRIPTION
  1. Lê o applicationId, o versionCode e o versionName do app/build.gradle(.kts).
  2. Verifica os requisitos do F-Droid (componentes proprietários, binários, assinatura, Gradle wrapper).
  3. Copia a licença e a pasta fastlane (textos da loja) para a raiz do projeto.
  4. Gera o ficheiro de metadados metadata/<applicationId>.yml para o repositório fdroiddata.
  Com -Publicar, faz também o commit, cria a tag da versão e envia tudo para o GitHub.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\preparar_fdroid.ps1 -Projeto "C:\Users\rodri\Desktop\PDM\painel-financeiro-android"

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\preparar_fdroid.ps1 -Projeto "C:\...\painel-financeiro-android" -Utilizador rodrigo -Autor "Rodrigo Silva" -Publicar
#>
param(
    [Parameter(Mandatory = $true)][string]$Projeto,
    [string]$Utilizador = "",
    [string]$Repositorio = "",
    [string]$Autor = "",
    [switch]$Publicar
)

$ErrorActionPreference = "Stop"
$Aqui = Split-Path -Parent $MyInvocation.MyCommand.Path
$Utf8SemBom = New-Object System.Text.UTF8Encoding $false
$Problemas = New-Object System.Collections.ArrayList
$Avisos = New-Object System.Collections.ArrayList

function Ok($t)       { Write-Host "  [OK]       $t" -ForegroundColor Green }
function Aviso($t)    { [void]$Avisos.Add($t);    Write-Host "  [AVISO]    $t" -ForegroundColor Yellow }
function Problema($t) { [void]$Problemas.Add($t); Write-Host "  [PROBLEMA] $t" -ForegroundColor Red }
function Escrever($caminho, $texto) {
    $texto = $texto -replace "`r`n", "`n"            # o F-Droid exige finais de linha LF
    [System.IO.File]::WriteAllText($caminho, $texto, $Utf8SemBom)
}
function Invoke-Git { & git -C $Projeto @args 2>$null }

Write-Host ""
Write-Host "Preparar o Painel Financeiro para o F-Droid" -ForegroundColor Cyan
Write-Host "Projeto: $Projeto"
Write-Host ""

# ── 1. Ler o build.gradle da app ─────────────────────────────────────────────
$Projeto = (Resolve-Path $Projeto).Path
$gradleApp = $null
foreach ($nome in @("build.gradle.kts", "build.gradle")) {
    $c = Join-Path (Join-Path $Projeto "app") $nome
    if (Test-Path $c) { $gradleApp = $c; break }
}
if (-not $gradleApp) { throw "Não encontrei app\build.gradle nem app\build.gradle.kts em '$Projeto'." }
$g = Get-Content $gradleApp -Raw -Encoding UTF8

$appId = $null
if ($g -match 'applicationId\s*=?\s*["'']([^"'']+)["'']') { $appId = $Matches[1] }
elseif ($g -match 'namespace\s*=?\s*["'']([^"'']+)["'']') { $appId = $Matches[1] }
$versionCode = $null
if ($g -match 'versionCode\s*=?\s*(\d+)') { $versionCode = [int]$Matches[1] }
$versionName = $null
if ($g -match 'versionName\s*=?\s*["'']([^"'']+)["'']') { $versionName = $Matches[1] }

Write-Host "1. Dados da app ($([System.IO.Path]::GetFileName($gradleApp)))" -ForegroundColor Cyan
if ($appId) { Ok "applicationId: $appId" } else { Problema "applicationId não encontrado no $gradleApp" }
if ($versionCode) { Ok "versionCode: $versionCode" } else { Problema "versionCode não encontrado (é obrigatório)" }
if ($versionName) { Ok "versionName: $versionName" } else { Problema "versionName não encontrado (é obrigatório)" }

# ── 2. Requisitos do F-Droid ─────────────────────────────────────────────────
Write-Host ""
Write-Host "2. Requisitos do F-Droid" -ForegroundColor Cyan
$ficheirosGradle = Get-ChildItem -Path $Projeto -Recurse -File -Include *.gradle, *.gradle.kts, libs.versions.toml |
    Where-Object { $_.FullName -notmatch '[\\/](build|\.gradle|\.idea)[\\/]' }
$proprietarios = @{
    'com\.google\.android\.gms|play-services' = "Google Play Services"
    'firebase'                                = "Firebase"
    'crashlytics'                             = "Crashlytics"
    'com\.google\.android\.play'              = "Google Play Core / atualizações da Play Store"
    'admob|google-mobile-ads'                 = "anúncios AdMob"
}
$encontrou = $false
foreach ($f in $ficheirosGradle) {
    $txt = Get-Content $f.FullName -Raw -Encoding UTF8
    foreach ($padrao in $proprietarios.Keys) {
        if ($txt -match $padrao) {
            Problema "$($proprietarios[$padrao]) em $($f.Name): o F-Droid não aceita componentes proprietários"
            $encontrou = $true
        }
    }
}
if (-not $encontrou) { Ok "sem Google Play Services, Firebase, Crashlytics nem anúncios" }

$binarios = Get-ChildItem -Path $Projeto -Recurse -File -Include *.jar, *.aar |
    Where-Object { $_.FullName -notmatch '[\\/](build|\.gradle|\.idea)[\\/]' -and $_.Name -ne "gradle-wrapper.jar" }
if ($binarios) {
    foreach ($b in $binarios) { Problema "biblioteca pré-compilada no repositório: $($b.FullName.Substring($Projeto.Length + 1))" }
} else { Ok "sem bibliotecas pré-compiladas (.jar/.aar) no repositório" }

if (Test-Path (Join-Path $Projeto "gradlew")) { Ok "Gradle wrapper presente (gradlew)" }
else { Problema "falta o gradlew na raiz do projeto" }

if ($g -match 'signingConfigs' -and $g -match 'storeFile') {
    Aviso "o build.gradle define uma chave de assinatura (storeFile). O F-Droid compila sem a tua chave: se a compilação falhar por falta do ficheiro, usa a assinatura só quando o ficheiro existir"
} else { Ok "sem assinatura fixa no build.gradle (o F-Droid assina com a chave dele)" }

$manifesto = Join-Path $Projeto "app/src/main/AndroidManifest.xml"
if ((Test-Path $manifesto) -and ((Get-Content $manifesto -Raw) -match 'android\.permission\.INTERNET')) {
    Aviso "a app pede acesso à Internet. Se usar um serviço externo não livre (ex.: cotações), o F-Droid marca-a com 'NonFreeNet'"
} else { Ok "a app não pede acesso à Internet" }

# ── 3. Repositório Git e GitHub ──────────────────────────────────────────────
Write-Host ""
Write-Host "3. Repositório Git" -ForegroundColor Cyan
$ehGit = (Invoke-Git rev-parse --is-inside-work-tree) -eq "true"
if (-not $ehGit) { Problema "a pasta não é um repositório Git. Publica o projeto no GitHub primeiro" }
else {
    $remoto = Invoke-Git remote get-url origin
    if ($remoto -and $remoto -match 'github\.com[:/]([^/]+)/([^/]+?)(\.git)?$') {
        if (-not $Utilizador) { $Utilizador = $Matches[1] }
        if (-not $Repositorio) { $Repositorio = $Matches[2] }
        Ok "GitHub: $Utilizador/$Repositorio"
    } elseif ($remoto) { Aviso "o remoto 'origin' não é do GitHub ($remoto): indica -Utilizador e -Repositorio" }
    else { Problema "o repositório não tem remoto 'origin'. Publica o projeto no GitHub primeiro" }
}
if (-not $Repositorio) { $Repositorio = Split-Path -Leaf $Projeto }
if (-not $Utilizador) { $Utilizador = "TEU_UTILIZADOR"; Aviso "utilizador do GitHub desconhecido: ficou TEU_UTILIZADOR no ficheiro de metadados" }
if (-not $Autor) { $Autor = $Utilizador }

# ── 4. Copiar licença, textos da loja e ícone ────────────────────────────────
Write-Host ""
Write-Host "4. Ficheiros para a raiz do projeto" -ForegroundColor Cyan
$origemProj = Join-Path $Aqui "1_projeto"
$licenca = Join-Path $Projeto "LICENSE"
if (Test-Path $licenca) { Ok "já existe LICENSE (mantido; confirma que é uma licença livre)" }
else { Copy-Item (Join-Path $origemProj "LICENSE") $licenca; Ok "LICENSE (GPL-3.0) copiado" }

$fastDestino = Join-Path $Projeto "fastlane"
if (Test-Path $fastDestino) { Aviso "já existe a pasta fastlane: não foi substituída" }
else {
    Copy-Item (Join-Path $origemProj "fastlane") $fastDestino -Recurse
    Ok "pasta fastlane copiada (textos em en-US e pt-PT)"
}
foreach ($loc in @("en-US", "pt-PT")) {
    $base = Join-Path $fastDestino "metadata/android/$loc"
    $ch1 = Join-Path $base "changelogs/1.txt"
    if ($versionCode -and $versionCode -ne 1 -and (Test-Path $ch1)) {
        Move-Item $ch1 (Join-Path $base "changelogs/$versionCode.txt") -Force
    }
    $icone = Join-Path $Projeto "app/src/main/ic_launcher-playstore.png"
    $iconeDest = Join-Path $base "images/icon.png"
    if ((Test-Path $icone) -and -not (Test-Path $iconeDest)) { Copy-Item $icone $iconeDest }
}
if ($versionCode -and $versionCode -ne 1) { Ok "notas da versão renomeadas para changelogs\$versionCode.txt" }
if (Test-Path (Join-Path $fastDestino "metadata/android/en-US/images/icon.png")) { Ok "ícone 512x512 copiado (ic_launcher-playstore.png)" }
else { Aviso "sem ícone 512x512: cria-o em File > New > Image Asset (gera app\src\main\ic_launcher-playstore.png) ou o F-Droid usa o ícone do APK" }
$capturas = Get-ChildItem (Join-Path $fastDestino "metadata/android/en-US/images/phoneScreenshots") -Filter *.png -ErrorAction SilentlyContinue
if ($capturas) { Ok "$($capturas.Count) captura(s) de ecrã" }
else { Aviso "faltam capturas de ecrã em fastlane\metadata\android\en-US\images\phoneScreenshots (1.png, 2.png...)" }

# ── 5. Commit, tag e hash ────────────────────────────────────────────────────
Write-Host ""
Write-Host "5. Versão e commit" -ForegroundColor Cyan
$tag = "v$versionName"
$hash = $null
if ($ehGit) {
    if ($Publicar) {
        Invoke-Git add LICENSE fastlane | Out-Null
        Invoke-Git commit -m "Metadados e licença para o F-Droid" | Out-Null
        if (-not (Invoke-Git tag --list $tag)) { Invoke-Git tag $tag | Out-Null; Ok "tag $tag criada" }
        Invoke-Git push origin HEAD | Out-Null
        Invoke-Git push origin $tag | Out-Null
        Ok "commit e tag enviados para o GitHub"
    }
    if (Invoke-Git tag --list $tag) {
        $hash = (Invoke-Git rev-list -n 1 $tag).Trim()
        Ok "tag $tag -> commit $hash"
        $pendentes = Invoke-Git status --porcelain -- LICENSE fastlane
        if ($pendentes) { Aviso "LICENSE ou fastlane ainda não estão no commit da tag: faz commit e cria a tag depois deles (ou usa -Publicar)" }
    } else {
        Aviso "ainda não existe a tag $tag. Volta a correr o script com -Publicar (ou faz commit, 'git tag $tag' e 'git push --tags')"
    }
}
if (-not $hash) { $hash = "HASH_COMPLETO_DO_COMMIT_DA_TAG" }

# ── 6. Ficheiro de metadados para o fdroiddata ───────────────────────────────
Write-Host ""
Write-Host "6. Metadados para o fdroiddata" -ForegroundColor Cyan
$modelo = Get-Content (Join-Path $Aqui "2_fdroiddata/modelo.yml") -Raw -Encoding UTF8
$yml = $modelo.Replace("{{UTILIZADOR}}", $Utilizador).Replace("{{REPOSITORIO}}", $Repositorio).Replace("{{AUTOR}}", $Autor)
# formato do "fdroid rewritemeta": aspas só quando o YAML leria a versão como número (ex.: 1.0)
$versaoYaml = "$versionName"
if ($versaoYaml -match '^\d+(\.\d+)?$') { $versaoYaml = "'$versaoYaml'" }
$yml = $yml.Replace("{{VERSION_NAME}}", $versaoYaml).Replace("{{VERSION_CODE}}", "$versionCode").Replace("{{COMMIT}}", $hash)
$destYml = Join-Path $Aqui "2_fdroiddata/metadata/$appId.yml"
Escrever $destYml $yml
Ok "criado: 2_fdroiddata\metadata\$appId.yml"

# ── Resumo ───────────────────────────────────────────────────────────────────
Write-Host ""
if ($Problemas.Count -gt 0) {
    Write-Host "Há $($Problemas.Count) problema(s) que impedem a entrada no F-Droid. Corrige-os e volta a correr o script." -ForegroundColor Red
} elseif ($Avisos.Count -gt 0) {
    Write-Host "Pronto, com $($Avisos.Count) aviso(s) para rever. Próximo passo: secção 4 do LEIA-ME.md (submissão no GitLab)." -ForegroundColor Yellow
} else {
    Write-Host "Tudo pronto. Próximo passo: secção 4 do LEIA-ME.md (submissão no GitLab)." -ForegroundColor Green
}
