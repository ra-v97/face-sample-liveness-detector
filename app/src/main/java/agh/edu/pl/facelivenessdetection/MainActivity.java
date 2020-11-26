package agh.edu.pl.facelivenessdetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.view.TextureView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.Optional;

import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetectorType;
import agh.edu.pl.facelivenessdetection.handler.StatusChangeHandler;
import agh.edu.pl.facelivenessdetection.controller.FaceLivenessDetectionController;
import agh.edu.pl.facelivenessdetection.model.LivenessDetectionStatus;
import agh.edu.pl.facelivenessdetection.visuals.DetectionVisualizer;

public class MainActivity extends AppCompatActivity implements DetectionVisualizer {
    /*
     *  Constants definition
     */
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;
    /*
     * UI Elements
     */
    private RadioButton method1RadioButton;

    private RadioButton method2RadioButton;

    private TextView livenessStatusTextView;

    private TextureView cameraPreviewTextureView;
    /*
     * Required elements
     */
    private final FaceLivenessDetectionController faceLivenessDetectionController;

    private final BiMap<FaceLivenessDetectorType, RadioButton> faceLivenessDetectorRadioButtonBiMap;

    private final StatusChangeHandler statusChangeHandler;

    public MainActivity() {
        faceLivenessDetectionController = new FaceLivenessDetectionController();
        faceLivenessDetectorRadioButtonBiMap = HashBiMap.create();

        statusChangeHandler = new StatusChangeHandler(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreviewTextureView = findViewById(R.id.texture);
        method1RadioButton = findViewById(R.id.radioButtonMethod1);
        method2RadioButton = findViewById(R.id.radioButtonMethod2);
        livenessStatusTextView = findViewById(R.id.statusView);

        initializeViewElements();
        setActiveFaceDetectionMethod(FaceLivenessDetectorType.FACE_ACTIVITY_METHOD);
    }

    private void initializeViewElements() {
        faceLivenessDetectorRadioButtonBiMap.put(FaceLivenessDetectorType.FACE_ACTIVITY_METHOD,
                method1RadioButton);
        faceLivenessDetectorRadioButtonBiMap.put(FaceLivenessDetectorType.FACE_FLASHING_METHOD,
                method2RadioButton);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                showToast(getString(R.string.camera_permissions_not_granted_message));
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        faceLivenessDetectionController.deactivateFaceLivenessDetector();
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show());
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("R string request permission")
                    .setPositiveButton(android.R.string.ok, (dialog, which) ->
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE))
                    .setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> finish())
                    .create();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION_CODE);
        }
    }
}