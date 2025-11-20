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

    // Image picker launcher
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
                        // Use the field name you stored in Firestore
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
        String title = binding.editTextRecipeName.getText().toString().trim();
        String source = binding.editTextSourceInput.getText().toString().trim();
        String ingredients = binding.editTextIngredientsInput.getText().toString().trim();
        String instructions = binding.editTextInstructionsInput.getText().toString().trim();
        String notes = binding.editTextNotesInput.getText().toString().trim();

        double serves = parseDouble(binding.editTextServesInput.getText().toString());
        double prep = parseDouble(binding.editTextPrepTimeInput.getText().toString());
        double cook = parseDouble(binding.editTextCookTimeInput.getText().toString());

        if (title.isEmpty()) {
            binding.editTextRecipeName.setError("Title required");
            return;
        }

        if (selectedImageUri != null) {
            uploadImageAndSave(title, source, ingredients, instructions, notes, serves, prep, cook);
        } else {
            saveRecipeToFirestore(title, source, ingredients, instructions, notes, serves, prep, cook, null);
        }
    }


    private void uploadImageAndSave(String title, String source, String ingredients,
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
    }



    private void saveRecipeToFirestore(String title, String source,
                                       String ingredients, String instructions, String notes,
                                       double serves, double prep, double cook, String imageUrl) {

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Map<String, Object> author = new HashMap<>();
        author.put("uid", currentUserId);

        Map<String, Object> recipe = new HashMap<>();
        recipe.put("title", title);
        recipe.put("source", source);
        recipe.put("ingredients", ingredients);
        recipe.put("instructions", instructions);
        recipe.put("notes", notes);
        recipe.put("serves", serves);
        recipe.put("prepTime", prep);
        recipe.put("cookTime", cook);
        recipe.put("tags", selectedTags);
        recipe.put("imageUrl", imageUrl);
        recipe.put("author", author);

        db.collection("recipes")
                .add(recipe)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show();

                    DocumentReference recipeRef = db.collection("recipes").document(docRef.getId());
                    db.collection("users")
                            .document(currentUserId)
                            .update("createdRecipes", com.google.firebase.firestore.FieldValue.arrayUnion(recipeRef))
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
