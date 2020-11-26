package agh.edu.pl.facelivenessdetection.detector.flashing;

import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetector;
import agh.edu.pl.facelivenessdetection.detector.DetectorMethod;
import agh.edu.pl.facelivenessdetection.visuals.DetectionVisualizer;

public class FaceFlashingLivenessDetector implements FaceLivenessDetector {

    @Override
    public void handleDetectorActivated(DetectionVisualizer detectionVisualizer) {
        System.out.println("FaceFlashingLivenessDetector activated");

    }

    @Override
    public void handleDetectorDeactivated() {
        System.out.println("FaceFlashingLivenessDetector deactivated");

    }

    @Override
    public void handleDetectionTriggered() {
        System.out.println("FaceFlashingLivenessDetector triggered");
    }

    @Override
    public void backgroundTask() throws InterruptedException {
        while (true){
            Thread.sleep(5000);
            System.out.println("FaceFlashingLivenessDetector background task step");
        }
    }
}
