package com.example.plately;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeListLayoutBinding;

import java.util.ArrayList;

public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

    private ArrayList<RecipeModel> recipes;
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

    //Modified MyAdapter that won't render the favourite button and will be given its intent launcher.
    public MyAdapter(ArrayList<RecipeModel> recipes) {
        this.recipes = recipes;
        this.favoriteListener = null;
        this.clickListener = null;
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
        //Check if adapter is not given a favourite listener. If not, do not render the favouriteButton
        if(this.favoriteListener != null)
            holder.bindData(recipes.get(position), favoriteListener, clickListener);
        else {
            holder.bindData(recipes.get(position), clickListener);
            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(holder.itemView.getContext(), RecipeDetailsActivity.class);

                holder.itemView.getContext().startActivity(i);
            });

        }
    }


    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public interface OnRecipeFavoriteListener {
        void onFavoriteClick(String recipeId, boolean newState);
    }
    
    public void setRecipes(ArrayList<RecipeModel> list){ this.recipes = list;}

}