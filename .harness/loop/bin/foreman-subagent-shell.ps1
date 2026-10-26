# foreman-subagent-shell.ps1 - a PreToolUse hook on Bash: a subagent may run the compile check and
# nothing else (ADR-044).
#
# A Worker holds no git and no test capability; since 2026-10-20 it holds one compile check,
# bin/foreman-compile.ps1, so it can fix its own compile errors before the Iteration builds. Giving it the
# Bash tool for that would otherwise give it every command its permission rules allow - in Autonomous
# mode, everything off the Deny List. So every Bash call a subagent makes passes through here, and only
# the exact command the Brief gives is let through: no other command, no second command chained to it,
# no argument that is not a plain name. The main engine's own calls name no agent and pass untouched.
#
# Exit 2 refuses the call; Claude Code hands stderr to the subagent. Input this script cannot parse is
# refused too: unlike the read guard, a missed refusal here would hand a Worker the shell.

$ErrorActionPreference = "Stop"

try {
    $raw = [Console]::In.ReadToEnd()
    $hook = $raw | ConvertFrom-Json
} catch {
    [Console]::Error.WriteLine("Refused by Foreman's subagent shell guard: the call could not be read.")
    exit 2
}

if (-not "$($hook.agent_type)") { exit 0 }
if ("$($hook.tool_name)" -ne "Bash") { exit 0 }

$command = "$($hook.tool_input.command)".Trim()
# The script may be named relatively or absolutely - a parallel Worker's shell can start outside the
# repository root (Kanso A/B, arm D, 2026-10-21) - but the path must end in .harness/loop/bin/foreman-compile.ps1
# and hold no character that could end the command and start another. A quoted path may hold spaces.
$path = '(?:"[^"`$;&|<>]*[\\/]\.harness[\\/]loop[\\/]bin[\\/]foreman-compile\.ps1"|(?:[^\s"''`$;&|<>]*[\\/])?\.harness[\\/]loop[\\/]bin[\\/]foreman-compile\.ps1)'
$allowed = '^powershell(\.exe)? -NoProfile( -ExecutionPolicy Bypass)? -File ' + $path + '( -Module [A-Za-z0-9_:-]+)?( -Variant [A-Za-z0-9]+)?$'
if ($command -cmatch $allowed) { exit 0 }

$own = ((Join-Path (Split-Path (Split-Path (Split-Path $PSScriptRoot -Parent) -Parent) -Parent) ".harness/loop/bin/foreman-compile.ps1") -replace '\\', '/')
[Console]::Error.WriteLine("Refused by Foreman's subagent shell guard (ADR-044): a Worker runs one command, alone and with nothing chained to it: ``powershell -NoProfile -File $own -Module <module>``. It has no git and no test; the Iteration builds and tests the combined tree.")
exit 2
