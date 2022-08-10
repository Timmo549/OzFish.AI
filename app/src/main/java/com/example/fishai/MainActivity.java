package com.example.fishai;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.fishai.databinding.ActivityMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.helloar.HelloArActivity;

import org.tensorflow.lite.examples.classification.Classifier;
import org.tensorflow.lite.examples.classification.ClassifierFloatMobileNet;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    Bitmap image;
    private static final int CAMERA_REQUEST = 1888;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static final int TIME_DELAY = 2000;
    private static long back_pressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCamera();
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
            }
        });

        // Enable AR-related functionality on ARCore supported devices only.
        if (maybeEnableArButton()) {
            Button button = findViewById(R.id.button_measure);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMeasure();
                }
            });
        }

        // Enable Camera related functionality on devices with accessible cameras only.
        if (maybeEnableCameraButton()) {
            Button button = findViewById(R.id.button_measure);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openMeasure();
                }
            });
        }
    }

    @Override
    protected void onDestroy() { super.onDestroy(); }

    @Override
    protected void onResume() { super.onResume(); }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if (id == R.id.action_test) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        // If the user double taps the back button, the application will close
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
        } else {
            Toast.makeText(getBaseContext(), "Press once again to exit!",
                    Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();
    }

    private boolean maybeEnableCameraButton() {
        FloatingActionButton button = findViewById(R.id.fab);
        // Test to see if the device has an available camera to leverage
        if (this.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            // This device has a camera, so enable the camera functionality
            button.setVisibility(View.VISIBLE);
            button.setEnabled(true);
            return true;
        } else {
            // No camera on this device, so disable the camera functionality
            button.setVisibility(View.INVISIBLE);
            button.setEnabled(false);
            return false;
        }
    }

    private boolean maybeEnableArButton() {
        Button button = findViewById(R.id.button_measure);
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    maybeEnableArButton();
                }
            }, 200);
        }
        if (availability.isSupported()) {
            // Enable AR functionality
                button.setVisibility(View.VISIBLE);
                button.setEnabled(true);
                return true;
        } else {
            // The device is unsupported or unknown.
            // AR not supported
            button.setVisibility(View.INVISIBLE);
            button.setEnabled(false);
            return false;
       }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    public void openCamera() {
        // Start Activity to take photos with the Phone's Camera
//        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//        startActivityForResult(cameraIntent, CAMERA_REQUEST);
//        Intent cameraIntent = new Intent(this, CameraActivity.class);
//        startActivity(cameraIntent);
    }

    public void openMeasure() {
        // Start Activity to measure fish with the ARCore Engine
        Intent measureFish = new Intent(this, HelloArActivity.class);
        startActivity(measureFish);
    }

    public void openMLEngine(Bitmap image) {
        // Start Activity to identify fish with the TensorFlow Lite ML Engine
//        Intent MLIntent = new Intent(this, TBA.class);
//        startActivity(MLIntent);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // When the camera image capture activity concludes
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            // Extract the image data from the activity results
            image = (Bitmap) data.getExtras().get("data");

            // Currently for Debug purposes
            // Displays the image as the background of the main application screen
            ImageView imageView = findViewById(R.id.image_view);
//            imageView.setImageBitmap(image);

            try {
                Classifier classifier = new ClassifierFloatMobileNet(this, Classifier.Device.NNAPI, 1);
                List<Classifier.Recognition> results = classifier.recognizeImage(image, 0);
                TextView textView = findViewById(R.id.textView);
                int counter = 1;
                textView.setText("Results: \n");
                for (Classifier.Recognition result : results) {
                    textView.append(counter++ + ": " + result.toString() + "\n");
                }
                imageView.setImageBitmap(loadImage(image, 0, classifier).getBitmap());
                classifier.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int sensorToDeviceRotation(CameraCharacteristics c, int deviceOrientation) {
        int sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION);

        // Get device orientation in degrees
        switch(deviceOrientation) {
            case 0:
                deviceOrientation = 0;
                break;
            case 1:
                deviceOrientation = 90;
                break;
            case 2:
                deviceOrientation = 180;
                break;
            case 3:
                deviceOrientation = 270;
                break;
        }
        // Reverse device orientation for front-facing cameras
        if (c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
            deviceOrientation = -deviceOrientation;
        }

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360;
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
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(classifier.getImageSizeX(), classifier.getImageSizeY(), ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(numRotation))
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }
}