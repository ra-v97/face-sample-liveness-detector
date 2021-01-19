package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;
import android.graphics.Bitmap;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Map;

public class SpoofingDetection {

    Context context;
    ImagePreparation imagePreparation;
    SVMClassifier svmClassifier;
    FaceEyesDetection faceEyesDetection;


    public SpoofingDetection(Context context) {
        this.context = context;
        imagePreparation = new ImagePreparation();
        svmClassifier = new SVMClassifier();
        faceEyesDetection = new FaceEyesDetection(context);
    }

    public float predict(Bitmap bitmapFlash, Bitmap bitmapBackground) {

        if (bitmapFlash == null || bitmapBackground == null) {
            return (float) 0.0;
        }

        if (!svmClassifier.isSVMLoaded()) {
            try {
                svmClassifier.load(context, "trained_svm_all.xml");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Mat flash = imagePreparation.bitmapToMat(bitmapFlash);
        Mat background = imagePreparation.bitmapToMat(bitmapBackground);

        Mat descriptor = getFaceDescriptor(flash, background);
        if (descriptor == null) {
            return (float) 0.0;
        }
        return svmClassifier.predict(descriptor);
    }

    Mat getFaceDescriptor(Mat flash, Mat background) {

        // ewentualny obrot

        flash = imagePreparation.applyGrayFilter(flash);
        background = imagePreparation.applyGrayFilter(background);

        Mat faceFlash = faceEyesDetection.cut_face(flash);

        if (faceFlash == null) {
            return null;
        }

        Map flashEyes = faceEyesDetection.detect_eyes(faceFlash);
        if (flashEyes.get("LEFT") == null || flashEyes.get("RIGHT") == null) {
            return null;
        }
        Mat leftEyeFlash = (Mat) flashEyes.get("LEFT");
        Mat rightEyeFlash = (Mat) flashEyes.get("RIGHT");


        Mat faceBackground = faceEyesDetection.cut_face(background);

        if (faceBackground == null) {
            return null;
        }

        Map backgroundEyes = faceEyesDetection.detect_eyes(faceBackground);

        if (backgroundEyes.get("LEFT") == null || backgroundEyes.get("RIGHT") == null) {
            return null;
        }

        Mat leftEyeBackground = (Mat) backgroundEyes.get("LEFT");
        Mat rightEyeBackground = (Mat) backgroundEyes.get("RIGHT");

        Descriptors descriptors = new Descriptors();

        /* for SPECULAR */
        leftEyeFlash = imagePreparation.applyGaussianBlur(leftEyeFlash, 2);
        rightEyeFlash = imagePreparation.applyGaussianBlur(rightEyeFlash, 2);
        leftEyeBackground = imagePreparation.applyGaussianBlur(leftEyeBackground, 2);
        rightEyeBackground = imagePreparation.applyGaussianBlur(rightEyeBackground, 2);

        /* resize */
        leftEyeFlash = imagePreparation.resize(leftEyeFlash, 40, 40);
        rightEyeFlash = imagePreparation.resize(rightEyeFlash, 40, 40);
        leftEyeBackground = imagePreparation.resize(leftEyeBackground, 40, 40);
        rightEyeBackground = imagePreparation.resize(rightEyeBackground, 40, 40);


        /* for DIFFUSE */
        faceFlash = imagePreparation.applyGaussianBlur(faceFlash, 5);
        faceBackground = imagePreparation.applyGaussianBlur(faceBackground, 5);

        faceFlash = imagePreparation.resize(faceFlash, 100, 100);
        faceBackground = imagePreparation.resize(faceBackground, 100, 100);

        Mat specDiffDesc = descriptors.applySpecDiffDesc(leftEyeFlash, rightEyeFlash, leftEyeBackground, rightEyeBackground,
                faceFlash, faceBackground);

        return specDiffDesc;
    }
}
