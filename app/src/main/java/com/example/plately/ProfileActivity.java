package com.example.plately;

//change package name
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.canhub.cropper.CropImageView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.auth.User;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.bumptech.glide.Glide;

public class ProfileActivity extends AppCompatActivity {
    // Widgets for Profile Activity
    private ImageButton buttonSettings;
    private TextView displayName;
    private UserModel user;
    private RecyclerView createdRecipesRV, savedRecipesRV, reviewsRV;
    private MyAdapter createdRecipesAdapter, savedRecipesAdapter;
    private ArrayList<RecipeModel> createdRecipes = new ArrayList<>(),
                                    savedRecipes = new ArrayList<>();
    private CropImageView ciw;
    private ImageView profileImage;

    AlertDialog.Builder builder;
    AlertDialog dialog;

    //DB variables
    private FirebaseFirestore db;
    private FirebaseAuth dbAuth;

    private FirebaseStorage storage;
    private void setUpCreatedRecipesRecyclerView(){
        //Check if the class variable for the RV is null
        if(createdRecipesRV == null) createdRecipesRV = findViewById(R.id.layoutMyRecipesContainer);

        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        createdRecipesRV.setLayoutManager(llm);

        createdRecipesAdapter= new MyAdapter(createdRecipes);
        createdRecipesRV.setAdapter(createdRecipesAdapter);

        ciw = new CropImageView(this);

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
    
    /* TODO:
           add this
     */
    private void setUpReviewsRecyclerView(){

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        dbAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        ciw = findViewById(R.id.cropImageView);

        // --- Setup Methods ---
        setUpWidgets();
        builder = new AlertDialog.Builder(this);
        dialog = builder.create();
        //Tried to change action bar text to white, did not work...
        getSupportActionBar().setTitle(Html.fromHtml("<font color='FFFFFF'>Your Profile</font>"));

        //Set up the bottom nav bar buttons
        findViewById(R.id.buttonAddRecipe).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AddRecipeActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonHome).setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // Apply Window Insets (Safety for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutMainProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
    }

