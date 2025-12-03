package com.example.plately;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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


import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MyAdapter adapter;
    private ArrayList<RecipeModel> recipes;
    private ArrayList<RecipeModel> recipesForTags;
    private ArrayList<String> tags;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private boolean tagsVisibility = false;
    private ArrayList<String> tagFilter = new ArrayList<>();

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
                (recipeId, newState) -> {  // favorite listener to update favorite status
                    if (newState) saveRecipeToSaved(recipeId);
                    else removeRecipeFromSaved(recipeId);
                },
                recipe -> { // click listener launches recipe details
                    Intent intent = new Intent(MainActivity.this, RecipeDetailsActivity.class);
                    intent.putExtra("recipeId", recipe.getId());
                    intent.putExtra("isFavorite", recipe.isFavorite());
                    recipeDetailsLauncher.launch(intent);
                }
        );
        binding.recyclerView.setAdapter(adapter);

        recipesForTags = new ArrayList<>();
        tags = new ArrayList<>();
        getRecipesFromDb();

        //Add recipes button
        binding.buttonAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });

        //Profile button
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
        });

        //Setup the recycler for the tags
        binding.TagsRV.setLayoutManager(new LinearLayoutManager(this));
        //The adapter initialization is in getRecipesFromDb();
        binding.TagsMenu.setVisibility(View.GONE); //HIDE THE TAGS
        
        //Filter
        binding.buttonFilter.setOnClickListener(v -> {
            if(tagsVisibility){ //if the tags are visible
                binding.TagsMenu.setVisibility(View.GONE);
            }
            else{// if the tags are not viisble
                binding.TagsMenu.setVisibility(View.VISIBLE);
            }
            
            tagsVisibility = !tagsVisibility;
            
        });
        
        //Search button
        binding.buttonSearch.setOnClickListener(v -> {
            ArrayList<RecipeModel> filteredRecipes = new ArrayList<>();
            String query = binding.editTextSearchBar.getText().toString().trim();
            
            for(RecipeModel recipe : recipes){
                if(recipe.getRecipeName().toLowerCase().contains(query.toLowerCase())){
                    //Check if the recipe has all the required tags
                    if(tagFilter.isEmpty() || recipe.getTags().containsAll(tagFilter))
                        filteredRecipes.add(recipe);
                }
            }
            for(String tag : tagFilter){
                Log.d("[ActivityList] TagFilter", "Found filter: " + tag);
                
            }
            adapter.setRecipes(filteredRecipes);
            adapter.notifyDataSetChanged();
        });
        
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

    // once recipe is saved update db
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

    // remove recipe from saved under users collection
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

    // retrieve recipes from database
    private void getRecipesFromDb() {
        String uid = auth.getUid();
        if (uid == null) return;

        // get user's saved recipes to load correct favorite button state
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

        // get all recipes from recipes collection
        db.collection("recipes")
                .addSnapshotListener((querySnapshot, ex) -> {
                    if (ex != null || querySnapshot == null) return;

                    // list one by one using recycler
                    List<RecipeModel> newRecipes = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        RecipeModel recipeModel = doc.toObject(RecipeModel.class);
                        if (recipeModel == null) continue;
                        recipeModel.setId(doc.getId());
                        recipeModel.setFavorite(savedRecipeIds.contains(doc.getId()));
                        newRecipes.add(recipeModel);
                    }

                    // handle deletions in main menu UI
                    for (int i = recipes.size() - 1; i >= 0; i--) {
                        RecipeModel oldItem = recipes.get(i);
                        boolean exists = false;
                        for (RecipeModel newItem : newRecipes) {
                            if (oldItem.getId().equals(newItem.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            recipes.remove(i);
                            adapter.notifyItemRemoved(i);
                        }
                    }

                    // handle addition and updates in main menu UI
                    for (int i = 0; i < newRecipes.size(); i++) {
                        RecipeModel newItem = newRecipes.get(i);
                        boolean found = false;
                        for (int j = 0; j < recipes.size(); j++) {
                            RecipeModel oldItem = recipes.get(j);
                            if (newItem.getId().equals(oldItem.getId())) {

                                // only update existing if something was changed
                                if (!newItem.equals(oldItem)) {
                                    recipes.set(j, newItem);
                                    adapter.notifyItemChanged(j);
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {

                            // add new recipe to the main menu UI
                            recipes.add(i, newItem);
                            adapter.notifyItemInserted(i);
                        }
                    }
                });

        //Getting the recipes and tags for the filters since we don't really need a snapshot listener for it
        //First, get the recipes
        db.collection("recipes").get().addOnCompleteListener(this, getRecipestask ->{
            if(getRecipestask.isSuccessful()){
                Log.d("[Firestore] Retrieve Tags", "Succefully retrieved recipes");
                //Then get the tags
                db.collection("tags").get().addOnCompleteListener(this, getTagsTask ->{
                    if(getTagsTask.isSuccessful()){
                        Log.d("[Firestore] Retrieve Tags", "Succefully retrieved tags");
                        //Unpack the recipes
                        for(DocumentSnapshot doc : getRecipestask.getResult())
                            recipesForTags.add(doc.toObject(RecipeModel.class));
                        //Unpack the tags
                        for(DocumentSnapshot doc : getTagsTask.getResult())
                            tags.add(doc.getString("tagName"));
                            
                        //Create and set the adapter    
                        TagsAdapter tagsAdapter = new TagsAdapter(
                                recipesForTags,
                                tags,
                                (tagName,  state) -> {  // favorite listener to update favorite status
                                    if (state){
                                        //Remove from filter list
                                        tagFilter.remove(tagName);
                                    }
                                    else{
                                        tagFilter.add(tagName);
                                    }
                                    updateCurrentTagDisplay();
                                });
                        binding.TagsRV.setAdapter(tagsAdapter);
                    }
                    else{
                        Log.w("[Firestore] Retrieve Tags", "Failed to retrieve tags");
                    }
                });
            }
            else{
                Log.w("[Firestore] Retrieve Tags", "Failed to retrieve recipes");
            }
        });
    }
    
    private void updateCurrentTagDisplay(){
        boolean firstTag = true;
        if(tagFilter.isEmpty())
            binding.currentTagsTV.setVisibility(View.GONE);
        else{
            binding.currentTagsTV.setVisibility(View.VISIBLE);
            StringBuilder currTags = new StringBuilder("Current filter: ");
            
            for(String tag : tagFilter){
                if(!firstTag){
                    currTags.append(", ").append(tag);
                }
                else{
                    currTags.append(tag);
                    firstTag = false;
                }
            }
            binding.currentTagsTV.setText(currTags);
        }
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