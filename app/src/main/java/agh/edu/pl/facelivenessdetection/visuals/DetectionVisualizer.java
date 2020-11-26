package agh.edu.pl.facelivenessdetection.visuals;

import agh.edu.pl.facelivenessdetection.model.LivenessDetectionStatus;

public interface DetectionVisualizer {
    /*
     * This method lets you invoke from any thread a context change on UI that will show detection
     * preview like frames or masks on the input pictures
     */
    void visualizeDetectionPreview();

    /*
     * This method lets you invoke from any thread a context change on UI that will indicate status
     * of detection action for face shown on the picture
     */
    void visualizeStatus(LivenessDetectionStatus status);
}
