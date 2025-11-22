package com.example.plately;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import com.example.plately.databinding.ActivityRecipeDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ActivityRecipeDetailsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String recipeId;
    private boolean isFavorite = false;
    private List<ReviewModel> reviewList = new ArrayList<>();
    private ReviewAdapter reviewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRecipeDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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

                    String authorId = recipe.getAuthor().get("uid").getId();
                    String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    applyAuthorVisibility(authorId, currentUid);

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
        startActivity(intent);
    }

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

        // description
        binding.textViewFullDescription.setText(recipe.getRecipeDescription() != null ? recipe.getRecipeDescription() : "No description available");

        // num servings
        binding.textViewServings.setText("Servings: " + (int) recipe.getServesPax());

        // prep & cook time
        binding.textViewPrepTime.setText("Prep time: " + (int) recipe.getPrepTime() + " min");
        binding.textViewCookingTime.setText("Cooking time: " + (int) recipe.getCookTime() + " min");
        binding.textViewTotalTime.setText("Total time: " + (int)(recipe.getPrepTime() + recipe.getCookTime()) + " min");

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

        // source
        binding.textViewSource.setText(recipe.getSource() != null ? recipe.getSource() : "No source available");

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
    }

    // submit and add review to db
    private void submitReview() {
        String reviewText = binding.editTextReview.getText().toString().trim();
        float rating = binding.ratingBarUser.getRating();

        if (reviewText.isEmpty()) {
            binding.editTextReview.setError("Review cannot be empty");
            return;
        }

        String currentUserId = auth.getUid();
        if (currentUserId == null) return;

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

        // add review to reviews collection
        db.collection("reviews").add(reviewMap)
                .addOnSuccessListener(reviewDocRef -> {

                    // add reviews to the recipe's review array
                    recipeRef.update("reviews", FieldValue.arrayUnion(reviewDocRef))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Review added!", Toast.LENGTH_SHORT).show();

                                // after adding reset the inputs
                                binding.editTextReview.setText("");
                                binding.ratingBarUser.setRating(0);
                                loadReviews();
                            });

                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to add review", Toast.LENGTH_SHORT).show());
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(reviewList);
        binding.recyclerViewReviews.setAdapter(reviewAdapter);
        binding.recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadReviews() {
        if (recipeId == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);

        // loads review of selected recipe
        db.collection("reviews")
                .whereEqualTo("recipeRef", recipeRef)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reviewList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ReviewModel review = doc.toObject(ReviewModel.class);
                        if (review != null) reviewList.add(review);
                    }
                    reviewAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show()
                );
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

}
