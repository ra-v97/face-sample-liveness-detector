package pl.edu.agh.facelivenessdetection.visualisation;


import android.widget.Toast;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;

public interface DetectionVisualizer {
    /**
     * This method lets you invoke from any thread a context change on UI that will indicate status
     * of detection action for face shown on the picture
     */
    void visualizeStatus(LivenessDetectionStatus status);

    /**
     * Shows a Toast on the UI thread.
     */
     void showToast(final String text);
}
