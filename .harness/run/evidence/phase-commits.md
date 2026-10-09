# Phase commits

Base: `76ecb97` (Run 5 starts from it; the bootstrap checkpoint is `a1e76b5`).

| Phase | Task | Commit subject | SHA |
|---|---|---|---|
| 1 | T-001 | `loop(phase-1): ...` | `d816bde17db519bfc545f268ef75966368ebaed1` |
| 2 | T-002 | `loop(phase-2): ...` | (the commit that holds this file; the next Iteration writes its SHA here, `git log --format=%H --grep '^loop(phase-2)' 76ecb97..HEAD`) |
| 3 | T-003 | `loop(phase-3): ...` | - (the Verifier adds it: a commit cannot name itself) |
