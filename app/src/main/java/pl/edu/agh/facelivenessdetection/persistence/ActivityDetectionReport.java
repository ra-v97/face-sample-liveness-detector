package pl.edu.agh.facelivenessdetection.persistence;

import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;

import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.processing.AuthWithFaceLivenessDetectMethodType;
import pl.edu.agh.facelivenessdetection.processing.liveness.activity.PossibleActivity;

public class ActivityDetectionReport {

    private AuthWithFaceLivenessDetectMethodType activeMethod;

    private LocalDateTime timestamp;

    private String activeTag;

    private int requiredActionsNumber;

    private int activityVerificationTimeout;

    private float actionRecognitionProbabilityLevel;

    private boolean autoDetectionActive;

    private int autoDetectionTimeout;

    private List<ActivityRecognitionStatus> recognizedActivities;

    private LivenessDetectionStatus finalStatus;

    public ActivityDetectionReport() {
        recognizedActivities = Lists.newLinkedList();
    }

    public void setAutoDetectionActive(boolean autoDetectionActive) {
        this.autoDetectionActive = autoDetectionActive;
    }

    public void setAutoDetectionTimeout(int autoDetectionTimeout) {
        this.autoDetectionTimeout = autoDetectionTimeout;
    }

    public void setActiveTag(String activeTag) {
        this.activeTag = activeTag;
    }

    public void setActiveMethod(AuthWithFaceLivenessDetectMethodType activeMethod) {
        this.activeMethod = activeMethod;
    }

    public void setRequiredActionsNumber(int requiredActionsNumber) {
        this.requiredActionsNumber = requiredActionsNumber;
    }

    public void setActivityVerificationTimeout(int activityVerificationTimeout) {
        this.activityVerificationTimeout = activityVerificationTimeout;
    }

    public void setActionRecognitionProbabilityLevel(float actionRecognitionProbabilityLevel) {
        this.actionRecognitionProbabilityLevel = actionRecognitionProbabilityLevel;
    }

    public void setFinalStatus(LivenessDetectionStatus finalStatus) {
        this.finalStatus = finalStatus;
        this.timestamp = LocalDateTime.now();
    }

    public void addNewRecognizedAction(PossibleActivity activity, float diff) {
        recognizedActivities.add(new ActivityRecognitionStatus(activity, diff));
    }

    public JSONObject getJson() throws JSONException {
        final JSONObject jsonObject = new JSONObject();

        jsonObject.put("method", activeMethod != null ? activeMethod.toString() : null);
        jsonObject.put("timestamp", timestamp != null ? timestamp.toString() : null);
        jsonObject.put("tag", activeTag);
        jsonObject.put("activity_no", requiredActionsNumber);
        jsonObject.put("activity_timeout", activityVerificationTimeout);
        jsonObject.put("activity_threshold", actionRecognitionProbabilityLevel);
        jsonObject.put("autodetection", autoDetectionActive);
        jsonObject.put("autodetection_timeout", autoDetectionTimeout);
        final JSONArray recognizedActivitiesArray = new JSONArray();
        for (ActivityRecognitionStatus activity : recognizedActivities){
            final JSONObject activityJsonObject = new JSONObject();
            activityJsonObject.put("activity", activity.getActivity());
            activityJsonObject.put("diff", activity.getProbability());
            recognizedActivitiesArray.put((Object) activityJsonObject);
        }
        jsonObject.put("recognized_activities", (Object) recognizedActivitiesArray);
        jsonObject.put("status", finalStatus);
        return jsonObject;
    }

    public static class ActivityRecognitionStatus {

        private final PossibleActivity activity;

        private final float diff;

        public ActivityRecognitionStatus(PossibleActivity activity, float diff) {
            this.activity = activity;
            this.diff = diff;
        }

        public PossibleActivity getActivity() {
            return activity;
        }

        public float getProbability() {
            return diff;
        }
    }
}
