package agh.edu.pl.facelivenessdetection.detector;

import agh.edu.pl.facelivenessdetection.model.MobileModel;

public interface FaceLivenessDetector {

    Runnable detect(MobileModel m);
}
