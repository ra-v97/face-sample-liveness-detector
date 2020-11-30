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

import java.time.LocalDateTime;
import java.util.List;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.processing.FaceLivenessDetector;
import pl.edu.agh.facelivenessdetection.processing.vision.VisionProcessorBase;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;
import pl.edu.agh.facelivenessdetection.visualisation.drawer.FaceGraphic;

public class FaceActivityLivenessDetector extends VisionProcessorBase<List<Face>> implements FaceLivenessDetector {

    private static final String TAG = "FaceDetectorProcessor";
    private final FaceDetector detector;

    private LivenessDetector livenessDetector = null;
    private DetectionVisualizer detectionVisualizer = null;

    public FaceActivityLivenessDetector(Context context) {
        this(context,
                new FaceDetectorOptions.Builder()
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .enableTracking()
                        .build());
    }

    public FaceActivityLivenessDetector(Context context, FaceDetectorOptions options) {
        super(context);
        Log.v(MANUAL_TESTING_LOG, "Face detector options: " + options);
        detector = FaceDetection.getClient(options);
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<List<Face>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
        for (Face face : faces) {
            graphicOverlay.add(new FaceGraphic(graphicOverlay, face));

            if (livenessDetector == null) {
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
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }

    @Override
    public void livenessDetectionTrigger(DetectionVisualizer visualizer) {
        Log.i(TAG, "FaceActivityLivenessDetector face liveness detection triggered");
        livenessDetector = new LivenessDetector(visualizer);
        detectionVisualizer = visualizer;
    }

    @Override
    public void terminate() {
        Log.i(TAG, "FaceActivityLivenessDetector face liveness detection terminated");
    }
}
