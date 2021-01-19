package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.ml.SVM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
        InputStream is = context.getResources().openRawResource(R.raw.trained_svm_all);
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile = new File(cascadeDir, file);
        FileOutputStream os = new FileOutputStream(mCascadeFile);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        is.close();
        os.close();
        svm = SVM.load(mCascadeFile.getAbsolutePath());
    }

    public float predict(Mat mat) {
        return svm.predict(mat);
    }

}
