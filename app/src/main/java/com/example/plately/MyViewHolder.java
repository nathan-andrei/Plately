package com.example.plately;

import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeListLayoutBinding;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private final RecipeListLayoutBinding binding;

    public MyViewHolder(@NonNull RecipeListLayoutBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bindData(RecipeModel recipe, MyAdapter.OnRecipeFavoriteListener listener, MyAdapter.OnRecipeClickListener clickListener) {
        binding.textViewRecipeNamePrev.setText(recipe.getTitle());
        binding.textViewRecipeAuthorPrev.setText(recipe.getSource());
        binding.textViewDescriptionPrev.setText(recipe.getNotes());

        // image loading using glide
        /*Glide.with(binding.imageViewFood.getContext())
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.ramen_photo)
                .into(binding.imageViewFood);
        */

        // initial state of icon
        binding.buttonFavorite.setImageResource(
                recipe.isFavorite()
                        ? R.drawable.baseline_bookmark_24
                        : R.drawable.outline_bookmark_24
        );

        // favorite click
        binding.buttonFavorite.setOnClickListener(v -> {
            boolean newState = !recipe.isFavorite();
            recipe.setFavorite(newState);

            binding.buttonFavorite.setImageResource(
                    newState
                            ? R.drawable.baseline_bookmark_24
                            : R.drawable.outline_bookmark_24
            );

            if (listener != null) {
                listener.onFavoriteClick(recipe.getId(), newState);
            }
        });

        binding.getRoot().setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecipeClick(recipe);
            }
        });
    }
}
