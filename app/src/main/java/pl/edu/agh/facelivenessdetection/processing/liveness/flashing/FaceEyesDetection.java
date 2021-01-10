package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

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

    public FaceEyesDetection(Context context){
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

    public Map<String, Rect> detect_eyes(Mat img){
            MatOfRect faceDetections = new MatOfRect();
            assert mJavaDetector != null;
            Map<String, Rect> res = new HashMap<String, Rect>();
            mJavaDetector.detectMultiScale(img, faceDetections);
            for(Rect rect:faceDetections.toArray()) {
                Rect eyearea_right = new Rect(rect.x +rect.width/16,(int)(rect.y + (rect.height/4.5)),(rect.width - 2*rect.width/16)/2,(int)( rect.height/3.0));
                Rect eyearea_left = new Rect(rect.x +rect.width/16 +(rect.width - 2*rect.width/16)/2,(int)(rect.y + (rect.height/4.5)),(rect.width - 2*rect.width/16)/2,(int)( rect.height/3.0));
                int size = 320;
                res.put("LEFT", get_template(img, eyearea_left, size));
                res.put("RIGHT", get_template(img, eyearea_right, size));
            }
        return res;
    }

    private Rect get_template(Mat img, Rect area, int size){
        Mat mROI = img.submat(area);
        MatOfRect eyes = new MatOfRect();
        mJavaDetector_eye.detectMultiScale(mROI, eyes, 1.15, 2, Objdetect.CASCADE_FIND_BIGGEST_OBJECT|Objdetect.CASCADE_SCALE_IMAGE, new Size(30,30),new Size());
        Rect[] eyesArray = eyes.toArray();
        for (int i = 0; i < eyesArray.length; i++){
            Point iris = new Point();
            Rect e = eyesArray[i];
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int)e.tl().x,(int)( e.tl().y + e.height*0.4),(int)e.width,(int)(e.height*0.6));
            mROI = img.submat(eye_only_rectangle);
            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            return new Rect((int)iris.x-size/2,(int)iris.y-size/2 ,size, size);
        }
        return null;
    }

    public Mat cut_face(Mat img){
        assert mJavaDetector != null;
        MatOfRect faceDetections = new MatOfRect();
        mJavaDetector.detectMultiScale(img, faceDetections);
        Mat roi = null;
        for(Rect rect:faceDetections.toArray()) {
            roi = new Mat(img, rect);
        }
        return roi;
    }
}
