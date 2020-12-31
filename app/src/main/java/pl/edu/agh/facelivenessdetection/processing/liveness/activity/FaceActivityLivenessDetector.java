package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.processing.vision.BaseImageAnalyzer;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;
import pl.edu.agh.facelivenessdetection.visualisation.drawer.FaceGraphic;

public class FaceActivityLivenessDetector extends BaseImageAnalyzer<List<Face>> {

    public static final String TAG = "DummyFaceDetectionProcessor";

    private final FaceDetector detector;

    private LivenessDetector livenessDetector = null;
    private DetectionVisualizer detectionVisualizer = null;

    public FaceActivityLivenessDetector(Context context, GraphicOverlay overlay, boolean isHorizontalMode,
                                        FaceDetectorOptions options) {
        super(context, overlay, isHorizontalMode);
        Log.v(TAG, "Face detector options: " + options);
        detector = FaceDetection.getClient(options);
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
    protected void onSuccess(List<Face> result, GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        for (Face face : result) {
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face));

            if (detector == null) {
                return;
            }

            if(livenessDetector == null){
                return;
            }
            livenessDetector.addFaceState(new FaceState(face));

            Boolean isAlive = livenessDetector.isAlive();
            Log.i(TAG, "FACE STATE: " + isAlive);

            if (isAlive == null) {
                detectionVisualizer.visualizeStatus(LivenessDetectionStatus.UNKNOWN);
            } else if (isAlive) {
                livenessDetector = null;
                detectionVisualizer.visualizeStatus(LivenessDetectionStatus.REAL);
            } else {
                livenessDetector = null;
                detectionVisualizer.visualizeStatus(LivenessDetectionStatus.FAKE);
            }
        }
        graphicOverlay.postInvalidate();
    }



    @Override
    public void livenessDetectionTrigger(DetectionVisualizer visualizer) {
        Log.i(TAG, "FaceActivityLivenessDetector face liveness detection triggered");
        livenessDetector = new LivenessDetector(visualizer);
        detectionVisualizer = visualizer;
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
