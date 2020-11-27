package pl.edu.agh.facelivenessdetection.model;

import android.graphics.Color;

import java.util.Objects;

public enum LivenessDetectionStatus {
    REAL("real", Color.parseColor("#008000")),
    FAKE("fake", Color.parseColor("#FF0000")),
    UNKNOWN("unknown", Color.parseColor("#FFFF00"));

    private final String key;

    private final int statusColor;

    LivenessDetectionStatus(String key, int statusColor) {
        this.key = key;
        this.statusColor = statusColor;
    }

    public int getColor() {
        return statusColor;
    }

    public String getKey() {
        return key;
    }

    public static LivenessDetectionStatus fromString(String text) {
        for (LivenessDetectionStatus o : LivenessDetectionStatus.values()) {
            if (Objects.equals(o.key, text.toLowerCase())) {
                return o;
            }
        }
        return null;
    }
}
