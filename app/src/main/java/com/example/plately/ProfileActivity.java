package com.example.plately;

//change package name
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ProfileActivity extends AppCompatActivity {

    // Widgets for Profile Activity
    private ImageButton buttonSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // --- Setup Methods ---
        setUpWidgets();
        setUpListeners();

        // Apply Window Insets (Safety for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMainProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setUpWidgets() {
        // Find the Settings button in the header toolbar
        buttonSettings = findViewById(R.id.buttonSettings);
    }

    private void setUpListeners() {
        // Placeholder listener for the Settings button
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(v -> {
                Toast.makeText(ProfileActivity.this, "Settings clicked (Feature not implemented)", Toast.LENGTH_SHORT).show();
            });
        }
    }

}
