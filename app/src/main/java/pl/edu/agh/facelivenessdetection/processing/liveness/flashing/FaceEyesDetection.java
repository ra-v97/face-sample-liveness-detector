package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import pl.edu.agh.facelivenessdetection.R;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

public class FaceEyesDetection {

    CascadeClassifier mJavaDetector;
    CascadeClassifier mJavaDetector_eye;

    public FaceEyesDetection(Context context) {
        configure(context);
    }

    public void configure(Context context) {
        try {
            InputStream is = context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();
            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());

            InputStream is_eye = context.getResources().openRawResource(R.raw.haarcascade_eye);
            File cascadeDir_eye = context.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile_eye = new File(cascadeDir_eye, "haarcascade_eye.xml");
            FileOutputStream os_eye = new FileOutputStream(mCascadeFile_eye);
            byte[] buffer_eye = new byte[4096];
            int bytesRead_eye;
            while ((bytesRead_eye = is_eye.read(buffer_eye)) != -1) {
                os_eye.write(buffer_eye, 0, bytesRead_eye);
            }
            is_eye.close();
            os_eye.close();
            mJavaDetector_eye = new CascadeClassifier(mCascadeFile_eye.getAbsolutePath());

            if (mJavaDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            if (mJavaDetector_eye.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector_eye = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile_eye.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    public Map<String, Mat> detect_eyes(Mat img) {
        MatOfRect faceDetections = new MatOfRect();
        assert mJavaDetector != null;
        Map<String, Mat> res = new HashMap<String, Mat>();
        mJavaDetector_eye.detectMultiScale(img, faceDetections, 1.3, 5);
        Mat left_eye = null;
        Mat right_eye = null;
        int width = img.width();
        int height = img.height();
        System.out.println("WIDHT " + width + "HEIGHT " + height);
        for (Rect rect : faceDetections.toArray()) {
            int x = rect.x;
            int y = rect.y;
            int w = rect.width;
            int h = rect.height;
            System.out.println(x + " " + y + " " + w + " " + h);
            if (y > height / 2) {
                continue;
            }
            int eyecenter = x + w / 2;
            if (eyecenter < width * 0.5) {
                left_eye = img.submat(new Rect((int) (x + (0.66 * w / 2)), (int) (y + (0.66 * h / 2)), (int) (w - (0.66 * w / 2)), (int) (h - (0.66 * h / 2))));
            } else {
                right_eye = img.submat(new Rect((int) (x + (0.66 * w / 2)), (int) (y + (0.66 * h / 2)), (int) (w - (0.66 * w / 2)), (int) (h - (0.66 * h / 2))));
            }
            res.put("LEFT", left_eye);
            res.put("RIGHT", right_eye);
        }
        return res;
    }

    public Mat cut_face(Mat img) {
        assert mJavaDetector != null;
        MatOfRect faceDetections = new MatOfRect();
        mJavaDetector.detectMultiScale(img, faceDetections);
        Mat roi = null;
        for (Rect rect : faceDetections.toArray()) {
            roi = new Mat(img, rect);
        }
        return roi;
    }
}
