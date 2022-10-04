package com.example.fishai;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.ImageDecoder;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fishai.databinding.InformationActivityBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.helloar.HelloArActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class InformationActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private InformationActivityBinding binding;

    private Map<String, Object> document;
    private String fishName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = InformationActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        fishName = bundle.getString("fishName").trim();

        TextView textView = findViewById(R.id.fish_name_text);
        textView.setText(fishName);

        // Populate page with input fish species information
        getFishRecord(fishName);

        // Check if application has camera permissions.
        checkCameraPermissionGiven();

        // Enable AR-related functionality on ARCore supported devices only.
        maybeEnableArButton();
    }

    @Override
    public void onPause() { super.onPause(); }

    @Override
    protected void onDestroy() { super.onDestroy(); }

    @Override
    protected void onResume() {
        super.onResume();

        // Perform another check to enable AR functionalities
        maybeEnableArButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_information, menu);
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
            AlertDialog.Builder builder = new AlertDialog.Builder(InformationActivity.this);
            builder.setMessage(R.string.infoactivity_help_message).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
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

    private void checkCameraPermissionGiven() {
        // Camera functionality requires camera permissions to operate. If we did not yet obtain runtime
        // permission on Android M and above, now is a good time to ask the user for it.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
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
            bag_limit: number
            freshwater: boolean
            minimum_size: number
            name: String
            photo: String
            possession_limit: number
            saltwater: boolean
            season: String
            note: String
 */


            TextView textView;

            // Bag Limit
            textView = findViewById(R.id.bag_limit);
            Integer limit = Integer.valueOf(String.valueOf(document.get("bag_limit")));
            if (limit >= 0) {
                textView.setText(limit.toString());
            } else {
                textView.setText(R.string.none);
            }

            // Minimum Size
            textView = findViewById(R.id.minimum_size);
            Integer size = Integer.valueOf(String.valueOf(document.get("minimum_size")));
            if (size != 0) {
                textView.setText(size.toString().concat(getString(R.string.cm)));
            } else {
                textView.setText(R.string.none);
            }

            // Photo Path
            ImageView fishPicture = findViewById(R.id.result_image);
            String path = String.valueOf(document.get("photo"));

            try {
                fishPicture.setImageBitmap(ImageDecoder.decodeBitmap(ImageDecoder.createSource(getAssets(), path)));
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Possession Limit
            textView = findViewById(R.id.possession_limit);
            limit = Integer.valueOf(String.valueOf(document.get("possession_limit")));
            if (limit >= 0) {
                textView.setText(limit.toString());
            } else {
                textView.setText(R.string.none);
            }

            // Season
            textView = findViewById(R.id.season);
            textView.setText(String.valueOf(document.get("season")));

            // Freshwater Yes/No?
            textView = findViewById(R.id.freshwater);
            if (Boolean.parseBoolean(String.valueOf(document.get("freshwater")))) {
                textView.setText(R.string.yes);
            } else {
                textView.setText(R.string.no);
            }

            // Saltwater Yes/No?
            textView = findViewById(R.id.saltwater);
            if (Boolean.parseBoolean(String.valueOf(document.get("saltwater")))) {
                textView.setText(R.string.yes);
            } else {
                textView.setText(R.string.no);
            }

            // Notes (if any)
            textView = findViewById(R.id.note);
            String note = String.valueOf(document.get("note"));
            if (note.contains("null")) {
                textView.setText(R.string.none);
            } else {
                textView.setText(note);
            }
        }
    }

    private void maybeEnableArButton() {
        Button measure_button = findViewById(R.id.info_measure_button);

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

                measure_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openMeasure(fishName);
                    }
                });
            }
        } else {
            // The device is unsupported or unknown.
            // AR not supported
            measure_button.setEnabled(false);
        }
    }

    private void openMeasure(String fishName) {
        // Start Activity to measure fish with the ARCore Engine
        Intent measureIntent = new Intent(this, HelloArActivity.class);
        measureIntent.putExtra("fishName", fishName);
        startActivity(measureIntent);
    }
}