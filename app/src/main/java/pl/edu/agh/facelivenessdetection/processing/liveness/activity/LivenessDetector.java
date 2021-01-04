package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;


class LivenessDetector {

    private static final String IMPLICIT_ACTIVITY_LOG_FORMAT = "Detection" +
            " stats: (leftEye,%.2f) (rightEye,%.2f) (smile,%.2f) (headRotation,%.2f)";

    private static final int MIN_ACTIVITIES = 1;

    private static final int MAX_ACTIVITIES = 4;

    private static final float DEFAULT_THRESHOLD = 0.6f;

    private static final float DEFAULT_HEAD_ROTATION = 20f;

    private static final int MIN_TIMEOUT = 1;

    private static final int MAX_TIMEOUT = 10;

    private static final int DEFAULT_TIMEOUT = 4;

    private final float changeThreshold;

    private final int numberOfActivities;

    private final int verificationTimeout;

    private final boolean autoDetection;

    private final Duration autoDetectionPeriod;

    private final LocalDateTime startTime;

    private final List<FaceState> stateList = new LinkedList<>();

    private final DetectionVisualizer detectionVisualizer;

    private RequestedActivityValidator requestedActivityValidator = null;

    LivenessDetector(DetectionVisualizer visualizer, float changeThreshold, int numberOfActivities,
                     int verificationTimeout, boolean autoDetection, int autoDetectionTimeout) {
        detectionVisualizer = visualizer;

        if (numberOfActivities >= MIN_ACTIVITIES && numberOfActivities <= MAX_ACTIVITIES) {
            this.numberOfActivities = numberOfActivities;
        } else {
            this.numberOfActivities = 2;
        }

        if (Math.abs(changeThreshold) <= 1.0) {
            this.changeThreshold = changeThreshold;
        } else {
            this.changeThreshold = DEFAULT_THRESHOLD;
        }
        this.autoDetection = autoDetection;

        if (verificationTimeout >= MIN_TIMEOUT && verificationTimeout <= MAX_TIMEOUT) {
            this.verificationTimeout = verificationTimeout;
        } else {
            this.verificationTimeout = DEFAULT_TIMEOUT;
        }

        this.startTime = LocalDateTime.now();

        if (autoDetectionTimeout >= MIN_TIMEOUT && autoDetectionTimeout <= MAX_TIMEOUT) {
            autoDetectionPeriod = Duration.ofSeconds(autoDetectionTimeout);
        } else {
            autoDetectionPeriod = Duration.ofSeconds(DEFAULT_TIMEOUT);
        }
    }

    void addFaceState(FaceState state) {
        stateList.add(state);
        if (requestedActivityValidator != null) {
            requestedActivityValidator.addFaceState(state);
        }
    }

    LivenessDetectionStatus isAlive() {
        if (stateList.isEmpty()) {
            return LivenessDetectionStatus.UNKNOWN;
        }
        if (requestedActivityValidator != null) {
            return requestedActivityValidator.performVerification();
        }
        if (checkAutoDetectionCondition()) {
            if (implicitlyAlive()) {
                detectionVisualizer.logInfo("Liveness confirmed by face movement");
                return LivenessDetectionStatus.REAL;
            }
            return LivenessDetectionStatus.UNKNOWN;
        }
        requestTasks();
        return LivenessDetectionStatus.UNKNOWN;
    }

    private void requestTasks() {
        List<PossibleActivity> requestedActivities = activitiesToPerform();

        requestedActivityValidator = new RequestedActivityValidator(detectionVisualizer,
                requestedActivities, changeThreshold, DEFAULT_HEAD_ROTATION, verificationTimeout);

        String activitiesNames = requestedActivities.stream()
                .map(activity -> PossibleActivity.name(activity))
                .collect(Collectors.joining(", "));

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

    private boolean checkAutoDetectionCondition() {
        return autoDetection
                && Duration.between(startTime, LocalDateTime.now()).compareTo(autoDetectionPeriod) < 0;
    }

    private boolean implicitlyAlive() {
        FaceState initial = stateList.get(0);
        Optional<FaceState> firstImplicitActiveState = stateList.stream()
                .skip(1)
                .filter(state -> changedSignificantly(initial, state))
                .findFirst();
        firstImplicitActiveState.ifPresent(state -> {
            FaceState diff = FaceState.diff(initial, state);
            detectionVisualizer.logInfo(String.format(Locale.US, IMPLICIT_ACTIVITY_LOG_FORMAT,
                    diff.getLeftEyeOpenedProb(),
                    diff.getRightEyeOpenedProb(),
                    diff.getSmileProb(),
                    diff.getHeadRotation()));
        });
        return firstImplicitActiveState.isPresent();
    }

    private boolean changedSignificantly(FaceState initial, FaceState state) {
        FaceState diff = FaceState.diff(initial, state);

        return Math.abs(diff.getHeadRotation()) > DEFAULT_HEAD_ROTATION ||
                (Math.abs(diff.getLeftEyeOpenedProb()) > changeThreshold && Math.abs(diff.getRightEyeOpenedProb()) > changeThreshold) ||
                Math.abs(diff.getSmileProb()) > changeThreshold;
    }
}
