package pl.edu.agh.facelivenessdetection.processing;

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

public class DummyFaceDetectionProcessor extends BaseImageAnalyzer<List<Face>> {

    public static final String TAG = "DummyFaceDetectionProcessor";

    private final FaceDetectorOptions realTimeOpts;

    private final FaceDetector detector;

    public DummyFaceDetectionProcessor(GraphicOverlay view, boolean isHorizontalMode) {
        super(view, isHorizontalMode);

        realTimeOpts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        detector = FaceDetection.getClient(realTimeOpts);
    }

    @Override
    public void livenessDetectionTrigger(DetectionVisualizer visualizer) {
        Log.i(TAG, "Method triggered");
    }

    @Override
    public void stop() {
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
    protected void onSuccess(List<Face> result, GraphicOverlay graphicOverlay, Rect rect) {
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
