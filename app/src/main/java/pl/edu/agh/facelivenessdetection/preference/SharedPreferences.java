package pl.edu.agh.facelivenessdetection.preference;

import androidx.camera.core.CameraSelector;

public final class SharedPreferences {

    private static final int CAMERA_LEANS_FACING = CameraSelector.LENS_FACING_FRONT;

    public static int getCameraLeansFacing() {
        return CAMERA_LEANS_FACING;
    }

    private SharedPreferences() {
    }
}
