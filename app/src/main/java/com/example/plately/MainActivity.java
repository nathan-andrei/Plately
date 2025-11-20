package com.example.plately;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.plately.databinding.ActivityMainBinding;


import java.util.ArrayList;
import java.util.List;

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
        adapter = new MyAdapter(recipes);
        binding.recyclerView.setAdapter(adapter);

        fetchRecipesFromFirestore();

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

    private void fetchRecipesFromFirestore() {
        db.collection("recipes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    recipes.clear();
                    for (var doc : querySnapshot) {
                        String recipeName = doc.getString("recipeName") != null ? doc.getString("recipeName") : "";
                        String source = doc.getString("source") != null ? doc.getString("source") : "";
                        String recipeDescription = doc.getString("recipeDescription") != null ? doc.getString("recipeDescription") : "";

                        // Arrays
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

                        recipes.add(new RecipeModel(
                                recipeName,
                                source,
                                ingredients.toString(), // combine as a string
                                steps.toString(),
                                recipeDescription,      // notes
                                servesNum.doubleValue(),
                                prepNum.doubleValue(),
                                cookNum.doubleValue(),
                                "",
                                tags
                        ));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch recipes: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}