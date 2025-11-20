package com.example.plately;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.plately.databinding.ActivityMainBinding;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MyAdapter adapter;
    private ArrayList<RecipeModel> recipes;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recipes = new ArrayList<>();
        adapter = new MyAdapter(
                recipes,
                (recipeId, newState) -> {  // favorite listener
                    if (newState) saveRecipeToSaved(recipeId);
                    else removeRecipeFromSaved(recipeId);
                },
                recipe -> { // click listener
                    Intent intent = new Intent(MainActivity.this, RecipeDetailsActivity.class);
                    intent.putExtra("recipeId", recipe.getId());
                    intent.putExtra("isFavorite", recipe.isFavorite());
                    recipeDetailsLauncher.launch(intent);
                }
        );


        binding.recyclerView.setAdapter(adapter);

        getRecipesFromDb();

        binding.buttonAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });

        binding.buttonProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        binding.buttonFilter.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Filter button clicked (Not yet implemented)", Toast.LENGTH_SHORT).show()
        );
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    //function to override back button and make it close the app instead of potentially going to LoginActivity
    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            finishAffinity();
            finish();
            setEnabled(false);
        }
    };

    private void saveRecipeToSaved(String recipeId) {
        String uid = auth.getUid();
        if (uid == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);

        db.collection("users")
                .document(uid)
                .update("savedRecipes", FieldValue.arrayUnion(recipeRef))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                );
    }

    private void removeRecipeFromSaved(String recipeId) {
        String uid = auth.getUid();
        if (uid == null) return;

        DocumentReference recipeRef = db.collection("recipes").document(recipeId);

        db.collection("users")
                .document(uid)
                .update("savedRecipes", FieldValue.arrayRemove(recipeRef))
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Removed!", Toast.LENGTH_SHORT).show()
                );
    }

    private Set<String> savedRecipeIds = new HashSet<>();
    private void getRecipesFromDb() {
        String uid = auth.getUid();
        if (uid == null) return;

        // Listen to user's saved recipes
        db.collection("users").document(uid)
                .addSnapshotListener((userDoc, e) -> {
                    if (e != null || userDoc == null) return;

                    List<DocumentReference> savedRecipes = (List<DocumentReference>) userDoc.get("savedRecipes");
                    savedRecipeIds.clear();
                    if (savedRecipes != null) {
                        for (DocumentReference ref : savedRecipes) {
                            savedRecipeIds.add(ref.getId());
                        }
                    }
                    adapter.notifyDataSetChanged(); // Refresh favorites only
                });

        // Listen to recipes collection
        db.collection("recipes")
                .addSnapshotListener((querySnapshot, ex) -> {
                    if (ex != null || querySnapshot == null) return;

                    recipes.clear(); // Clear old recipes before re-adding
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String recipeName = doc.getString("recipeName") != null ? doc.getString("recipeName") : "";
                        String source = doc.getString("source") != null ? doc.getString("source") : "";
                        String recipeDescription = doc.getString("recipeDescription") != null ? doc.getString("recipeDescription") : "";

                        ArrayList<String> ingredients = new ArrayList<>();
                        Object ingredientsObj = doc.get("ingredients");
                        if (ingredientsObj instanceof List<?>) {
                            for (Object ing : (List<?>) ingredientsObj) {
                                if (ing instanceof String) ingredients.add((String) ing);
                            }
                        }

                        ArrayList<String> steps = new ArrayList<>();
                        Object stepsObj = doc.get("steps");
                        if (stepsObj instanceof List<?>) {
                            for (Object step : (List<?>) stepsObj) {
                                if (step instanceof String) steps.add((String) step);
                            }
                        }

                        ArrayList<String> tags = new ArrayList<>();
                        Object tagsObj = doc.get("tags");
                        if (tagsObj instanceof List<?>) {
                            for (Object tag : (List<?>) tagsObj) {
                                if (tag instanceof String) tags.add((String) tag);
                            }
                        }

                        Number servesNum = doc.get("servesPax") instanceof Number ? (Number) doc.get("servesPax") : 0;
                        Number prepNum = doc.get("prepTime") instanceof Number ? (Number) doc.get("prepTime") : 0;
                        Number cookNum = doc.get("cookTime") instanceof Number ? (Number) doc.get("cookTime") : 0;

                        String recipeId = doc.getId();

                        RecipeModel recipeModel = new RecipeModel(
                                recipeName,
                                source,
                                ingredients.toString(),
                                steps.toString(),
                                recipeDescription,
                                servesNum.doubleValue(),
                                prepNum.doubleValue(),
                                cookNum.doubleValue(),
                                "",
                                tags
                        );
                        recipeModel.setId(recipeId);

                        // check if recipe is saved
                        recipeModel.setFavorite(savedRecipeIds.contains(recipeId));

                        recipes.add(recipeModel);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private final ActivityResultLauncher<Intent> recipeDetailsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String recipeId = data.getStringExtra("recipeId");
                        boolean newState = data.getBooleanExtra("isFavorite", false);

                        for (RecipeModel r : recipes) {
                            if (r.getId().equals(recipeId)) {
                                r.setFavorite(newState);
                                break;
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
            }
    );

}