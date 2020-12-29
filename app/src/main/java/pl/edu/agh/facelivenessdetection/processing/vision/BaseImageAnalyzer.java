package pl.edu.agh.facelivenessdetection.processing.vision;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;

import java.util.Optional;

import pl.edu.agh.facelivenessdetection.preference.SharedPreferences;
import pl.edu.agh.facelivenessdetection.processing.FaceLivenessDetector;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;

public abstract class BaseImageAnalyzer<T> implements ImageAnalysis.Analyzer, FaceLivenessDetector {

    private final GraphicOverlay graphicOverlay;

    private boolean isOverlayInitialized;

    private final boolean isHorizontalMode;

    public BaseImageAnalyzer(GraphicOverlay graphicOverlay, boolean isHorizontalMode) {
        this.graphicOverlay = graphicOverlay;
        this.isOverlayInitialized = false;
        this.isHorizontalMode = isHorizontalMode;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public void analyze(ImageProxy imageProxy) {
        configureOverlay(imageProxy);
        final Image mediaImage = imageProxy.getImage();
        Optional.ofNullable(mediaImage)
                .ifPresent(img -> {
                    final Task<T> tTask = detectInImage(InputImage.fromMediaImage(img, imageProxy.getImageInfo().getRotationDegrees()));
                    tTask.addOnSuccessListener(result -> onSuccess(result, graphicOverlay, img.getCropRect()));
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

    public abstract void stop();

    protected abstract Task<T> detectInImage(InputImage image);

    protected abstract void onSuccess(T result, GraphicOverlay graphicOverlay, Rect rect);

    protected abstract void onFailure(Exception e);
}
