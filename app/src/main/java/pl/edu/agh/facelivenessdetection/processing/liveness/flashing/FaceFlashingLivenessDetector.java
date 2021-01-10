package pl.edu.agh.facelivenessdetection.processing.liveness.flashing;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

import pl.edu.agh.facelivenessdetection.processing.vision.BaseImageAnalyzer;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;
import pl.edu.agh.facelivenessdetection.visualisation.drawer.FaceGraphic;

public class FaceFlashingLivenessDetector extends BaseImageAnalyzer<List<Face>> {

    private static final String TAG = "FaceDetectorProcessor";

    private final FaceDetector detector;

    public FaceFlashingLivenessDetector(Context context, GraphicOverlay overlay, boolean isHorizontalMode,
                                        FaceDetectorOptions options) {
        super(context, overlay, isHorizontalMode);
        Log.v(TAG, "Face detector options: " + options);
        detector = FaceDetection.getClient(options);
    }

    @Override
    public void livenessDetectionTrigger(DetectionVisualizer visualizer) {
        Log.i(TAG, "Method triggered");

//        ImagePreparation imagePreparation = new ImagePreparation();
//        Bitmap bitmapFlash = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.f1);
//        Bitmap bitmapBackground = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.b1);

//        SpoofingDetection spoofingDetection = new SpoofingDetection(getContext());
//        float prediction = spoofingDetection.predict(bitmapFlash, bitmapBackground);
//        float prediction = 0;

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
    protected Task<List<Face>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(List<Face> result) {
        final GraphicOverlay graphicOverlay = getGraphicOverlay();
        graphicOverlay.clear();
        result.forEach(res -> {
            // TODO Check rect param
            final FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay, res);
            graphicOverlay.add(faceGraphic);
        });
        graphicOverlay.postInvalidate();
    }

    @Override
    protected void onFailure(Exception e) {
        Log.w(TAG, "Face Detector failed.$e");
    }
}
