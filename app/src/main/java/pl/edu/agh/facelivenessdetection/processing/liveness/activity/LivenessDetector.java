package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;


class LivenessDetector {
    static final float eyesChangeThreshold = 0.6f;
    static final float headRotationChangeThreshold = 30f;
    static final float smileChangeThreshold = 0.6f;
    private static final float numberOfActivities = 2;

    private final Duration detectionPeriod = Duration.ofSeconds(3);

    private final LocalDateTime startTime = LocalDateTime.now();
    private final List<FaceState> stateList = new LinkedList<>();

    private final DetectionVisualizer detectionVisualizer;
    private RequestedActivityValidator requestedActivityValidator = null;

    LivenessDetector(DetectionVisualizer visualizer) {
        detectionVisualizer = visualizer;
    }

    void addFaceState(FaceState state) {
        stateList.add(state);
        if (requestedActivityValidator != null) {
            requestedActivityValidator.addFaceState(state);
        }
    }


    Boolean isAlive() {
        if (stateList.isEmpty()) {
            return null;
        }

        if (requestedActivityValidator != null) {
            return requestedActivityValidator.succeeded();
        }

        if (implicitlyAlive() && !timeExpired()) {
            return true;
        }

        if (!implicitlyAlive() && timeExpired()) {
            requestTasks();
            return null;
        }
        return null;
    }

    private void requestTasks() {
        List<PossibleActivity> requestedActivities = activitiesToPerform();
        requestedActivityValidator = new RequestedActivityValidator(requestedActivities);

        String activitiesNames = requestedActivities.stream().map(activity -> PossibleActivity.name(activity)).collect(Collectors.joining(", "));
        detectionVisualizer.showToast("Please perform those activities: " + activitiesNames);
    }

    private List<PossibleActivity> activitiesToPerform() {
        List<PossibleActivity> activities = new LinkedList<>();

        List<PossibleActivity> possibleActivities = Arrays.asList(PossibleActivity.values());
        Collections.shuffle(possibleActivities);
        for (int i = 0; i < numberOfActivities; i++) {
            activities.add(possibleActivities.get(i));
        }
        return activities;
    }

    private boolean timeExpired() {
        return Duration.between(startTime, LocalDateTime.now()).compareTo(detectionPeriod) > 0;
    }

    private boolean implicitlyAlive() {
        FaceState initial = stateList.get(0);
        return stateList.stream().skip(1).anyMatch(state -> LivenessDetector.changedSignificantly(initial, state));
    }

    private static boolean changedSignificantly(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);

        return Math.abs(diff.getHeadRotation()) > headRotationChangeThreshold ||
                (Math.abs(diff.getLeftEyeOpenedProb()) > eyesChangeThreshold && Math.abs(diff.getRightEyeOpenedProb()) > eyesChangeThreshold) ||
                Math.abs(diff.getSmileProb()) > smileChangeThreshold;
    }

}
