package pl.edu.agh.facelivenessdetection.processing;

import com.google.common.base.Objects;

public enum AuthWithFaceLivenessDetectMethodType {
    FACE_FLASHING_METHOD("face_flashing_method"),
    FACE_ACTIVITY_METHOD("face_activity_method");

    private static final AuthWithFaceLivenessDetectMethodType DEFAULT_METHOD = FACE_ACTIVITY_METHOD;

    private final String key;

    AuthWithFaceLivenessDetectMethodType(String key) {
        this.key = key;
    }

    public static AuthWithFaceLivenessDetectMethodType fromString(String key){
        for(AuthWithFaceLivenessDetectMethodType value : AuthWithFaceLivenessDetectMethodType.values()){
            if(Objects.equal(value.key, key)){
                return value;
            }
        }
        return DEFAULT_METHOD;
    }
}
