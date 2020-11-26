package agh.edu.pl.facelivenessdetection.detector;

import agh.edu.pl.facelivenessdetection.visuals.DetectionVisualizer;

public interface FaceLivenessDetector {

    /*
     * Method is invoked when user choose face liveness detection method with radio button.
     */
    void handleDetectorActivated(DetectionVisualizer detectionVisualizer);

    /*
     * Method is invoked when detector is turned off.
     */
    void handleDetectorDeactivated();

    /*
     * Method is invoked when user perform face liveness detection action.
     * Output  status should be set on the visualizer passed in handleDetectorActivated method.
     */
    void handleDetectionTriggered();

    /*
     * Method represents endless task for executing in the background in separate thread while method is active.
     * The background task can be used to show some frames or masks on the screen.
     * The task is managed by the controller and detector doesn't have to start or stop it.
     */
    void backgroundTask() throws InterruptedException;
}
