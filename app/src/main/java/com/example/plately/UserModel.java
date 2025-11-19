package com.example.plately;

public class UserModel {
    private String username, email, password;
    //TEMPORARY DATATYPES
    private String[] createdRecipes;
    private String[] createdReviews;
    private String[] savedRecipes;
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

    public String[] getCreatedRecipes() { return createdRecipes; }

    public void setCreatedRecipes(String[] createdRecipes) { this.createdRecipes = createdRecipes; }

    public String[] getCreatedReviews() { return createdReviews; }

    public void setCreatedReviews(String[] createdReviews) { this.createdReviews = createdReviews; }

    public String[] getSavedRecipes() { return savedRecipes; }

    public void setSavedRecipes(String[] savedRecipes) { this.savedRecipes = savedRecipes; }

    public String getProfilePicture() { return profilePicture; }

    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

}
