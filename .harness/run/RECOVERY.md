# RECOVERY

> Autonomous mode only (ENGINE.md §14.3, ADR-027). Every action this run took that `git revert` alone
> cannot undo, with what was captured first and the exact command that restores it. Entries are
> appended by the Recovery Wrappers in `.harness/loop/bin/`; the engine appends one by hand only for a
> database dump taken from a recipe in `.harness/knowledge/`. Nobody edits an entry after it is written.
>
> Captured copies live in `.harness/trash/`, which is excluded from git and never cleaned up
> automatically. Delete it yourself once you are satisfied nothing needs restoring.

---

## R-001 - deleted

- **When:** 2026-10-13T21:42:17.1148046+07:00
- **Target:** `C:\Users\MAC_PC\AppData\Local\Temp\tmp.Rr2lP8Y14u`
- **Captured:** moved to `D:\Valve\Personal\Kanso\.harness\trash\20261013-214217-080\tmp.Rr2lP8Y14u`
- **Restore:** `Copy-Item -LiteralPath 'D:\Valve\Personal\Kanso\.harness\trash\20261013-214217-080\tmp.Rr2lP8Y14u' -Destination 'C:\Users\MAC_PC\AppData\Local\Temp\tmp.Rr2lP8Y14u' -Recurse`

## R-002 - deleted

- **When:** 2026-10-13T21:42:17.6512761+07:00
- **Target:** `C:\Users\MAC_PC\AppData\Local\Temp\tmp.RcQsgWoHNJ`
- **Captured:** moved to `D:\Valve\Personal\Kanso\.harness\trash\20261013-214217-622\tmp.RcQsgWoHNJ`
- **Restore:** `Copy-Item -LiteralPath 'D:\Valve\Personal\Kanso\.harness\trash\20261013-214217-622\tmp.RcQsgWoHNJ' -Destination 'C:\Users\MAC_PC\AppData\Local\Temp\tmp.RcQsgWoHNJ' -Recurse`
