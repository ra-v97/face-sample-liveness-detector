package agh.edu.pl.facelivenessdetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import agh.edu.pl.facelivenessdetection.detector.FaceLivenessDetectorType;
import agh.edu.pl.facelivenessdetection.model.FaceLivenessDetectionModel;
import agh.edu.pl.facelivenessdetection.model.MobileModel;

public class MainActivity extends AppCompatActivity implements MobileModel {
    /*
     *  Constants definition
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;
    /*
     * UI Elements
     */
    private RadioButton method1RadioButton;

    private RadioButton method2RadioButton;

    private TextView livenessStatusTextView;

    private TextureView cameraPreviewTextureView;
    /*
     * Required elements
     */
    private final FaceLivenessDetectionModel faceLivenessDetectionModel;

    private final BiMap<FaceLivenessDetectorType, RadioButton> faceLivenessDetectorRadioButtonBiMap;

    public MainActivity() {
        faceLivenessDetectionModel = new FaceLivenessDetectionModel();

        faceLivenessDetectorRadioButtonBiMap = HashBiMap.create();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreviewTextureView = findViewById(R.id.texture);
        method1RadioButton = findViewById(R.id.radioButtonMethod1);
        method2RadioButton = findViewById(R.id.radioButtonMethod2);
        livenessStatusTextView = findViewById(R.id.statusView);

        initializeViewElements();
        setActiveFaceDetectionMethod(FaceLivenessDetectorType.FACE_ACTIVITY_METHOD);
    }

    private void initializeViewElements(){
        faceLivenessDetectorRadioButtonBiMap.put(FaceLivenessDetectorType.FACE_ACTIVITY_METHOD,
                method1RadioButton);
        faceLivenessDetectorRadioButtonBiMap.put(FaceLivenessDetectorType.FACE_FLASHING_METHOD,
                method2RadioButton);
    }

    public void onDetectButtonClick(View view) {
        faceLivenessDetectionModel.resolveActiveFaceLivenessDetector()
                .map(faceLivenessDetector -> faceLivenessDetector.detect(this))
                .ifPresent(executor::execute);
    }

    public void onToggleMethod1(View view) {
        setActiveFaceDetectionMethod(method1RadioButton);
    }

    public void onToggleMethod2(View view) {
        setActiveFaceDetectionMethod(method2RadioButton);
    }

    private void setActiveFaceDetectionMethod(FaceLivenessDetectorType activeFaceDetectionMethod) {
        faceLivenessDetectionModel
                .setActiveFaceLivenessDetector(activeFaceDetectionMethod.getDetector());
        Optional.ofNullable(faceLivenessDetectorRadioButtonBiMap.get(activeFaceDetectionMethod))
                .ifPresent(RadioButton::toggle);
    }

    private void setActiveFaceDetectionMethod(RadioButton methodRadioButton) {
        Optional.ofNullable(faceLivenessDetectorRadioButtonBiMap.inverse().get(methodRadioButton))
                .ifPresent(livenessDetector -> faceLivenessDetectionModel
                        .setActiveFaceLivenessDetector(livenessDetector.getDetector()));
        changeStatusToUnknown();
    }
    
    private Size largest;
    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private int count = 0;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private BlockingQueue<Bitmap> photoQueue = new LinkedBlockingQueue<Bitmap>();

    Executor executor = new ThreadPerTaskExecutor();

    class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> sActivity;

        MyHandler(MainActivity activity) {
            sActivity = new WeakReference<MainActivity>(activity);
        }

        public void handleMessage(Message msg) {
            MainActivity activity = sActivity.get();
            String toggle = msg.getData().getString("STATUS");
            String command = msg.getData().getString("CMD");

            if (toggle != null) {
                if (toggle.equals("REAL")) {
                    activity.changeStatusToReal();
                } else if (toggle.equals("FAKE")) {
                    activity.changeStatusToFake();
                } else {
                    activity.changeStatusToUnknown();
                }
            }

            if (command != null && command.equals("TAKE_PHOTO")) {
                activity.takeSnapshot();
            }

        }
    }

    Handler myHandler = new MyHandler(this);

    @Override
    public Bitmap getPhoto() {
        try {
            takePhoto();
            return photoQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (cameraPreviewTextureView.isAvailable()) {
            openCamera(cameraPreviewTextureView.getWidth(), cameraPreviewTextureView.getHeight());
        } else {
            cameraPreviewTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "ERROR: Camera permissions not granted", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    @Override
    public void setFake() {
        Message msg = myHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("STATUS", "FAKE");
        msg.setData(b);
        myHandler.sendMessage(msg);
    }

    void changeStatusToFake() {
        livenessStatusTextView.setText("FAKE");
        livenessStatusTextView.setBackgroundColor(Color.parseColor("#FF0000"));
    }

    @Override
    public void setReal() {
        Message msg = myHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("STATUS", "REAL");
        msg.setData(b);
        myHandler.sendMessage(msg);
    }

    void changeStatusToReal() {
        livenessStatusTextView.setText("REAL");
        livenessStatusTextView.setBackgroundColor(Color.parseColor("#008000"));
    }

    void changeStatusToUnknown() {
        livenessStatusTextView.setText("UNKNOWN");
        livenessStatusTextView.setBackgroundColor(Color.parseColor("#FFFF00"));
    }

    public void takePhoto() {
        Message msg = myHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("CMD", "TAKE_PHOTO");
        msg.setData(b);
        myHandler.sendMessage(msg);
    }

    public void takeSnapshot() {
        try {

            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            mCaptureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {

            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();

            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void setUpImageReader() {
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                ImageFormat.JPEG, /*maxImages*/2);
        mImageReader.setOnImageAvailableListener(
                null, mBackgroundHandler);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    buffer.rewind();
                    byte[] data = new byte[buffer.capacity()];
                    buffer.get(data);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    photoQueue.put(bitmap);
                    //use your bitmap
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (image != null) {
                    image.close();
                }
            }
        }, mBackgroundHandler);
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing != CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                setUpImageReader();

                Point displaySize = new Point();
                getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Toast.makeText(MainActivity.this, "Camera2 API not supported on this device", Toast.LENGTH_LONG).show();
        }
    }


    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = cameraPreviewTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        null, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage("R string request permission")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();

                                }
                            })
                    .create();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e("Camera2", "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == cameraPreviewTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        cameraPreviewTextureView.setTransform(matrix);
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            finish();
        }

    };

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}