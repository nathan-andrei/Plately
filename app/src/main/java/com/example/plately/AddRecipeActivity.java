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
import com.google.firebase.storage.UploadTask;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRecipeActivity extends AppCompatActivity {
    private ActivityAddRecipeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth dbAuth;
    private StorageReference storageRef;

    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private List<String> tagList = new ArrayList<>();
    private List<String> selectedTags = new ArrayList<>();

    // Image picker launcher - using GetContent like ProfileActivity
    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    if (selectedImageUris.size() < 3) {
                        selectedImageUris.add(uri);
                        updateImagePreviews();
                    } else {
                        Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase
        dbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("recipes");

        binding.buttonCancel.setOnClickListener(v -> finish());
        binding.buttonSave.setOnClickListener(v -> saveRecipe());

        binding.buttonImageInput.setOnClickListener(v -> {
            if (selectedImageUris.size() < 3) {
                imagePickerLauncher.launch("image/*");
            } else {
                Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
            }
        });

        // Remove image buttons
        binding.buttonRemoveImage1.setOnClickListener(v -> removeImage(0));
        binding.buttonRemoveImage2.setOnClickListener(v -> removeImage(1));
        binding.buttonRemoveImage3.setOnClickListener(v -> removeImage(2));

        binding.buttonTagInput.setOnClickListener(v -> showTagDialog());

        loadTagsFromDatabase();
    }

    private void removeImage(int index) {
        if (index >= 0 && index < selectedImageUris.size()) {
            selectedImageUris.remove(index);
            updateImagePreviews();
        }
    }

    private void updateImagePreviews() {
        // Reset all image views
        binding.imageViewSelected1.setVisibility(android.view.View.GONE);
        binding.imageViewSelected2.setVisibility(android.view.View.GONE);
        binding.imageViewSelected3.setVisibility(android.view.View.GONE);
        binding.buttonRemoveImage1.setVisibility(android.view.View.GONE);
        binding.buttonRemoveImage2.setVisibility(android.view.View.GONE);
        binding.buttonRemoveImage3.setVisibility(android.view.View.GONE);

        // show selected images
        if (selectedImageUris.size() > 0) {
            Glide.with(this).load(selectedImageUris.get(0)).into(binding.imageViewSelected1);
            binding.imageViewSelected1.setVisibility(android.view.View.VISIBLE);
            binding.buttonRemoveImage1.setVisibility(android.view.View.VISIBLE);
        }
        if (selectedImageUris.size() > 1) {
            Glide.with(this).load(selectedImageUris.get(1)).into(binding.imageViewSelected2);
            binding.imageViewSelected2.setVisibility(android.view.View.VISIBLE);
            binding.buttonRemoveImage2.setVisibility(android.view.View.VISIBLE);
        }
        if (selectedImageUris.size() > 2) {
            Glide.with(this).load(selectedImageUris.get(2)).into(binding.imageViewSelected3);
            binding.imageViewSelected3.setVisibility(android.view.View.VISIBLE);
            binding.buttonRemoveImage3.setVisibility(android.view.View.VISIBLE);
        }
    }

    // load tags collection
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

    // save recipe
    private void saveRecipe() {
        String recipeName = binding.editTextRecipeName.getText().toString().trim();
        String source = binding.editTextSourceInput.getText().toString().trim();
        String ingredientsInput = binding.editTextIngredientsInput.getText().toString().trim();
        String stepsInput = binding.editTextInstructionsInput.getText().toString().trim();
        String recipeDescription = binding.editTextNotesInput.getText().toString().trim();

        double servesPax = parseDouble(binding.editTextServesInput.getText().toString());
        double prepTime = parseDouble(binding.editTextPrepTimeInput.getText().toString());
        double cookTime = parseDouble(binding.editTextCookTimeInput.getText().toString());

        // Validation - everything required except source and notes
        if (recipeName.isEmpty()) {
            binding.editTextRecipeName.setError("Recipe name is required");
            binding.editTextRecipeName.requestFocus();
            return;
        }

        if (selectedTags.isEmpty()) {
            Toast.makeText(this, "Please select at least one tag", Toast.LENGTH_SHORT).show();
            return;
        }

        if (servesPax <= 0) {
            binding.editTextServesInput.setError("Serves must be greater than 0");
            binding.editTextServesInput.requestFocus();
            return;
        }

        if (prepTime <= 0) {
            binding.editTextPrepTimeInput.setError("Prep time must be greater than 0");
            binding.editTextPrepTimeInput.requestFocus();
            return;
        }

        if (cookTime <= 0) {
            binding.editTextCookTimeInput.setError("Cook time must be greater than 0");
            binding.editTextCookTimeInput.requestFocus();
            return;
        }

        if (ingredientsInput.isEmpty()) {
            binding.editTextIngredientsInput.setError("At least one ingredient is required");
            binding.editTextIngredientsInput.requestFocus();
            return;
        }

        if (stepsInput.isEmpty()) {
            binding.editTextInstructionsInput.setError("At least one instruction step is required");
            binding.editTextInstructionsInput.requestFocus();
            return;
        }

        // convert ingredients to a List<String>, separating the strings per line
        List<String> ingredientsList = new ArrayList<>();
        if (!ingredientsInput.isEmpty()) {
            // Split by newline
            String[] items = ingredientsInput.split("\\r?\\n");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) ingredientsList.add(trimmed);
            }
        }

        // convert steps to a List<String>, separating the strings per line
        List<String> stepsList = new ArrayList<>();
        if (!stepsInput.isEmpty()) {
            String[] stepsArray = stepsInput.split("\\r?\\n");
            for (String step : stepsArray) {
                String trimmed = step.trim();
                if (!trimmed.isEmpty()) stepsList.add(trimmed);
            }
        }

        // use the tags selected from the popup
        List<String> tagsList = selectedTags;

        // save recipe
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

    private void saveRecipeToFirestore(String recipeName, String source,
                                       List<String> ingredients, List<String> steps, String recipeDescription,
                                       double servesPax, double prepTime, double cookTime, List<String> tags) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) return;

        // Upload images first, then save recipe with URLs
        uploadImagesAndSaveRecipe(recipeName, source, ingredients, steps, recipeDescription,
                servesPax, prepTime, cookTime, tags, currentUserId);
    }

    private void uploadImagesAndSaveRecipe(String recipeName, String source,
                                          List<String> ingredients, List<String> steps, String recipeDescription,
                                          double servesPax, double prepTime, double cookTime, List<String> tags,
                                          String currentUserId) {
        // If no images selected, save recipe with empty image list
        if (selectedImageUris.isEmpty()) {
            saveRecipeWithImageUrls(recipeName, source, ingredients, steps, recipeDescription,
                    servesPax, prepTime, cookTime, tags, new ArrayList<>(), currentUserId);
            return;
        }

        // Upload all images to Firebase Storage
        ArrayList<String> imageUrls = new ArrayList<>();
        int totalImages = selectedImageUris.size();
        final int[] uploadedCount = {0};

        for (int i = 0; i < selectedImageUris.size(); i++) {
            Uri imageUri = selectedImageUris.get(i);
            String imageFileName = "recipe_" + System.currentTimeMillis() + "_" + i + ".jpg";
            StorageReference imageRef = storageRef.child(imageFileName);

            UploadTask uploadTask = imageRef.putFile(imageUri);
            int finalI = i;
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    imageUrls.add(downloadUri.toString());
                    uploadedCount[0]++;

                    // upload images first then save recipe
                    if (uploadedCount[0] == totalImages) {
                        saveRecipeWithImageUrls(recipeName, source, ingredients, steps, recipeDescription,
                                servesPax, prepTime, cookTime, tags, imageUrls, currentUserId);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("AddRecipe", "Failed to get download URL for image " + finalI, e);
                    e.printStackTrace();
                    uploadedCount[0]++;
                    Toast.makeText(this, "Failed to upload image " + (finalI + 1) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // If all uploads are done (success or failure), still try to save with what we have
                    if (uploadedCount[0] == totalImages && !imageUrls.isEmpty()) {
                        saveRecipeWithImageUrls(recipeName, source, ingredients, steps, recipeDescription,
                                servesPax, prepTime, cookTime, tags, imageUrls, currentUserId);
                    } else if (uploadedCount[0] == totalImages && imageUrls.isEmpty()) {
                        Toast.makeText(this, "All image uploads failed. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e("AddRecipe", "Failed to upload image " + finalI + " to storage", e);
                e.printStackTrace();
                uploadedCount[0]++;
                Toast.makeText(this, "Failed to upload image " + (finalI + 1) + " to storage: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // If all uploads are done (success or failure), still try to save with what we have
                if (uploadedCount[0] == totalImages && !imageUrls.isEmpty()) {
                    saveRecipeWithImageUrls(recipeName, source, ingredients, steps, recipeDescription,
                            servesPax, prepTime, cookTime, tags, imageUrls, currentUserId);
                } else if (uploadedCount[0] == totalImages && imageUrls.isEmpty()) {
                    Toast.makeText(this, "All image uploads failed. Please try again.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void saveRecipeWithImageUrls(String recipeName, String source,
                                         List<String> ingredients, List<String> steps, String recipeDescription,
                                         double servesPax, double prepTime, double cookTime, List<String> tags,
                                         ArrayList<String> imageUrls, String currentUserId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // create a reference for the author (users/[author])
        DocumentReference authorRef = db.collection("users").document(currentUserId);
        Map<String, Object> authorMap = new HashMap<>();
        authorMap.put("uid", authorRef);

        // set up the recipe model
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
        recipe.put("recipeImages", imageUrls); 
        recipe.put("reviews", new ArrayList<>()); // Will hold DocumentReferences to reviews collection
        recipe.put("author", authorMap);

        // save to cloud db
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

    // update the text view below tags
    private void updateSelectedTagsTextView() {
        if (selectedTags.isEmpty()) {
            binding.textViewSelectedTags.setText("No tags selected");
        } else {
            binding.textViewSelectedTags.setText(String.join(", ", selectedTags));
        }
    }

    // show tag popup
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

        builder.setPositiveButton("Done", (dialog, which) -> {
            updateSelectedTagsTextView();
        });

        builder.create().show();
    }

}
