package com.example.rpb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG ="MSG:";
    private final int CODE = 1010;
    private final String[] RequiredPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private Button cameraActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraActivity = findViewById(R.id.openCameraActivity);

        if(allPermissionsGranted()){
            Log.d("TAG", "Permissions Granted");
        } else {
            ActivityCompat.requestPermissions(this,RequiredPermissions, CODE);
        }
        cameraActivity.setOnClickListener(v -> {
            Intent cameraIntent = new Intent(this, CameraActivity.class);
            startActivity(cameraIntent);
        });

    }
    
    private boolean allPermissionsGranted(){
        for(String permissions : RequiredPermissions){
            if(ContextCompat.checkSelfPermission(this,permissions) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE) {
            if (allPermissionsGranted()) {
                Toast.makeText(this, "Access granted.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Permissions Denied.", Toast.LENGTH_LONG).show();
                Intent permIntent = new Intent();
                permIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package",getPackageName(),null);
                permIntent.setData(uri);
                startActivity(permIntent);
            }
        }
    }

}