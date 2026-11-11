# foreman-android-guard.ps1 - a PreToolUse hook on Bash: the engine may run every Android CLI command
# except the ones that change what the whole machine shares, and of raw adb only what the android-cli
# skill itself tells an agent to run (ADR-048).
#
# Decided by the human, 2026-11-06: "whatever Android CLI lets an agent do, Foreman lets it do", minus
# three groups that reach past this repository - `android emulator stop|remove` (another project's
# emulator), `android sdk remove|update` (the SDK every project on the machine builds with) and
# `android update` (the binary itself, mid-run; /foreman updates it at launch instead). Raw adb stays
# closed except `adb devices` and `adb shell input ...`, the two forms the skill's own instructions use.
#
# Permission globs cannot say "adb, but only shell input": deny always wins over allow, and in
# Autonomous mode everything off the Deny List is allowed, so a deny list of the dangerous forms would
# always miss one. A regex over each command segment can, which is why this is a hook (the same
# reasoning as foreman-subagent-shell.ps1, ADR-044).
#
# What reaches the device's own shell matters as much as what reaches this one: `adb shell input text
# "a;reboot"` runs `reboot` on the device, because adb joins its arguments and the device's sh parses
# them. So an argument may carry ; & | < > ( ) or a backquote only quoted AND backslash-escaped, which
# is the one form that reaches the device still escaped.
#
# Exit 2 refuses the call; Claude Code hands stderr to the engine. Exit 0 with no output leaves the
# decision to the permission rules. Input this script cannot parse is refused.

$ErrorActionPreference = "Stop"

try {
    $raw = [Console]::In.ReadToEnd()
    $hook = $raw | ConvertFrom-Json
} catch {
    [Console]::Error.WriteLine("Refused by Foreman's Android guard: the call could not be read.")
    exit 2
}

if ("$($hook.tool_name)" -ne "Bash") { exit 0 }
$command = "$($hook.tool_input.command)"

function Refuse([string]$why) {
    [Console]::Error.WriteLine("Refused by Foreman's Android guard (ADR-048): $why")
    exit 2
}

