package com.example.plately;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.plately.databinding.ActivityEditRecipeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditRecipeActivity extends AppCompatActivity {

    private ActivityEditRecipeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String recipeId;
    private DocumentReference recipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityEditRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recipeId = getIntent().getStringExtra("recipeId");
        if (recipeId == null) {
            Toast.makeText(this, "Invalid recipe", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipeRef = db.collection("recipes").document(recipeId);

        loadRecipeData();

        binding.editButtonCancel.setOnClickListener(v -> finish());
        binding.editButtonSave.setOnClickListener(v -> saveRecipe());
    }

    // load existing recipe data in the edit text fields
    private void loadRecipeData() {
        recipeRef.get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            binding.editEditTextRecipeName.setText(doc.getString("recipeName"));
            binding.editEditTextSourceInput.setText(doc.getString("source"));
            binding.editEditTextServesInput.setText(String.valueOf(doc.getLong("servesPax") != null ? doc.getLong("servesPax") : ""));
            binding.editEditTextPrepTimeInput.setText(String.valueOf(doc.getLong("prepTime") != null ? doc.getLong("prepTime") : ""));
            binding.editEditTextCookTimeInput.setText(String.valueOf(doc.getLong("cookTime") != null ? doc.getLong("cookTime") : ""));
            binding.editEditTextIngredientsInput.setText(joinStringList(doc.get("ingredients")));
            binding.editEditTextInstructionsInput.setText(joinStringList(doc.get("steps")));
            binding.editEditTextNotesInput.setText(joinStringList(doc.get("notes")));
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load recipe", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // save new recipe details to db
    private void saveRecipe() {
        String recipeName = binding.editEditTextRecipeName.getText().toString().trim();
        String source = binding.editEditTextSourceInput.getText().toString().trim();
        int serves = parseInt(binding.editEditTextServesInput.getText().toString().trim());
        int prepTime = parseInt(binding.editEditTextPrepTimeInput.getText().toString().trim());
        int cookTime = parseInt(binding.editEditTextCookTimeInput.getText().toString().trim());
        List<String> ingredients = splitLines(binding.editEditTextIngredientsInput.getText().toString());
        List<String> steps = splitLines(binding.editEditTextInstructionsInput.getText().toString());
        List<String> notes = splitLines(binding.editEditTextNotesInput.getText().toString());

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("recipeName", recipeName);
        updatedData.put("source", source);
        updatedData.put("servesPax", serves);
        updatedData.put("prepTime", prepTime);
        updatedData.put("cookTime", cookTime);
        updatedData.put("ingredients", ingredients);
        updatedData.put("steps", steps);
        updatedData.put("notes", notes);

        recipeRef.update(updatedData)
                .addOnSuccessListener(ignored -> {
                    Toast.makeText(EditRecipeActivity.this, "Recipe updated!", Toast.LENGTH_SHORT).show();

                    // send notification to main that something was updated for it to update main UI
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("recipeUpdated", true);
                    setResult(RESULT_OK, resultIntent);

                    // close return to recipe details for now
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditRecipeActivity.this, "Failed to update recipe", Toast.LENGTH_SHORT).show()
                );
    }

    // data handling helpers
    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> splitLines(String input) {
        return List.of(input.split("\\r?\\n"));
    }

    private String joinStringList(Object obj) {
        if (!(obj instanceof List)) return "";
        List<?> list = (List<?>) obj;
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            sb.append(item.toString()).append("\n");
        }
        return sb.toString().trim();
    }
}
