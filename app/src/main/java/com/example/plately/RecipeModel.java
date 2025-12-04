package com.example.plately;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeModel {
    private String id;
    private String recipeName;
    private String recipeDescription;
    private String source;
    private ArrayList<String> ingredients;
    private String instructions;
    private double servesPax;
    private double prepTime;
    private double cookTime;
    private String recipeImage;
    private ArrayList<String> recipeImages;
    private ArrayList<String> tags;
    private boolean isFavorite = false;
    private HashMap<String, DocumentReference> author;
    private ArrayList<String> steps;

    public RecipeModel() {
        id = null;
        recipeName = null;
        source = null;
        recipeDescription = null;
        ingredients = null;
        steps = null;
        instructions = null;
        servesPax = 0.0;
        prepTime = 0.0;
        cookTime = 0.0;
        recipeImage = null; // updated
        tags = new ArrayList<>();
        isFavorite = false;
        author = null;
    }

    public RecipeModel(String name, String source, ArrayList<String> ingredients, String instructions, double serves, double prepTime, double cookTime,
                       String recipeImage, ArrayList<String> tags, String recipeDescription, HashMap<String, DocumentReference> author) {
        this.recipeName = name;
        this.source = source;
        this.recipeDescription = recipeDescription;
        this.ingredients = ingredients;
        this.steps = steps;
        this.instructions = instructions;
        this.servesPax = serves;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.recipeImage = recipeImage;
        this.tags = tags;
        this.author = author;
    }

    // getters
    public String getId() { return id; }
    public String getRecipeName() { return recipeName; }
    public String getSource() { return source; }
    public ArrayList<String> getIngredients() { return ingredients; }
    public String getRecipeDescription() { return recipeDescription; }
    public String getInstructions() { return instructions; }
    public double getServesPax() { return servesPax; }
    public double getPrepTime() { return prepTime; }
    public double getCookTime() { return cookTime; }
    public String getRecipeImage() { return recipeImage; }
    public ArrayList<String> getRecipeImages() { return recipeImages; }
    public ArrayList<String> getTags() { return tags; }
    public boolean isFavorite() { return isFavorite; }
    public HashMap<String, DocumentReference> getAuthor() { return author; }
    public ArrayList<String> getSteps() { return steps; }
    public void setSteps(ArrayList<String> steps) { this.steps = steps; }

    // setters
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
    public void setId(String id) { this.id = id; }
    public void setAuthor(HashMap<String, DocumentReference> author) { this.author = author; }
    public void setRecipeImage(String recipeImage) { this.recipeImage = recipeImage; }
    public void setRecipeImages(ArrayList<String> recipeImages) { this.recipeImages = recipeImages; }
    public void setRecipeName(String recipeName) { this.recipeName = recipeName; }
    public void setSource(String source) { this.source = source; }
    public void setRecipeDescription(String recipeDescription) { this.recipeDescription = recipeDescription; }
    public void setIngredients(ArrayList<String> ingredients) { this.ingredients = ingredients; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setServesPax(double servesPax) { this.servesPax = servesPax; }
    public void setPrepTime(double prepTime) { this.prepTime = prepTime; }
    public void setCookTime(double cookTime) { this.cookTime = cookTime; }
    public void setTags(ArrayList<String> tags) { this.tags = tags; }
    
    // Helper method to get the first image from recipeImages array, with fallback to recipeImage
    public String getFirstImage() {
        try {
            if (recipeImages != null && !recipeImages.isEmpty()) {
                String firstImage = recipeImages.get(0);
                if (firstImage != null && !firstImage.trim().isEmpty()) {
                    return firstImage;
                }
            }
        } catch (Exception e) {
            // toast or something
        }
        return recipeImage;
    }
}
