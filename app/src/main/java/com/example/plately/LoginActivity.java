package com.example.plately;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseAuth dbAuth;

    private ViewAnimator viewAnimator;

    //Welcome widgets
    private Button toLoginBtn, toRegisterBtn, guestBtn;
    //Add buttons for the SSOs if needed

    //Login widgets
    private EditText loginEmail, loginPassword;
    private Button loginBtn, loginBackbtn;
    private boolean loginPasswordVisibility;

    //Register widgets
    private EditText registerName, registerEmail, registerPassword;
    private Button registerBtn, registerBackBtn, showPasswordBtn;
    private boolean registerPasswordVisibility;



    /*
        Indices for the viewAnimator:
            0   -   Welcome Screen
            1   -   Login Screen
            2   -   Register Screen
            3   -   OTP authentication
     */
    private void setUpViewAnimator(){
        viewAnimator = findViewById(R.id.welcome_switcher_va);
        viewAnimator.setInAnimation(AnimationUtils.loadAnimation(LoginActivity.this, android.R.anim.slide_in_left));
        viewAnimator.setOutAnimation(AnimationUtils.loadAnimation(LoginActivity.this, android.R.anim.slide_out_right));
    }

    private void setUpWelcome(){
        //Initialize the listener for going to login
        findViewById(R.id.ToLogin_btn).setOnClickListener(v -> {
            setUpLogin();
            viewAnimator.setDisplayedChild(1);
        });

        //Initialize the listener for going to registration
        findViewById(R.id.ToRegister_btn).setOnClickListener(v -> {
            setUpRegister();
            viewAnimator.setDisplayedChild(2);
        });
        
        //Initialize the listener for going straight to Main
        findViewById(R.id.Guest_btn).setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            //Throw session data here
            startActivity(i);
            finish();
        });
    }

    private void setUpLogin(){
        if(loginEmail == null)      loginEmail = findViewById(R.id.Login_Email_etv);
        if(loginPassword == null)   loginPassword = findViewById(R.id.Login_Password_etv);
        loginPasswordVisibility = false; //Default it back to not show

        findViewById(R.id.Login_btn).setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();
            Intent i = new Intent(this, MainActivity.class);

            //Log in
            dbAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Log in success
                    Log.d("[Firebase] Login", "signInWithEmailAndPassword:success");

                    //Throw session data here
                    startActivity(i);
                    finish();
                } else {
                    // If login fails, uhhh somehow say the error??
                    Log.w("[Firebase] Login", "signInWithEmailAndPassword:success", task.getException());
                    //Actually find out the error
                        // Account error
                        // server error
                    Toast.makeText(LoginActivity.this, "Account or password error", Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.Login_Back_btn).setOnClickListener(v -> {
            viewAnimator.setDisplayedChild(0);
        });
    }

    private void setUpRegister() {
        if(registerName == null)        registerName = findViewById(R.id.Display_Name_etv);
        if(registerEmail == null)       registerEmail = findViewById(R.id.Register_Email_etv);
        if(registerPassword == null)    registerPassword = findViewById(R.id.Register_Password_etv);
        registerPasswordVisibility = false;

        findViewById(R.id.Register_btn).setOnClickListener(v -> {
            setUpOTP();
            viewAnimator.setDisplayedChild(3);
        });

        findViewById(R.id.Register_back_btn).setOnClickListener(v ->{
            viewAnimator.setDisplayedChild(0);
        });

        findViewById(R.id.Register_Toggle_Password_Visibility_btn).setOnClickListener(v -> {
            if(!registerPasswordVisibility){
                registerPassword.setTransformationMethod(null);
            } else {
                registerPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
            registerPasswordVisibility = !registerPasswordVisibility;
        });

        // Register user
        findViewById(R.id.Register_btn).setOnClickListener(v -> {
            String displayName = registerName.getText().toString().trim();
            String email = registerEmail.getText().toString().trim();
            String password = registerPassword.getText().toString().trim();

            UserModel newUser = new UserModel(displayName, email, password);

            dbAuth.createUserWithEmailAndPassword(newUser.getEmail(), newUser.getPassword())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("[Firebase] Register", "createUserWithEmail:success");
                            FirebaseUser user = dbAuth.getCurrentUser();

                            if (user != null) {
                                String uid = user.getUid();

                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("username", newUser.getUsername());
                                userMap.put("email", newUser.getEmail());
                                userMap.put("password", newUser.getPassword()); // optional, consider security
                                userMap.put("createdRecipes", new ArrayList<>());
                                userMap.put("createdReviews", new ArrayList<>());
                                userMap.put("savedRecipes", new ArrayList<>());
                                userMap.put("profilePicture", null);

                                db.collection("users")
                                        .document(uid)
                                        .set(userMap)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("[Firebase] Register", "User successfully added to Firestore!");
                                            Toast.makeText(LoginActivity.this, "Registered Successfully!", Toast.LENGTH_SHORT).show();
                                            viewAnimator.setDisplayedChild(0);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("[Firebase] Register", "Failed to add user to Firestore!", e);
                                            Toast.makeText(LoginActivity.this, "Failed to register! Please try again", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            Exception e = task.getException();
                            if (e != null) {
                                Log.e("[Firebase] Register", "createUserWithEmail:failure", e);
                                Toast.makeText(LoginActivity.this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    private void setUpOTP(){
        findViewById(R.id.OTP_Register_btn).setOnClickListener(v -> {
            setUpLogin();
            viewAnimator.setDisplayedChild(1);
            Toast.makeText(LoginActivity.this, "Registered Successfully!.", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.OTP_Resend_btn).setOnClickListener(v ->{
            Toast.makeText(LoginActivity.this, "Resent OTP!", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.OTP_back_btn).setOnClickListener(v ->{
            viewAnimator.setDisplayedChild(2);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        dbAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        //Skip this screen if user is logged in.
        if(FirebaseController.getInstance().isLoggedIn()){
            Intent i = new Intent(this, MainActivity.class);
            //Throw session data here
            startActivity(i);
            finish();
        }

        setUpViewAnimator();
        setUpWelcome();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}