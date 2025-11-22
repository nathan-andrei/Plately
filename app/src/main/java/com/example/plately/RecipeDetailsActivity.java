package com.example.plately;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.plately.databinding.ActivityRecipeDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

public class RecipeDetailsActivity extends AppCompatActivity {

    private ActivityRecipeDetailsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String recipeId;
    private boolean isFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityRecipeDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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

                    //update info here
                    //sendFavoriteResult();
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

}
