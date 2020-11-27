package pl.edu.agh.facelivenessdetection.utils;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public final class PermissionManager {

    private static final String TAG = "PermissionManager";

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;

    public static String[] getRequiredPermissions(Activity activity) {
        try {
            PackageInfo info =
                    activity.getPackageManager()
                            .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    public static boolean allPermissionsGranted(Activity activity) {
        for (String permission : getRequiredPermissions(activity)) {
            if (!isPermissionGranted(activity, permission)) {
                return false;
            }
        }
        return true;
    }

    public static void getRuntimePermissions(Activity activity) {
        List<String> allNeededPermissions = new ArrayList<>();
        for (String permission : getRequiredPermissions(activity)) {
            if (!isPermissionGranted(activity, permission)) {
                allNeededPermissions.add(permission);
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    activity, allNeededPermissions.toArray(new String[0]), REQUEST_CAMERA_PERMISSION_CODE);
        }
    }

    public static boolean isPermissionGranted(Activity activity, String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted: " + permission);
            return true;
        }
        Log.i(TAG, "Permission NOT granted: " + permission);
        return false;
    }

    private PermissionManager() {
    }
}
