package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;
import android.util.Pair;

import androidx.camera.core.ImageProxy;
import androidx.camera.core.internal.utils.ImageUtil;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.common.internal.ImageConvertUtils;
import com.google.mlkit.vision.common.internal.ImageUtils;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import pl.edu.agh.facelivenessdetection.MainActivity;
import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.processing.vision.BaseImageAnalyzer;
import pl.edu.agh.facelivenessdetection.utils.BitmapUtils;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;

public class FaceFlashingLivenessDetector extends BaseImageAnalyzer<Image> {

    private static final String TAG = "FaceDetectorProcessor";

    private final FaceDetector detector;
    DetectionVisualizer visualizer;
    private SpoofingDetection spoofingDetection;
    private Image img_save1;
    private boolean performDetection = false;
//    private boolean flashActive = false;
//    private List<byte[]> image_list;
    private boolean takeBackgroundPhoto;
    private boolean takeFlashPhoto;
    private List<byte[]> image_list;

    public FaceFlashingLivenessDetector(Context context, GraphicOverlay overlay, boolean isHorizontalMode,
                                        FaceDetectorOptions options) {
        super(context, overlay, isHorizontalMode);
        Log.v(TAG, "Face detector options: " + options);
        detector = FaceDetection.getClient(options);
        spoofingDetection = new SpoofingDetection(context);
        image_list = new LinkedList<byte[]>();

    }

    @Override
    public void livenessDetectionTrigger(DetectionVisualizer visualizer) {
        Log.i(TAG, "Method triggered");
        this.visualizer = visualizer;
        //performDetection = true;

        if (takeBackgroundPhoto) {
            takeFlashPhoto = true;
            takeBackgroundPhoto = false;
        } else {
            takeBackgroundPhoto = true;
        }

//        Bitmap bitmapFlash = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.f1);
//        Bitmap bitmapBackground = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.b1);


//        SpoofingDetection spoofingDetection = new SpoofingDetection(getContext());
//        float prediction = spoofingDetection.predict(bitmapFlash, bitmapBackground);

//        if (prediction == -1) {
//            visualizer.visualizeStatus(LivenessDetectionStatus.REAL);
//        } else if (prediction == 1) {
//            visualizer.visualizeStatus(LivenessDetectionStatus.FAKE);
//        } else {
//            visualizer.visualizeStatus(LivenessDetectionStatus.UNKNOWN);
//        }
    }

    @Override
    public void stop() {
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

                    MainActivity mainActivity = (MainActivity) getContext();
                    mainActivity.setFlashStatus("ON");
//                    try {
//                        Thread.sleep(200);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                }
                return img;
            }
        });
//        return detector.process(image);
    }

    private Bitmap imageToBitmap(Image image) {
        byte[] data = NV21toJPEG(
                YUV_420_888toNV21(image),
                image.getWidth(),
                image.getHeight());
        return BitmapFactory.decodeByteArray(data, 0, data.length);
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

//        if (!performDetection) {
//            return;
//        }
        byte[] flashImage = NV21toJPEG(
                YUV_420_888toNV21(img),
                img.getWidth(), img.getHeight());
        byte[] backgroundImage = image_list.get(0);
        System.out.println("Flash photo taken");
        final GraphicOverlay graphicOverlay = getGraphicOverlay();
        graphicOverlay.clear();

        Bitmap flashBitmap = BitmapFactory.decodeByteArray(flashImage, 0, flashImage.length);
        Bitmap backgroundBitmap = BitmapFactory.decodeByteArray(backgroundImage, 0, backgroundImage.length);

        System.out.println("->"+flashBitmap);
        System.out.println("->"+backgroundBitmap);

        float prediction;
        try{
            prediction = spoofingDetection.predict(flashBitmap, backgroundBitmap);
        } catch (CvException cvException){
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

        MainActivity mainActivity = (MainActivity) getContext();
        mainActivity.setFlashStatus("OFF");
        //performDetection = false;

        image_list.clear();

//        result.forEach(res -> {
//            // TODO Check rect param
//            final FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, res);
//            graphicOverlay.add(faceGraphic);
//        });
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
