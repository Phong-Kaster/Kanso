# Engine digest (Run 8, Iteration 3, javap of libs/localml.aar)

No network, no download: selfie segmentation + Real-ESRGAN (assets/realesrgan_64x64_float16.tflite, 4x, 64x64 windows) are bundled. SubjectSegmentationUC is the Play-services (downloading) path: do NOT use it. Bitmap-in/Bitmap-out; mask semantics of SelfieSegmentationUC.invoke(Bitmap, Boolean) are unverified (read segmentationToMask/ToImageMask names): the app must keep it behind an interface and treat the returned bitmap as the mask/foreground it documents in its own KDoc after a device check. OpenCV: call OpenCVLoader.initLocal() first. Look-up images for filters: AAR assets in imageprocessor.aar: assets/lookup/{lookup_origin,lookup_gray,lookup_light,lookup_skin,lookup_custom}.png; editimage.aar: assets/filter_materials/map/*.png (1977map, amaroMap, earlybirdMap, hefeMap, hudsonMap, inkwellMap, kelvinMap, lomoMap, nashvilleMap, riseMap, sierraMap ...). They merge into app assets and read with context.assets.

```
Compiled from "SourceFile"
public final class com.photo.editor.localml.enhance.EnhanceImageUC {
  public static final com.photo.editor.localml.enhance.EnhanceImageUC$Companion Companion;
  private static final java.lang.String TAG;
  private static final java.lang.String MODEL_PATH;
  private static final int WINDOW_SIZE_WIDTH;
  private static final int WINDOW_SIZE_HEIGHT;
  private static final int PAD_SIZE;
  private static final int UPSCALE_FACTOR;
  private final android.content.Context context;
  private org.tensorflow.lite.Interpreter interpreter;
  private kotlinx.coroutines.Job modelInitJob;
  private boolean released;
  public com.photo.editor.localml.enhance.EnhanceImageUC(android.content.Context);
  private final float[] upscaleImage(float[]);
  public static final float[] access$upscaleImage(com.photo.editor.localml.enhance.EnhanceImageUC, float[]);
  public static final android.content.Context access$getContext$p(com.photo.editor.localml.enhance.EnhanceImageUC);
  public static final void access$setInterpreter$p(com.photo.editor.localml.enhance.EnhanceImageUC, org.tensorflow.lite.Interpreter);
  static {};
  public final kotlinx.coroutines.flow.Flow<com.photo.editor.localml.enhance.ProgressStatus<android.graphics.Bitmap>> invoke(android.graphics.Bitmap);
  public final kotlinx.coroutines.Job initModel();
}
Compiled from "SourceFile"
public abstract class com.photo.editor.localml.enhance.ProgressStatus<T> {
  private com.photo.editor.localml.enhance.ProgressStatus();
  public com.photo.editor.localml.enhance.ProgressStatus(kotlin.jvm.internal.DefaultConstructorMarker);
}
Compiled from "SourceFile"
public final class com.photo.editor.localml.enhance.ProgressStatus$Processing<T> extends com.photo.editor.localml.enhance.ProgressStatus<T> {
  private final float progress;
  public com.photo.editor.localml.enhance.ProgressStatus$Processing(float);
  public static com.photo.editor.localml.enhance.ProgressStatus$Processing copy$default(com.photo.editor.localml.enhance.ProgressStatus$Processing, float, int, java.lang.Object);
  public final float getProgress();
  public final float component1();
  public final com.photo.editor.localml.enhance.ProgressStatus$Processing<T> copy(float);
  public java.lang.String toString();
  public int hashCode();
  public boolean equals(java.lang.Object);
}
Compiled from "SourceFile"
public final class com.photo.editor.localml.enhance.ProgressStatus$Success<T> extends com.photo.editor.localml.enhance.ProgressStatus<T> {
  private final T result;
  public com.photo.editor.localml.enhance.ProgressStatus$Success(T);
  public static com.photo.editor.localml.enhance.ProgressStatus$Success copy$default(com.photo.editor.localml.enhance.ProgressStatus$Success, java.lang.Object, int, java.lang.Object);
  public final T getResult();
  public final T component1();
  public final com.photo.editor.localml.enhance.ProgressStatus$Success<T> copy(T);
  public java.lang.String toString();
  public int hashCode();
  public boolean equals(java.lang.Object);
}
Compiled from "SourceFile"
public final class com.photo.editor.localml.enhance.ProgressStatus$Error<T> extends com.photo.editor.localml.enhance.ProgressStatus<T> {
  private final java.lang.String message;
  public com.photo.editor.localml.enhance.ProgressStatus$Error(java.lang.String);
  public static com.photo.editor.localml.enhance.ProgressStatus$Error copy$default(com.photo.editor.localml.enhance.ProgressStatus$Error, java.lang.String, int, java.lang.Object);
  public final java.lang.String getMessage();
  public final java.lang.String component1();
  public final com.photo.editor.localml.enhance.ProgressStatus$Error<T> copy(java.lang.String);
  public java.lang.String toString();
  public int hashCode();
  public boolean equals(java.lang.Object);
}
Compiled from "SourceFile"
public final class com.photo.editor.localml.removebackground.RemoveBackgroundUC {
  private final com.photo.editor.localml.removebackground.SubjectSegmentationUC subjectSegmentationUC;
  private final com.photo.editor.localml.removebackground.SelfieSegmentationUC selfieSegmentationUC;
  public com.photo.editor.localml.removebackground.RemoveBackgroundUC(com.photo.editor.localml.removebackground.SubjectSegmentationUC, com.photo.editor.localml.removebackground.SelfieSegmentationUC);
  public final java.lang.Object invoke(android.graphics.Bitmap, kotlin.coroutines.Continuation<? super android.graphics.Bitmap>);
}
Compiled from "SourceFile"
public final class com.photo.editor.localml.removebackground.SelfieSegmentationUC {
  public static final com.photo.editor.localml.removebackground.SelfieSegmentationUC$Companion Companion;
  private static final java.lang.String TAG;
  private com.google.mlkit.vision.segmentation.Segmenter segmenter;
  public com.photo.editor.localml.removebackground.SelfieSegmentationUC();
  public static java.lang.Object invoke$default(com.photo.editor.localml.removebackground.SelfieSegmentationUC, org.opencv.core.Mat, boolean, kotlin.coroutines.Continuation, int, java.lang.Object);
  public static java.lang.Object invoke$default(com.photo.editor.localml.removebackground.SelfieSegmentationUC, android.graphics.Bitmap, boolean, kotlin.coroutines.Continuation, int, java.lang.Object);
  private final android.graphics.Bitmap segmentationToMask(com.google.mlkit.vision.segmentation.SegmentationMask, android.graphics.Bitmap);
  private final android.graphics.Bitmap segmentationToImageMask(com.google.mlkit.vision.segmentation.SegmentationMask, android.graphics.Bitmap);
  public static final com.google.mlkit.vision.segmentation.Segmenter access$getSegmenter$p(com.photo.editor.localml.removebackground.SelfieSegmentationUC);
  static {};
  public final java.lang.Object invoke(org.opencv.core.Mat, boolean, kotlin.coroutines.Continuation<? super android.graphics.Bitmap>);
  public final java.lang.Object invoke(android.graphics.Bitmap, boolean, kotlin.coroutines.Continuation<? super android.graphics.Bitmap>);
}
Compiled from "SourceFile"
public final class com.photo.editor.localml.removebackground.RefineMaskUC {
  public com.photo.editor.localml.removebackground.RefineMaskUC();
  public final java.lang.Object invoke(org.opencv.core.Mat, android.graphics.Bitmap, kotlin.coroutines.Continuation<? super kotlin.Unit>);
}
Compiled from "SourceFile"
public final class com.photo.editor.localml.removebackground.ForegroundToMaskUC {
  public com.photo.editor.localml.removebackground.ForegroundToMaskUC();
  public final java.lang.Object invoke(org.opencv.core.Mat, kotlin.coroutines.Continuation<? super org.opencv.core.Mat>);
  public final java.lang.Object invoke(android.graphics.Bitmap, kotlin.coroutines.Continuation<? super org.opencv.core.Mat>);
}
```
