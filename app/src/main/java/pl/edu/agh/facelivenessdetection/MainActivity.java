package pl.edu.agh.facelivenessdetection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.annotation.KeepName;
import com.google.common.collect.Lists;

import org.opencv.android.OpenCVLoader;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import pl.edu.agh.facelivenessdetection.controller.CameraManager;
import pl.edu.agh.facelivenessdetection.handler.FlashHandler;
import pl.edu.agh.facelivenessdetection.handler.LoggingHandler;
import pl.edu.agh.facelivenessdetection.persistence.PersistenceManager;
import pl.edu.agh.facelivenessdetection.processing.AuthWithFaceLivenessDetectMethodType;
import pl.edu.agh.facelivenessdetection.utils.PermissionManager;
import pl.edu.agh.facelivenessdetection.visualisation.GraphicOverlay;

import pl.edu.agh.facelivenessdetection.handler.StatusChangeHandler;
import pl.edu.agh.facelivenessdetection.model.LivenessDetectionStatus;
import pl.edu.agh.facelivenessdetection.visualisation.DetectionVisualizer;

@KeepName
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements DetectionVisualizer {
    /*
     *  Constants definition
     */
    private static final String TAG = "LivePreviewMainActivity";

    private TextView livenessStatusTextView;

    private ListView loggingList;

    private PreviewView previewView;

    private ImageView flashView;

    private GraphicOverlay graphicOverlay;

    private Button button;

    private final StatusChangeHandler statusChangeHandler;

    private final LoggingHandler loggingHandler;

    private final FlashHandler flashHandler;

    private final List<String> logs;

    private ArrayAdapter<String> adapter;

    private final Lock loggingLock;

    @Nullable
    private CameraManager cameraManager;

    @Nullable
    private PersistenceManager persistenceManager;

    public MainActivity() {
        statusChangeHandler = new StatusChangeHandler(this);
        loggingHandler = new LoggingHandler(this);
        flashHandler = new FlashHandler(this);
        logs = Lists.newArrayList();
        loggingLock = new ReentrantLock();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        livenessStatusTextView = findViewById(R.id.statusView);
        loggingList = findViewById(R.id.logger_list);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logs);
        loggingList.setAdapter(adapter);

        previewView = findViewById(R.id.preview_view);
        if (previewView == null) {
            Log.d(TAG, "Preview is null");
        }

        flashView = findViewById(R.id.flashView);
        if(flashView == null){
            Log.d(TAG, "flashView is null");
        }

        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        button = findViewById(R.id.button);

        if (!PermissionManager.allPermissionsGranted(this)) {
            PermissionManager.getRuntimePermissions(this);
        }

        createCameraManager();
        createPersistenceManager();
        setUpViewModelProvider();

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV");
        } else {
            Log.d("OpenCV", "OpenCV loaded");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                final Intent settingsStartIntent = new Intent(MainActivity.this, SettingsActivity.class);
                MainActivity.this.startActivity(settingsStartIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void startFrontFlashEmulator() {
        if(!Settings.System.canWrite(getApplicationContext())){
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
        ScreenBrightness(255,getApplicationContext());
        setButton("Take flash", Color.RED);
    }

    public void setButton(String text, int color) {
        button.setBackgroundColor(color);
        button.setText(text);
    }

    public void changeButton(String text, int color) {
        final Message msg = flashHandler.obtainMessage();
        final Bundle b = new Bundle();
        b.putString("BUTTON_TEXT", text);
        b.putString("BUTTON_COLOR", String.valueOf(color));
        msg.setData(b);
        flashHandler.sendMessage(msg);
    }

    private boolean ScreenBrightness(int level, Context context) {
        try {
            android.provider.Settings.System.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS, level);
            android.provider.Settings.System.putInt(context.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            android.provider.Settings.System.putInt(
                    context.getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS,
                    level);
            return true;
        }

        catch (Exception e) {
            Log.e("Screen Brightness", "error changing screen brightness");
            e.printStackTrace();
            return false;
        }
    }

    public void stopFrontFlashEmulator() {
        ScreenBrightness(0,getApplicationContext());
        setButton("Take back", Color.GREEN);
    }

    public void setFlashStatus(String status) {
        final Message msg = flashHandler.obtainMessage();
        final Bundle b = new Bundle();
        b.putString("STATUS", status);
        msg.setData(b);
        flashHandler.sendMessage(msg);
    }

    private void createCameraManager() {
        cameraManager = new CameraManager(this, previewView, this, graphicOverlay);
    }

    private void createPersistenceManager(){
        persistenceManager = new PersistenceManager(this);
    }

    private void setUpViewModelProvider() {
        if (PermissionManager.allPermissionsGranted(this) && cameraManager != null) {
            setActiveFaceDetectionMethod(loadActiveMethodFromPreferences());
            cameraManager.startCamera();
        }
    }

    public void onDetectButtonClick(View view) {
        clearInfo();
        Objects.requireNonNull(cameraManager).performFaceLivenessDetectionTrigger(this);
    }

    private void setActiveFaceDetectionMethod(AuthWithFaceLivenessDetectMethodType activeFaceDetectionMethod) {
        Objects.requireNonNull(cameraManager).changeAnalyzer(activeFaceDetectionMethod);
    }

    private AuthWithFaceLivenessDetectMethodType loadActiveMethodFromPreferences() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final String activeMethodStr = prefs.getString("active_method", "face_activity_method");
        return AuthWithFaceLivenessDetectMethodType.fromString(activeMethodStr);
    }

    public void addLoggingMessage(String message){
        logs.add(message);
        adapter.notifyDataSetChanged();
    }

    public void clearLogger(){
        logs.clear();
        adapter.notifyDataSetChanged();
    }

    public void setDetectionStatus(LivenessDetectionStatus status) {
        livenessStatusTextView.setText(getStatusLabel(status));
        livenessStatusTextView.setBackgroundColor(status.getColor());
    }

    private String getStatusLabel(LivenessDetectionStatus status) {
        switch (status) {
            case FAKE:
                return getString(R.string.fake_status_label);
            case REAL:
                return getString(R.string.real_status_label);
            default:
                return getString(R.string.unknown_status_label);
        }
    }

    @Override
    public void visualizeStatus(LivenessDetectionStatus status) {
        final Message msg = statusChangeHandler.obtainMessage();
        final Bundle b = new Bundle();
        b.putString(StatusChangeHandler.STATUS_KEY, status.getKey());
        msg.setData(b);
        statusChangeHandler.sendMessage(msg);
    }

    @Override
    public void logInfo(String message) {
        final Message msg = loggingHandler.obtainMessage();
        final Bundle b = new Bundle();
        b.putString(LoggingHandler.LOG_MESSAGE_KEY, message);
        msg.setData(b);
        loggingHandler.sendMessage(msg);
    }

    @Override
    public void clearInfo() {
        final Message msg = loggingHandler.obtainMessage();
        final Bundle b = new Bundle();
        b.putBoolean(LoggingHandler.CLEAR_KEY, true);
        msg.setData(b);
        loggingHandler.sendMessage(msg);
    }

    @Override
    public void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        clearInfo();
        setActiveFaceDetectionMethod(loadActiveMethodFromPreferences());
        setDetectionStatus(LivenessDetectionStatus.UNKNOWN);
        if (cameraManager != null) {
            cameraManager.startCamera();
        } else {
            Log.e(TAG, "Camera is unavailable");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Objects.requireNonNull(cameraManager).stopCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Objects.requireNonNull(cameraManager).stopCamera();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "Permission granted!");
        if (PermissionManager.allPermissionsGranted(this) && cameraManager != null) {
            cameraManager.startCamera();
        } else {
            Log.e(TAG, "Camera start error");
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}