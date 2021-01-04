package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import android.util.Log;

import com.google.common.collect.Lists;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;

public class RequestedActivityValidator {

    private final DetectionVisualizer detectionVisualizer;

    private final Duration detectionPeriod;

    private final LocalDateTime startTime;

    private final List<FaceState> stateList;

    private final List<PossibleActivity> requestedActivities;

    private final float changeThreshold;

    private final float headRotationChangeThreshold;

    RequestedActivityValidator(DetectionVisualizer visualizer, List<PossibleActivity> requestedActivities, float changeThreshold,
                               float headRotationChangeThreshold, int timeout) {
        this.detectionVisualizer = visualizer;
        this.requestedActivities = requestedActivities;
        this.changeThreshold = changeThreshold;
        this.headRotationChangeThreshold = headRotationChangeThreshold;
        this.detectionPeriod = Duration.ofSeconds(timeout);
        this.startTime = LocalDateTime.now();
        this.stateList = Lists.newLinkedList();
    }

    void addFaceState(FaceState state) {
        stateList.add(state);
    }

    LivenessDetectionStatus performVerification() {
        if (stateList.size() < requestedActivities.size()) {
            return LivenessDetectionStatus.UNKNOWN;
        }

        final boolean taskCompleted = tasksCompleted();
        final boolean timeExpired = timeExpired();


        if (taskCompleted && !timeExpired) {
            detectionVisualizer.logInfo("Face is real");
            return LivenessDetectionStatus.REAL;
        }

        if (!taskCompleted && timeExpired) {
            detectionVisualizer.logInfo("Face is fake");
            return LivenessDetectionStatus.FAKE;
        }

        if(timeExpired){
            return LivenessDetectionStatus.FAKE;
        }

         return LivenessDetectionStatus.UNKNOWN;
    }

    private boolean tasksCompleted() {
        int idx = 1;
        int counter = 0;
        final FaceState initial = stateList.get(0);
        for (PossibleActivity activity : requestedActivities) {
            for (int i = idx; i < stateList.size(); i++) {
                if (checkActivity(activity, initial, stateList.get(i))) {
                    idx = i + 1;
                    counter++;
                    break;
                }
            }
        }
        return counter == requestedActivities.size();
    }

    private boolean checkActivity(PossibleActivity activity, FaceState initial, FaceState state) {
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

    private boolean blinked(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        return (Math.abs(diff.getLeftEyeOpenedProb()) > changeThreshold && Math.abs(diff.getRightEyeOpenedProb()) > changeThreshold);
    }

    private boolean smiled(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        return Math.abs(diff.getSmileProb()) > changeThreshold;
    }

    private boolean turnedHeadLeft(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        Log.d("HEAD_TURN", "Turned head left: " + diff.getHeadRotation());
        return diff.getHeadRotation() < -headRotationChangeThreshold;
    }

    private boolean turnedHeadRight(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);
        Log.d("HEAD_TURN", "Turned head right: " + diff.getHeadRotation());
        return diff.getHeadRotation() > headRotationChangeThreshold;
    }

    private boolean timeExpired() {
        return Duration.between(startTime, LocalDateTime.now()).compareTo(detectionPeriod) > 0;
    }
}
