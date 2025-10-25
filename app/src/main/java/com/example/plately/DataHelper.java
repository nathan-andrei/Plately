package com.example.plately;

import java.util.ArrayList;

public class DataHelper {
    public static ArrayList<RecipeModel> generateRecipes() {
        ArrayList<RecipeModel> recipes = new ArrayList<>();

        recipes.add(new RecipeModel("Ramen1"));
        recipes.add(new RecipeModel("Ramen2"));
        recipes.add(new RecipeModel("Ramen3"));
        recipes.add(new RecipeModel("Ramen4"));

        return recipes;
    }
}
