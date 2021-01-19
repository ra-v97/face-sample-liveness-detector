package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.LinkedList;
import java.util.List;

import static org.opencv.core.Core.SORT_ASCENDING;
import static org.opencv.core.Core.SORT_EVERY_ROW;
import static org.opencv.core.Core.add;
import static org.opencv.core.Core.divide;
import static org.opencv.core.Core.hconcat;
import static org.opencv.core.Core.sort;
import static org.opencv.core.Core.subtract;

public class Descriptors {

    public Mat applyPartialDescriptor(Mat flash, Mat background) {

        /* Divide input Mats by 10 (this is done to prevent exceeding max value) */

        flash.convertTo(flash, CvType.CV_32FC1);
        divide(flash, new Scalar(10), flash);

        background.convertTo(background, CvType.CV_32FC1);
        divide(background, new Scalar(10), background);

        Mat added = new Mat();
        add(flash, background, added);

        Mat subtracted = new Mat();
        subtract(flash, background, subtracted);

        Mat result = new Mat();
        divide(subtracted, added, result);

        return result;
    }

    public Mat applyDiffDesc(Mat flash, Mat background) {
        Mat desc = applyPartialDescriptor(flash, background);
        desc = desc.reshape(1, 1);

        return desc;
    }

    public Mat applySpecDesc(Mat flashLeft, Mat flashRight, Mat backgroundLeft, Mat backgroundRight) {
        Mat left = applyPartialDescriptor(flashLeft, backgroundLeft);
        Mat right = applyPartialDescriptor(flashRight, backgroundRight);

        Mat leftSorted = new Mat();
        sort(left.reshape(1, 1), leftSorted, SORT_EVERY_ROW + SORT_ASCENDING);

        Mat rightSorted = new Mat();
        sort(right.reshape(1, 1), rightSorted, SORT_EVERY_ROW + SORT_ASCENDING);

        Mat bothEyes = new Mat();
        List<Mat> eyesList = new LinkedList<>();
        eyesList.add(leftSorted);
        eyesList.add(rightSorted);
        hconcat(eyesList, bothEyes);

        return bothEyes;
    }

    public Mat applySpecDiffDesc(Mat flashLeft, Mat flashRight, Mat backgroundLeft, Mat backgroundRight, Mat faceFlash, Mat faceBackground) {
        Mat spec = applySpecDesc(flashLeft, flashRight, backgroundLeft, backgroundRight);
        Mat diff = applyDiffDesc(faceFlash, faceBackground);

        Mat specDiff = new Mat();
        List<Mat> descriptors = new LinkedList<>();
        descriptors.add(spec);
        descriptors.add(diff);

        hconcat(descriptors, specDiff);

        System.out.println(specDiff.dump());

        return specDiff;
    }
}
