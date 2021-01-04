package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.persistence.ActivityDetectionReport;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;

public class RequestedActivityValidator {

    private static final String ACTIVITY_RECOGNITION_FROM_QUEST_LOG = "Found activity (q): %s\nwith probability: %.2f";

    private static final String ACTIVITY_RECOGNITION_ANGLE_FROM_QUEST_LOG = "Found activity (q): %s\nwith angle: %.2f";

    private static final String MAX_ACTIVITY_VALUES_LOG_FORMAT = "Max activity stat:\n(%s, %.2f)";

    private final DetectionVisualizer detectionVisualizer;

    private final ActivityDetectionReport detectionReport;

    private final Duration detectionPeriod;

    private final LocalDateTime startTime;

    private final List<FaceState> stateList;

    private final List<PossibleActivity> requestedActivities;

    private final List<PossibleActivity> remainingActivities;

    private int processedIdx = 1;

    private final float changeThreshold;

    private final float headRotationChangeThreshold;

    private final Map<PossibleActivity, Float> maxActivityDiffs;

    RequestedActivityValidator(DetectionVisualizer visualizer, ActivityDetectionReport detectionReport,
                               List<PossibleActivity> requestedActivities, float changeThreshold,
                               float headRotationChangeThreshold, int timeout) {
        this.detectionVisualizer = visualizer;
        this.detectionReport = detectionReport;
        this.requestedActivities = requestedActivities;
        this.remainingActivities = Lists.newLinkedList(requestedActivities);
        this.changeThreshold = changeThreshold;
        this.headRotationChangeThreshold = headRotationChangeThreshold;
        this.detectionPeriod = Duration.ofSeconds(timeout);
        this.startTime = LocalDateTime.now();
        this.stateList = Lists.newLinkedList();
        this.maxActivityDiffs = Maps.newHashMap();

        requestedActivities.forEach(activity -> maxActivityDiffs.put(activity, 0.0f));
    }

    void addFaceState(FaceState state) {
        stateList.add(state);
    }

    @SuppressLint("DefaultLocale")
    LivenessDetectionStatus performVerification() {
        if (stateList.size() < requestedActivities.size()) {
            return LivenessDetectionStatus.UNKNOWN;
        }

        final boolean taskCompleted = tasksCompleted();
        final boolean timeExpired = timeExpired();

        if (taskCompleted && !timeExpired) {
            detectionVisualizer.logInfo("Face is real");
            requestedActivities.forEach(activity ->
                    detectionReport.addNewRecognizedAction(activity, maxActivityDiffs.get(activity)));
            return LivenessDetectionStatus.REAL;
        }

        if (timeExpired) {
            requestedActivities.forEach(activity -> {
                detectionReport.addNewRecognizedAction(activity, maxActivityDiffs.get(activity));
                detectionVisualizer
                        .logInfo(String.format(MAX_ACTIVITY_VALUES_LOG_FORMAT, activity.toString(),
                                maxActivityDiffs.get(activity)));
            });
            detectionVisualizer.logInfo("Face is fake");
            return LivenessDetectionStatus.FAKE;
        }

        return LivenessDetectionStatus.UNKNOWN;
    }

    private boolean tasksCompleted() {
        final FaceState initial = stateList.get(0);

        if (remainingActivities.size() == 0) {
            return false;
        }

        final PossibleActivity activity = remainingActivities.get(0);

        for (int i = processedIdx; i < stateList.size(); i++) {
            if (checkActivity(activity, initial, stateList.get(i))) {
                processedIdx = i + 1;
                remainingActivities.remove(activity);
                logActivityResult(activity, initial, stateList.get(i));
                break;
            }
        }

        return remainingActivities.size() == 0;
    }

    @SuppressLint("DefaultLocale")
    private void logActivityResult(PossibleActivity activity, FaceState initial, FaceState state) {
        final FaceState diff = FaceState.diff(initial, state);
        switch (activity) {
            case SMILE:
                final float smileDiff = Math.abs(diff.getSmileProb());
                detectionVisualizer
                        .logInfo(String.format(ACTIVITY_RECOGNITION_FROM_QUEST_LOG,
                                activity.toString(), smileDiff));
                //detectionReport.addNewRecognizedAction(PossibleActivity.SMILE, smileDiff);
                break;
            case BLINK:
                final float blinkDiff = Math.min(Math.abs(diff.getLeftEyeOpenedProb()),
                        Math.abs(diff.getRightEyeOpenedProb()));
                detectionVisualizer
                        .logInfo(String.format(ACTIVITY_RECOGNITION_FROM_QUEST_LOG,
                                activity.toString(), blinkDiff));
                //detectionReport.addNewRecognizedAction(PossibleActivity.BLINK, blinkDiff);
                break;

            case TURN_HEAD_LEFT:
            case TURN_HEAD_RIGHT:
                detectionVisualizer
                        .logInfo(String.format(ACTIVITY_RECOGNITION_ANGLE_FROM_QUEST_LOG,
                                activity.toString(), Math.abs(diff.getHeadRotation())));
                //detectionReport.addNewRecognizedAction(activity, diff.getHeadRotation());
        }
    }

    private boolean checkActivity(PossibleActivity activity, FaceState initial, FaceState state) {
        final FaceState diff = FaceState.diff(initial, state);
        switch (activity) {
            case SMILE:
                return smiled(diff);
            case BLINK:
                return blinked(diff);
            case TURN_HEAD_LEFT:
                return turnedHeadLeft(diff);
            case TURN_HEAD_RIGHT:
                return turnedHeadRight(diff);
        }
        return false;
    }

    private boolean blinked(FaceState diff) {
        final float blinkProb = Math.min(Math.abs(diff.getLeftEyeOpenedProb()),
                Math.abs(diff.getRightEyeOpenedProb()));
        if (maxActivityDiffs.get(PossibleActivity.BLINK) < blinkProb) {
            maxActivityDiffs.put(PossibleActivity.BLINK, blinkProb);
        }
        return blinkProb > changeThreshold;
    }

    private boolean smiled(FaceState diff) {
        final float smiledProb = Math.abs(diff.getSmileProb());
        if (maxActivityDiffs.get(PossibleActivity.SMILE) < smiledProb) {
            maxActivityDiffs.put(PossibleActivity.SMILE, smiledProb);
        }
        return smiledProb > changeThreshold;
    }

    private boolean turnedHeadLeft(FaceState diff) {
        final float headRotation = Math.abs(diff.getHeadRotation());
        if (maxActivityDiffs.get(PossibleActivity.TURN_HEAD_LEFT) < headRotation) {
            maxActivityDiffs.put(PossibleActivity.TURN_HEAD_LEFT, headRotation);
        }
        return headRotation > headRotationChangeThreshold;
    }

    private boolean turnedHeadRight(FaceState diff) {
        final float headRotation = Math.abs(diff.getHeadRotation());
        if (maxActivityDiffs.get(PossibleActivity.TURN_HEAD_RIGHT) < headRotation) {
            maxActivityDiffs.put(PossibleActivity.TURN_HEAD_RIGHT, headRotation);
        }
        return headRotation > headRotationChangeThreshold;
    }

    private boolean timeExpired() {
        return Duration.between(startTime, LocalDateTime.now()).compareTo(detectionPeriod) > 0;
    }
}
