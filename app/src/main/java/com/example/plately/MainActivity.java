package com.example.plately;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
            String uid = auth.getCurrentUser().getUid();
            if (uid == null) return;

            Log.d("[Firestore] Profile: Access", "profile pressed");
            Log.d("[Firestore] Profile: Access", "Current UID:" + uid);
            //Get the current uid from auth, and check if it exists in the db
            //If it doesn't, then the user is anonymous.
            db.collection("users")
                    .document(uid).get().addOnCompleteListener(this, task -> {
                        if(task.isSuccessful() && task.getResult().exists()){
                            DocumentSnapshot doc = task.getResult();
                                Log.d("[Firestore] Profile: Access", "profile retrieved");
                                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                                startActivity(intent);
                        }
                        else{
                            //While we do not boot the user back to login if they come back as an anonymous user
                            //They must login when accessing features that need authentication
                            Log.d("[Firestore] Profile: Access", "no profile found");
                            Toast.makeText(MainActivity.this, "Please login first!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            intent.putExtra("WAS_ANONYMOUS", true);
                            startActivity(intent);
                            finish();
                        }
                    });

            /*
            if(auth != null && auth.getCurrentUser() != null) {
                Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
            else{
                Toast.makeText(MainActivity.this, "Please login first!", Toast.LENGTH_SHORT).show();
            }*/
        });

        binding.buttonFilter.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Filter button clicked (Not yet implemented)", Toast.LENGTH_SHORT).show()
        );
        getOnBackPressedDispatcher().addCallback(this, callback);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
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

                    //Add a suppress warning here for an unchecked Type reference, it works so :shrug:
                    @SuppressWarnings("unchecked")
                    List<DocumentReference> savedRecipes = (List<DocumentReference>)userDoc.get("savedRecipes");
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
                        Log.d("[Firestore] RecipeList: Read", "Recieved recipe: " + doc.getId());
                        RecipeModel recipeModel = doc.toObject(RecipeModel.class);
                        recipeModel.setId(doc.getId());

                        // check if recipe is saved
                        recipeModel.setFavorite(savedRecipeIds.contains(doc.getId()));

                        recipes.add(recipeModel);
                        adapter.notifyItemInserted(recipes.size());
                    }
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