package com.example.plately;

import java.lang.reflect.Array;
import java.util.ArrayList;

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
    private ArrayList<String> imageUrl;
    private ArrayList<String> tags;
    private boolean isFavorite = false;

    public RecipeModel() {
        id = null;
        recipeName = null;
        source = null;
        recipeDescription = null;
        ingredients = null;
        instructions = null;
        servesPax = 0.0;
        prepTime = 0.0;
        cookTime = 0.0;
        imageUrl = null;
        tags = new ArrayList<>();
        isFavorite = false;
    }

    public RecipeModel(String name, String source, ArrayList<String> ingredients, String instructions, double serves, double prepTime, double cookTime,
                       ArrayList<String> imageUrl, ArrayList<String> tags, String recipeDescription) {
        this.recipeName = name;
        this.source = source;
        this.recipeDescription = recipeDescription;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.servesPax = serves;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.imageUrl = imageUrl;
        this.tags = tags;
    }

    //getters
    public String getId() { return id;}
    public String getRecipeName() { return recipeName; }
    public String getSource() { return source; }
    public ArrayList<String> getIngredients() { return ingredients; }
    public String getRecipeDescription() {return recipeDescription; }
    public String getInstructions() { return instructions; }
    public double getServesPax() { return servesPax; }
    public double getPrepTime() { return prepTime; }
    public double getCookTime() { return cookTime; }
    public ArrayList<String> getImageUrl() { return imageUrl; }
    public ArrayList<String> getTags() { return tags; }
    public boolean isFavorite() { return isFavorite; }

    // setters
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
    public void setId(String id) { this.id = id;}
}
