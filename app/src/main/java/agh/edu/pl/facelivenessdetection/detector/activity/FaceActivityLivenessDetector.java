package agh.edu.pl.facelivenessdetection.detector.activity;

import agh.edu.pl.facelivenessdetection.detector.DetectorMethod;
import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetector;
import agh.edu.pl.facelivenessdetection.model.MobileModel;

public class FaceActivityLivenessDetector implements FaceLivenessDetector {

    @Override
    public Runnable detect(MobileModel m) {
        return new DetectorMethod(m);
    }
}
