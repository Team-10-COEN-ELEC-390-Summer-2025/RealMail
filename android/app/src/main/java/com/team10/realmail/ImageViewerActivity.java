package com.team10.realmail;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.team10.realmail.api.YoloDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageViewerActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvTimestamp;
    private TextView tvStatus;
    private TextView tvImageCounter;
    private TextView tvAiStatus;
    private Button btnPrev;
    private Button btnNext;
    private Button btnClose;
    private Button btnAiDetection;
    private ProgressBar progressBar;

    private String folderName;
    private String timestamp;
    private List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;
    private YoloDetector yoloDetector;
    private Bitmap currentBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        initViews();

        // Get data from intent
        folderName = getIntent().getStringExtra("folderName");
        timestamp = getIntent().getStringExtra("timestamp");

        setupUI();
        initializeYolo();
        loadImagesFromFirebase();
    }

    private void initViews() {
        imageView = findViewById(R.id.image_view);
        tvTimestamp = findViewById(R.id.tv_timestamp);
        tvStatus = findViewById(R.id.tv_status);
        tvImageCounter = findViewById(R.id.tv_image_counter);
        tvAiStatus = findViewById(R.id.tv_ai_status);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnClose = findViewById(R.id.btn_close);
        btnAiDetection = findViewById(R.id.btn_ai_detection);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupUI() {
        // Format and display timestamp
        String formattedTime = formatTimestamp(timestamp);
        tvTimestamp.setText("Images from " + formattedTime);

        // Set up click listeners
        btnClose.setOnClickListener(v -> finish());
        btnPrev.setOnClickListener(v -> showPreviousImage());
        btnNext.setOnClickListener(v -> showNextImage());
        btnAiDetection.setOnClickListener(v -> performYoloDetection());
    }

    private String formatTimestamp(String timestamp) {
        return DateFormatter.formatDateForNotification(timestamp);
    }

    private void initializeYolo() {
        try {
            yoloDetector = new YoloDetector(this);
            Log.d("ImageViewer", "YOLO detector initialized successfully");
        } catch (IOException e) {
            Log.e("ImageViewer", "Failed to initialize YOLO detector", e);
            btnAiDetection.setText("❌ AI Detection Unavailable");
        }
    }

    private void loadImagesFromFirebase() {
        if (folderName == null || folderName.isEmpty()) {
            showStatus("❌ No folder specified");
            return;
        }

        showLoading(true);
        showStatus("Loading images...");

        StorageReference folderRef = FirebaseStorage.getInstance().getReference().child(folderName);

        folderRef.listAll().addOnSuccessListener(listResult -> {
            if (listResult.getItems().isEmpty()) {
                showLoading(false);
                showStatus("❌ No images found in this folder");
                return;
            }

            // Get download URLs for all images
            List<StorageReference> items = listResult.getItems();
            int totalItems = items.size();

            for (StorageReference fileRef : items) {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrls.add(uri.toString());

                    // If this is the first image URL, display it
                    if (imageUrls.size() == 1) {
                        displayCurrentImage();
                        updateNavigationButtons();
                        updateImageCounter();
                    } else if (imageUrls.size() == totalItems) {
                        // All URLs loaded, update UI
                        updateNavigationButtons();
                        updateImageCounter();
                    }

                }).addOnFailureListener(e -> {
                    Log.e("ImageViewer", "Failed to get download URL for: " + fileRef.getName(), e);
                });
            }

        }).addOnFailureListener(e -> {
            Log.e("ImageViewer", "Failed to list images in folder: " + folderName, e);
            showLoading(false);
            showStatus("❌ Failed to load images");
        });
    }

    private void displayCurrentImage() {
        if (imageUrls.isEmpty() || currentImageIndex >= imageUrls.size()) {
            return;
        }

        showLoading(true);
        String imageUrl = imageUrls.get(currentImageIndex);

        Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        currentBitmap = resource;
                        imageView.setImageBitmap(resource);
                        showLoading(false);
                        showStatus("");

                        // Enable AI detection button
                        if (yoloDetector != null) {
                            btnAiDetection.setEnabled(true);
                            btnAiDetection.setText("🔍 AI Smart Detection");
                        }

                        // Clear previous AI results
                        tvAiStatus.setVisibility(View.GONE);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        // Handle cleanup if needed
                    }
                });
    }

    private void showPreviousImage() {
        if (currentImageIndex > 0) {
            currentImageIndex--;
            displayCurrentImage();
            updateNavigationButtons();
            updateImageCounter();
        }
    }

    private void showNextImage() {
        if (currentImageIndex < imageUrls.size() - 1) {
            currentImageIndex++;
            displayCurrentImage();
            updateNavigationButtons();
            updateImageCounter();
        }
    }

    private void updateNavigationButtons() {
        btnPrev.setEnabled(currentImageIndex > 0);
        btnNext.setEnabled(currentImageIndex < imageUrls.size() - 1);
    }

    private void updateImageCounter() {
        if (imageUrls.isEmpty()) {
            tvImageCounter.setText("0 / 0");
        } else {
            tvImageCounter.setText((currentImageIndex + 1) + " / " + imageUrls.size());
        }
    }

    private void performYoloDetection() {
        if (yoloDetector == null || currentBitmap == null) {
            showAiStatus("❌ AI detection not available");
            return;
        }

        btnAiDetection.setEnabled(false);
        btnAiDetection.setText("🔄 Analyzing...");
        showAiStatus("Analyzing image with AI...");

        // Run detection in background thread
        new Thread(() -> {
            try {
                Map<String, Integer> detections = yoloDetector.detect(currentBitmap);

                runOnUiThread(() -> {
                    btnAiDetection.setEnabled(true);
                    btnAiDetection.setText("🔍 AI Smart Detection");

                    if (detections.isEmpty()) {
                        showAiStatus("✅ No mail items detected in image");
                    } else {
                        StringBuilder result = new StringBuilder("🔍 Detected: ");
                        boolean first = true;
                        for (Map.Entry<String, Integer> entry : detections.entrySet()) {
                            if (!first) result.append(", ");
                            result.append(entry.getValue()).append("x ").append(entry.getKey());
                            first = false;
                        }
                        showAiStatus(result.toString());
                    }
                });

            } catch (Exception e) {
                Log.e("ImageViewer", "YOLO detection failed", e);
                runOnUiThread(() -> {
                    btnAiDetection.setEnabled(true);
                    btnAiDetection.setText("🔍 AI Smart Detection");
                    showAiStatus("❌ Detection failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showStatus(String message) {
        if (message.isEmpty()) {
            tvStatus.setVisibility(View.GONE);
        } else {
            tvStatus.setText(message);
            tvStatus.setVisibility(View.VISIBLE);
        }
    }

    private void showAiStatus(String message) {
        if (message.isEmpty()) {
            tvAiStatus.setVisibility(View.GONE);
        } else {
            tvAiStatus.setText(message);
            tvAiStatus.setVisibility(View.VISIBLE);
        }
    }
}
