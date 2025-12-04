package com.example.plately;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.plately.databinding.ActivityEditRecipeBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditRecipeActivity extends AppCompatActivity {

    private ActivityEditRecipeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storageRef;

    private String recipeId;
    private DocumentReference recipeRef;
    private ArrayList<Uri> selectedImageUris = new ArrayList<>();
    private ArrayList<String> existingImageUrls = new ArrayList<>();
    private ArrayList<String> imagesToDelete = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityEditRecipeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("recipe_images");

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

        binding.editButtonImageInput.setOnClickListener(v -> {
            if (getTotalImageCount() < 3) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
                imagePickerLauncher.launch(intent);
            } else {
                Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
            }
        });

        // remove image buttons
        binding.editButtonRemoveImage1.setOnClickListener(v -> removeImage(0));
        binding.editButtonRemoveImage2.setOnClickListener(v -> removeImage(1));
        binding.editButtonRemoveImage3.setOnClickListener(v -> removeImage(2));
    }

    // image picker launcher - using OpenDocument for persistent access
    ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        if (getTotalImageCount() < 3) {
                            // Take persistent URI permission to ensure Firebase can access it
                            try {
                                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                                ContentResolver resolver = getContentResolver();
                                resolver.takePersistableUriPermission(uri, takeFlags);
                                selectedImageUris.add(uri);
                                updateImagePreviews();
                            } catch (SecurityException e) {
                                Log.e("EditRecipe", "Failed to take persistent URI permission", e);
                                Toast.makeText(this, "Failed to access image. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private int getTotalImageCount() {
        return existingImageUrls.size() + selectedImageUris.size() - imagesToDelete.size();
    }

    private void removeImage(int index) {
        if (index < existingImageUrls.size()) {
            // Remove existing image
            imagesToDelete.add(existingImageUrls.get(index));
            existingImageUrls.remove(index);
        } else {
            // Remove newly selected image
            int newIndex = index - existingImageUrls.size();
            if (newIndex >= 0 && newIndex < selectedImageUris.size()) {
                selectedImageUris.remove(newIndex);
            }
        }
        updateImagePreviews();
    }

    private void updateImagePreviews() {
        // reset all image views
        binding.editImageViewSelected1.setVisibility(android.view.View.GONE);
        binding.editImageViewSelected2.setVisibility(android.view.View.GONE);
        binding.editImageViewSelected3.setVisibility(android.view.View.GONE);
        binding.editButtonRemoveImage1.setVisibility(android.view.View.GONE);
        binding.editButtonRemoveImage2.setVisibility(android.view.View.GONE);
        binding.editButtonRemoveImage3.setVisibility(android.view.View.GONE);

        int displayIndex = 0;

        // show existing images
        for (int i = 0; i < existingImageUrls.size() && displayIndex < 3; i++) {
            String imageUrl = existingImageUrls.get(i);
            if (displayIndex == 0) {
                Glide.with(this).load(imageUrl).into(binding.editImageViewSelected1);
                binding.editImageViewSelected1.setVisibility(android.view.View.VISIBLE);
                binding.editButtonRemoveImage1.setVisibility(android.view.View.VISIBLE);
            } else if (displayIndex == 1) {
                Glide.with(this).load(imageUrl).into(binding.editImageViewSelected2);
                binding.editImageViewSelected2.setVisibility(android.view.View.VISIBLE);
                binding.editButtonRemoveImage2.setVisibility(android.view.View.VISIBLE);
            } else if (displayIndex == 2) {
                Glide.with(this).load(imageUrl).into(binding.editImageViewSelected3);
                binding.editImageViewSelected3.setVisibility(android.view.View.VISIBLE);
                binding.editButtonRemoveImage3.setVisibility(android.view.View.VISIBLE);
            }
            displayIndex++;
        }

        // show newly selected images
        for (int i = 0; i < selectedImageUris.size() && displayIndex < 3; i++) {
            Uri imageUri = selectedImageUris.get(i);
            if (displayIndex == 0) {
                Glide.with(this).load(imageUri).into(binding.editImageViewSelected1);
                binding.editImageViewSelected1.setVisibility(android.view.View.VISIBLE);
                binding.editButtonRemoveImage1.setVisibility(android.view.View.VISIBLE);
            } else if (displayIndex == 1) {
                Glide.with(this).load(imageUri).into(binding.editImageViewSelected2);
                binding.editImageViewSelected2.setVisibility(android.view.View.VISIBLE);
                binding.editButtonRemoveImage2.setVisibility(android.view.View.VISIBLE);
            } else if (displayIndex == 2) {
                Glide.with(this).load(imageUri).into(binding.editImageViewSelected3);
                binding.editImageViewSelected3.setVisibility(android.view.View.VISIBLE);
                binding.editButtonRemoveImage3.setVisibility(android.view.View.VISIBLE);
            }
            displayIndex++;
        }
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
            
            // Get prep and cook time (stored as minutes)
            Object prepTimeObj = doc.get("prepTime");
            Object cookTimeObj = doc.get("cookTime");
            double prepTime = prepTimeObj instanceof Number ? ((Number) prepTimeObj).doubleValue() : 0.0;
            double cookTime = cookTimeObj instanceof Number ? ((Number) cookTimeObj).doubleValue() : 0.0;
            binding.editEditTextPrepTimeInput.setText(prepTime > 0 ? String.valueOf((int) prepTime) : "");
            binding.editEditTextCookTimeInput.setText(cookTime > 0 ? String.valueOf((int) cookTime) : "");
            binding.editEditTextIngredientsInput.setText(joinStringList(doc.get("ingredients")));
            binding.editEditTextInstructionsInput.setText(joinStringList(doc.get("steps")));
            binding.editEditTextNotesInput.setText(joinStringList(doc.get("notes")));

            // load existing images
            @SuppressWarnings("unchecked")
            ArrayList<String> recipeImages = (ArrayList<String>) doc.get("recipeImages");
            if (recipeImages != null) {
                existingImageUrls = new ArrayList<>(recipeImages);
                updateImagePreviews();
            }
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
        double prepTime = parseDouble(binding.editEditTextPrepTimeInput.getText().toString().trim());
        double cookTime = parseDouble(binding.editEditTextCookTimeInput.getText().toString().trim());
        List<String> ingredients = splitLines(binding.editEditTextIngredientsInput.getText().toString());
        List<String> steps = splitLines(binding.editEditTextInstructionsInput.getText().toString());
        List<String> notes = splitLines(binding.editEditTextNotesInput.getText().toString());

        // upload new images first then update recipe
        if (!selectedImageUris.isEmpty()) {
            uploadNewImagesAndUpdate(recipeName, source, serves, prepTime, cookTime, ingredients, steps, notes);
        } else {
            // no new images, just update with existing images
            updateRecipeWithImageUrls(recipeName, source, serves, prepTime, cookTime, ingredients, steps, notes, existingImageUrls);
        }
    }

    private void uploadNewImagesAndUpdate(String recipeName, String source, int serves,
                                         double prepTime, double cookTime, List<String> ingredients,
                                         List<String> steps, List<String> notes) {
        ArrayList<String> newImageUrls = new ArrayList<>();
        int totalImages = selectedImageUris.size();
        final int[] uploadedCount = {0};

        for (int i = 0; i < selectedImageUris.size(); i++) {
            Uri imageUri = selectedImageUris.get(i);
            String imageFileName = "recipe_" + System.currentTimeMillis() + "_" + i + ".jpg";
            StorageReference imageRef = storageRef.child(imageFileName);

            UploadTask uploadTask = imageRef.putFile(imageUri);
            int finalI = i;
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    newImageUrls.add(downloadUri.toString());
                    uploadedCount[0]++;

                    if (uploadedCount[0] == totalImages) {
                        // Combine existing and new image URLs
                        ArrayList<String> allImageUrls = new ArrayList<>(existingImageUrls);
                        allImageUrls.addAll(newImageUrls);
                        updateRecipeWithImageUrls(recipeName, source, serves, prepTime, cookTime, ingredients, steps, notes, allImageUrls);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("EditRecipe", "Failed to get download URL for image " + finalI, e);
                    e.printStackTrace();
                    uploadedCount[0]++;
                    Toast.makeText(this, "Failed to upload image " + (finalI + 1) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // If all uploads are done (success or failure), still try to update with what we have
                    if (uploadedCount[0] == totalImages) {
                        ArrayList<String> allImageUrls = new ArrayList<>(existingImageUrls);
                        allImageUrls.addAll(newImageUrls);
                        updateRecipeWithImageUrls(recipeName, source, serves, prepTime, cookTime, ingredients, steps, notes, allImageUrls);
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e("EditRecipe", "Failed to upload image " + finalI + " to storage", e);
                e.printStackTrace();
                uploadedCount[0]++;
                Toast.makeText(this, "Failed to upload image " + (finalI + 1) + " to storage: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // If all uploads are done (success or failure), still try to update with what we have
                if (uploadedCount[0] == totalImages) {
                    ArrayList<String> allImageUrls = new ArrayList<>(existingImageUrls);
                    allImageUrls.addAll(newImageUrls);
                    updateRecipeWithImageUrls(recipeName, source, serves, prepTime, cookTime, ingredients, steps, notes, allImageUrls);
                }
            });
        }
    }

    private void updateRecipeWithImageUrls(String recipeName, String source, int serves,
                                           double prepTime, double cookTime, List<String> ingredients,
                                           List<String> steps, List<String> notes,
                                           ArrayList<String> imageUrls) {
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("recipeName", recipeName);
        updatedData.put("source", source);
        updatedData.put("servesPax", serves);
        updatedData.put("prepTime", prepTime);
        updatedData.put("cookTime", cookTime);
        updatedData.put("ingredients", ingredients);
        updatedData.put("steps", steps);
        updatedData.put("notes", notes);
        updatedData.put("recipeImages", imageUrls); // Save as ArrayList<String>

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

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
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
