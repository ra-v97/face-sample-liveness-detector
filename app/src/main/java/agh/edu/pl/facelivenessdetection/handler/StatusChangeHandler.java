package agh.edu.pl.facelivenessdetection.handler;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.Optional;

import agh.edu.pl.facelivenessdetection.MainActivity;
import agh.edu.pl.facelivenessdetection.model.LivenessDetectionStatus;

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
