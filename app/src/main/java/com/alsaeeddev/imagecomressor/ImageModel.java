package com.alsaeeddev.imagecomressor;

import android.graphics.Bitmap;
import android.net.Uri;

public class ImageModel {
    private Bitmap originalImage;
    private Bitmap compressedImage;
    private Uri imageUri;

    public ImageModel(Bitmap originalImage, Uri imageUri) {
        this.originalImage = originalImage;
        this.imageUri = imageUri;
        this.compressedImage = originalImage;
    }

    public Bitmap getOriginalImage() {
        return originalImage;
    }

    public Bitmap getCompressedImage() {
        return compressedImage;
    }

    public void setCompressedImage(Bitmap compressedImage) {
        this.compressedImage = compressedImage;
    }

    public Uri getImageUri() {
        return imageUri;
    }
}
