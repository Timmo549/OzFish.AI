package com.example.fishai;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fishai.databinding.ActivityMainBinding;
import com.example.fishai.databinding.ResultsActivityBinding;
import com.google.android.material.snackbar.Snackbar;

import org.tensorflow.lite.examples.classification.Classifier;
import org.tensorflow.lite.examples.classification.ClassifierFloatMobileNet;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class ResultsActivity extends AppCompatActivity {

//    Bitmap image;
    private ResultsActivityBinding binding;
    private int counter;
    private String fishName;

    private List<Classifier.Recognition> results;

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ResultsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        String pathToPicture = bundle.getString("path");
//        TextView textView = findViewById(R.id.textView);
//        textView.setText("Extras: " + pathToPicture);

//        Bitmap image = BitmapFactory.decodeFile(pathToPicture);




        // Code for testing with sailfish image (or subsequently any image within the asset folder)
        ImageDecoder.Source source = ImageDecoder.createSource(getAssets(), "pictures/sailfish.jpg");

        ImageDecoder.OnHeaderDecodedListener listener = new ImageDecoder.OnHeaderDecodedListener() {
            public void onHeaderDecoded(ImageDecoder decoder, ImageDecoder.ImageInfo info, ImageDecoder.Source source) {
                decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
                decoder.setMutableRequired(true);
            }
        };
        @SuppressLint("WrongThread") Bitmap image = null;
        try {
            image = ImageDecoder.decodeBitmap(source, listener);
        } catch (IOException e) {
            e.printStackTrace();
        }

        image.setConfig(Bitmap.Config.ARGB_8888);






        TextView fishNameView = findViewById(R.id.fish_name_text);

        binding.buttonResultsYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFishInformation();
            }
        });

        binding.buttonResultsNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Expected counter range = 0-2
                if (counter < 2) {
                    counter++;
                    fishName = results.get(counter).getTitle();
                    fishNameView.setText(fishName);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ResultsActivity.this);
                    builder.setMessage(R.string.results_error_message).setPositiveButton(R.string.results_error_okay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int id) {
                            navigateUpTo(getSupportParentActivityIntent());
                        }
                    });
                    builder.create().show();
                }
            }
        });

        if (image != null) {
            ImageView imageView = findViewById(R.id.result_image);
            imageView.setImageBitmap(rotateBitmap(image,90));

            results = performClassification(image);

            if (results != null) {
                counter = 0;
                fishName = results.get(counter).getTitle();
                fishNameView.setText(fishName);
            }
        } else {
            TextView textView = findViewById(R.id.debug_text);
            textView.setText("Couldn't find Bitmap");
        }




    }

    protected void nextFish() {

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void openFishInformation() {
        // Start Activity to display fish results based on identified fish
        Intent informationIntent = new Intent(ResultsActivity.this, InformationActivity.class);
        informationIntent.putExtra("fishName", fishName);
        startActivity(informationIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//            ImageView imageView = (ImageView) findViewById(R.id.result_image);
//            ImageView imageView = findViewById(R.id.result_image);

//            imageView.setImageBitmap(image);
//            TextView textView = findViewById(R.id.textView);

//            int counter = 1;
//            textView.setText("Results: \n");
/*            for (Classifier.Recognition result : results) {
                textView.append(counter++ + ": " + result.toString() + "\n");
            }
*/
    }

    protected List<Classifier.Recognition> performClassification(Bitmap image) {
        try {
            Classifier classifier = new ClassifierFloatMobileNet(this, Classifier.Device.NNAPI, 1);
            List<Classifier.Recognition> results = classifier.recognizeImage(image, 90);

                TextView textView = findViewById(R.id.debug_text);
                int counter = 1;
                textView.setText("Results: \n");
                for (Classifier.Recognition result : results) {
                    textView.append(counter++ + ": " + result.toString() + "\n");
                }

                ImageView imageView = findViewById(R.id.result_image);
                imageView.setImageBitmap(loadImage(image, 90, classifier).getBitmap());
//                image = loadImage(image, 270, classifier).getBitmap();

            classifier.close();

            return results;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /** Loads input image, and applies preprocessing. */
    private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation, Classifier classifier) {
        // Loads bitmap into a TensorImage.
        TensorImage inputImageBuffer = new TensorImage();
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90;
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
        // Currently squashing input image! Uncomment this line to crop the image instead
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(classifier.getImageSizeX(), classifier.getImageSizeY(), ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(-numRotation))
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }
}