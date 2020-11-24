package agh.edu.pl.facelivenessdetection.model.method2;

import agh.edu.pl.facelivenessdetection.model.LivenessDetector;
import agh.edu.pl.facelivenessdetection.model.MobileModel;
import agh.edu.pl.facelivenessdetection.model.method2.Method;

public class Method2Detector implements LivenessDetector {

    @Override
    public Runnable detect(MobileModel m) {
        return new Method(m);
    }
}
