package com.example.plately;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicBoolean;

//This class might become redundant as development continues
public class FirebaseController {
    private static FirebaseController dbController;
    private FirebaseFirestore db;
    private FirebaseAuth dbAuth;

    private FirebaseController(){
        db = FirebaseFirestore.getInstance();
        dbAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseController getInstance(){
        if(dbController == null){
            dbController = new FirebaseController();
        }
        return dbController;
    }

    public boolean isLoggedIn(){
        FirebaseUser currentUser = dbAuth.getCurrentUser();

        return currentUser != null;
    }

    public void logOut(){
        dbAuth.signOut();
    }
}
