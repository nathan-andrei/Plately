package com.example.plately;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.plately.databinding.ActivityAddRecipeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecipeActivity extends AppCompatActivity {
    private ActivityAddRecipeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth dbAuth;
    private StorageReference storageRef;

    private Uri selectedImageUri = null;
    private List<String> tagList = new ArrayList<>();
    private List<String> selectedTags = new ArrayList<>();

    //imaeg picker launcher
    ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.imageViewSelected.setImageURI(uri);
                    binding.imageViewSelected.setVisibility(android.view.View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        dbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("recipe_images");

        binding.buttonCancel.setOnClickListener(v -> finish());
        binding.buttonSave.setOnClickListener(v -> saveRecipe());

        binding.buttonImageInput.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.buttonTagInput.setOnClickListener(v -> showTagDialog());

        loadTagsFromDatabase();
    }

    private void loadTagsFromDatabase() {
        db.collection("tags")
                .get()
                .addOnSuccessListener(query -> {
                    tagList.clear();
                    for (var doc : query.getDocuments()) {
                        tagList.add(doc.getString("tagName"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load tags: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showTagDialog() {
        if (tagList.isEmpty()) {
            Toast.makeText(this, "No tags available", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean[] checkedItems = new boolean[tagList.size()];
        for (int i = 0; i < tagList.size(); i++) {
            checkedItems[i] = selectedTags.contains(tagList.get(i));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Tags");
        builder.setMultiChoiceItems(tagList.toArray(new String[0]), checkedItems,
                (dialog, index, isChecked) -> {
                    if (isChecked) {
                        if (!selectedTags.contains(tagList.get(index)))
                            selectedTags.add(tagList.get(index));
                    } else {
                        selectedTags.remove(tagList.get(index));
                    }
                });
        builder.setPositiveButton("Done", null);
        builder.create().show();
    }

    private void saveRecipe() {
        String recipeName = binding.editTextRecipeName.getText().toString().trim();
        String source = binding.editTextSourceInput.getText().toString().trim();
        String ingredientsInput = binding.editTextIngredientsInput.getText().toString().trim();
        String stepsInput = binding.editTextInstructionsInput.getText().toString().trim();
        String recipeDescription = binding.editTextNotesInput.getText().toString().trim(); // Using notes as description

        double servesPax = parseDouble(binding.editTextServesInput.getText().toString());
        double prepTime = parseDouble(binding.editTextPrepTimeInput.getText().toString());
        double cookTime = parseDouble(binding.editTextCookTimeInput.getText().toString());

        if (recipeName.isEmpty()) {
            binding.editTextRecipeName.setError("Recipe name required");
            return;
        }

        // Convert ingredients and steps to List<String>
        List<String> ingredientsList = new ArrayList<>();
        if (!ingredientsInput.isEmpty()) {
            // Split by newline
            String[] items = ingredientsInput.split("\\r?\\n");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) ingredientsList.add(trimmed);
            }
        }

        List<String> stepsList = new ArrayList<>();
        if (!stepsInput.isEmpty()) {
            String[] stepsArray = stepsInput.split("\\r?\\n");
            for (String step : stepsArray) {
                String trimmed = step.trim();
                if (!trimmed.isEmpty()) stepsList.add(trimmed);
            }
        }


        // Use tags selected in the dialog
        List<String> tagsList = selectedTags;

        // Save recipe
        saveRecipeToFirestore(
                recipeName,
                source,
                ingredientsList,
                stepsList,
                recipeDescription,
                servesPax,
                prepTime,
                cookTime,
                tagsList
        );
    }


    /*private void uploadImageAndSave(String title, String source, String ingredients,
                                    String instructions, String notes, double serves,
                                    double prep, double cook) {

        String currentUserId = dbAuth.getCurrentUser().getUid();
        StorageReference imageRef = storageRef.child(currentUserId + "_" + System.currentTimeMillis() + ".jpg");

        imageRef.putFile(selectedImageUri)
                .addOnSuccessListener(task -> imageRef.getDownloadUrl().addOnSuccessListener(url -> {
                    saveRecipeToFirestore(title, source, ingredients, instructions, notes,
                            serves, prep, cook, url.toString());
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }*/

    private void saveRecipeToFirestore(String recipeName, String source,
                                       List<String> ingredients, List<String> steps, String recipeDescription,
                                       double servesPax, double prepTime, double cookTime, List<String> tags) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) return;

        // Create a DocumentReference for the author
        DocumentReference authorRef = db.collection("users").document(currentUserId);
        Map<String, Object> authorMap = new HashMap<>();
        authorMap.put("uid", authorRef);


        // Prepare recipe document
        Map<String, Object> recipe = new HashMap<>();
        recipe.put("recipeName", recipeName);
        recipe.put("source", source);
        recipe.put("ingredients", ingredients != null ? ingredients : new ArrayList<String>());
        recipe.put("steps", steps != null ? steps : new ArrayList<String>());
        recipe.put("recipeDescription", recipeDescription != null ? recipeDescription : "");
        recipe.put("servesPax", servesPax);
        recipe.put("prepTime", prepTime);
        recipe.put("cookTime", cookTime);
        recipe.put("tags", tags != null ? tags : new ArrayList<String>());
        recipe.put("recipeImages", new ArrayList<String>());
        recipe.put("reviews", new ArrayList<String>());
        recipe.put("author", authorMap);

        // Save to Firestore
        db.collection("recipes")
                .add(recipe)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show();

                    // Add recipe reference to user's createdRecipes
                    db.collection("users")
                            .document(currentUserId)
                            .update("createdRecipes", FieldValue.arrayUnion(docRef))
                            .addOnSuccessListener(unused -> Log.d("Firestore", "User createdRecipes updated"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Failed to update createdRecipes", e));

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }


    private double parseDouble(String value) {
        try { return Double.parseDouble(value); }
        catch (Exception e) { return 0; }
    }

    private void updateSelectedTagsTextView() {
        if (selectedTags.isEmpty()) {
            binding.textViewSelectedTags.setText("No tags selected");
        } else {
            binding.textViewSelectedTags.setText(String.join(", ", selectedTags));
        }
    }


}
