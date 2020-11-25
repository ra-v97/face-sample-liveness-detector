package agh.edu.pl.facelivenessdetection.detector.flashing;

import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetector;
import agh.edu.pl.facelivenessdetection.model.MobileModel;
import agh.edu.pl.facelivenessdetection.detector.DetectorMethod;

public class FaceFlashingLivenessDetector implements FaceLivenessDetector {

    @Override
    public Runnable detect(MobileModel m) {
        return new DetectorMethod(m);
    }
}
