package agh.edu.pl.facelivenessdetection.visuals;

import agh.edu.pl.facelivenessdetection.model.LivenessDetectionStatus;

public interface StatusVisualizer {

    /*
     * This method lets you invoke from any thread a context change on UI that will indicate status
     * of detection action for face shown on the picture
     */
    void visualizeStatus(LivenessDetectionStatus status);
}
