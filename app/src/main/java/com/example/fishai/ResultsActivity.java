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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fishai.databinding.ActivityMainBinding;
import com.example.fishai.databinding.ResultsActivityBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import org.tensorflow.lite.examples.classification.Classifier;
import org.tensorflow.lite.examples.classification.ClassifierFloatMobileNet;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResultsActivity extends AppCompatActivity {
//    Bitmap image;
    private ResultsActivityBinding binding;
    private int counter;
    private String fishName;

    private List<Classifier.Recognition> results;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private Map<String, Object> document;

//    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ResultsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        String pathToPicture = bundle.getString("path");

        Bitmap image = BitmapFactory.decodeFile(pathToPicture);

/*
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
*/

        TextView fishNameView = findViewById(R.id.fish_name_result);

        // If displayed fish matches the users expected result
        binding.buttonResultsYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFishInformation();
            }
        });

        // If displayed fish does not match the users expected result
        binding.buttonResultsNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show the next fish until 3 fish species have been displayed
                // Expected counter range = 0-2
                if (counter < 2) {
                    counter++;
                    fishName = results.get(counter).getTitle().trim();
                    fishNameView.setText(fishName);

                    getFishRecord(fishName);
                } else {
                    // If all 3 fish species are not correct, display an error message
                    // and return user to the main screen
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
            // Attempt to classify user input image per ML model
            results = performClassification(image);

            // Display the first fish species to the user if classification successful
            if (results != null) {
                counter = 0;
                fishName = results.get(counter).getTitle().trim();
                fishNameView.setText(fishName);

                getFishRecord(fishName);
            }
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ResultsActivity.this);
            builder.setMessage(R.string.resultsactivity_help_message).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    dialogInterface.dismiss();
                }
            });
            builder.create().show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getFishRecord(String fishName) {
        Source source = Source.CACHE;
//        Source source = Source.DEFAULT;

        // Query database for fish species record
        DocumentReference docRef = db.collection("fish").document(fishName);
        docRef.get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        // Document found
                        document = doc.getData();
                        populateFields();
                    } else {
                        // Error, no document found
                    }
                } else {
                    // Error, something went wrong
                }
            }
        });
    }

    private void populateFields() {
        // Display fish species information if possible
        if (document != null) {
/*
            name: String
            photo: String
 */

            TextView textView = findViewById(R.id.fish_name_result);
            textView.setText(String.valueOf(document.get("name")));

            ImageView fishPicture = findViewById(R.id.result_image);
            String path = String.valueOf(document.get("photo"));

            try {
                fishPicture.setImageBitmap(ImageDecoder.decodeBitmap(ImageDecoder.createSource(getAssets(), path)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected void openFishInformation() {
        // Start Activity to display fish results based on identified fish
        Intent informationIntent = new Intent(ResultsActivity.this, InformationActivity.class);
        informationIntent.putExtra("fishName", fishName);
        startActivity(informationIntent);
    }

    protected List<Classifier.Recognition> performClassification(Bitmap image) {
        // Attempt to classify fish image input by user
        try {
            Classifier classifier = new ClassifierFloatMobileNet(this, Classifier.Device.NNAPI, 1);
            List<Classifier.Recognition> results = classifier.recognizeImage(image, 90);

            classifier.close();

            return results;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

/*
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
*/
}
