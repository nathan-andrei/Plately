package com.example.plately;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ViewAnimator;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {
    private ViewAnimator viewAnimator;

    //Welcome widgets
    private Button toLoginBtn, toRegisterBtn, guestBtn;
    //Add buttons for the SSOs if needed

    //Login widgets
    private EditText loginEmail, loginPassword;
    private Button loginBtn, loginBackbtn;

    //Register widgets

    /*
        Indices for the viewAnimator:
            0   -   Welcome Screen
            1   -   Login Screen
            2   -   Register Screen
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
            //setUpRegister();
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
        findViewById(R.id.Login_btn).setOnClickListener(v -> {
            Intent i = new Intent(this, MainActivity.class);
            //Throw session data here
            startActivity(i);
            finish();
        });
        findViewById(R.id.Login_Back_btn).setOnClickListener(v -> {
            viewAnimator.setDisplayedChild(0);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        setUpViewAnimator();
        setUpWelcome();


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}