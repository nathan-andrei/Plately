package com.example.plately;

import android.os.Bundle;
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
    }
}
