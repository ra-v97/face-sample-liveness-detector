package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import com.google.mlkit.vision.face.Face;

class FaceState {
    private Float leftEyeOpenedProb = 0f;
    private Float rightEyeOpenedProb = 0f;
    private Float headRotation = 0f;
    private Float smileProb = 0f;

    public FaceState(float leftEyeOpenedProb, float rightEyeOpenedProb, float headRotation, float smileProb) {
        this.leftEyeOpenedProb = leftEyeOpenedProb;
        this.rightEyeOpenedProb = rightEyeOpenedProb;
        this.headRotation = headRotation;
        this.smileProb = smileProb;
    }

    public FaceState(Face face) {
        this.leftEyeOpenedProb = face.getLeftEyeOpenProbability();
        this.rightEyeOpenedProb = face.getRightEyeOpenProbability();
        this.headRotation = face.getHeadEulerAngleY();
        this.smileProb = face.getSmilingProbability();
    }

    public Float getLeftEyeOpenedProb() {
        return leftEyeOpenedProb;
    }

    public Float getRightEyeOpenedProb() {
        return rightEyeOpenedProb;
    }

    public Float getSmileProb() {
        return smileProb;
    }

    public Float getHeadRotation() {
        return headRotation;
    }

    static FaceState diff(FaceState one, FaceState two) {
        return new FaceState(one.getLeftEyeOpenedProb() - two.getLeftEyeOpenedProb(),
                one.getRightEyeOpenedProb() - two.getRightEyeOpenedProb(),
                one.getHeadRotation() - two.getHeadRotation(),
                one.getSmileProb() - two.getSmileProb());
    }
}