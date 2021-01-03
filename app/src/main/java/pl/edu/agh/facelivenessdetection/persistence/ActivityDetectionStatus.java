package pl.edu.agh.facelivenessdetection.persistence;

import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.common.collect.Lists;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.List;

import pl.edu.agh.facelivenessdetection.processing.AuthWithFaceLivenessDetectMethodType;
import pl.edu.agh.facelivenessdetection.processing.liveness.activity.FaceState;
import pl.edu.agh.facelivenessdetection.processing.liveness.activity.PossibleActivity;

public class ActivityDetectionStatus {

    private AuthWithFaceLivenessDetectMethodType activeMethod;

    private int requiredActionsNumber;

    private int activityVerificationTimeout;

    private float actionRecognitionProbabilityLevel;

    private List<ActivityRecognitionStatus> lastRecognizedActionsSequence;

    private List<FaceState> allFaceStates;

    private ActivityDetectionStatus finalStatus;

    private LocalDateTime timestamp;

    private String activeTag;

    public ActivityDetectionStatus() {
        allFaceStates = Lists.newLinkedList();
        lastRecognizedActionsSequence = Lists.newLinkedList();
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

    public void addFaceState(FaceState faceState) {
        allFaceStates.add(faceState);
    }

    public void setFinalStatus(ActivityDetectionStatus finalStatus) {
        this.finalStatus = finalStatus;
        this.timestamp = LocalDateTime.now();
    }

    public void addNewRecognizedAction(ActivityRecognitionStatus action) {
        if (lastRecognizedActionsSequence.size() > requiredActionsNumber) {
            lastRecognizedActionsSequence.remove(0);
        }
        lastRecognizedActionsSequence.add(action);
    }

    public JSONObject getJson() throws JSONException {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("timestamp", timestamp != null ? timestamp.toString() : null);
        jsonObject.put("activeMethod", activeMethod != null ? activeMethod.toString() : null);
        jsonObject.put("activeTag", activeTag);
        jsonObject.put("requiredActionsNumber", requiredActionsNumber);
        jsonObject.put("activityVerificationTimeout", activityVerificationTimeout);
        jsonObject.put("actionRecognitionProbabilityLevel", actionRecognitionProbabilityLevel);

        final JSONArray lastRecognizedActionsSequenceArray = new JSONArray();
        jsonObject.put("lastRecognizedActionsSequenceArray", (Object) lastRecognizedActionsSequenceArray);
        final JSONArray allFaceStatesArray = new JSONArray();
        jsonObject.put("allFaceStates", (Object) allFaceStatesArray);
        jsonObject.put("finalStatus", finalStatus);
        return jsonObject;
    }

    public static class ActivityRecognitionStatus {

        private final PossibleActivity activity;

        private final float probability;

        public ActivityRecognitionStatus(PossibleActivity activity, float probability) {
            this.activity = activity;
            this.probability = probability;
        }

        public PossibleActivity getActivity() {
            return activity;
        }

        public float getProbability() {
            return probability;
        }
    }
}
