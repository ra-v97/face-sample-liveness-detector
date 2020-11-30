package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import android.util.Log;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import static pl.edu.agh.facelivenessdetection.processing.liveness.activity.LivenessDetector.eyesChangeThreshold;
import static pl.edu.agh.facelivenessdetection.processing.liveness.activity.LivenessDetector.headRotationChangeThreshold;
import static pl.edu.agh.facelivenessdetection.processing.liveness.activity.LivenessDetector.smileChangeThreshold;

public class RequestedActivityValidator {
    private static final Duration detectionPeriod = Duration.ofSeconds(10);

    private final LocalDateTime startTime = LocalDateTime.now();
    private final List<FaceState> stateList = new LinkedList<>();
    private final List<PossibleActivity> requestedActivities;

    RequestedActivityValidator(List<PossibleActivity> requestedActivities) {
        this.requestedActivities = requestedActivities;
    }

    void addFaceState(FaceState state) {
        stateList.add(state);
    }

    Boolean succeeded() {
        if (stateList.isEmpty()) {
            return null;
        }

        if (tasksCompleted() && !timeExpired()) {
            return true;
        }

        if (!tasksCompleted() && timeExpired()) {
            return false;
        }
        return null;
    }

    private boolean tasksCompleted() {
        FaceState initial = stateList.get(0);
        return requestedActivities.stream().allMatch(activity -> stateList.stream().skip(1).anyMatch(state -> checkActivity(activity, initial, state)));
    }

    private static boolean checkActivity(PossibleActivity activity, FaceState initial, FaceState state) {
        switch (activity) {
            case SMILE:
                return smiled(initial, state);
            case BLINK:
                return blinked(initial, state);
            case TURN_HEAD_LEFT:
                return turnedHeadLeft(initial, state);
            case TURN_HEAD_RIGHT:
                return turnedHeadRight(initial, state);
        }
        return false;
    }

    private static boolean blinked(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        return (Math.abs(diff.getLeftEyeOpenedProb()) > eyesChangeThreshold && Math.abs(diff.getRightEyeOpenedProb()) > eyesChangeThreshold);
    }

    private static boolean smiled(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        return Math.abs(diff.getSmileProb()) > smileChangeThreshold;
    }

    private static boolean turnedHeadLeft(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        Log.d("HEAD_TURN", "Turned head left: " + diff.getHeadRotation());
        return diff.getHeadRotation() < -headRotationChangeThreshold;
    }

    private static boolean turnedHeadRight(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        Log.d("HEAD_TURN", "Turned head right: " + diff.getHeadRotation());
        return diff.getHeadRotation() > headRotationChangeThreshold;
    }

    private boolean timeExpired() {
        return Duration.between(startTime, LocalDateTime.now()).compareTo(detectionPeriod) > 0;
    }
}
