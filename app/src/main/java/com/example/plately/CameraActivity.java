package com.example.plately;

import static androidx.camera.core.ImageCaptureExtKt.takePicture;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private ImageButton captureBtn;
    private PreviewView preview;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private String URI_KEY = "URI_KEY";

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if(result){
                startCamera();
            }
        }
    });

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_camera);

        //Initialize up widgets
        captureBtn = findViewById(R.id.Camera_Capture_Btn);
        preview = findViewById(R.id.Camera_Preview);

        // Apply Window Insets (Safety for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMainCamera), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        //Permission helper
        //Permission for camera
        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) { //If permission was granted

                    }
                    else{
                        Toast.makeText(CameraActivity.this, "Camera needs proper permissions!", Toast.LENGTH_LONG).show();
                        //Immediately return
                        Intent resultIntent = new Intent();
                        setResult(RESULT_CANCELED, resultIntent);
                        finish();
                    }
                }
        );

        //Check for permissions
        //Check for camera permission
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Log.d("[Camera] Permissions", "Asking for camera perms");
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
           // requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100); //100 request code is arbitrary
        }
        //check for write permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            Log.d("[Camera] Permissions", "Asking for write ext  perms");
            //COMMENTED OUT, THE PERM LAUNCHER WASN'T PLAYING NICE
            //TURNS OUT, NOT REQUIRED??
            //requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE); //100 request code is arbitrary
        }

        startCamera();
    }

    //Could not figure out error with preview.getSurfaceProvided, so just suppressed it
    @SuppressLint("RestrictedApi")
    private void startCamera() {
        int aspectRatio = aspectRatio(preview.getWidth(), preview.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() ->{
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();

                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

                ImageCapture imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                captureBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        }
                        takePicture(imageCapture);
                    }
                });

                /*
                toggleFlash.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setFlashIcon(camera);
                    }
                });*/

                preview.setSurfaceProvider(preview.getSurfaceProvider());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

    }

    public void takePicture(ImageCapture imageCapture) {
        final File file = new File(getFilesDir(), System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();

        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //DO NOT TRUST THE GET PATH()
                        //Using getFilesDir(), the path is data/data/com.example.plately/files/~

                        Toast.makeText(CameraActivity.this, "Image saved at: " + file.getPath(), Toast.LENGTH_SHORT).show();
                        Log.d("[Camera] Camera: Write", "Image saved at: " + file.getPath() );
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("URI_KEY", Uri.fromFile(file).toString());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                });
                //startCamera();
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraActivity.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
                //startCamera();
            }
        });
    }

    private int aspectRatio(int width, int height){
        double ratio = (double)Math.max(width, height) / Math.min(width, height);
        if(Math.abs(ratio - 4.0/3.0) <= Math.abs(ratio - 16.0/9.0)){
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }
}
