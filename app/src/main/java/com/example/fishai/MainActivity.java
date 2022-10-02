package com.example.fishai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.fishai.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.helloar.HelloArActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
//    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int CAMERA_REQUEST = 1888;
    private static final int TIME_DELAY = 2000;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private static long back_pressed;
    private boolean arEnabled = false;
    private boolean cameraEnabled = false;

    private String currentPhotoPath;
    private String fishToSearch;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ArrayList<CharSequence> fishDataset;

    NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        navController = navHostFragment.getNavController();
//        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


        fishDataset = new ArrayList<CharSequence>();
        getFishRecords(false);

        checkCameraPermissionGiven();

        // Enable AR-related functionality on ARCore supported devices only.
        if (maybeEnableArButton()) {
            arEnabled = true;

            Button measure_button = findViewById(R.id.measure_button);
            measure_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openMeasure();
                }
            });
        }

        // Enable Camera related functionality on devices with accessible cameras only.
        if (maybeEnableCameraButton()) {
            cameraEnabled = true;

            Button camera_button = findViewById(R.id.identify_button);
            camera_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openCamera();
                }
            });
        }

        Button search_button = findViewById(R.id.search_button);
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View searchView = getLayoutInflater().inflate(R.layout.search_spinner_menu, null);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(searchView);
                builder.setMessage("Select the fish species to view...");
                builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int id) {
                        openSearch(fishToSearch);
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                AlertDialog dialog = builder.create();

                Spinner spinner = searchView.findViewById(R.id.search_spinner);

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(dialog.getContext(), R.array.fish_dataset, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                spinner.setAdapter(adapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        // An item was selected. You can retrieve the selected item using
                        // parent.getItemAtPosition(pos)
                        fishToSearch = String.valueOf(adapterView.getItemAtPosition(i));
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        // Another interface callback
                    }
                });
                dialog.show();
            }
        });
    }


    @Override
    protected void onDestroy() { super.onDestroy(); }

    @Override
    protected void onResume() {
        super.onResume();
//        checkCameraPermissionGiven();
        maybeEnableCameraButton();
        maybeEnableArButton();
    }

    @Override
    public void onPause() { super.onPause(); }

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
        if (id == R.id.action_help) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.mainactivity_help_message).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    dialogInterface.dismiss();
                }
            });
            builder.create().show();
            return true;
        }
        else if (id == R.id.action_update_db) {
            getFishRecords(true);
            return true;
        }
        else if (id == R.id.action_about) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.about).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
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

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
//        navController.navigateUp();
        // If the user is currently on the home screen
        if (navController.getCurrentDestination().getId() == R.id.FirstFragment) {
            // If the user double taps the back button, the application will close
            if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
                super.onBackPressed();
            } else {
                Toast.makeText(getBaseContext(), "Press once again to exit!",
                        Toast.LENGTH_SHORT).show();
            }
            back_pressed = System.currentTimeMillis();
        }
    }

    private void checkCameraPermissionGiven() {
        // Camera functionality requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    private boolean maybeEnableCameraButton() {
        Button camera_button = findViewById(R.id.identify_button);

        // Test to see if the device has an available camera to leverage
        if (this.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)){
            if (CameraPermissionHelper.hasCameraPermission(this)) {
                // This device has a camera, so enable the camera functionality
                camera_button.setEnabled(true);
                return true;
            }
            return false;
        } else {
            // No camera on this device, so disable the camera functionality
            camera_button.setEnabled(false);
            return false;
        }
    }

    private boolean maybeEnableArButton() {
        Button measure_button = findViewById(R.id.measure_button);

        // Query availability of AR functionality on host device
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
            if (CameraPermissionHelper.hasCameraPermission(this)) {
                measure_button.setEnabled(true);
            }
            return true;
        } else {
            // The device is unsupported or unknown.
            // AR not supported
            measure_button.setEnabled(false);
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

    protected void openCamera() {
        // Start Activity to take photos with the Phone's Camera
        dispatchTakePictureIntent();
    }

    protected void openMeasure() {
        // Start Activity to measure fish with the ARCore Engine
        Intent measureIntent = new Intent(this, HelloArActivity.class);
        startActivity(measureIntent);
    }

    protected void openMLEngine(String path) {
        // Start Activity to identify fish with the TensorFlow Lite ML Engine
        Intent MLIntent = new Intent(this, ResultsActivity.class);
        MLIntent.putExtra("path", path);
        startActivity(MLIntent);
    }

    private void openSearch(String fishName) {
        Intent searchIntent = new Intent(this, InformationActivity.class);
        searchIntent.putExtra("fishName", fishName);
        startActivity(searchIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // When the camera image capture activity concludes
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            // Navigate to results page
            openMLEngine(currentPhotoPath);
        }
    }

    private void getFishRecords(boolean confirm) {
        Source source = Source.SERVER;
//        Source source = Source.DEFAULT;

        CollectionReference docRef = db.collection("fish");
        docRef.get(source).addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        fishDataset.add((CharSequence) doc.getData().get("name"));
                    }
                    if (confirm) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setMessage(R.string.database_updated_message).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int id) {
                                dialogInterface.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                } else {
                    // Error, something went wrong
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.database_error_message).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int id) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.create().show();
                }
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                photoFile.deleteOnExit();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
                // TODO: remove deprecated function
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

/*    private int sensorToDeviceRotation(CameraCharacteristics c, int deviceOrientation) {
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
*/

}