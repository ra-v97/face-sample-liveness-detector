package pl.edu.agh.facelivenessdetection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pl.edu.agh.facelivenessdetection.R;

import pl.edu.agh.facelivenessdetection.camera.CameraSource;
import pl.edu.agh.facelivenessdetection.camera.CameraSourcePreview;
import pl.edu.agh.facelivenessdetection.camera.GraphicOverlay;
import pl.edu.agh.facelivenessdetection.controller.FaceLivenessDetectionController;
import pl.edu.agh.facelivenessdetection.detector.FaceLivenessDetectorType;
import pl.edu.agh.facelivenessdetection.detector.face.FaceDetectorProcessor;
import pl.edu.agh.facelivenessdetection.handler.StatusChangeHandler;
import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.preference.PreferenceUtils;
import pl.edu.agh.facelivenessdetection.visuals.DetectionVisualizer;

public class MainActivity extends AppCompatActivity implements DetectionVisualizer {
    /*
     *  Constants definition
     */
    private static final String TAG = "LivePreviewMainActivity";

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;

    private static final String FACE_DETECTION = "Face Detection";

    /*
     * UI Elements
     */
    private RadioButton method1RadioButton;

    private RadioButton method2RadioButton;

    private TextView livenessStatusTextView;

    /*
     * Required elements
     */
    private final FaceLivenessDetectionController faceLivenessDetectionController;

    private final BiMap<FaceLivenessDetectorType, RadioButton> faceLivenessDetectorRadioButtonBiMap;

    private final StatusChangeHandler statusChangeHandler;

    private CameraSource cameraSource = null;

    private CameraSourcePreview preview;

    private GraphicOverlay graphicOverlay;

    public MainActivity() {
        faceLivenessDetectionController = new FaceLivenessDetectionController();
        faceLivenessDetectorRadioButtonBiMap = HashBiMap.create();

        statusChangeHandler = new StatusChangeHandler(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        method1RadioButton = findViewById(R.id.radioButtonMethod1);
        method2RadioButton = findViewById(R.id.radioButtonMethod2);

        preview = findViewById(R.id.preview_view);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        livenessStatusTextView = findViewById(R.id.statusView);

        initializeViewElements();
        setActiveFaceDetectionMethod(FaceLivenessDetectorType.FACE_ACTIVITY_METHOD);

        if (allPermissionsGranted()) {
            createCameraSource(FACE_DETECTION);
        } else {
            getRuntimePermissions();
        }
    }

    private void initializeViewElements() {
        faceLivenessDetectorRadioButtonBiMap.put(FaceLivenessDetectorType.FACE_ACTIVITY_METHOD,
                method1RadioButton);
        faceLivenessDetectorRadioButtonBiMap.put(FaceLivenessDetectorType.FACE_FLASHING_METHOD,
                method2RadioButton);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (allPermissionsGranted()) {
            createCameraSource(FACE_DETECTION);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private String[] getRequiredPermissions() {
        try {
            PackageInfo info =
                    this.getPackageManager()
                            .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }
    private static boolean isPermissionGranted(Context context, String permission) {
        if (ContextCompat.checkSelfPermission(context, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                return false;
            }
        }
        return true;
    }

    private void getRuntimePermissions() {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (!isPermissionGranted(this, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toArray(new String[0]), REQUEST_CAMERA_PERMISSION_CODE);
        }
    }

    public void onDetectButtonClick(View view) {
        faceLivenessDetectionController.performFaceLivenessVerification();
    }

    public void onToggleMethod1(View view) {
        setActiveFaceDetectionMethod(method1RadioButton);
    }

    public void onToggleMethod2(View view) {
        setActiveFaceDetectionMethod(method2RadioButton);
    }

    private void setActiveFaceDetectionMethod(FaceLivenessDetectorType activeFaceDetectionMethod) {
        faceLivenessDetectionController
                .activateFaceLivenessDetector(activeFaceDetectionMethod.getDetector(), this);
        Optional.ofNullable(faceLivenessDetectorRadioButtonBiMap.get(activeFaceDetectionMethod))
                .ifPresent(RadioButton::toggle);
    }

    private void setActiveFaceDetectionMethod(RadioButton methodRadioButton) {
        Optional.ofNullable(faceLivenessDetectorRadioButtonBiMap.inverse().get(methodRadioButton))
                .ifPresent(livenessDetector -> faceLivenessDetectionController
                        .activateFaceLivenessDetector(livenessDetector.getDetector(), this));
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
    public void visualizeDetectionPreview() {
        // TODO show detection preview on the screen
    }

    @Override
    public void visualizeStatus(LivenessDetectionStatus status) {
        final Message msg = statusChangeHandler.obtainMessage();
        final Bundle b = new Bundle();
        b.putString(StatusChangeHandler.STATUS_KEY, status.getKey());
        msg.setData(b);
        statusChangeHandler.sendMessage(msg);
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show());
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (NullPointerException | IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        createCameraSource(FACE_DETECTION);
        startCameraSource();
    }

    /** Stops the camera. */
    @Override
    protected void onPause() {
        super.onPause();
        preview.stop();
        faceLivenessDetectionController.deactivateFaceLivenessDetector();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    private void createCameraSource(String model) {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        try {
            switch (model) {
                case FACE_DETECTION:
                    Log.i(TAG, "Using Face Detector Processor");
                    FaceDetectorOptions faceDetectorOptions =
                            PreferenceUtils.getFaceDetectorOptionsForLivePreview(this);
                    cameraSource.setMachineLearningFrameProcessor(
                            new FaceDetectorProcessor(this, faceDetectorOptions));
                    break;
                default:
                    Log.e(TAG, "Unknown model: " + model);
            }
        } catch (Exception e) {
            Log.e(TAG, "Can not create image processor: " + model, e);
            Toast.makeText(
                    getApplicationContext(),
                    "Can not create image processor: " + e.getMessage(),
                    Toast.LENGTH_LONG)
                    .show();
        }
    }
}