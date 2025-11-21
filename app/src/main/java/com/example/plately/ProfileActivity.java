package com.example.plately;

//change package name
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import org.checkerframework.checker.units.qual.A;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {
    // Widgets for Profile Activity
    private ImageButton buttonSettings;
    private TextView displayName;
    private UserModel user;
    private RecyclerView createdRecipesRV, savedRecipesRV, reviewsRV;
    private MyAdapter createdRecipesAdapter, savedRecipesAdapter;
    private ArrayList<RecipeModel> createdRecipes = new ArrayList<>(),
                                    savedRecipes = new ArrayList<>();

    //DB variables
    private FirebaseFirestore db;
    private FirebaseAuth dbAuth;

    private void setUpCreatedRecipesRecyclerView(){
        //Check if the class variable for the RV is null
        if(createdRecipesRV == null) createdRecipesRV = findViewById(R.id.layoutMyRecipesContainer);

        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        createdRecipesRV.setLayoutManager(llm);

        createdRecipesAdapter= new MyAdapter(createdRecipes);
        createdRecipesRV.setAdapter(createdRecipesAdapter);

        //Set the class variable
        for(DocumentReference dr : user.getCreatedRecipes()){
            //Retrieve the documents from the reference
            dr.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        Log.d("[Firestore] Profile>Recipe: Read", "Successfully retrieved recipe data");
                        //When successfully retrieved, add the data to our class variables and notify the adapter
                        createdRecipes.add(doc.toObject(RecipeModel.class));
                        createdRecipesAdapter.notifyItemInserted(createdRecipes.size());
                    } else {
                        Log.w("[Firestore] Profile>Recipe: Read", "Found an empty recipe?");
                    }

                }
                else{
                    Log.w("[Firestore] Profile>Recipe: Read", "Failed retrieving recipe data");
                }
            });
        }


    }

    private void setUpSavedRecipesRecyclerView(){
        //Check if the class variable for the RV is null
        if(savedRecipesRV == null) savedRecipesRV = findViewById(R.id.layoutSavedRecipesContainer);

        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        savedRecipesRV.setLayoutManager(llm);

        savedRecipesAdapter= new MyAdapter(savedRecipes);
        savedRecipesRV.setAdapter(savedRecipesAdapter);

        //Set the class variable
        for(DocumentReference dr : user.getSavedRecipes()){
            //Retrieve the documents from the reference
            dr.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    if (doc.exists()) {
                        Log.d("[Firestore] Profile>Recipe: Read", "Successfully retrieved recipe data");
                        //When successfully retrieved, add the data to our class variables and notify the adapter
                        savedRecipes.add(doc.toObject(RecipeModel.class));
                        savedRecipesAdapter.notifyItemInserted(savedRecipes.size());
                    } else {
                        Log.w("[Firestore] Profile>Recipe: Read", "Found an empty recipe?");
                    }

                }
                else{
                    Log.w("[Firestore] Profile>Recipe: Read", "Failed retrieving recipe data");
                }
            });
        }
    }

    private void setUpReviewsRecyclerView(){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        dbAuth = FirebaseAuth.getInstance();

        // --- Setup Methods ---
        setUpWidgets();
        setUpListeners();

        // Apply Window Insets (Safety for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMainProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    //SETUP FUNCTS

    private void setUpWidgets() {
        // Find the Settings button in the header toolbar
        buttonSettings = findViewById(R.id.buttonSettings);
        displayName = findViewById(R.id.textViewUserName);

        //Retrieve user data
        String uid;
        //Check if user is signed in. If not, oh no! Handle later.
        if(dbAuth.getCurrentUser() != null)  uid = dbAuth.getCurrentUser().getUid();
        else                                 return; //Handle the error if it ever becomes an issue.

        db.collection("users")
                .document(uid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            Log.d("[Firestore] Profile: Read", "Successfully retrieved user data");
                            user = doc.toObject(UserModel.class);

                            //Populate fields
                            displayName.setText(user.getUsername());
                            setUpCreatedRecipesRecyclerView();
                            setUpSavedRecipesRecyclerView();
                        } else {
                            Log.w("[Firestore] Profile: Read", "User logged in but no info on DB!");
                        }

                    }
                    else{
                        Log.w("[Firestore] Profile: Read", "Failed retrieving user data");
                    }
                });
    }

    private void setUpListeners() {
        // Placeholder listener for the Settings button
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(v -> {
                //Toast.makeText(ProfileActivity.this, "Settings clicked (Feature not implemented)", Toast.LENGTH_SHORT).show();

                //For now, we log out for testing...
                FirebaseController.getInstance().logOut();
                Intent i = new Intent(this, LoginActivity.class);
                startActivity(i);
                finish();
            });
        }
    }

    /*private final ActivityResultLauncher<Intent> recipeDetailsLauncher = registerForActivityResult(
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
    );*/
}
