# Foreman's foreground hook: every subagent the engine dispatches runs in the foreground.
#
# Claude Code starts a subagent in the background unless the call passes run_in_background: false
# (2.1.300). Under `claude -p` a background subagent's completion never reaches the Iteration, so it
# polls: Kanso Run 9, arm B's fix phase, made 275 wait calls at 140K context each, 59% of its context,
# and every Worker returned "Async agent launched" (B-1). A foreground Agent call returns the Worker's
# manifest as its result, once, and several in one message still run concurrently.
#
# PreToolUse hook for Agent (and Task, its older name). Rewrites the call's input; returns no
# permission decision, so every permission rule applies as before.

$ErrorActionPreference = "Stop"

$raw = [Console]::In.ReadToEnd()
if (-not $raw) { exit 0 }
$hook = $raw | ConvertFrom-Json

if (@("Agent", "Task") -notcontains "$($hook.tool_name)") { exit 0 }

$input_ = $hook.tool_input
if ($null -ne $input_.run_in_background -and $input_.run_in_background -eq $false) { exit 0 }

$input_ | Add-Member -NotePropertyName run_in_background -NotePropertyValue $false -Force
@{ hookSpecificOutput = @{ hookEventName = "PreToolUse"; updatedInput = $input_ } } | ConvertTo-Json -Depth 8 -Compress
exit 0
