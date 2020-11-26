package agh.edu.pl.facelivenessdetection.detector.activity;

import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetector;
import agh.edu.pl.facelivenessdetection.visuals.DetectionVisualizer;

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
