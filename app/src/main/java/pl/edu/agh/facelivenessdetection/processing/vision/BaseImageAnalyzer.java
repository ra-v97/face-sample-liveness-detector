package pl.edu.agh.facelivenessdetection.processing.vision;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Rect;
import android.media.Image;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import java.util.Optional;

import pl.edu.agh.facelivenessdetection.preference.SharedPreferences;
import pl.edu.agh.facelivenessdetection.processing.FaceLivenessDetector;
import pl.edu.agh.facelivenessdetection.processing.ProcessingMonitor;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;
import pl.edu.agh.facelivenessdetection.visualisation.drawer.InferenceInfoGraphic;

public abstract class BaseImageAnalyzer<T> implements ImageAnalysis.Analyzer, FaceLivenessDetector {

    private final Context context;

    private final ProcessingMonitor monitor;

    private final GraphicOverlay graphicOverlay;

    private boolean isOverlayInitialized;

    private final boolean isHorizontalMode;

    public BaseImageAnalyzer(Context context, GraphicOverlay graphicOverlay, boolean isHorizontalMode) {
        this.graphicOverlay = graphicOverlay;
        this.context = context;
        this.isOverlayInitialized = false;
        this.isHorizontalMode = isHorizontalMode;
        this.monitor = new ProcessingMonitor((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        this.monitor.startTimer();
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public void analyze(ImageProxy imageProxy) {
        configureOverlay(imageProxy);
        final Image mediaImage = imageProxy.getImage();
        Optional.ofNullable(mediaImage)
                .ifPresent(img -> {
                    monitor.notifyDetectionStart();
                    final Task<T> tTask = detectInImage(InputImage.fromMediaImage(img, imageProxy.getImageInfo().getRotationDegrees()));
                    tTask.addOnSuccessListener(result -> {
                        onSuccess(result);
                        graphicOverlay.add(new InferenceInfoGraphic(graphicOverlay,
                                monitor.getAverageLatency(), monitor.getFPSRate()));
                        monitor.notifyDetectionCompleted();
                    });
                    tTask.addOnFailureListener(e -> {
                        graphicOverlay.clear();
                        graphicOverlay.postInvalidate();
                        onFailure(e);
                    });
                    tTask.addOnCompleteListener(res -> imageProxy.close());
                });
    }

    private void configureOverlay(ImageProxy imageProxy) {
        if (!isOverlayInitialized) {
            boolean isImageFlipped = SharedPreferences.getCameraLeansFacing() == CameraSelector.LENS_FACING_FRONT;
            if (isHorizontalMode) {
                graphicOverlay.setImageSourceInfo(imageProxy.getWidth(), imageProxy.getHeight(),
                        isImageFlipped);
            } else {
                graphicOverlay.setImageSourceInfo(imageProxy.getHeight(), imageProxy.getWidth(),
                        isImageFlipped);
            }
            isOverlayInitialized = true;
        }
    }

    protected GraphicOverlay getGraphicOverlay() {
        return graphicOverlay;
    }

    protected String resolveActiveTag() {
        final android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("logging_tag", null);
    }

    public void stop() {
        monitor.stopTimer();
    }

    protected abstract Task<T> detectInImage(InputImage image);

    protected abstract void onSuccess(T result);

    protected abstract void onFailure(Exception e);
}
