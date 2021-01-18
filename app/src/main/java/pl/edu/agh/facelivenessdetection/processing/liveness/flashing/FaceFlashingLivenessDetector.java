package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;


import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.opencv.core.CvException;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import pl.edu.agh.facelivenessdetection.MainActivity;
import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.processing.vision.BaseImageAnalyzer;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;

import static android.graphics.Color.parseColor;

public class FaceFlashingLivenessDetector extends BaseImageAnalyzer<Image> {

    private static final String TAG = "FaceDetectorProcessor";

    private final FaceDetector detector;
    DetectionVisualizer visualizer;
    private SpoofingDetection spoofingDetection;
    //    private boolean flashActive = false;
//    private List<byte[]> image_list;
    private boolean takeBackgroundPhoto;
    private boolean takeFlashPhoto;
    private List<byte[]> image_list;

    MainActivity mainActivity;

    public FaceFlashingLivenessDetector(Context context, GraphicOverlay overlay, boolean isHorizontalMode,
                                        FaceDetectorOptions options) {
        super(context, overlay, isHorizontalMode);
        Log.v(TAG, "Face detector options: " + options);
        detector = FaceDetection.getClient(options);
        spoofingDetection = new SpoofingDetection(context);
        image_list = new LinkedList<byte[]>();
        mainActivity = (MainActivity) getContext();
        mainActivity.changeButton("take back", Color.GREEN);

    }

    @Override
    public void livenessDetectionTrigger(DetectionVisualizer visualizer) {
        Log.i(TAG, "Method triggered");
        this.visualizer = visualizer;

        if (takeBackgroundPhoto) {
            takeFlashPhoto = true;
            takeBackgroundPhoto = false;
        } else {
            takeBackgroundPhoto = true;
        }
    }

    @Override
    public void stop() {
        mainActivity.changeButton("DETECT", parseColor("#FF673AB7"));
        super.stop();
        try {
            detector.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: $e");
        }
    }

    @Override
    protected Task<Image> detectInImage(InputImage image, Image img) {
        return Tasks.call(new Callable<Image>() {
            @Override
            public Image call() throws Exception {
                if (takeBackgroundPhoto) {
                    ;
//                    MainActivity mainActivity = (MainActivity) getContext();
//                    mainActivity.setFlashStatus("OFF");
                }
                return img;
            }
        });
    }

    @Override
    protected void onSuccess(Image img) {
        if (takeBackgroundPhoto) {
            if (image_list.size() < 10) {
                image_list.add(NV21toJPEG(
                        YUV_420_888toNV21(img),
                        img.getWidth(), img.getHeight()));
            }
        }

        if (!takeFlashPhoto) {
            return;
        }

        takeBackgroundPhoto = false;
        takeFlashPhoto = false;

        byte[] flashImage = NV21toJPEG(
                YUV_420_888toNV21(img),
                img.getWidth(), img.getHeight());
        byte[] backgroundImage = image_list.get(0);
        System.out.println("Flash photo taken");
        final GraphicOverlay graphicOverlay = getGraphicOverlay();
        graphicOverlay.clear();

        Bitmap flashBitmap = BitmapFactory.decodeByteArray(flashImage, 0, flashImage.length);
        Bitmap backgroundBitmap = BitmapFactory.decodeByteArray(backgroundImage, 0, backgroundImage.length);

        float prediction;
        try {
            prediction = spoofingDetection.predict(flashBitmap, backgroundBitmap);
        } catch (CvException cvException) {
            cvException.printStackTrace();
            return;
        }

        if (visualizer != null) {
            if (prediction == -1.0) {
                visualizer.visualizeStatus(LivenessDetectionStatus.REAL);
            } else if (prediction == 1.0) {
                visualizer.visualizeStatus(LivenessDetectionStatus.FAKE);
            } else {
                visualizer.visualizeStatus(LivenessDetectionStatus.UNKNOWN);
            }
        }

//        MainActivity mainActivity = (MainActivity) getContext();
//        mainActivity.setFlashStatus("OFF");

        image_list.clear();
    }

    private static byte[] YUV_420_888toNV21(Image image) {
        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }

    private static byte[] NV21toJPEG(byte[] nv21, int width, int height) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        return out.toByteArray();
    }


    @Override
    protected void onFailure(Exception e) {
        Log.w(TAG, "Face Detector failed: " + e);
    }
}
