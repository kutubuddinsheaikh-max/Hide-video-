package com.example.bgrecorder;

import android.Manifest;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.*;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        }, 100);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        Button btnStart = new Button(this);
        btnStart.setText("Start Background Recording");

        Button btnStop = new Button(this);
        btnStop.setText("Stop Recording");

        Button btnExport = new Button(this);
        btnExport.setText("Latest Video Gallery Mein Bhejein");

        layout.addView(btnStart);
        layout.addView(btnStop);
        layout.addView(btnExport);
        setContentView(layout);

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, RecordingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            Toast.makeText(this, "Recording Shuru! Ab screen lock kar sakte hain.", Toast.LENGTH_LONG).show();
        });

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, RecordingService.class));
            Toast.makeText(this, "Recording Ruk Gayi aur App mein Save ho gayi.", Toast.LENGTH_SHORT).show();
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

        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Video.Media.DISPLAY_NAME, latestFile.getName());
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SafeRecorder");

            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (InputStream in = new FileInputStream(latestFile);
                     OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
                Toast.makeText(this, "Video Gallery mein save ho gayi!", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
