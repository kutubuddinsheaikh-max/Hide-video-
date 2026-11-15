
package com.example.bgrecorder;

import android.app.*;
import android.content.Intent;
import android.hardware.camera2.*;
import android.media.MediaRecorder;
import android.os.*;
import android.view.Surface;
import androidx.core.app.NotificationCompat;
import java.io.File;
import java.util.Collections;

public class RecordingService extends Service {
    private CameraDevice cameraDevice;
    private MediaRecorder mediaRecorder;
    private PowerManager.WakeLock wakeLock;
    private boolean useFrontCamera = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            useFrontCamera = intent.getBooleanExtra("USE_FRONT", false);
        }

        stopRecordingResources(); // Agar pehle se chal raha ho toh safai se band karein

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (wakeLock == null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "App::VideoWakeLock");
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        String channelId = "rec_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Background Service", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        String camType = useFrontCamera ? "Selfie Camera" : "Back Camera";
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("System Service")
                .setContentText("Running (" + camType + ")...")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();

        startForeground(1, notification);
        startBackgroundRecording();
        return START_STICKY;
    }

    private String getCameraId(CameraManager manager, boolean front) throws CameraAccessException {
        int targetFacing = front ? CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;
        for (String id : manager.getCameraIdList()) {
            CameraCharacteristics chars = manager.getCameraCharacteristics(id);
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == targetFacing) {
                return id;
            }
        }
        return manager.getCameraIdList()[0];
    }

    private void startBackgroundRecording() {
        try {
            File privateDir = new File(getFilesDir(), "SavedVideos");
            if (!privateDir.exists()) privateDir.mkdirs();

            String prefix = useFrontCamera ? "SELFIE_" : "BACK_";
            File videoFile = new File(privateDir, prefix + "VID_" + System.currentTimeMillis() + ".mp4");

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
            mediaRecorder.setVideoEncodingBitRate(5000000);
            mediaRecorder.setVideoFrameRate(30);
            mediaRecorder.setVideoSize(1280, 720);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.prepare();

            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            String cameraId = getCameraId(manager, useFrontCamera);

            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    try {
                        Surface surface = mediaRecorder.getSurface();
                        CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                        builder.addTarget(surface);
                        cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                try {
                                    session.setRepeatingRequest(builder.build(), null, null);
                                    mediaRecorder.start();
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {}
                        }, null);
                    } catch (Exception e) { e.printStackTrace(); }
                }

                @Override
                public void onDisconnected(CameraDevice camera) { camera.close(); }

                @Override
                public void onError(CameraDevice camera, int error) { camera.close(); }
            }, null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopRecordingResources() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (Exception e) {
            mediaRecorder = null;
            cameraDevice = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecordingResources();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
