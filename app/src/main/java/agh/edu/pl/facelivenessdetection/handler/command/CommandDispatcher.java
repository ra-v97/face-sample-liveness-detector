package agh.edu.pl.facelivenessdetection.handler.command;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class CommandDispatcher {

    private final static Map<String, Supplier<MainActivityCommand>> keyCommandMap = Maps.newHashMap();

    static {
        keyCommandMap.put(TakeSnapshotCommand.KEY, TakeSnapshotCommand::new);
    }

    public static MainActivityCommand getCommandByKey(String key) {
        return Optional.ofNullable(keyCommandMap.get(key))
                .map(Supplier::get)
                .orElse(null);
    }
}
