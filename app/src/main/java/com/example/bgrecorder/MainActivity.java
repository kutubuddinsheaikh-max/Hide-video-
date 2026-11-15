package com.example.bgrecorder;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.*;

public class MainActivity extends AppCompatActivity {
    private boolean isFrontCamera = false;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 100, 60, 60);

        Button btnSwitchCam = new Button(this);
        btnSwitchCam.setText("Camera: BACK (Switch karne ke liye dabayein)");

        Button btnStart = new Button(this);
        btnStart.setText("Start Background Recording");

        Button btnStop = new Button(this);
        btnStop.setText("Stop Recording");

        Button btnExport = new Button(this);
        btnExport.setText("Latest Video Gallery Mein Bhejein");

        layout.addView(btnSwitchCam);
        layout.addView(btnStart);
        layout.addView(btnStop);
        layout.addView(btnExport);
        setContentView(layout);

        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 101);
        }

        btnSwitchCam.setOnClickListener(v -> {
            isFrontCamera = !isFrontCamera;
            if (isFrontCamera) {
                btnSwitchCam.setText("Camera: SELFIE (Switch karne ke liye dabayein)");
                Toast.makeText(this, "Selfie Camera Selected", Toast.LENGTH_SHORT).show();
            } else {
                btnSwitchCam.setText("Camera: BACK (Switch karne ke liye dabayein)");
                Toast.makeText(this, "Back Camera Selected", Toast.LENGTH_SHORT).show();
            }

            if (isRecording) {
                Intent serviceIntent = new Intent(this, RecordingService.class);
                serviceIntent.putExtra("USE_FRONT", isFrontCamera);
                ContextCompat.startForegroundService(this, serviceIntent);
            }
        });

        btnStart.setOnClickListener(v -> {
            isRecording = true;
            Intent serviceIntent = new Intent(this, RecordingService.class);
            serviceIntent.putExtra("USE_FRONT", isFrontCamera);
            ContextCompat.startForegroundService(this, serviceIntent);
            String camName = isFrontCamera ? "Selfie Camera" : "Back Camera";
            Toast.makeText(this, camName + " Recording Shuru!", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            isRecording = false;
            stopService(new Intent(this, RecordingService.class));
            Toast.makeText(this, "Recording Band Aur Save Ho Gayi!", Toast.LENGTH_SHORT).show();
        });

        btnExport.setOnClickListener(v -> exportLatestVideoToGallery());
    }

    private void exportLatestVideoToGallery() {
        File privateDir = new File(getFilesDir(), "SavedVideos");
        File[] files = privateDir.listFiles();
        if (files == null || files.length == 0) {
            Toast.makeText(this, "Koi video nahi mili!", Toast.LENGTH_SHORT).show();
            return;
        }

        File latestFile = files[files.length - 1];
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, latestFile.getName());
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/MyHiddenVideos");

        ContentResolver resolver = getContentResolver();
        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        try (InputStream in = new FileInputStream(latestFile);
             OutputStream out = resolver.openOutputStream(uri)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            Toast.makeText(this, "Gallery mein save ho gayi!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
