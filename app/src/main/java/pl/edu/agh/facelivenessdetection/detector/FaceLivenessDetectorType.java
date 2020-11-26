package pl.edu.agh.facelivenessdetection.detector;

import androidx.core.util.Supplier;

import pl.edu.agh.facelivenessdetection.detector.activity.FaceActivityLivenessDetector;
import pl.edu.agh.facelivenessdetection.detector.flashing.FaceFlashingLivenessDetector;

public enum FaceLivenessDetectorType {
    FACE_FLASHING_METHOD(FaceFlashingLivenessDetector::new),
    FACE_ACTIVITY_METHOD(FaceActivityLivenessDetector::new);

    private final Supplier<FaceLivenessDetector> faceLivenessDetectorSupplier;

    FaceLivenessDetectorType(Supplier<FaceLivenessDetector> faceLivenessDetectorSupplier) {
        this.faceLivenessDetectorSupplier = faceLivenessDetectorSupplier;
    }

    public FaceLivenessDetector getDetector() {
        return faceLivenessDetectorSupplier.get();
    }
}
