package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

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

    public static final String TAG = "FaceActivityLivenessDetector";

    private final FaceDetector detector;

    private LivenessDetector livenessDetector = null;

    private DetectionVisualizer detectionVisualizer = null;

    private LivenessDetectionStatus lastStatus;

    private LivenessDetectionStatus actualStatus;

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
    protected void onSuccess(List<Face> result) {
        final GraphicOverlay graphicOverlay = getGraphicOverlay();
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
            lastStatus = actualStatus;
            actualStatus = livenessDetector.isAlive();

            Log.i(TAG, "FACE STATE: " + actualStatus);

            if(lastStatus != actualStatus){
                detectionVisualizer.visualizeStatus(actualStatus);
            }
            if(actualStatus != LivenessDetectionStatus.UNKNOWN){
                livenessDetector = null;
            }
        }
        graphicOverlay.postInvalidate();
    }

    @Override
    public void livenessDetectionTrigger(DetectionVisualizer visualizer) {
        Log.i(TAG, "FaceActivityLivenessDetector face liveness detection triggered");
        livenessDetector = new LivenessDetector(visualizer,
                resolveThreshold(),
                resolveActionsNumber(),
                resolveTimeout(),
                resolveAutoDetection(),
                resolveAutoDetectionTimeout());
        detectionVisualizer = visualizer;
    }

    private int resolveActionsNumber() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getInt("face_actions_number", -1);
    }

    private float resolveThreshold() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return (float) (Integer.parseInt(prefs.getString("probability_threshold", "-1")) / 100.0);
    }

    private boolean resolveAutoDetection() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getBoolean("activity_method_auto_check", false);

    }

    private int resolveTimeout() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getInt("verification_timeout", 4);
    }

    private int resolveAutoDetectionTimeout() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        return prefs.getInt("auto_check_timeout", 2);
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
