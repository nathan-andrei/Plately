package com.example.plately;

import android.net.Uri;

import com.google.firebase.firestore.DocumentReference;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class UserModel {
    private String username, email, password;
    private ArrayList<DocumentReference> createdRecipes;
    private ArrayList<DocumentReference> createdReviews;
    private ArrayList<DocumentReference> savedRecipes;
    private String profilePicture;

    public UserModel(){
        this.username = "default";
        this.email = null;
        this.password = null;
        createdRecipes = null;
        createdReviews = null;
        savedRecipes = null;
        profilePicture = null;
    }

    public UserModel(String name, String email, String password){
        this.username = name;
        this.email = email;
        this.password = password;
        createdRecipes = null;
        createdReviews = null;
        savedRecipes = null;
        profilePicture = null;
    }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

    public ArrayList<DocumentReference> getCreatedRecipes() { return createdRecipes; }

    public void setCreatedRecipes(ArrayList<DocumentReference> createdRecipes) { this.createdRecipes = createdRecipes;}

    public ArrayList<DocumentReference> getCreatedReviews() { return createdReviews; }

    public void setCreatedReviews(ArrayList<DocumentReference> createdReviews) { this.createdReviews = createdReviews;}

    public ArrayList<DocumentReference> getSavedRecipes() { return savedRecipes; }

    public void setSavedRecipes(ArrayList<DocumentReference> savedRecipes) { this.savedRecipes = savedRecipes; }

    public String getProfilePicture() { return profilePicture; }

    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

}
