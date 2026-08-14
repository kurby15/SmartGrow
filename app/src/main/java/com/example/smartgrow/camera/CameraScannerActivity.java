package com.example.smartgrow.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.smartgrow.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraScannerActivity extends AppCompatActivity {

    private static final String TAG = "CameraScannerActivity";
    public static final String EXTRA_IMAGE_PATH = "extra_scanned_image_path";
    private static final String TEMP_IMAGE_NAME = "temp_scanned_plant.jpg";

    private PreviewView viewFinder;
    private ScannerOverlayView overlayView;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;

    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isTorchOn = false;

    private RelativeLayout sliderContainer;
    private View sliderThumb;
    private ImageButton btnFlash;

    private ExecutorService cameraExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            // Ensure the layout exists and the ScannerOverlayView package is correct in XML
            setContentView(R.layout.activity_camera_scanner);
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate layout", e);
            Toast.makeText(this, "Camera initialization error", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewFinder = findViewById(R.id.viewFinder);
        overlayView = findViewById(R.id.scanner_overlay);

        ImageButton btnClose = findViewById(R.id.btn_close_scanner);
        ImageButton btnHelp = findViewById(R.id.btn_help);
        btnFlash = findViewById(R.id.btn_flash);
        View btnCapture = findViewById(R.id.btn_capture_photo);
        ImageButton btnGallery = findViewById(R.id.btn_gallery_shortcut);

        sliderContainer = findViewById(R.id.layout_slider_container);
        sliderThumb = findViewById(R.id.slider_thumb);

        if (btnClose != null) btnClose.setOnClickListener(v -> finish());
        if (btnCapture != null) btnCapture.setOnClickListener(v -> takePhotoSafe());
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 1001);
            });
        }

        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> showSnapTipsDialog());
        }

        if (btnFlash != null) {
            btnFlash.setOnClickListener(v -> toggleTorch());
        }

        setupZoomSliderTouchListener();
        startCameraX();
    }

    private void showSnapTipsDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_snap_tips);
        View btnContinue = dialog.findViewById(R.id.btn_continue_tips);
        if (btnContinue != null) btnContinue.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void startCameraX() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "CameraProvider initialization failed", e);
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                camera.getCameraControl().enableTorch(isTorchOn);
            }
        } catch (Exception e) {
            Log.e(TAG, "Binding camera use cases failed", e);
            Toast.makeText(this, "Failed to bind camera", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePhotoSafe() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to temporary file to avoid keeping large raw bitmaps in memory
        File photoFile = new File(getFilesDir(), "raw_capture.jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                processSavedImage(photoFile);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed", exception);
                runOnUiThread(() -> Toast.makeText(CameraScannerActivity.this, "Capture failed", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void processSavedImage(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            // Sub-sample image to save memory during decoding
            int maxSize = 1024;
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                // Correct orientation and scale precisely
                Bitmap processed = resizeAndRotatePrecise(bitmap, 0);
                
                try (FileOutputStream fos = openFileOutput(TEMP_IMAGE_NAME, Context.MODE_PRIVATE)) {
                    processed.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    
                    // Cleanup memory immediately
                    if (processed != bitmap) {
                        processed.recycle();
                    }
                    bitmap.recycle();

                    runOnUiThread(() -> {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra(EXTRA_IMAGE_PATH, TEMP_IMAGE_NAME);
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    });
                }
            }
            if (file.exists()) file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error processing saved image", e);
            runOnUiThread(() -> finish());
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap resizeAndRotatePrecise(Bitmap source, int rotation) {
        Matrix matrix = new Matrix();
        if (rotation != 0) matrix.postRotate(rotation);
        
        float scale = Math.min((float) 1024 / source.getWidth(), (float) 1024 / source.getHeight());
        if (scale < 1.0f) matrix.postScale(scale, scale);
        
        if (matrix.isIdentity()) return source;
        
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void toggleTorch() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isTorchOn = !isTorchOn;
            camera.getCameraControl().enableTorch(isTorchOn);
            btnFlash.setBackgroundResource(isTorchOn ? R.drawable.bg_circle_yellow : R.drawable.bg_circle_translucent);
            btnFlash.setColorFilter(isTorchOn ? 0xFF000000 : 0xFFFFFFFF);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupZoomSliderTouchListener() {
        if (sliderContainer == null) return;
        sliderContainer.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                float y = Math.max(0, Math.min(event.getY(), sliderContainer.getHeight()));
                if (sliderThumb != null) sliderThumb.setY(y - (sliderThumb.getHeight() / 2f));
                if (camera != null) camera.getCameraControl().setLinearZoom(1.0f - (y / sliderContainer.getHeight()));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                cameraExecutor.execute(() -> {
                    try {
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
                        } else {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        }
                        if (bitmap != null) {
                            processAndSaveGalleryImage(bitmap);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Gallery image loading failed", e);
                    }
                });
            }
        }
    }

    private void processAndSaveGalleryImage(Bitmap rawBitmap) {
        Bitmap processed = resizeAndRotatePrecise(rawBitmap, 0);
        try (FileOutputStream fos = openFileOutput(TEMP_IMAGE_NAME, Context.MODE_PRIVATE)) {
            processed.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            if (processed != rawBitmap) processed.recycle();
            rawBitmap.recycle();
            runOnUiThread(() -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_IMAGE_PATH, TEMP_IMAGE_NAME);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            });
        } catch (IOException e) {
            Log.e(TAG, "Gallery image processing failed", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (overlayView != null) overlayView.startScanning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (overlayView != null) overlayView.stopScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}
