package pl.edu.agh.facelivenessdetection.controller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.ZoomState;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import pl.edu.agh.facelivenessdetection.MainActivity;
import pl.edu.agh.facelivenessdetection.preference.PreferenceUtils;
import pl.edu.agh.facelivenessdetection.preference.SharedPreferences;
import pl.edu.agh.facelivenessdetection.processing.AuthWithFaceLivenessDetectMethodType;
import pl.edu.agh.facelivenessdetection.processing.DummyFaceDetectionProcessor;
import pl.edu.agh.facelivenessdetection.processing.FaceLivenessDetector;
import pl.edu.agh.facelivenessdetection.processing.liveness.activity.FaceActivityLivenessDetector;
import pl.edu.agh.facelivenessdetection.processing.liveness.flashing.FaceFlashingLivenessDetector;
import pl.edu.agh.facelivenessdetection.processing.vision.BaseImageAnalyzer;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;

public class CameraManager {

    public static final String TAG = "CameraManager";

    // Change this value if camera preview is rotated.
    private static final int CAMERA_ANALYZER_ROTATION = Surface.ROTATION_90;

    private static final int DEFAULT_ANALYZER_ROTATION = Surface.ROTATION_0;

    private static final float CAMERA_PREVIEW_ROTATION = 270f;

    private static final float DEFAULT_PREVIEW_ROTATION = 0f;

    private final Context context;
    private final PreviewView previewView;
    private final LifecycleOwner lifecycleOwner;
    private final GraphicOverlay graphicOverlay;

    @Nullable
    private Preview preview;

    @Nullable
    private Camera camera;

    @Nullable
    private ProcessCameraProvider cameraProvider;

    @Nullable
    private ImageAnalysis imageAnalyzer;

    @Nullable
    private DisplayMetrics displayMetrics;

    @Nullable
    private ExecutorService cameraExecutor;

    @Nullable
    private ImageCapture imageCapture;

    private final int cameraSelectorOption = SharedPreferences.getCameraLeansFacing();

    // default analyzer FACE_ACTIVITY_METHOD
    private AuthWithFaceLivenessDetectMethodType activeAnalyzerType = AuthWithFaceLivenessDetectMethodType.FACE_ACTIVITY_METHOD;

    @Nullable
    private BaseImageAnalyzer<?> activeLivenessAnalyzer;

    public CameraManager(Context context, PreviewView previewView, LifecycleOwner lifecycleOwner, GraphicOverlay graphicOverlay) {
        this.context = context;
        this.previewView = previewView;
        this.lifecycleOwner = lifecycleOwner;
        this.graphicOverlay = graphicOverlay;

        createCameraExecutor();
    }

    private void createCameraExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    public Optional<ImageAnalysis.Analyzer> resolveAnalyzer() {
        if (activeAnalyzerType == null) {
            return Optional.empty();
        }
        try {
            switch (activeAnalyzerType) {
                case FACE_ACTIVITY_METHOD:
                    Log.i(TAG, "Using FaceActivityLivenessDetector");
//                    final FaceActivityLivenessDetector faceActivityLivenessDetector =
//                            new FaceActivityLivenessDetector(activity,
//                                    PreferenceUtils.getFaceDetectorOptionsForLivePreview(activity));
                    activeLivenessAnalyzer = new DummyFaceDetectionProcessor(graphicOverlay, isHorizontalMode());
                    return Optional.of(activeLivenessAnalyzer);

                case FACE_FLASHING_METHOD:
                    Log.i(TAG, "Using FaceFlashingLivenessDetector");
                    activeLivenessAnalyzer = new FaceFlashingLivenessDetector(graphicOverlay,
                            isHorizontalMode(),
                            PreferenceUtils.getFaceDetectorOptionsForLivePreview(context));
                    return Optional.of(activeLivenessAnalyzer);
                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + activeAnalyzerType, e);
            return Optional.empty();
        }
    }

