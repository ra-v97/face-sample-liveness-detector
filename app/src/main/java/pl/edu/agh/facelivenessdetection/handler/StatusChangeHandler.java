package pl.edu.agh.facelivenessdetection.handler;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.Optional;

import pl.edu.agh.facelivenessdetection.MainActivity;
import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;


public class StatusChangeHandler extends Handler {

    public static final String STATUS_KEY = "STATUS";

    private final WeakReference<MainActivity> mainActivityReference;

    public StatusChangeHandler(MainActivity activity) {
        mainActivityReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        final MainActivity activity = mainActivityReference.get();

        Optional.ofNullable(msg.getData().getString(STATUS_KEY))
                .map(LivenessDetectionStatus::fromString)
                .ifPresent(activity::setDetectionStatus);
    }
}
