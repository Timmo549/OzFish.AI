package com.example.fishai;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fishai.databinding.InformationActivityBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import org.w3c.dom.Text;

import java.util.Map;
import java.util.Objects;

public class InformationActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private InformationActivityBinding binding;

    private Map<String, Object> document;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = InformationActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        String fishName = bundle.getString("fishName");

        TextView textView = findViewById(R.id.fish_name_text);
        textView.setText(fishName);

        getFishRecord(fishName);

    }

    @Override
    public void onPause() { super.onPause(); }

    @Override
    protected void onDestroy() { super.onDestroy(); }

    @Override
    protected void onResume() { super.onResume(); }

    private void getFishRecord(String fishName) {
//        Source source = Source.CACHE;
        Source source = Source.SERVER;

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
                        TextView textView = findViewById(R.id.debug_text);
                        textView.setText("Document Found");
                    } else {
                        // Error, no document found
                        TextView textView = findViewById(R.id.debug_text);
                        textView.setText("Document Not Found");
                    }
                } else {
                    // Error, something went wrong
                    TextView textView = findViewById(R.id.debug_text);
                    textView.setText("Document Error");
                }
            }
        });
    }

    private void populateFields() {
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
 */

            TextView textView;
            textView = findViewById(R.id.fish_name);
            textView.setText(String.valueOf(document.get("name")));

            // Bag Limit
            textView = findViewById(R.id.bag_limit);
            textView.setText(String.valueOf(document.get("bag_limit")));

            // Minimum Size
            textView = findViewById(R.id.minimum_size);
            textView.setText(String.valueOf(document.get("minimum_size")));

            //document.get("photo");

            // Possession Limit
            textView = findViewById(R.id.possession_limit);
            textView.setText(String.valueOf(document.get("possession_limit")));

            // Season
            textView = findViewById(R.id.season);
            textView.setText(String.valueOf(document.get("season")));

            // Freshwater Yes/No?
            textView = findViewById(R.id.freshwater);
            if (Boolean.valueOf(String.valueOf(document.get("freshwater")))) {
                textView.setText("Yes");
            } else {
                textView.setText("No");
            }

            // Saltwater Yes/No?
            textView = findViewById(R.id.saltwater);
            if (Boolean.valueOf(String.valueOf(document.get("saltwater")))) {
                textView.setText("Yes");
            } else {
                textView.setText("No");
            }
        }
    }
}