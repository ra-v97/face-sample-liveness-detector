package agh.edu.pl.facelivenessdetection.model;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetector;

public class FaceLivenessDetectionModel {

    private final AtomicReference<FaceLivenessDetector> activeFaceLivenessDetector;

    public FaceLivenessDetectionModel(FaceLivenessDetector initialFaceLivenessDetector) {
        this.activeFaceLivenessDetector = new AtomicReference<>(initialFaceLivenessDetector);
    }

    public FaceLivenessDetectionModel() {
        this.activeFaceLivenessDetector = new AtomicReference<>();
    }

    public Optional<FaceLivenessDetector> resolveActiveFaceLivenessDetector() {
        return Optional.ofNullable(activeFaceLivenessDetector.get());
    }

    public void setActiveFaceLivenessDetector(FaceLivenessDetector detector) {
        Objects.requireNonNull(detector);
        activeFaceLivenessDetector.set(detector);
    }
}
