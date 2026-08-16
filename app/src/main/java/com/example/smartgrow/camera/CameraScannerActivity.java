package com.example.smartgrow.camera;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
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
import androidx.exifinterface.media.ExifInterface;

import com.example.smartgrow.R;
import com.example.smartgrow.plants.PlantDetailsActivity;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraScannerActivity extends AppCompatActivity {

    private static final String TAG = "CameraScannerActivity";
    public static final String EXTRA_IMAGE_PATH = "extra_scanned_image_path";
    public static final String TEMP_IMAGE_NAME = "temp_scanned_plant.jpg";

    private PreviewView viewFinder;
    private ScannerOverlayView overlayView;
    private ImageView ivFrozenPreview; // Freeze frame preview overlay

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;

    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isTorchOn = false;

    private RelativeLayout sliderContainer;
    private View sliderThumb;
    private ImageButton btnFlash;

    private ExecutorService cameraExecutor;

    // Custom Glassmorphism Dialog & Animation Controllers
    private Dialog loadingDialog;
    private TextView tvProgressPercentage;
    private TextView tvLoadingMessage;
    private ImageView flowerBottomLeft, flowerTopRight, flowerTopLeft, flowerBottomRight;
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable;
    private int progressCount = 0;
    private boolean isAnalyzing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraExecutor = Executors.newSingleThreadExecutor();

        try {
            setContentView(R.layout.activity_camera_scanner);
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate layout", e);
            Toast.makeText(this, "Camera initialization error", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        viewFinder = findViewById(R.id.viewFinder);
        overlayView = findViewById(R.id.scanner_overlay);
        ivFrozenPreview = findViewById(R.id.iv_frozen_preview);

        ImageButton btnClose = findViewById(R.id.btn_close_scanner);
        ImageButton btnHelp = findViewById(R.id.btn_help);
        btnFlash = findViewById(R.id.btn_flash);
        View btnCapture = findViewById(R.id.btn_capture_photo);
        ImageButton btnGallery = findViewById(R.id.btn_gallery_shortcut);

        sliderContainer = findViewById(R.id.layout_slider_container);
        sliderThumb = findViewById(R.id.slider_thumb);

        initCustomLoadingDialog();

        if (btnClose != null) btnClose.setOnClickListener(v -> finish());
        if (btnCapture != null) btnCapture.setOnClickListener(v -> {
            if (!isAnalyzing) takePhotoSafe();
        });
        if (btnGallery != null) {
            btnGallery.setOnClickListener(v -> {
                if (!isAnalyzing) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 1001);
                }
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

    private void initCustomLoadingDialog() {
        loadingDialog = new Dialog(this);
        loadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.dialog_analyzing_plant);
        loadingDialog.setCancelable(false);

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            loadingDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        tvProgressPercentage = loadingDialog.findViewById(R.id.tv_progress_percentage);
        tvLoadingMessage = loadingDialog.findViewById(R.id.tv_loading_message);

        flowerBottomLeft = loadingDialog.findViewById(R.id.flower_bottom_left);
        flowerTopRight = loadingDialog.findViewById(R.id.flower_top_right);
        flowerTopLeft = loadingDialog.findViewById(R.id.flower_top_left);
        flowerBottomRight = loadingDialog.findViewById(R.id.flower_bottom_right);
    }

    private void showLoading(boolean show) {
        isAnalyzing = show;
        if (loadingDialog == null) return;

        runOnUiThread(() -> {
            if (show) {
                if (!loadingDialog.isShowing() && !isFinishing()) {
                    progressCount = 0;
                    if (tvProgressPercentage != null) tvProgressPercentage.setText("0%");
                    if (tvLoadingMessage != null) tvLoadingMessage.setText("Scanning plant features...");

                    if (flowerBottomLeft != null) flowerBottomLeft.setVisibility(View.INVISIBLE);
                    if (flowerTopRight != null) flowerTopRight.setVisibility(View.INVISIBLE);
                    if (flowerTopLeft != null) flowerTopLeft.setVisibility(View.INVISIBLE);
                    if (flowerBottomRight != null) flowerBottomRight.setVisibility(View.INVISIBLE);

                    loadingDialog.show();
                    startLoadingProgressAnimation();
                }
            } else {
                if (loadingDialog.isShowing()) {
                    if (animationHandler != null && progressRunnable != null) {
                        animationHandler.removeCallbacks(progressRunnable);
                    }
                    loadingDialog.dismiss();
                }
            }
        });
    }

    /**
     * Display frozen image overlay & unbind/pause camera live feed.
     */
    private void freezeScreenWithBitmap(Bitmap bitmap) {
        runOnUiThread(() -> {
            if (ivFrozenPreview != null) {
                ivFrozenPreview.setImageBitmap(bitmap);
                ivFrozenPreview.setVisibility(View.VISIBLE);
            }
            if (overlayView != null) {
                overlayView.stopScanning();
            }
            // Stop live camera preview so it won't move behind the frozen image
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
            }
        });
    }

    /**
     * Unfreeze screen and restart live camera preview if scanning fails or restarts.
     */
    private void unfreezeScreen() {
        runOnUiThread(() -> {
            if (ivFrozenPreview != null) {
                ivFrozenPreview.setVisibility(View.GONE);
                ivFrozenPreview.setImageBitmap(null);
            }
            if (overlayView != null) {
                overlayView.startScanning();
            }
            bindCameraUseCases();
        });
    }

    private void startLoadingProgressAnimation() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (progressCount < 95) {
                    progressCount++;
                    if (tvProgressPercentage != null) {
                        tvProgressPercentage.setText(progressCount + "%");
                    }

                    if (progressCount == 20) {
                        if (tvLoadingMessage != null) tvLoadingMessage.setText("Analyzing leaf structure...");
                        animateBloom(flowerBottomLeft);
                    } else if (progressCount == 45) {
                        if (tvLoadingMessage != null) tvLoadingMessage.setText("Identifying species...");
                        animateBloom(flowerTopRight);
                    } else if (progressCount == 70) {
                        if (tvLoadingMessage != null) tvLoadingMessage.setText("Checking health condition...");
                        animateBloom(flowerTopLeft);
                    } else if (progressCount == 88) {
                        if (tvLoadingMessage != null) tvLoadingMessage.setText("Compiling plant guide...");
                        animateBloom(flowerBottomRight);
                    }

                    long delay = progressCount < 50 ? 50 : 80;
                    animationHandler.postDelayed(this, delay);
                }
            }
        };
        animationHandler.postDelayed(progressRunnable, 50);
    }

    private void animateBloom(ImageView flowerView) {
        if (flowerView == null) return;
        flowerView.setVisibility(View.VISIBLE);
        flowerView.setScaleX(0f);
        flowerView.setScaleY(0f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(flowerView, "scaleX", 0f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(flowerView, "scaleY", 0f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(400);
        set.setInterpolator(new OvershootInterpolator());
        set.start();
    }

    private void showSnapTipsDialog() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
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
                .setTargetRotation(viewFinder.getDisplay().getRotation())
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

        showLoading(true);

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
                runOnUiThread(() -> {
                    showLoading(false);
                    unfreezeScreen();
                    if (!isFinishing() && !isDestroyed()) {
                        Toast.makeText(CameraScannerActivity.this, "Capture failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void processSavedImage(File file) {
        try {
            int exifRotation = getExifRotationDegrees(file.getAbsolutePath());

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            int maxSize = 1024;
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap != null) {
                Bitmap processed = resizeAndRotatePrecise(bitmap, exifRotation);

                // Freeze live preview immediately
                freezeScreenWithBitmap(processed);

                try (FileOutputStream fos = openFileOutput(TEMP_IMAGE_NAME, Context.MODE_PRIVATE)) {
                    processed.compress(Bitmap.CompressFormat.JPEG, 85, fos);

                    analyzeAndOpenDetails(processed);

                    if (processed != bitmap) {
                        bitmap.recycle();
                    }
                    if (file.exists()) file.delete();
                    return;
                }
            }
            if (file.exists()) file.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error processing saved image", e);
        }

        runOnUiThread(() -> {
            showLoading(false);
            unfreezeScreen();
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(CameraScannerActivity.this, "Unable to process captured photo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void analyzeAndOpenDetails(Bitmap bitmap) {
        showLoading(true);

        PlantAnalyzer analyzer = new PlantAnalyzer();
        analyzer.analyzePlantDetailed(bitmap, new PlantAnalyzer.PlantAnalysisCallback() {
            @Override
            public void onSuccess(String formattedResult, String rawJson) {
                runOnUiThread(() -> {
                    showLoading(false);

                    if (rawJson == null || rawJson.isEmpty() || rawJson.contains("\"is_plant\": false") || rawJson.contains("\"is_plant\":false")) {
                        unfreezeScreen();
                        Toast.makeText(CameraScannerActivity.this,
                                "The image does not appear to contain a plant. Please scan a clear plant image.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    PlantDetailsActivity.tempScannedBitmap = bitmap;

                    Intent intent = new Intent(CameraScannerActivity.this, PlantDetailsActivity.class);
                    intent.putExtra("raw_ai_json", rawJson);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    unfreezeScreen();
                    if (error.equals(PlantAnalyzer.ERROR_NON_PLANT)) {
                        Toast.makeText(CameraScannerActivity.this,
                                "The image does not appear to contain a plant. Please scan a clear plant image.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(CameraScannerActivity.this, "Analysis Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private int getExifRotationDegrees(String filePath) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read EXIF orientation", e);
            return 0;
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
                // Show custom loading dialog immediately
                showLoading(true);

                cameraExecutor.execute(() -> {
                    try {
                        Bitmap bitmap;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                            bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                                decoder.setTargetColorSpace(android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB));
                                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                            });
                        } else {
                            try (InputStream is = getContentResolver().openInputStream(uri)) {
                                bitmap = BitmapFactory.decodeStream(is);
                            }
                        }
                        if (bitmap != null) {
                            processAndSaveGalleryImage(bitmap);
                        } else {
                            runOnUiThread(() -> {
                                showLoading(false);
                                Toast.makeText(CameraScannerActivity.this, "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Gallery image loading failed", e);
                        runOnUiThread(() -> {
                            showLoading(false);
                            if (!isFinishing() && !isDestroyed()) {
                                Toast.makeText(CameraScannerActivity.this, "Failed to load selected image", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        }
    }

    private void processAndSaveGalleryImage(Bitmap rawBitmap) {
        Bitmap processed = resizeAndRotatePrecise(rawBitmap, 0);

        // Paste/Freeze image onto the screen instantly and stop camera movements
        freezeScreenWithBitmap(processed);

        try (FileOutputStream fos = openFileOutput(TEMP_IMAGE_NAME, Context.MODE_PRIVATE)) {
            processed.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            analyzeAndOpenDetails(processed);
            if (processed != rawBitmap) rawBitmap.recycle();
        } catch (IOException e) {
            Log.e(TAG, "Gallery image processing failed", e);
            runOnUiThread(() -> {
                showLoading(false);
                unfreezeScreen();
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(CameraScannerActivity.this, "Failed to process gallery image", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ivFrozenPreview == null || ivFrozenPreview.getVisibility() != View.VISIBLE) {
            if (overlayView != null) overlayView.startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (overlayView != null) overlayView.stopScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        if (animationHandler != null && progressRunnable != null) {
            animationHandler.removeCallbacks(progressRunnable);
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}