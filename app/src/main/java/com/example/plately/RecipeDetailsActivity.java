package com.example.plately;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.example.plately.databinding.ActivityRecipeDetailsBinding;
import com.google.firebase.firestore.DocumentReference;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ActivityRecipeDetailsBinding binding;
    private boolean isFavorite = false;

    private FirebaseFirestore db;
    private FirebaseAuth dbAuth;

    private String recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRecipeDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        dbAuth = FirebaseAuth.getInstance();

        recipeId = getIntent().getStringExtra("recipeId");

        String title = getIntent().getStringExtra("title");
        if (title != null) {
            binding.textViewRecipeName.setText(title);
        }

        isFavorite = getIntent().getBooleanExtra("isFavorite", false);
        binding.imageBtnSaveRecipe.setImageResource(
                isFavorite ? R.drawable.baseline_bookmark_24 : R.drawable.outline_bookmark_24
        );

        binding.imageBtnSaveRecipe.setOnClickListener(v -> {
            isFavorite = !isFavorite;

            if (isFavorite) {
                binding.imageBtnSaveRecipe.setImageResource(R.drawable.baseline_bookmark_24);
                saveRecipeToSaved();
            } else {
                binding.imageBtnSaveRecipe.setImageResource(R.drawable.outline_bookmark_24);
                removeRecipeFromSaved();
            }

            sendFavoriteResult();
        });
    }

    private void sendFavoriteResult() {
        if (recipeId == null) return;

        Intent resultIntent = new Intent();
        resultIntent.putExtra("recipeId", recipeId);
        resultIntent.putExtra("isFavorite", isFavorite);
        setResult(RESULT_OK, resultIntent);
    }

    private void saveRecipeToSaved() {
        String uid = dbAuth.getUid();
        if (uid == null || recipeId == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);

        db.collection("users")
                .document(uid)
                .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayUnion(recipeRef))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
    }

    private void removeRecipeFromSaved() {
        String uid = dbAuth.getUid();
        if (uid == null || recipeId == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);

        db.collection("users")
                .document(uid)
                .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayRemove(recipeRef))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Removed!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show());
    }
}


