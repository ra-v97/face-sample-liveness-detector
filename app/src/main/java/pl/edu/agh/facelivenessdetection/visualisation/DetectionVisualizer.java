package pl.edu.agh.facelivenessdetection.visualisation;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;

public interface DetectionVisualizer {
    /**
     * This method lets you invoke from any thread a context change on UI that will indicate status
     * of detection action for face shown on the picture
     */
    void visualizeStatus(LivenessDetectionStatus status);

    /**
     * This method lets you to log information into main screen list from any thread.
     */
    void logInfo(String message);

    /**
     * This method lets you to clear log information from main screen list from any thread.
     */
    void clearInfo();

    /**
     * Shows a Toast on the UI thread.
     */
    void showToast(final String text);
}
