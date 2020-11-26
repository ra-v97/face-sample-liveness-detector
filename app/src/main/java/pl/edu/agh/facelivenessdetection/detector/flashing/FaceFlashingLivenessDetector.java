package pl.edu.agh.facelivenessdetection.detector.flashing;

import pl.edu.agh.facelivenessdetection.detector.FaceLivenessDetector;
import pl.edu.agh.facelivenessdetection.visuals.DetectionVisualizer;

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
