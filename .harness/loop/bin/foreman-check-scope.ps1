# foreman-check-scope.ps1 - the three scope checks of ENGINE.md 6.6, answered by a script.
#
#   powershell -NoProfile -File .harness/loop/bin/foreman-check-scope.ps1 -Tasks T-001,T-002 [-Wiring <file>,<file>]
#
# Reads each task's "Declared File Scope" line from .harness/run/TASKS/<id>.md (the paths in backticks;
# a trailing / or a * names a folder or a pattern) and the working tree against HEAD, and checks:
#   1. no two tasks of the Phase declare the same file;
#   2. every changed file lies in exactly one task's scope, or in the Wiring Worker's shared files;
#   3. so the union of the scopes covers git status - nothing changed outside them.
# .harness/ is the Iteration's own and is never a Worker's change; nor are the agent definitions the
# Runtime publishes into .claude/agents/.
#
# Exit 0: the Phase's work may be trusted. Exit 3: a violation, each one printed; ENGINE.md 6.6 says
# what to do. A task whose scope line names no path in backticks is a violation too: a scope that
# cannot be read cannot be checked, and a check that cannot run must not pass.

param(
    [Parameter(Mandatory = $true)][string]$Tasks,
    [string]$Wiring = ""
)

$ErrorActionPreference = "Stop"
$problems = New-Object System.Collections.Generic.List[string]

function Test-InScope([string]$file, [string[]]$scope) {
    foreach ($s in $scope) {
        if ($s.EndsWith('/')) { if ($file.StartsWith($s, [StringComparison]::OrdinalIgnoreCase)) { return $true } }
        elseif ($s.Contains('*')) { if ($file -like $s) { return $true } }
        elseif ($file -ieq $s) { return $true }
    }
    return $false
}

$scopes = [ordered]@{}
foreach ($id in @($Tasks -split '[,\s]+' | Where-Object { $_ })) {
    $taskFile = Join-Path ".harness/run/TASKS" "$id.md"
    if (-not (Test-Path $taskFile)) { $problems.Add("$id has no task file at $taskFile"); continue }
    # The field is one line, or that line followed by indented lines - bootstrap writes a list, one path
    # per line (Kanso A/B, arm C, 2026-10-20). It ends at the next unindented line.
    $field = New-Object System.Collections.Generic.List[string]
    $inField = $false
    foreach ($l in @(Get-Content $taskFile)) {
        if (-not $inField) { if ($l -match 'Declared File Scope') { $inField = $true; $field.Add($l) }; continue }
        if ($l -match '^\s+\S') { $field.Add($l) } else { break }
    }
    $paths = @()
    if ($field.Count) { $paths = @([regex]::Matches(($field -join "`n"), '`([^`]+)`') | ForEach-Object { ($_.Groups[1].Value.Trim() -replace '\\', '/') -replace '^\./', '' } | Where-Object { $_ -match '[/.*]' }) }
    if ($paths.Count -eq 0) { $problems.Add("$id`: its Declared File Scope names no path in backticks, so it cannot be checked"); continue }
    $scopes[$id] = $paths
}
$shared = @($Wiring -split '[,\s]+' | Where-Object { $_ } | ForEach-Object { ($_ -replace '\\', '/') -replace '^\./', '' })

# 1. Disjoint: an exact path declared by two tasks.
$ids = @($scopes.Keys)
for ($i = 0; $i -lt $ids.Count; $i++) {
    for ($j = $i + 1; $j -lt $ids.Count; $j++) {
        foreach ($p in $scopes[$ids[$i]]) {
            if (Test-InScope $p $scopes[$ids[$j]]) { $problems.Add("$p is declared by both $($ids[$i]) and $($ids[$j])") }
        }
    }
}

# The Runtime republishes .harness/loop/agents/ into .claude/agents/ before every Iteration, so those
# files can show as changed in a consumer whose committed copy is older. They are its, not a Worker's.
$runtimeWritten = @()
if (Test-Path ".harness/loop/agents") {
    $runtimeWritten = @(Get-ChildItem ".harness/loop/agents" -Filter "*.md" -File | ForEach-Object { (".claude/agents/" + $_.Name).ToLowerInvariant() })
}

# 2 and 3. Every change since the checkpoint, tracked or new, against the scopes.
$old = $ErrorActionPreference; $ErrorActionPreference = "Continue"
try { $status = & git -c core.quotepath=off status --porcelain=v1 -uall 2>$null } catch { $status = $null }
$gitExit = $LASTEXITCODE
$ErrorActionPreference = $old
if ($gitExit -ne 0) { $problems.Add("git status failed (exit $gitExit), so the working tree could not be checked") }
foreach ($entry in @($status)) {
    if (-not $entry -or $entry.Length -lt 4) { continue }
    $file = $entry.Substring(3)
    if ($file.Contains(' -> ')) { $file = $file.Split(@(' -> '), [StringSplitOptions]::None)[1] }
    $file = $file.Trim('"') -replace '\\', '/'
    if ($file.StartsWith('.harness/')) { continue }
    if ($runtimeWritten -contains $file.ToLowerInvariant()) { continue }
    $owners = @($ids | Where-Object { Test-InScope $file $scopes[$_] })
    if (Test-InScope $file $shared) { $owners += "wiring" }
    if ($owners.Count -eq 0) { $problems.Add("$file changed, but is in no task's Declared File Scope and is not a Wiring file") }
    elseif ($owners.Count -gt 1) { $problems.Add("$file changed, and is in the scope of $($owners -join ' and ')") }
}

if ($problems.Count -eq 0) {
    Write-Output "Scope check passed: every change is inside exactly one scope ($($ids -join ', ')$(if ($shared.Count) { ', wiring' }))."
    exit 0
}
Write-Output "Scope check FAILED ($($problems.Count)):"
foreach ($p in $problems) { Write-Output "- $p" }
exit 3
