package com.mobdeve.ss1.milan.john.moc2;

//change package name
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RecipeDetailsActivity extends AppCompatActivity {

    // Widgets for Recipe Details Activity
    private Button buttonSubmitReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe_details);

        // --- Setup Methods ---
        setUpWidgets();
        setUpListeners();

        // Apply Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMainRecipeDetails), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setUpWidgets() {
        // Find the Submit Review button
        buttonSubmitReview = findViewById(R.id.buttonSubmitReview);
    }

    private void setUpListeners() {
        // Placeholder listener for the Submit Review button
        if (buttonSubmitReview != null) {
            buttonSubmitReview.setOnClickListener(v -> {
                Toast.makeText(RecipeDetailsActivity.this, "Review submitted!", Toast.LENGTH_SHORT).show();
            });
        }
    }
}