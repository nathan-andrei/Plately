package com.example.plately;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeListLayoutBinding;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private final RecipeListLayoutBinding binding;

    public MyViewHolder(@NonNull RecipeListLayoutBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bindData(RecipeModel recipe) {
        binding.textViewRecipeNamePrev.setText(recipe.getTitle());
    }
}
