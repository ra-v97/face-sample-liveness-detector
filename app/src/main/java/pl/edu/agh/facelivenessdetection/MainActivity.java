package pl.edu.agh.facelivenessdetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.annotation.KeepName;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Objects;
import java.util.Optional;

import pl.edu.agh.facelivenessdetection.controller.CameraManager;
import pl.edu.agh.facelivenessdetection.processing.AuthWithFaceLivenessDetectMethodType;
import pl.edu.agh.facelivenessdetection.utils.PermissionManager;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;

import pl.edu.agh.facelivenessdetection.handler.StatusChangeHandler;
import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
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
    private final BiMap<AuthWithFaceLivenessDetectMethodType, RadioButton> faceLivenessDetectorRadioButtonBiMap;

    private final StatusChangeHandler statusChangeHandler;

    @Nullable
    private CameraManager cameraManager;

    public MainActivity() {
        faceLivenessDetectorRadioButtonBiMap = HashBiMap.create();

        statusChangeHandler = new StatusChangeHandler(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        createCameraManager();
        setActiveFaceDetectionMethod(Objects.requireNonNull(cameraManager).getActiveAnalyzerType());
        setUpViewModelProvider();

        if (!PermissionManager.allPermissionsGranted(this)) {
            PermissionManager.getRuntimePermissions(this);
        }
    }

    private void createCameraManager() {
        cameraManager = new CameraManager(this, previewView, this, graphicOverlay);
    }

    private void initializeViewElements() {
        faceLivenessDetectorRadioButtonBiMap.put(AuthWithFaceLivenessDetectMethodType.FACE_ACTIVITY_METHOD,
                method1RadioButton);
        faceLivenessDetectorRadioButtonBiMap.put(AuthWithFaceLivenessDetectMethodType.FACE_FLASHING_METHOD,
                method2RadioButton);
    }

    private void setUpViewModelProvider() {
        if (PermissionManager.allPermissionsGranted(this) && cameraManager != null) {
            cameraManager.startCamera();
        }
    }

    public void onDetectButtonClick(View view) {
        Objects.requireNonNull(cameraManager).performFaceLivenessDetectionTrigger(this);
    }

    public void onToggleMethod1(View view) {
        refreshFaceDetectionMethod(method1RadioButton);
    }

    public void onToggleMethod2(View view) {
        refreshFaceDetectionMethod(method2RadioButton);
    }

    private void setActiveFaceDetectionMethod(AuthWithFaceLivenessDetectMethodType activeFaceDetectionMethod) {
        refreshFaceDetectionMethod(activeFaceDetectionMethod);
        Optional.ofNullable(faceLivenessDetectorRadioButtonBiMap.get(activeFaceDetectionMethod))
                .ifPresent(RadioButton::toggle);
    }

    private void refreshFaceDetectionMethod(AuthWithFaceLivenessDetectMethodType activeFaceDetectionMethod) {
        Objects.requireNonNull(cameraManager).changeAnalyzer(activeFaceDetectionMethod);
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
        runOnUiThread(() -> Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraManager != null) {
            cameraManager.startCamera();
        } else {
            Log.e(TAG, "Camera is unavailable");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Objects.requireNonNull(cameraManager).stopCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Objects.requireNonNull(cameraManager).stopCamera();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (PermissionManager.allPermissionsGranted(this) && cameraManager != null) {

            cameraManager.startCamera();
        } else {
            Log.e(TAG, "Camera start error");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}