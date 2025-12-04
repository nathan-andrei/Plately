package com.example.plately;

//change package name
import static android.view.View.VISIBLE;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    //private Uri takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) {
     //   if (it) {
            //startCameraWithUri()
    //    } else {
      //      showErrorMessage("taking picture failed")
     //   }
    //}

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
        setUpListeners();
        //Tried to change action bar text to white, did not work...
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            getSupportActionBar().setTitle(Html.fromHtml("<font color='FFFFFF'>Your Profile</font>", Html.FROM_HTML_MODE_LEGACY));
        } else {
            //noinspection deprecation
            getSupportActionBar().setTitle(Html.fromHtml("<font color='FFFFFF'>Your Profile</font>"));
        }

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
        /*
        if(item.getItemId() == R.id.Profile_Edit_Photo){
            //Check if we don't have permissions to use camera and write to storage. (maybe move to seperate function)
            //If so, request permissions
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100); //100 request code is arbitrary

            }
            //User DOES have camera permissions.
            else{

            }

            //Check if user allows access to storage (gallery)
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                //Request only for the write perms
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100); //100 request code is arbitrary
            }
            return true;
        }*/
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
            //TODO: make the code
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

    //Launch the camera and get the uri for the captured image.
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = Uri.parse(data.getStringExtra("URI_KEY"));
                        //This is supposed to open the image cropper, not working!!
                        ciw.setVisibility(VISIBLE);
                        ciw.setImageUriAsync(uri);
                        /*
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),Uri.parse(data.getStringExtra("URI_KEY")));
                            
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        */
                        StorageReference newPfpRef = FirebaseStorage.getInstance().getReference().child("users/" + dbAuth.getCurrentUser().getUid() + "/ProfilePicture.jpg");
                        UploadTask uploadTask = newPfpRef.putFile(uri);
                        
                        uploadTask.addOnCompleteListener(this, task ->{
                            if(task.isSuccessful()){
                                 Log.d("[Firestore] Image Upload", "Sucessfully uploaded image yay");
                            }
                            else{
                                Log.d("[Firestore] Image Upload", "Failed to upload image noo");
                            }
                        });
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

        // tas put here users/uid/profile.jpg
        StorageReference storageRef = storage.getReference().child("users/" + uid + "/profile.jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {

                    //get url then upate the document
                    storageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("profilePicture", downloadUri.toString());

                        db.collection("users").document(uid)
                                .set(data, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(ProfileActivity.this, "Profile picture updated!", Toast.LENGTH_SHORT).show();

                                    // update ui but it doesn't work
                                    ciw.setVisibility(View.VISIBLE);
                                    ciw.setImageUriAsync(downloadUri);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ProfileActivity.this, "Failed to update Firestore", Toast.LENGTH_SHORT).show();
                                    e.printStackTrace();
                                });

                    }).addOnFailureListener(e -> {
                        Toast.makeText(ProfileActivity.this, "Failed to get download URL", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }


}
