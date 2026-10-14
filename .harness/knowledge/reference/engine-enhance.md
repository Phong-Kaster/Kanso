# Engine: Enhance (Run 8, T-004)

- Class: `com.photo.editor.localml.enhance.EnhanceImageUC(context: Context)` from `core/localml`
  (the module `api`-exposes `libs/localml.aar`, so the app sees it). Model bundled:
  `assets/realesrgan_64x64_float16.tflite`, 4x upscale, 64x64 windows. No download.
- `initModel(): Job` loads the TFLite interpreter in the background. Retica calls it once in the
  ViewModel's `init` before `invoke`. Kanso's `EngineUpscaler` calls `initModel().join()` before
  every `invoke` (one engine object per run, made by a factory lambda). Whether `invoke` itself
  waits for the init job is unverified (no source); joining is the safe order.
- `invoke(bitmap): Flow<ProgressStatus<Bitmap>>`. The engine's `ProgressStatus` (in the AAR, not
  Retica's app copy) has three subclasses per javap: `Processing(progress: Float)` (Retica treats it
  as 0..1 and sets 1f itself at the end), `Success(result: T)`, `Error(message: String)` (a
  String, no Throwable). Retica's own app-side `ProgressStatus` also has `None`; the engine's does
  not appear to. Kanso keeps an `else -> false` branch in the `when` in case.
- Kanso collects with `flow.first { }` so collection stops at the first `Success`/`Error`
  (Retica cancels its own job instead). `Error` -> `IllegalStateException`, which
  `EnhanceProcessor` maps to `EditFailure.ModelUnavailable`; `OutOfMemoryError` -> `OutOfMemory`;
  `LinkageError` (native library / class init failure) -> `ModelUnavailable`.
- No public release/close method in the javap (a private `released` flag exists), so an engine
  object is simply dropped after its run.
- Memory: input is capped at 512 px long edge (`WorkingSize.ENHANCE`) inside `EnhanceProcessor`
  too, so output is at most 2048 px (16 MB ARGB). Never hand it an unscaled photo.
- Unverified on device: progress range, the thread the flow emits on (the processor wraps
  everything in `withContext(Dispatchers.Default)`), and how long a 512 px input takes on the
  32-bit Galaxy A13.