# Split a command line into its segments at ; & | && || and newlines, outside quotes and outside $( ).
function Split-Segments([string]$line) {
    $segments = @(); $current = New-Object System.Text.StringBuilder
    $single = $false; $double = $false; $depth = 0
    for ($i = 0; $i -lt $line.Length; $i++) {
        $c = $line[$i]
        if ($c -eq '\' -and -not $single -and $i + 1 -lt $line.Length) {
            [void]$current.Append($c); [void]$current.Append($line[$i + 1]); $i++; continue
        }
        if ($c -eq "'" -and -not $double) { $single = -not $single }
        elseif ($c -eq '"' -and -not $single) { $double = -not $double }
        elseif (-not $single -and $c -eq '$' -and $i + 1 -lt $line.Length -and $line[$i + 1] -eq '(') { $depth++ }
        elseif (-not $single -and $c -eq ')' -and $depth -gt 0) { $depth-- }
        elseif (-not $single -and -not $double -and $depth -eq 0 -and ($c -eq ';' -or $c -eq '&' -or $c -eq '|' -or $c -eq "`n" -or $c -eq "`r")) {
            $segments += $current.ToString(); [void]$current.Clear(); continue
        }
        [void]$current.Append($c)
    }
    $segments += $current.ToString()
    return , @($segments | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

# Leading VAR=value assignments do not change which program runs.
function Strip-Assignments([string]$segment) {
    return ($segment -replace '^(?:[A-Za-z_][A-Za-z0-9_]*=(?:"[^"]*"|''[^'']*''|\S*)\s+)+', '')
}

function First-Word([string]$segment) {
    if ($segment -match '^\s*("[^"]*"|''[^'']*''|\S+)') { return $Matches[1].Trim('"', "'") }
    return ""
}

function Program-Name([string]$word) {
    return ($word -split '[\\/]')[-1].ToLowerInvariant()
}

# ---------- Android CLI ----------

function Test-AndroidSegment([string]$segment) {
    $tokens = @([regex]::Matches($segment, '"[^"]*"|''[^'']*''|\S+') | ForEach-Object { $_.Value.Trim('"', "'") })
    $words = @(); $skipNext = $false
    for ($i = 1; $i -lt $tokens.Count; $i++) {
        $t = $tokens[$i]
        if ($skipNext) { $skipNext = $false; continue }
        if ($t -eq '--sdk') { $skipNext = $true; continue }
        if ($t.StartsWith('-')) { continue }
        $words += $t.ToLowerInvariant()
        if ($words.Count -eq 2) { break }
    }
    $cmd = if ($words.Count -gt 0) { $words[0] } else { "" }
    $sub = if ($words.Count -gt 1) { $words[1] } else { "" }
    if ($cmd -eq 'update') {
        Refuse "``android update`` replaces the Android CLI binary every project on this machine uses. /foreman updates it once, at launch (ADR-047); a run never does."
    }
    if ($cmd -eq 'emulator' -and ($sub -eq 'stop' -or $sub -eq 'remove')) {
        Refuse "``android emulator $sub`` can end or delete an emulator another project's run is driving (2026-09-30). Leave a running emulator running; one you started for this run stops when the run ends."
    }
    if ($cmd -eq 'sdk' -and ($sub -eq 'remove' -or $sub -eq 'update')) {
        Refuse "``android sdk $sub`` changes the Android SDK every project on this machine builds with. Install what is missing with ``android sdk install``; never remove or update."
    }
}

# ---------- raw adb ----------

$serial = '[A-Za-z0-9._:-]+'
# One argument as it must look so the device's shell sees no operator: plain characters unquoted;
# inside quotes an operator only backslash-escaped.
$unquoted = '[^\s;&|<>`$"''()\\]+'
$doubleQ = '"(?:[^"`$\\;&|<>()\r\n]|\\[^\r\n])*"'
$singleQ = '''(?:[^''`\\;&|<>()\r\n]|\\[^\r\n])*'''
$arg = "(?:$unquoted|$doubleQ|$singleQ)"
$source = '(?:touchscreen|touchpad|keyboard|mouse|stylus|trackball|dpad|gamepad|joystick|touchnavigation|rotaryencoder)'
$verb = '(?:tap|swipe|text|keyevent|draganddrop|press|roll|motionevent|keycombination)'
# The skill's own shortcut: adb shell input $(android screen resolve --screenshot <png> --string "tap #3")
$resolve = '\$\(android screen resolve --screenshot (?:[^\s;&|<>`$"''()\\]+|"[^"`$;&|<>()\r\n]+") --string "[A-Za-z0-9 #_.,-]*"\)'
$adbPrefix = "^adb(?: -s $serial)?"
$adbAllowed = @(
    "$adbPrefix devices(?: -l)?$",
    "$adbPrefix shell input(?: $source)? $verb(?: $arg)*$",
    "$adbPrefix shell input $resolve$"
)

function Test-AdbSegment([string]$segment) {
    $normalised = ($segment -replace '\s+', ' ').Trim()
    foreach ($pattern in $adbAllowed) { if ($normalised -cmatch $pattern) { return } }
    Refuse "of adb the engine may run only ``adb [-s <serial>] devices`` and ``adb [-s <serial>] shell input <tap|swipe|text|keyevent|...> <args>``, each alone. Everything else on a device goes through ``android`` or .harness/loop/bin/foreman-device.ps1 (install, clear, grant, revoke)."
}

# ---------- every segment, and what a shell wrapper hides ----------

function Test-Line([string]$line, [int]$level) {
    if ($level -gt 3) { Refuse "the command nests shells too deeply to read." }
    # A substitution's output becomes a command too: $(adb ...) or `adb ...` is refused outright, and
    # $(android ...) is held to the same three exclusions as a plain call.
    if ($line -match '(?:\$\(|`)\s*(?:\S*[\\/])?adb(?:\.exe)?(?:\s|\)|`|$)') { Refuse "adb inside a substitution is not run." }
    foreach ($m in [regex]::Matches($line, '\$\(\s*((?:\S*[\\/])?android(?:\.exe|\.cmd|\.bat)?\s[^)]*)\)')) {
        Test-AndroidSegment $m.Groups[1].Value
    }
    foreach ($segment in (Split-Segments $line)) {
        $s = Strip-Assignments $segment
        $program = Program-Name (First-Word $s)
        switch -Regex ($program) {
            '^adb$' { Test-AdbSegment $s; continue }
            '^adb\.exe$' { Refuse "adb is run by its plain name only."; continue }
            '^android(\.exe|\.cmd|\.bat)?$' { Test-AndroidSegment $s; continue }
            '^(bash|sh|bash\.exe|sh\.exe)$' {
                if ($s -match '\s-c\s+(?:"((?:[^"\\]|\\.)*)"|''([^'']*)''|(.+))$') { Test-Line ($Matches[1] + $Matches[2] + $Matches[3]) ($level + 1) }
                continue
            }
            '^(cmd|cmd\.exe)$' {
                if ($s -match '\s/[cCkK]\s+(.+)$') { Test-Line ($Matches[1].Trim('"')) ($level + 1) }
                continue
            }
            '^(powershell|pwsh|powershell\.exe|pwsh\.exe)$' {
                if ($s -match '\s-(?:Command|c)\s+(.+)$') { Test-Line ($Matches[1].Trim('"', "'")) ($level + 1) }
                continue
            }
        }
    }
}

Test-Line $command 0
exit 0
