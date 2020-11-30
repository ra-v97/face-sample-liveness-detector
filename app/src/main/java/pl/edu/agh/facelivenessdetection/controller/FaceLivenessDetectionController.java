package pl.edu.agh.facelivenessdetection.controller;

import android.util.Log;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import pl.edu.agh.facelivenessdetection.MainActivity;
import pl.edu.agh.facelivenessdetection.processing.AuthWithFaceLivenessDetectMethodType;
import pl.edu.agh.facelivenessdetection.processing.FaceLivenessDetector;
import pl.edu.agh.facelivenessdetection.preference.PreferenceUtils;
import pl.edu.agh.facelivenessdetection.processing.VisionImageProcessor;
import pl.edu.agh.facelivenessdetection.processing.liveness.activity.FaceActivityLivenessDetector;
import pl.edu.agh.facelivenessdetection.processing.liveness.flashing.FaceFlashingLivenessDetector;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;

/**
 * Class is responsible for triggering events on active detector.
 * The controller also manages threads in application, because we cannot run detecting action with
 * the UI thread.
 */
public class FaceLivenessDetectionController {

    private static final String TAG = "FaceLivenessDetectionController";

    private final AtomicReference<AuthWithFaceLivenessDetectMethodType> activeFaceProcessorMethod;

    private final AtomicReference<FaceLivenessDetector> activeLivenessDetectorRef;

    public FaceLivenessDetectionController() {
        this.activeFaceProcessorMethod = new AtomicReference<>();
        this.activeLivenessDetectorRef = new AtomicReference<>();
    }

    public void setFaceProcessorMethod(AuthWithFaceLivenessDetectMethodType methodType) {
        activeFaceProcessorMethod.set(methodType);
    }

    public Optional<VisionImageProcessor> obtainVisionProcessor(MainActivity activity) {
        if (activeFaceProcessorMethod.get() == null) {
            Log.d(TAG, "1");

            return Optional.empty();
        }
        try {
            System.out.println("2");
            switch (activeFaceProcessorMethod.get()) {
                case FACE_ACTIVITY_METHOD:
                    Log.d(TAG, "3");
                    Log.i(TAG, "Using FaceActivityLivenessDetector");
                    final FaceActivityLivenessDetector faceActivityLivenessDetector =
                            new FaceActivityLivenessDetector(activity,
                                    PreferenceUtils.getFaceDetectorOptionsForLivePreview(activity));
                    activeLivenessDetectorRef.set(faceActivityLivenessDetector);
                    return Optional.of(faceActivityLivenessDetector);

                case FACE_FLASHING_METHOD:
                    Log.d(TAG, "4");
                    Log.i(TAG, "Using FaceFlashingLivenessDetector");
                    final FaceFlashingLivenessDetector faceFlashingLivenessDetector =
                            new FaceFlashingLivenessDetector(activity,
                                    PreferenceUtils.getFaceDetectorOptionsForLivePreview(activity));
                    activeLivenessDetectorRef.set(faceFlashingLivenessDetector);
                    return Optional.of(faceFlashingLivenessDetector);
                default:
                    throw new IllegalStateException("Invalid model name");
            }
        } catch (Exception e) {
            Log.d(TAG, "5");
            Log.e(TAG, "Can not create image processor: " + activeFaceProcessorMethod.get(), e);
            activity.showToast("Can not create image processor: " + e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    public void deactivateFaceLivenessDetector() {
        final FaceLivenessDetector faceLivenessDetector = activeLivenessDetectorRef.get();
        if (faceLivenessDetector != null) {
            faceLivenessDetector.terminate();
            activeLivenessDetectorRef.set(null);
        }
    }

    public void performFaceLivenessDetectionTrigger(DetectionVisualizer visualizer) {
        Optional.ofNullable(activeLivenessDetectorRef.get())
                .ifPresent(faceLivenessDetector ->
                        faceLivenessDetector.livenessDetectionTrigger(visualizer));
    }
}
