package com.example.plately;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.plately.databinding.ActivityRecipeDetailsBinding;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ActivityRecipeDetailsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRecipeDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutMainRecipeDetails, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String title = getIntent().getStringExtra("title");
        if (title != null) {
            binding.textViewRecipeName.setText(title);
        }

        binding.buttonSubmitReview.setOnClickListener(v ->
                Toast.makeText(this, "Review submitted!", Toast.LENGTH_SHORT).show()
        );
    }
}
