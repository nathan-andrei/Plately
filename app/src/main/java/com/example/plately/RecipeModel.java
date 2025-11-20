package com.example.plately;

import java.util.ArrayList;

public class RecipeModel {
    private String id;
    private String title;
    private String source;
    private String ingredients;
    private String instructions;
    private String notes;
    private double serves;
    private double prepTime;
    private double cookTime;
    private String imageUrl;
    private ArrayList<String> tags;
    private boolean isFavorite = false;

    public RecipeModel(String title, String source, String ingredients, String instructions,
                       String notes, double serves, double prepTime, double cookTime,
                       String imageUrl, ArrayList<String> tags) {
        this.title = title;
        this.source = source;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.notes = notes;
        this.serves = serves;
        this.prepTime = prepTime;
        this.cookTime = cookTime;
        this.imageUrl = imageUrl;
        this.tags = tags;
    }

    //getters
    public String getId() { return id;}
    public String getTitle() { return title; }
    public String getSource() { return source; }
    public String getIngredients() { return ingredients; }
    public String getInstructions() { return instructions; }
    public String getNotes() { return notes; }
    public double getServes() { return serves; }
    public double getPrepTime() { return prepTime; }
    public double getCookTime() { return cookTime; }
    public String getImageUrl() { return imageUrl; }
    public ArrayList<String> getTags() { return tags; }
    public boolean isFavorite() { return isFavorite; }

    // setters
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
    public void setId(String id) { this.id = id;}
}
