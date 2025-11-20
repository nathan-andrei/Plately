package com.example.plately;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeListLayoutBinding;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

    private final ArrayList<RecipeModel> recipes;
    private OnRecipeFavoriteListener favoriteListener;

    public interface OnRecipeClickListener {
        void onRecipeClick(RecipeModel recipe);
    }

    private final OnRecipeClickListener clickListener;

    public MyAdapter(ArrayList<RecipeModel> recipes,
                     OnRecipeFavoriteListener favoriteListener,
                     OnRecipeClickListener clickListener) {
        this.recipes = recipes;
        this.favoriteListener = favoriteListener;
        this.clickListener = clickListener;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecipeListLayoutBinding binding = RecipeListLayoutBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bindData(recipes.get(position), favoriteListener, clickListener);
    }


    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public interface OnRecipeFavoriteListener {
        void onFavoriteClick(String recipeId, boolean newState);
    }

}
