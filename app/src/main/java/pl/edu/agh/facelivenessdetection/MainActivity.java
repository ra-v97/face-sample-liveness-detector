package pl.edu.agh.facelivenessdetection;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.annotation.KeepName;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.mlkit.common.MlKitException;

import java.util.Optional;

import pl.edu.agh.facelivenessdetection.processing.AuthWithFaceLivenessDetectMethodType;
import pl.edu.agh.facelivenessdetection.preference.SharedPreferences;
import pl.edu.agh.facelivenessdetection.utils.PermissionManager;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;
import pl.edu.agh.facelivenessdetection.processing.VisionImageProcessor;
import pl.edu.agh.facelivenessdetection.model.CameraXViewModel;
import pl.edu.agh.facelivenessdetection.controller.FaceLivenessDetectionController;
import pl.edu.agh.facelivenessdetection.handler.StatusChangeHandler;
import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.preference.PreferenceUtils;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;

@KeepName
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements DetectionVisualizer {
    /*
     *  Constants definition
     */
    private static final String TAG = "LivePreviewMainActivity";

    /*
     * UI Elements
     */
    private RadioButton method1RadioButton;

    private RadioButton method2RadioButton;

    private TextView livenessStatusTextView;

    private PreviewView previewView;

    private GraphicOverlay graphicOverlay;
    /*
     * Required elements
     */
    private final FaceLivenessDetectionController faceLivenessDetectionController;

    private final BiMap<AuthWithFaceLivenessDetectMethodType, RadioButton> faceLivenessDetectorRadioButtonBiMap;

    private final StatusChangeHandler statusChangeHandler;

    @Nullable
    private ProcessCameraProvider cameraProvider;

    @Nullable
    private Preview previewUseCase;

    @Nullable
    private ImageAnalysis analysisUseCase;

    @Nullable
    private VisionImageProcessor imageProcessor;

    private boolean needUpdateGraphicOverlayImageSourceInfo;

    private CameraSelector cameraSelector;

    public MainActivity() {
        faceLivenessDetectionController = new FaceLivenessDetectionController();
        faceLivenessDetectorRadioButtonBiMap = HashBiMap.create();

        statusChangeHandler = new StatusChangeHandler(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createCameraSelector();

        setContentView(R.layout.activity_main);

        method1RadioButton = findViewById(R.id.radioButtonMethod1);
        method2RadioButton = findViewById(R.id.radioButtonMethod2);
        livenessStatusTextView = findViewById(R.id.statusView);

        previewView = findViewById(R.id.preview_view);
        if (previewView == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        initializeViewElements();
        setActiveFaceDetectionMethod(AuthWithFaceLivenessDetectMethodType.FACE_ACTIVITY_METHOD);
        setUpViewModelProvider();

        if (!PermissionManager.allPermissionsGranted(this)) {
            PermissionManager.getRuntimePermissions(this);
        }
    }

    private void createCameraSelector() {
        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(SharedPreferences.getCameraLeansFacing())
                .build();
    }

    private void initializeViewElements() {
        faceLivenessDetectorRadioButtonBiMap.put(AuthWithFaceLivenessDetectMethodType.FACE_ACTIVITY_METHOD,
                method1RadioButton);
        faceLivenessDetectorRadioButtonBiMap.put(AuthWithFaceLivenessDetectMethodType.FACE_FLASHING_METHOD,
                method2RadioButton);
    }

    private void setUpViewModelProvider() {
        new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()))
                .get(CameraXViewModel.class)
                .getProcessCameraProvider()
                .observe(this, provider -> {
                    cameraProvider = provider;
                    if (PermissionManager.allPermissionsGranted(this)) {
                        bindAllCameraUseCases();
                    }
                });
    }

    public void onDetectButtonClick(View view) {
        faceLivenessDetectionController.performFaceLivenessDetectionTrigger(this);
    }

    public void onToggleMethod1(View view) {
        refreshFaceDetectionMethod(method1RadioButton);
    }

    public void onToggleMethod2(View view) {
        refreshFaceDetectionMethod(method2RadioButton);
    }

    private void setActiveFaceDetectionMethod(AuthWithFaceLivenessDetectMethodType activeFaceDetectionMethod) {
        faceLivenessDetectionController
                .setFaceProcessorMethod(activeFaceDetectionMethod);
        Optional.ofNullable(faceLivenessDetectorRadioButtonBiMap.get(activeFaceDetectionMethod))
                .ifPresent(RadioButton::toggle);
    }

    private void refreshFaceDetectionMethod(AuthWithFaceLivenessDetectMethodType activeFaceDetectionMethod) {
        faceLivenessDetectionController.deactivateFaceLivenessDetector();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        setActiveFaceDetectionMethod(activeFaceDetectionMethod);
        bindAllCameraUseCases();

    }

    private void refreshFaceDetectionMethod(RadioButton methodRadioButton) {
        refreshFaceDetectionMethod(faceLivenessDetectorRadioButtonBiMap.inverse()
                .get(methodRadioButton));
        setDetectionStatus(LivenessDetectionStatus.UNKNOWN);
    }

    public void setDetectionStatus(LivenessDetectionStatus status) {
        livenessStatusTextView.setText(getStatusLabel(status));
        livenessStatusTextView.setBackgroundColor(status.getColor());
    }

    private String getStatusLabel(LivenessDetectionStatus status) {
        switch (status) {
            case FAKE:
                return getString(R.string.fake_status_label);
            case REAL:
                return getString(R.string.real_status_label);
            default:
                return getString(R.string.unknown_status_label);
        }
    }

    @Override
    public void visualizeStatus(LivenessDetectionStatus status) {
        final Message msg = statusChangeHandler.obtainMessage();
        final Bundle b = new Bundle();
        b.putString(StatusChangeHandler.STATUS_KEY, status.getKey());
        msg.setData(b);
        statusChangeHandler.sendMessage(msg);
    }

    @Override
    public void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        bindAllCameraUseCases();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        faceLivenessDetectionController.deactivateFaceLivenessDetector();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        faceLivenessDetectionController.deactivateFaceLivenessDetector();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (PermissionManager.allPermissionsGranted(this)) {
            bindAllCameraUseCases();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void bindAllCameraUseCases() {
        if (cameraProvider != null) {
            // As required by CameraX API, unbinds all use cases before trying to re-bind any of them.
            cameraProvider.unbindAll();
            bindPreviewUseCase();
            bindAnalysisUseCase();
        }
    }

    private void bindPreviewUseCase() {
        if (!PreferenceUtils.isCameraLiveViewportEnabled(this)) {
            return;
        }
        if (cameraProvider == null) {
            return;
        }
        if (previewUseCase != null) {
            cameraProvider.unbind(previewUseCase);
        }
        final Preview.Builder builder = new Preview.Builder();
        final Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this);
        if (targetResolution != null) {
            builder.setTargetResolution(targetResolution);
        }
        previewUseCase = builder.build();
        previewUseCase.setSurfaceProvider(previewView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
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


    private void bindAnalysisUseCase() {
        if (cameraProvider == null) {
            return;
        }
        if (analysisUseCase != null) {
            cameraProvider.unbind(analysisUseCase);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        faceLivenessDetectionController.obtainVisionProcessor(this).ifPresent(processor -> {
            imageProcessor = processor;
            final ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
            final Size targetResolution = PreferenceUtils.getCameraXTargetResolution(this);
            if (targetResolution != null) {
                builder.setTargetResolution(targetResolution);
            }
            if (isEmulator()) {
                builder.setTargetRotation(Surface.ROTATION_270);
            }

            analysisUseCase = builder.build();

            needUpdateGraphicOverlayImageSourceInfo = true;
            analysisUseCase.setAnalyzer(
                    // imageProcessor.processImageProxy will use another thread to run the detection underneath,
                    // thus we can just runs the analyzer itself on main thread.
                    ContextCompat.getMainExecutor(this),
                    imageProxy -> {
                        if (needUpdateGraphicOverlayImageSourceInfo) {
                            boolean isImageFlipped = SharedPreferences.getCameraLeansFacing() == CameraSelector.LENS_FACING_FRONT;
                            int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                            if (rotationDegrees == 0 || rotationDegrees == 180) {
                                graphicOverlay.setImageSourceInfo(
                                        imageProxy.getWidth(), imageProxy.getHeight(), isImageFlipped);
                            } else {
                                graphicOverlay.setImageSourceInfo(
                                        imageProxy.getHeight(), imageProxy.getWidth(), isImageFlipped);
                            }
                            needUpdateGraphicOverlayImageSourceInfo = false;
                        }
                        try {
                            imageProcessor.processImageProxy(imageProxy, graphicOverlay);
                        } catch (MlKitException e) {
                            Log.e(TAG, "Failed to process image. Error: " + e.getLocalizedMessage());
                            Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
            cameraProvider.bindToLifecycle(this, cameraSelector, analysisUseCase);
        });
    }
}