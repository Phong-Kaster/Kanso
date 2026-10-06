# AMENDMENTS

> Every plan mutation, newest first.

- Iteration 2 (Tier 1): D-001 consumed (DoD approved, baselines 6-9 listed). Iteration removed placeholder screenshot tests and pictures 7-9 in Phase 1 (approved); Camera tile keeps Placeholder until Phase 3. Fake PhotoSource moves to Phase 2 (T-004), not needed by T-003. Reviewer findings fixed in-iteration: DoD 31 comments, DoD 17/1 tests strengthened, debounce write scope.
- Bootstrap, after critique (Tier 1, before approval): split T-001 into T-001 (labels, Fast) and T-010 (picker alpha, Capable: new parameter on a module `core/common` also calls); T-007 now depends on T-003; Placeholder deletion moved to Phase 3; fakes and `BackArrowIcon` move owned by the Iteration; DoD 12, 9, 8, 6, 30, 37 sharpened.
- Iteration 3 (Tier 1): added `T/navigation/PhotoToToolNavigationTest.kt` (reviewer: DoD 3 needed the real nav host); Iteration fixed two reviewer findings in Worker files (PhotoLoader EXIF fallback, thumbnail cache key).
