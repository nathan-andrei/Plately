package com.example.plately;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.plately.databinding.ActivityAddRecipeBinding;

public class AddRecipeActivity extends AppCompatActivity {
    private ActivityAddRecipeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.buttonCancel.setOnClickListener(v -> finish());

        binding.buttonSave.setOnClickListener(v ->
                Toast.makeText(AddRecipeActivity.this, "Save button clicked (Not yet implemented)", Toast.LENGTH_SHORT).show()
        );
    }
}