    private void setCameraConfig(ProcessCameraProvider cameraProvider, CameraSelector cameraSelector) {
        try {
            camera = Optional.ofNullable(cameraProvider)
                    .map(processCameraProvider -> {
                        processCameraProvider.unbindAll();
                        return processCameraProvider;
                    })
                    .map(processCameraProvider -> processCameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalyzer))
                    .orElseThrow(() -> new RuntimeException("Cannot obtain camera"));

            Optional.ofNullable(preview)
                    .ifPresent(p -> p.setSurfaceProvider(previewView.getSurfaceProvider()));
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpPinchToZoom() {
        final ScaleGestureDetector.SimpleOnScaleGestureListener listener =
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        float currentZoomRatio = Optional.ofNullable(camera)
                                .map(Camera::getCameraInfo)
                                .map(CameraInfo::getZoomState)
                                .map(LiveData::getValue)
                                .map(ZoomState::getZoomRatio)
                                .orElse(1f);
                        float delta = detector.getScaleFactor();
                        Optional.ofNullable(camera)
                                .map(Camera::getCameraControl)
                                .ifPresent(cameraControl ->
                                        cameraControl.setZoomRatio(currentZoomRatio * delta));
                        return true;
                    }
                };
        final ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(context, listener);
        previewView.setOnTouchListener((v, event) -> {
            previewView.post(() -> {
                scaleGestureDetector.onTouchEvent(event);
            });
            return true;
        });
    }

    public void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(context);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                preview = new Preview.Builder().build();

                imageAnalyzer = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetRotation(isEmulator() ? CAMERA_ANALYZER_ROTATION : DEFAULT_ANALYZER_ROTATION)
                        .build();

                previewView.setRotation(isEmulator() ? CAMERA_PREVIEW_ROTATION : DEFAULT_PREVIEW_ROTATION);

                resolveAnalyzer()
                        .ifPresent(analyzer -> imageAnalyzer.setAnalyzer(Objects.requireNonNull(cameraExecutor), analyzer));

                final CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraSelectorOption)
                        .build();

                displayMetrics = new DisplayMetrics();
                previewView.getDisplay().getRealMetrics(displayMetrics);

                imageCapture = new ImageCapture.Builder()
                        .setTargetResolution(new Size(displayMetrics.widthPixels, displayMetrics.heightPixels))
                        .build();

                setUpPinchToZoom();
                setCameraConfig(cameraProvider, cameraSelector);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Exception thrown while camera starting: $e");
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void stopCamera() {
        if (activeLivenessAnalyzer != null) {
            activeLivenessAnalyzer.stop();
            activeLivenessAnalyzer = null;
        }
    }

    private boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MODEL.contains("sdk_gphone")
                || "google_sdk".equals(Build.PRODUCT);
    }

    public void changeAnalyzer(AuthWithFaceLivenessDetectMethodType analyzerType) {
        if (analyzerType != activeAnalyzerType) {
            Optional.ofNullable(cameraProvider)
                    .ifPresent(ProcessCameraProvider::unbindAll);
            if (activeLivenessAnalyzer != null) {
                activeLivenessAnalyzer.stop();
            }
            activeAnalyzerType = analyzerType;
            startCamera();
        }
    }

    public boolean isHorizontalMode() {
        Objects.requireNonNull(imageAnalyzer);
        return imageAnalyzer.getTargetRotation() == Surface.ROTATION_90
                || imageAnalyzer.getTargetRotation() == Surface.ROTATION_270;
    }

    public boolean isFrontMode() {
        return cameraSelectorOption == CameraSelector.LENS_FACING_FRONT;
    }

    public void performFaceLivenessDetectionTrigger(DetectionVisualizer visualizer) {
        Optional.ofNullable(activeLivenessAnalyzer)
                .ifPresent(faceLivenessDetector ->
                        faceLivenessDetector.livenessDetectionTrigger(visualizer));
    }

    public AuthWithFaceLivenessDetectMethodType getActiveAnalyzerType() {
        return activeAnalyzerType;
    }
}
