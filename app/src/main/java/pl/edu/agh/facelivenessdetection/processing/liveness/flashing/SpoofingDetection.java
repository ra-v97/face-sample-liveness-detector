package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.IOException;
import java.util.Map;

import pl.edu.agh.facelivenessdetection.R;

public class SpoofingDetection {

    Context context;
    ImagePreparation imagePreparation;
    SVMClassifier svmClassifier;


    public SpoofingDetection(Context context){
        this.context = context;
        imagePreparation = new ImagePreparation();
        svmClassifier = new SVMClassifier();
    }

    public float predict(Bitmap bitmapFlash, Bitmap bitmapBackground) {

        if (bitmapFlash == null || bitmapBackground == null) {
            return (float) 0.0;
        }

        try {
            svmClassifier.load(context, "trained_svm.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Mat flash = imagePreparation.bitmapToMat(bitmapFlash);
        Mat background = imagePreparation.bitmapToMat(bitmapBackground);

//        int resizeFactor = 4;
//        flash = imagePreparation.resize(flash, flash.width()/resizeFactor, flash.height()/resizeFactor);
//        background = imagePreparation.resize(background, background.width()/resizeFactor, background.height()/resizeFactor);


        Mat descriptor = getFaceDescriptor(flash, background);
        if (descriptor == null) {
            return (float) 0.0;
        }
        float res = svmClassifier.predict(descriptor);

//        System.out.println(res);
        return res;
    }

    Mat getFaceDescriptor(Mat flash, Mat background) {

        // ewentualny obrot

        ImagePreparation imagePreparation = new ImagePreparation();


        flash = imagePreparation.applyGrayFilter(flash);
        background = imagePreparation.applyGrayFilter(background);

        FaceEyesDetection faceEyesDetection = new FaceEyesDetection(context);
        Mat faceFlash = faceEyesDetection.cut_face(flash);
        Map flashEyes = faceEyesDetection.detect_eyes(flash);
        if (flashEyes.get("LEFT") == null || flashEyes.get("RIGHT") == null) {
            return null;
        }
        Mat leftEyeFlash = flash.submat((Rect) flashEyes.get("LEFT"));
        Mat rightEyeFlash = flash.submat((Rect) flashEyes.get("RIGHT"));

        Mat faceBackground = faceEyesDetection.cut_face(background);
        Map backgroundEyes = faceEyesDetection.detect_eyes(background);
        if (backgroundEyes.get("LEFT") == null || backgroundEyes.get("RIGHT") == null) {
            return null;
        }
        Mat leftEyeBackground = background.submat((Rect) backgroundEyes.get("LEFT"));
        Mat rightEyeBackground = background.submat((Rect) backgroundEyes.get("RIGHT"));

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
