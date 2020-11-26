package pl.edu.agh.facelivenessdetection.detector.activity;

import pl.edu.agh.facelivenessdetection.detector.FaceLivenessDetector;
import pl.edu.agh.facelivenessdetection.visuals.DetectionVisualizer;

public class FaceActivityLivenessDetector implements FaceLivenessDetector {

    @Override
    public void handleDetectorActivated(DetectionVisualizer detectionVisualizer) {
        System.out.println("FaceActivityLivenessDetector activated");

    }

    @Override
    public void handleDetectorDeactivated() {
        System.out.println("FaceActivityLivenessDetector deactivated");

    }

    @Override
    public void handleDetectionTriggered() {
        System.out.println("FaceActivityLivenessDetector triggered");
    }

    @Override
    public void backgroundTask() throws InterruptedException {
        while (true){
            Thread.sleep(5000);
            System.out.println("FaceActivityLivenessDetector background task step");
        }
    }
}
