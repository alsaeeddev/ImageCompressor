package com.alsaeeddev.imagecomressor;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {


    private Button btnImagesSelect, btnSaveImages, btnCompressImages;
    private TextView tvSeekValue;
    private SeekBar compressionSeekBar;
    private int compressionQuality = 100;  // Default to 100%
    ImageModel imageModel;
    private ImageView originalImageView, compressedImageView;
    private ProgressBar progressBar;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {


                if (uri != null) {
                    // Handle the selected media URI (image or video)
                    Log.d("Selected URI", uri.toString());
                    setImageInImageView(uri);
                } else {
                    Log.d("PickMedia", "No media selected");
                }


            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        originalImageView = findViewById(R.id.originalImageView);
        compressedImageView = findViewById(R.id.compressedImageView);
        btnImagesSelect = findViewById(R.id.selectImagesButton);
        btnSaveImages = findViewById(R.id.saveImagesButton);
        btnCompressImages = findViewById(R.id.compressImages);
        compressionSeekBar = findViewById(R.id.compressionSeekBar);
        tvSeekValue = findViewById(R.id.tvSeekValue);
        progressBar = findViewById(R.id.progressBar);


        btnImagesSelect.setOnClickListener(view -> openImagePicker());
        btnSaveImages.setOnClickListener(view -> saveCompressedImages());

        btnCompressImages.setOnClickListener(v -> compressImagesInBackground());

        compressionSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                compressionQuality = progress;
                tvSeekValue.setText("Compression: " + progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    // open image picker to pick the image
    private void openImagePicker() {
        pickMultipleMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }



    // set the selected image in the original image view
    private void setImageInImageView(Uri uri) {
        executorService.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
                imageModel = new ImageModel(originalBitmap, uri);


                runOnUiThread(() -> {
                    originalImageView.setImageBitmap(originalBitmap);
                    compressedImageView.setImageBitmap(null);

                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }



    // compressed image in the background
    private void compressImagesInBackground() {
        progressBar.setVisibility(View.VISIBLE);
        executorService.execute(() -> {

            Bitmap compressedBitmap = compressImage(imageModel.getOriginalImage(), compressionQuality);
            imageModel.setCompressedImage(compressedBitmap);

            runOnUiThread(() -> {
                compressedImageView.setImageBitmap(compressedBitmap);
                progressBar.setVisibility(View.GONE);
            });

        });
    }


    //compress image method, which is calling in the compressImagesInBackground method
    private Bitmap compressImage(Bitmap original, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        original.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        byte[] byteArray = stream.toByteArray();
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }



    // save the compressed image in the phone storage
    private void saveCompressedImages() {
        executorService.execute(() -> {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CompressedImages");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, "compressed_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                imageModel.getCompressedImage().compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                addImageToGallery(file);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Show toast with full image path
            runOnUiThread(() -> Toast.makeText(
                    this,
                    "Image saved at:\n" + file.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show());
        });
    }


    //show the image in the gallery
    private void addImageToGallery(File file) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
