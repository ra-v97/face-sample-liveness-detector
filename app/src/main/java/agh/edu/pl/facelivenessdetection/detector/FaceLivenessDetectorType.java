package agh.edu.pl.facelivenessdetection.detector;

import androidx.core.util.Supplier;

import agh.edu.pl.facelivenessdetection.detector.activity.FaceActivityLivenessDetector;
import agh.edu.pl.facelivenessdetection.detector.flashing.FaceFlashingLivenessDetector;

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