    //OPtions menus??
    /* Lifted from our SharedPref Exercise.
     * Responsible for inflating the options menu on the upper right corner of the screen.
     * */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    /*
     * A little overkill tbh, but this method is responsible for handling the selection of items
     * in the options menu. There's only one item anyway -- Settings, which leads the user to the
     * Settings activity.
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.Profile_Edit_Camera){
            //This image cropper lets you crop your selected image.
            //Idk how to make this work...
            ciw.setOnCropImageCompleteListener(new CropImageView.OnCropImageCompleteListener() {
                @Override
                public void onCropImageComplete(@NonNull CropImageView cropImageView, @NonNull CropImageView.CropResult cropResult) {
                    ciw.getCroppedImage();
                }
            });
            Intent intent = new Intent(ProfileActivity.this, CameraActivity.class);
            cameraLauncher.launch(intent); //Retrieve the info from the intentLauncher
            return true;
        }
        else if(item.getItemId() == R.id.Profile_Edit_Gallery){
            //Retrieve an image from the gallery
            /*TODO: PERM CHECKS*/
            galleryLauncher.launch("image/*");
            return true;
        }
        else if(item.getItemId() == R.id.Profile_Edit_Name){
            //Intent i = new Intent(ProfileActivity.this, SettingsActivity.class);
            //startActivity(i);
            return true;
        }
        else if(item.getItemId() == R.id.Profile_LogOut){
            FirebaseController.getInstance().logOut();
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }

    //SETUP FUNCS
    private void setUpWidgets() {
        // Find the Settings button in the header toolbar
        buttonSettings = findViewById(R.id.buttonSettings);
        displayName = findViewById(R.id.textViewUserName);
        profileImage = findViewById(R.id.imageViewProfilePicture);

        //Retrieve user data
        String uid;
        //Check if user is signed in. If not, oh no! Handle later.
        if(dbAuth.getCurrentUser() != null)  uid = dbAuth.getCurrentUser().getUid();
        else                                 return; //Handle the error if it ever becomes an issue.
        
        //Check if profile picture is in local file system
        File pfpFile = new File(getFilesDir(), "images/ProfilePicture.jpg");

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
                            if(pfpFile.exists()){
                                //pfp is in local file system
                                Bitmap pfpImg = BitmapFactory.decodeFile(pfpFile.getAbsolutePath());
                                profileImage.setImageBitmap(pfpImg);
                            }
                            else{
                                //Pfp is not in local file system, download it.
                                Log.d("[Firestore] Profile: Read", "User pfp uri:" + user.getProfilePicture());
                                //Check if there's a uri in the system, if not, then do nothing.
                                if(!Objects.equals(user.getProfilePicture(), "") && !Objects.equals(user.getProfilePicture(), "null") && user.getProfilePicture() != null) {
                                    Log.d("[Firestore Profile: Read", "Entered to update");
                                    updateProfilePicture(user.getProfilePicture());
                                }
                            }
                        } else {
                            Log.w("[Firestore] Profile: Read", "User logged in but no info on DB!");
                        }

                    }
                    else{
                        Log.w("[Firestore] Profile: Read", "Failed retrieving user data");
                    }
                });
    }
    
    //Launch the camera and get the uri for the captured image.
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = Uri.parse(data.getStringExtra("URI_KEY"));
                        //Upload image to firestore
                        uploadProfilePicture(uri);
                        //This is supposed to open the image cropper, not working!!
                        ciw.setVisibility(VISIBLE);
                        ciw.setImageUriAsync(uri);
                    }
                }
            }
    );

    // launch gallery
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfilePicture(uri);
                }
            }
    );

    // upload profile pic :D
    private void uploadProfilePicture(Uri imageUri) {
        // get uid
        String uid = dbAuth.getCurrentUser().getUid();

        builder.setMessage("Please wait...");
        dialog = builder.create();
        dialog.show();

        // tas put here users/uid/profile.jpg
        StorageReference storageRef = storage.getReference().child("users/" + uid + "/profile.jpg");
        
        storageRef.putFile(imageUri).addOnCompleteListener(this, task ->{
            if(task.isSuccessful()){
                Log.d("[Firestore] Image Upload", "Sucessfully uploaded image to storage yay");
                /*
                    TODO: We should delete the temporary file now.
                 */
                //File uri.delete?
                
                //Get the download url for the new pfp
                storageRef.getDownloadUrl().addOnCompleteListener(this, getDownloadTask -> {
                    if(getDownloadTask.isSuccessful()){
                        Log.d("[Firestore] Image Download", "Sucessfully retrieved the download URI");
                        Uri downloadUri = getDownloadTask.getResult();
                        Map<String, Object> data = new HashMap<>();
                        data.put("profilePicture", downloadUri.toString());
                        
                        //Change the reference from the db
                        db.collection("users").document(uid)
                                .set(data, SetOptions.merge()).addOnCompleteListener(this, updateRefTask ->{
                                   if(updateRefTask.isSuccessful()){
                                       //Download the File and update the image view
                                       Log.d("[Firestore] Image Download", "Succesfully changed image reference in db");
                                       updateProfilePicture(downloadUri.toString());
                                   }
                                   else{
                                       Log.w("[Firestore] Image Download", "Failed to change image reference in db");
                                       dialog.dismiss();
                                       Toast.makeText(ProfileActivity.this, "Failed to change profile picture. Please try again later.", Toast.LENGTH_SHORT).show();
                                   }
                                });
                    }
                    else{
                        Log.w("[Firestore] Image Download", "Failed to retrieve the download URI");
                        dialog.dismiss();
                        Toast.makeText(ProfileActivity.this, "Failed to change profile picture. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else{
                Log.w("[Firestore] Image Upload", "Failed to upload image noo");
                dialog.dismiss();
                Toast.makeText(ProfileActivity.this, "Failed to change profile picture. Please try again later.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfilePicture(String downloadUri){
        Glide.with(this).load(downloadUri).into(profileImage);
        dialog.dismiss();        
    }
}