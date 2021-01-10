package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.ml.SVM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pl.edu.agh.facelivenessdetection.R;

import static org.opencv.ml.Ml.ROW_SAMPLE;

public class SVMClassifier {

    private SVM svm;

    /* training data should be organised as every row is a different example */
    public void trainSVM(Mat trainingData, Mat labels, String outputFile) {
        SVM svm = SVM.create();

        svm.setType(SVM.C_SVC); // Which one is the best?
        svm.setKernel(SVM.RBF); // Also, which one
        /* other parameters */

        svm.train(trainingData, ROW_SAMPLE, labels);
        svm.save(outputFile);

    }

    public boolean isSVMLoaded() {
        return svm != null;
    }

    public void load(Context context, String file) throws IOException {


        InputStream is = context.getResources().openRawResource(R.raw.trained_svm2);
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile = new File(cascadeDir, "trained_svm.xml");
        FileOutputStream os = new FileOutputStream(mCascadeFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
//        String test = mCascadeFile.getAbsolutePath();
        svm = SVM.load(mCascadeFile.getAbsolutePath());
//        svm = SVM.load(file);
    }

    public float predict(Mat mat) {
        return svm.predict(mat);
    }

    public Mat concatenateMats(List<Mat> mats) { // don't know if that works
//        Mat concatenated = new Mat();   // Maybe we need some specific type and maybe would not work
//                                        // without specifying number of rows and columns
//
//        for(Mat mat : mats) {
//            concatenated.push_back(mat);
//        }
//        return concatenated;

        Mat dst = new Mat();
        ImagePreparation imagePrepration = new ImagePreparation();
        int w = mats.get(0).width();
        int h = mats.size();
        List<Mat> src = new ArrayList<>();
        for(Mat mat: mats){
            Mat mat1 = imagePrepration.resize(mat, w, h);
            src.add(mat1);
        }
//        List<Mat> src = Arrays.asList(imagePreparation.resize(mat, w, h), imagePreparation.resize(face, w, h), imagePreparation.resize(left_eye,w,h), imagePreparation.resize(right_eye,w,h));
//

        Core.vconcat(mats, dst);
        return dst;
    }
}
