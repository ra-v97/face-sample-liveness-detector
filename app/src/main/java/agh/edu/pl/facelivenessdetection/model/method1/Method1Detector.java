package agh.edu.pl.facelivenessdetection.model.method1;

import agh.edu.pl.facelivenessdetection.model.LivenessDetector;
import agh.edu.pl.facelivenessdetection.model.MobileModel;

public class Method1Detector implements LivenessDetector {

    @Override
    public Runnable detect(MobileModel m) {
        return new Method(m);
    }
}
