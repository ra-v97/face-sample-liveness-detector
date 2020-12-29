package pl.edu.agh.facelivenessdetection.processing;


import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;

public interface FaceLivenessDetector {
    /**
     * Method is invoked when user perform face liveness detection action.
     * Output  status should be set on the visualizer passed in handleDetectorActivated method.
     * Method should run in separate thread.
     * For for details take a look at processImageProxy(... ,...) method
     */
    void livenessDetectionTrigger(DetectionVisualizer visualizer);

    /**
     * Interrupts liveness detection process and releases all resources used by detection algorithm
     * */
    void stop();
}
