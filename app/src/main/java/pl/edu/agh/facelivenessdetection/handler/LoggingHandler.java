package pl.edu.agh.facelivenessdetection.handler;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.Optional;

import pl.edu.agh.facelivenessdetection.MainActivity;

public class LoggingHandler extends Handler {
    public static final String CLEAR_KEY = "CLEAR";

    public static final String LOG_MESSAGE_KEY = "LOG";

    private final WeakReference<MainActivity> mainActivityReference;

    public LoggingHandler(MainActivity activity) {
        mainActivityReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        final MainActivity activity = mainActivityReference.get();

        Optional.ofNullable(msg.getData().getBoolean(CLEAR_KEY))
                .ifPresent(status -> activity.clearLogger());

        Optional.ofNullable(msg.getData().getString(LOG_MESSAGE_KEY))
                .ifPresent(activity::addLoggingMessage);

    }
}
