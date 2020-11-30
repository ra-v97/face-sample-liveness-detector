package pl.edu.agh.facelivenessdetection.processing.liveness.activity;

enum PossibleActivity {
    TURN_HEAD_LEFT, TURN_HEAD_RIGHT, SMILE, BLINK;

    static String name(PossibleActivity activity) {
        switch (activity){
            case BLINK: return "blink";
            case TURN_HEAD_LEFT: return "turn head left";
            case TURN_HEAD_RIGHT: return "turn head right";
            case SMILE: return "smile";
        }
        throw new IllegalStateException("Unrecognized activity " + activity);
    }
}
