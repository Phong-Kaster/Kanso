# foreman-compile.ps1 - the one command a Worker may run: compile its module's code and unit tests (ADR-044).
#
#   powershell -NoProfile -File .harness/loop/bin/foreman-compile.ps1 [-Module app] [-Variant Debug]
#
# It compiles and runs nothing: no test, no app, no git. The Iteration still builds and tests the
# combined tree once (ENGINE.md 6.7); this only lets a Worker see a compile error in seconds and fix it
# itself, instead of the Iteration reverting its work and re-dispatching it at the Capable tier.
#
# Earned by the Kanso A/B run, arm C, 2026-10-20: both Fast Workers that failed Phase 2 failed on what a
# compiler would have said at once - T-004's test lacked `import androidx.compose.ui.semantics.getOrNull`,
# and T-003's test compared 120 px with 48 dp. Decided by the human the same day: a compile check, and
# only that.
#
# Other Workers may be editing other files at the same moment: an error in a file outside your Declared
# File Scope is theirs, not yours. Exit codes: the compiler's own (0 passed), 2 a malformed argument,
# 4 no compile check exists for this repository.

param(
    [string]$Module = "app",
    [string]$Variant = "Debug"
)

$ErrorActionPreference = "Stop"

# Plain names only: these become one Gradle task path, never a command line of the caller's choosing.
if ($Module -notmatch '^[A-Za-z0-9_-]+(:[A-Za-z0-9_-]+)*$') { [Console]::Error.WriteLine("foreman-compile: -Module must be a plain module path such as app or core:imagecrop."); exit 2 }
if ($Variant -notmatch '^[A-Za-z0-9]+$') { [Console]::Error.WriteLine("foreman-compile: -Variant must be a plain variant name such as Debug."); exit 2 }

# The repository is the one this script lives in (.harness/loop/bin/), never the shell's working directory:
# Workers dispatched in parallel had their shell start in .harness/, where the Iteration's stood, and the
# relative path failed with exit 127 (Kanso A/B, arm D, 2026-10-21).
$repoRoot = Split-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) -Parent
Set-Location -LiteralPath $repoRoot
$gradlew = Join-Path $repoRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Output "foreman-compile: no compile check for this repository (no gradlew.bat at its root). Report that you could not compile."
    exit 4
}

# compile<Variant>UnitTestKotlin compiles the module's own Kotlin first, then its unit tests.
$task = ":" + $Module + ":compile" + $Variant + "UnitTestKotlin"
$old = $ErrorActionPreference; $ErrorActionPreference = "Continue"
try { $out = & $gradlew $task "--console=plain" "-q" 2>&1 } catch { $out = @("$_") }
$code = $LASTEXITCODE
$ErrorActionPreference = $old

# Only what a Worker acts on: the errors, and Gradle's own verdict. Warnings would cost context and change nothing.
$keep = @($out | ForEach-Object { "$_" } | Where-Object { $_ -match '^(e: |.*error:|\* What went wrong|BUILD (SUCCESSFUL|FAILED))' })
$keep | Select-Object -First 40 | ForEach-Object { Write-Output $_ }
if ($keep.Count -gt 40) { Write-Output "... $($keep.Count - 40) more lines; fix the first errors and compile again." }

Write-Output "task: $task"
if ($code -eq 0) { Write-Output "foreman-compile: compile check passed." }
else { Write-Output "foreman-compile: compile check FAILED (exit $code). Fix the errors in your own files and compile again." }
exit $code
