package agh.edu.pl.facelivenessdetection.handler;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.util.Optional;

import agh.edu.pl.facelivenessdetection.MainActivity;
import agh.edu.pl.facelivenessdetection.handler.command.CommandDispatcher;

public class CommandHandler extends Handler {

    public final String COMMAND_KEY = "CMD";

    private final WeakReference<MainActivity> mainActivityReference;

    public CommandHandler(MainActivity activity) {
        mainActivityReference = new WeakReference<>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        final MainActivity activity = mainActivityReference.get();

        Optional.ofNullable(msg.getData().getString(COMMAND_KEY))
                .map(CommandDispatcher::getCommandByKey)
                .ifPresent(command -> command.execute(activity));
    }
}
