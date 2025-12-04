package com.example.plately;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.plately.databinding.ActivityRecipeDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ActivityRecipeDetailsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private StorageReference storageRef;
    private String recipeId;
    private String authorId;
    private boolean isFavorite = false;
    private List<ReviewModel> reviewList = new ArrayList<>();
    private ReviewAdapter reviewAdapter;
    private ArrayList<Uri> selectedReviewImageUris = new ArrayList<>();
    
    // slideshow stuff
    private Handler slideshowHandler;
    private Runnable slideshowRunnable;
    private ArrayList<String> recipeImageUrls = new ArrayList<>();
    private int currentImageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRecipeDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("reviews");

        setupRecyclerView();

        // submit review button
        binding.buttonSubmitReview.setOnClickListener(v -> submitReview());

        recipeId = getIntent().getStringExtra("recipeId");
        if (recipeId == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String title = getIntent().getStringExtra("title");
        if (title != null) {
            binding.textViewRecipeName.setText(title);
        }

        isFavorite = getIntent().getBooleanExtra("isFavorite", false);
        binding.imageBtnSaveRecipe.setImageResource(
                isFavorite ? R.drawable.baseline_bookmark_24 : R.drawable.outline_bookmark_24
        );

        binding.imageBtnSaveRecipe.setOnClickListener(v -> toggleFavorite());

        binding.imageBtnViewMore.setOnClickListener(v -> showOptionsMenu());


        // get the recipe from main menu then update the UI (wip)
        db.collection("recipes").document(recipeId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    RecipeModel recipe = snapshot.toObject(RecipeModel.class);

                    if (recipe == null) {
                        Toast.makeText(this, "Error loading recipe", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Safely get author ID
                    if (recipe.getAuthor() != null && recipe.getAuthor().get("uid") != null) {
                        authorId = recipe.getAuthor().get("uid").getId();
                    }
                    String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

                    if (authorId != null && currentUid != null) {
                        applyAuthorVisibility(authorId, currentUid);
                    }

                    displayRecipeDetails(recipe);
                    loadReviews();
                 })
                  .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed loading recipe", Toast.LENGTH_SHORT).show();
                    finish();
               });
        

        // Apply Window Insets (Safety for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMainRecipeDetails), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // handle favorite button actions
    private void toggleFavorite() {
        isFavorite = !isFavorite;

        binding.imageBtnSaveRecipe.setImageResource(
                isFavorite ? R.drawable.baseline_bookmark_24 : R.drawable.outline_bookmark_24
        );

        String uid = auth.getUid();
        if (uid == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);

        if (isFavorite) {
            db.collection("users").document(uid)
                    .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayUnion(recipeRef))
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("users").document(uid)
                    .update("savedRecipes", com.google.firebase.firestore.FieldValue.arrayRemove(recipeRef))
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Removed!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show());
        }

        // send result back to main menu for consistency
        Intent resultIntent = new Intent();
        resultIntent.putExtra("recipeId", recipeId);
        resultIntent.putExtra("isFavorite", isFavorite);
        setResult(RESULT_OK, resultIntent);
    }

    // show options once view more button is clicked
    private void showOptionsMenu() {
        PopupMenu popup = new PopupMenu(this, binding.imageBtnViewMore);
        popup.getMenuInflater().inflate(R.menu.menu_recipe_options, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_edit_recipe) {
                openEditRecipe();
                return true;
            } else if (id == R.id.menu_delete_recipe) {
                confirmDelete();
                return true;
            }
            return false;
        });
        popup.show();
    }

    // launch edit recipe if user wants to edit
    private void openEditRecipe() {
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra("recipeId", recipeId);
        editRecipeLauncher.launch(intent);
    }

    // launcher for editRecipeActivity to get result and refresh
    private final ActivityResultLauncher<Intent> editRecipeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // recipe update - reload the recipe details -
                    if (recipeId != null) {
                        db.collection("recipes").document(recipeId)
                                .get()
                                .addOnSuccessListener(snapshot -> {
                                    if (snapshot.exists()) {
                                        RecipeModel recipe = snapshot.toObject(RecipeModel.class);
                                        if (recipe != null) {
                                            displayRecipeDetails(recipe);
                                        }
                                    }
                                });
                    }
                }
            }
    );

    // show alert before deletion
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipe())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // handle delete functionality by deleting it from db o7
    private void deleteRecipe() {
        if (recipeId == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUid = FirebaseAuth.getInstance().getUid();

        db.collection("users")
                .whereArrayContains("savedRecipes", recipeRef)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();

                    // delete the recipe from recipes
                    batch.delete(recipeRef);

                    // remove recipe from all users' savedRecipes
                    for (DocumentSnapshot userDoc : querySnapshot.getDocuments()) {
                        batch.update(userDoc.getReference(),
                                "savedRecipes", FieldValue.arrayRemove(recipeRef));
                    }

                    // remove recipe from author's createdRecipes
                    if (currentUid != null) {
                        DocumentReference authorRef = db.collection("users").document(currentUid);
                        batch.update(authorRef, "createdRecipes", FieldValue.arrayRemove(recipeRef));
                    }

                    // Commit batch
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show();
                                finish(); // return to main
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete recipe", Toast.LENGTH_SHORT).show()
                            );
                });
    }

    // author visibility for the favorite and view more buttons
    private void applyAuthorVisibility(String authorId, String currentUid) {

        if (currentUid != null && currentUid.equals(authorId)) {
            // case 1: user is author of recipe
            binding.imageBtnSaveRecipe.setVisibility(View.GONE);      // hide favorite button
            binding.imageBtnViewMore.setVisibility(View.VISIBLE);     // show view more button - allow edit/delete
        } else {
            // case 2: user is not the author of the recipe
            binding.imageBtnSaveRecipe.setVisibility(View.VISIBLE);   // show favorite button
            binding.imageBtnViewMore.setVisibility(View.GONE);        // hide view more button
        }
    }

    private void displayRecipeDetails(RecipeModel recipe) {
        // recipe name
        binding.textViewRecipeName.setText(recipe.getRecipeName() != null ? recipe.getRecipeName() : "Untitled");

        // stop any existing slideshow
        stopSlideshow();

        // get recipe images
        recipeImageUrls = recipe.getRecipeImages();
        if (recipeImageUrls == null) {
            recipeImageUrls = new ArrayList<>();
        }

        // image - use first image from recipeImages array, with fallback to recipeImage
        String imageUrl = recipe.getFirstImage();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(binding.imageViewRecipePhoto);
        }

        // start slideshow if there are 2 or more images
        if (recipeImageUrls.size() > 1) {
            startSlideshow();
        }

        // description
        binding.textViewFullDescription.setText(recipe.getRecipeDescription() != null ? recipe.getRecipeDescription() : "No description available");

        // num servings
        binding.textViewServings.setText("Servings: " + (int) recipe.getServesPax());

        // prep & cook time (formatted as hours and minutes)
        binding.textViewPrepTime.setText("Prep time: " + formatTimeInHoursAndMinutes(recipe.getPrepTime()));
        binding.textViewCookingTime.setText("Cooking time: " + formatTimeInHoursAndMinutes(recipe.getCookTime()));
        binding.textViewTotalTime.setText("Total time: " + formatTimeInHoursAndMinutes(recipe.getPrepTime() + recipe.getCookTime()));

        // tags
        if (recipe.getTags() != null && !recipe.getTags().isEmpty()) {
            binding.textViewTagsList.setText(String.join(", ", recipe.getTags()));
        } else {
            binding.textViewTagsList.setText("No tags");
        }

        // ingredients
        binding.ingredientsContainer.removeAllViews();
        if (recipe.getIngredients() != null) {
            for (String ingredient : recipe.getIngredients()) {
                TextView tv = new TextView(this);
                tv.setText("• " + ingredient);
                tv.setTextSize(16f);
                tv.setPadding(0, 0, 0, 8);
                binding.ingredientsContainer.addView(tv);
            }
        }

        // steps
        binding.stepsContainer.removeAllViews();
        if (recipe.getSteps() != null && !recipe.getSteps().isEmpty()) {
            int stepNumber = 1;
            for (String step : recipe.getSteps()) {
                TextView tv = new TextView(this);
                tv.setText(stepNumber + ". " + step);
                tv.setTextSize(16f);
                tv.setPadding(0, 0, 0, 8);
                binding.stepsContainer.addView(tv);
                stepNumber++;
            }
        } else if (recipe.getInstructions() != null) {
            String[] stepsArr = recipe.getInstructions().split("\\r?\\n");
            int stepNumber = 1;
            for (String step : stepsArr) {
                TextView tv = new TextView(this);
                tv.setText(stepNumber + ". " + step);
                tv.setTextSize(16f);
                tv.setPadding(0, 0, 0, 8);
                binding.stepsContainer.addView(tv);
                stepNumber++;
            }
        }

        // only show source if not empty
        if (recipe.getSource() != null && !recipe.getSource().trim().isEmpty()) {
            binding.textViewSource.setText(recipe.getSource());
            binding.textViewSource.setVisibility(View.VISIBLE);
            View sourceLabel = findViewById(R.id.textViewSourceLabel);
            if (sourceLabel != null) {
                sourceLabel.setVisibility(View.VISIBLE);
            }
        } else {
            // hide source label & body
            binding.textViewSource.setVisibility(View.GONE);
            View sourceLabel = findViewById(R.id.textViewSourceLabel);
            if (sourceLabel != null) {
                sourceLabel.setVisibility(View.GONE);
            }
        }

        // recipe author
        if (recipe.getAuthor() != null && recipe.getAuthor().get("uid") != null) {
            recipe.getAuthor().get("uid").get()
                    .addOnSuccessListener(docSnap -> {
                        String username = docSnap.getString("username");
                        binding.textViewAuthor.setText("By: " + (username != null ? username : "Unknown"));
                    })
                    .addOnFailureListener(e -> binding.textViewAuthor.setText("By: Unknown"));
        }

        // camera activity for reviews
        binding.imageBtnAddPhotoReview.setOnClickListener(v -> {
            // Launch camera activity
            Intent intent = new Intent(RecipeDetailsActivity.this, CameraActivity.class);
            reviewCameraLauncher.launch(intent);
        });

        // gallery picker for reviews
        binding.imageBtnAddPhotoGallery.setOnClickListener(v -> {
            if (selectedReviewImageUris.size() < 3) {
                reviewImagePickerLauncher.launch("image/*");
            } else {
                Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
            }
        });

        // remove review image buttons
        binding.buttonRemoveReviewImage1.setOnClickListener(v -> removeReviewImage(0));
        binding.buttonRemoveReviewImage2.setOnClickListener(v -> removeReviewImage(1));
        binding.buttonRemoveReviewImage3.setOnClickListener(v -> removeReviewImage(2));
    }

    private void removeReviewImage(int index) {
        if (index >= 0 && index < selectedReviewImageUris.size()) {
            selectedReviewImageUris.remove(index);
            updateReviewImagePreviews();
        }
    }

    private void updateReviewImagePreviews() {
        // Show/hide the entire preview container based on whether there are images
        if (selectedReviewImageUris.isEmpty()) {
            binding.linearLayoutReviewImagePreviews.setVisibility(View.GONE);
            return;
        }

        // Show the preview container
        binding.linearLayoutReviewImagePreviews.setVisibility(View.VISIBLE);

        // reset all image views
        binding.imageViewReviewPreview1.setVisibility(View.GONE);
        binding.imageViewReviewPreview2.setVisibility(View.GONE);
        binding.imageViewReviewPreview3.setVisibility(View.GONE);
        binding.buttonRemoveReviewImage1.setVisibility(View.GONE);
        binding.buttonRemoveReviewImage2.setVisibility(View.GONE);
        binding.buttonRemoveReviewImage3.setVisibility(View.GONE);

        // show selected images
        if (selectedReviewImageUris.size() > 0) {
            Glide.with(this).load(selectedReviewImageUris.get(0)).into(binding.imageViewReviewPreview1);
            binding.imageViewReviewPreview1.setVisibility(View.VISIBLE);
            binding.buttonRemoveReviewImage1.setVisibility(View.VISIBLE);
        }
        if (selectedReviewImageUris.size() > 1) {
            Glide.with(this).load(selectedReviewImageUris.get(1)).into(binding.imageViewReviewPreview2);
            binding.imageViewReviewPreview2.setVisibility(View.VISIBLE);
            binding.buttonRemoveReviewImage2.setVisibility(View.VISIBLE);
        }
        if (selectedReviewImageUris.size() > 2) {
            Glide.with(this).load(selectedReviewImageUris.get(2)).into(binding.imageViewReviewPreview3);
            binding.imageViewReviewPreview3.setVisibility(View.VISIBLE);
            binding.buttonRemoveReviewImage3.setVisibility(View.VISIBLE);
        }
    }

    // Image picker launcher for review gallery - using GetContent like ProfileActivity
    private final ActivityResultLauncher<String> reviewImagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    if (selectedReviewImageUris.size() < 3) {
                        selectedReviewImageUris.add(uri);
                        updateReviewImagePreviews();
                    } else {
                        Toast.makeText(this, "You can only select up to 3 images", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // submit and add review to db
    private void submitReview() {
        String reviewText = binding.editTextReview.getText().toString().trim();
        float rating = binding.ratingBarUser.getRating();

        if (reviewText.isEmpty()) {
            binding.editTextReview.setError("Review cannot be empty");
            return;
        }

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            Toast.makeText(this, "You must be logged in to submit a review", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload images first, then save review with URLs
        uploadReviewImagesAndSave(reviewText, rating, currentUserId);
    }

    private void uploadReviewImagesAndSave(String reviewText, float rating, String currentUserId) {
        // If no images selected, save review with empty image list
        if (selectedReviewImageUris.isEmpty()) {
            saveReviewToFirestore(reviewText, rating, currentUserId, new ArrayList<>());
            return;
        }

        // Upload all images to Firebase Storage
        ArrayList<String> imageUrls = new ArrayList<>();
        int totalImages = selectedReviewImageUris.size();
        final int[] uploadedCount = {0};

        for (int i = 0; i < selectedReviewImageUris.size(); i++) {
            Uri imageUri = selectedReviewImageUris.get(i);
            String imageFileName = "review_" + System.currentTimeMillis() + "_" + i + ".jpg";
            StorageReference imageRef = storageRef.child(imageFileName);

            UploadTask uploadTask = imageRef.putFile(imageUri);
            int finalI = i;
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                    imageUrls.add(downloadUri.toString());
                    uploadedCount[0]++;

                    // upload images first then save review
                    if (uploadedCount[0] == totalImages) {
                        saveReviewToFirestore(reviewText, rating, currentUserId, imageUrls);
                    }
                }).addOnFailureListener(e -> {
                    Log.e("RecipeDetails", "Failed to get download URL for review image " + finalI, e);
                    e.printStackTrace();
                    uploadedCount[0]++;
                    Toast.makeText(this, "Failed to upload image " + (finalI + 1) + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // If all uploads are done (success or failure), still try to save with what we have
                    if (uploadedCount[0] == totalImages && !imageUrls.isEmpty()) {
                        saveReviewToFirestore(reviewText, rating, currentUserId, imageUrls);
                    } else if (uploadedCount[0] == totalImages && imageUrls.isEmpty()) {
                        // Save review without images if all uploads failed
                        saveReviewToFirestore(reviewText, rating, currentUserId, new ArrayList<>());
                    }
                });
            }).addOnFailureListener(e -> {
                Log.e("RecipeDetails", "Failed to upload review image " + finalI + " to storage", e);
                e.printStackTrace();
                uploadedCount[0]++;
                Toast.makeText(this, "Failed to upload image " + (finalI + 1) + " to storage: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // If all uploads are done (success or failure), still try to save with what we have
                if (uploadedCount[0] == totalImages && !imageUrls.isEmpty()) {
                    saveReviewToFirestore(reviewText, rating, currentUserId, imageUrls);
                } else if (uploadedCount[0] == totalImages && imageUrls.isEmpty()) {
                    // Save review without images if all uploads failed
                    saveReviewToFirestore(reviewText, rating, currentUserId, new ArrayList<>());
                }
            });
        }
    }

    private void saveReviewToFirestore(String reviewText, float rating, String currentUserId, ArrayList<String> imageUrls) {
        // author reference
        DocumentReference authorRef = db.collection("users").document(currentUserId);
        Map<String, Object> authorMap = new HashMap<>();
        authorMap.put("uid", authorRef);

        // recipe reference
        DocumentReference recipeRef = db.collection("recipes").document(recipeId);

        // review map to be added
        Map<String, Object> reviewMap = new HashMap<>();
        reviewMap.put("author", authorMap);
        reviewMap.put("rating", rating);
        reviewMap.put("text", reviewText);
        reviewMap.put("recipeRef", recipeRef);
        reviewMap.put("reviewImages", imageUrls);

        // add review to reviews collection
        db.collection("reviews").add(reviewMap)
                .addOnSuccessListener(reviewDocRef -> {
                    Log.d("RecipeDetails", "Review created with ID: " + reviewDocRef.getId());
                    // add review reference to user createdReviews array
                    authorRef.update("createdReviews", FieldValue.arrayUnion(reviewDocRef))
                            .addOnSuccessListener(aVoid -> {
                                Log.d("RecipeDetails", "Added review to user's createdReviews");
                                // add review reference to the recipe's reviews array
                                recipeRef.update("reviews", FieldValue.arrayUnion(reviewDocRef))
                                        .addOnSuccessListener(aVoid2 -> {
                                            Log.d("RecipeDetails", "Successfully added review reference to recipe");
                                            Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();

                                            // after adding reset the inputs
                                            binding.editTextReview.setText("");
                                            binding.ratingBarUser.setRating(0);
                                            binding.editTextReview.clearFocus();
                                            selectedReviewImageUris.clear();
                                            updateReviewImagePreviews();
                                            loadReviews();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("RecipeDetails", "Failed to update recipe with review reference", e);
                                            Toast.makeText(this, "Failed to update recipe with review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            loadReviews();
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("RecipeDetails", "Failed to add review to user profile", e);
                                Toast.makeText(this, "Failed to add review to user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                // try to update recipe still and refresh
                                recipeRef.update("reviews", FieldValue.arrayUnion(reviewDocRef))
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("RecipeDetails", "Added review to recipe after user update failed");
                                            loadReviews();
                                        })
                                        .addOnFailureListener(e2 -> {
                                            Log.e("RecipeDetails", "Failed to add review to recipe in fallback", e2);
                                            loadReviews();
                                        });
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("RecipeDetails", "Failed to create review document", e);
                    Toast.makeText(this, "Failed to submit review: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(reviewList);
        reviewAdapter.setDeleteListener(reviewId -> {
            // Refresh reviews after deletion
            loadReviews();
        });
        binding.recyclerViewReviews.setAdapter(reviewAdapter);
        binding.recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadReviews() {
        if (recipeId == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);
        String currentUserId = auth.getUid();

        db.collection("reviews")
                .whereEqualTo("recipeRef", recipeRef)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) return;

                    reviewList.clear();
                    float totalRating = 0;
                    int reviewCount = 0;
                    boolean userHasReview = false;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ReviewModel review = doc.toObject(ReviewModel.class);
                        if (review != null) {
                            // Set the document ID for deletion purposes
                            review.setReviewId(doc.getId());
                            reviewList.add(review);
                            totalRating += review.getRating();
                            reviewCount++;

                            // Check if current user already has a review
                            if (currentUserId != null && review.getAuthor() != null && 
                                review.getAuthor().get("uid") != null &&
                                review.getAuthor().get("uid").getId().equals(currentUserId)) {
                                userHasReview = true;
                            }
                        }
                    }
                    reviewAdapter.notifyDataSetChanged();

                    // get and display overall rating
                    updateOverallRating(totalRating, reviewCount);

                    // check if review section should be disabled
                    checkAndDisableReviewSection(userHasReview);
                });
    }

    private void updateOverallRating(float totalRating, int reviewCount) {
        if (reviewCount == 0) {
            binding.ratingBarOverall.setRating(0);
            return;
        }

        float averageRating = totalRating / reviewCount;
        
        // If x.0 - x.4, round down = x, if x.5 - x.9, show x.5 stars
        float displayRating;
        int wholePart = (int) averageRating;
        float decimalPart = averageRating - wholePart;
        
        if (decimalPart >= 0.5f) {
            displayRating = wholePart + 0.5f;
        } else {
            displayRating = wholePart;
        }
        
        binding.ratingBarOverall.setRating(displayRating);
    }

    // disable review section if user is author or already has a review
    private void checkAndDisableReviewSection(boolean userHasReview) {
        if (recipeId == null || authorId == null) return;

        String currentUserId = auth.getUid();
        if (currentUserId == null) return;

        boolean isAuthor = currentUserId.equals(authorId);

        if (isAuthor || userHasReview) {
            binding.reviewSectionContainer.setVisibility(View.GONE);
        } else {
            binding.reviewSectionContainer.setVisibility(View.VISIBLE);
        }
    }

    // camera launcher
    private final ActivityResultLauncher<Intent> reviewCameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri imageUri = Uri.parse(data.getStringExtra("URI_KEY"));
                        // Do something with the URI, e.g., display in an ImageView or attach to review
                        Toast.makeText(this, "Photo selected: " + imageUri, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    // format minutes as hours and minutes
    private String formatTimeInHoursAndMinutes(double minutes) {
        int totalMinutes = (int) minutes;
        if (totalMinutes < 60) {
            return totalMinutes + " min";
        } else {
            int hours = totalMinutes / 60;
            int mins = totalMinutes % 60;
            if (mins == 0) {
                return hours + " hr";
            } else {
                return hours + " hr " + mins + " min";
            }
        }
    }

    // start slideshow for recipe images
    private void startSlideshow() {
        if (recipeImageUrls == null || recipeImageUrls.size() <= 1) {
            return;
        }

        stopSlideshow(); // stop any slideshow
        currentImageIndex = 0; // start at first image (default)
        slideshowHandler = new Handler(Looper.getMainLooper());

        slideshowRunnable = new Runnable() {
            @Override
            public void run() {
                if (recipeImageUrls != null && !recipeImageUrls.isEmpty()) {
                    // cycle to next image
                    currentImageIndex = (currentImageIndex + 1) % recipeImageUrls.size();
                    String imageUrl = recipeImageUrls.get(currentImageIndex);
                    
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(RecipeDetailsActivity.this)
                                .load(imageUrl)
                                .into(binding.imageViewRecipePhoto);
                    }

                    // schedule next image change (4 seconds)
                    if (slideshowHandler != null) {
                        slideshowHandler.postDelayed(this, 4000);
                    }
                }
            }
        };

        // start the slideshow after 4 seconds 
        slideshowHandler.postDelayed(slideshowRunnable, 4000);
    }

    // stop slideshow
    private void stopSlideshow() {
        if (slideshowHandler != null && slideshowRunnable != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
            slideshowHandler = null;
            slideshowRunnable = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSlideshow();
    }

}
