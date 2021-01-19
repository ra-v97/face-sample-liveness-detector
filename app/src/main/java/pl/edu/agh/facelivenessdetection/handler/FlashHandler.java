package pl.edu.agh.facelivenessdetection.handler;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

import pl.edu.agh.facelivenessdetection.MainActivity;

public class FlashHandler extends Handler {

    private final WeakReference<MainActivity> mainActivityReference;

    public FlashHandler(MainActivity activity) {
        mainActivityReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        final MainActivity activity = mainActivityReference.get();

        String status = msg.getData().getString("STATUS");
        if (status != null) {
            if (status.equals("ON")) {
                activity.startFrontFlashEmulator();
            } else if (status.equals("OFF")) {
                activity.stopFrontFlashEmulator();
            }
        } else {
            String text = msg.getData().getString("BUTTON_TEXT");
            int color = Integer.parseInt(msg.getData().getString("BUTTON_COLOR"));
            activity.setButton(text, color);
        }
    }
}
