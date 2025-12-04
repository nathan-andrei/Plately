package com.example.plately;

import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.plately.databinding.RecipeListLayoutBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private final RecipeListLayoutBinding binding;

    public MyViewHolder(@NonNull RecipeListLayoutBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public void bindData(RecipeModel recipe, MyAdapter.OnRecipeFavoriteListener listener, MyAdapter.OnRecipeClickListener clickListener) {
        displayPreviewRecipe(recipe);
        // initial state of icon
        binding.buttonFavorite.setImageResource(
                recipe.isFavorite()
                        ? R.drawable.baseline_bookmark_24
                        : R.drawable.outline_bookmark_24
        );

        //Check if the recipe is owned by the user
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        String authorUid = null;
        
        if (recipe.getAuthor() != null && recipe.getAuthor().get("uid") != null) {
            authorUid = recipe.getAuthor().get("uid").getId();
        }

        if (currentUid != null && authorUid != null && !Objects.equals(authorUid, currentUid)) {
            //If not the author, show and enable the button
            binding.buttonFavorite.setVisibility(View.VISIBLE);
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
        }
        else{
            //If the user IS the author (or author check failed), hide the favourites button.
            binding.buttonFavorite.setVisibility(View.GONE);
            binding.buttonFavorite.setOnClickListener(null); // clear any previous listener to be sure
        }


        binding.getRoot().setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecipeClick(recipe);
            }
        });
    }

    //Modified function for recipe that doesn't have favourite buttons (User created/owns the recipe)
    public void bindData(RecipeModel recipe, MyAdapter.OnRecipeClickListener clickListener) {
        displayPreviewRecipe(recipe);

        // initial state of icon
        binding.buttonFavorite.setImageResource(
                recipe.isFavorite()
                        ? R.drawable.baseline_bookmark_24
                        : R.drawable.outline_bookmark_24
        );

        // favorite click
        binding.buttonFavorite.setVisibility(View.GONE);

        binding.getRoot().setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecipeClick(recipe);
            }
        });
    }

    private void displayPreviewRecipe(RecipeModel recipe) {
        // recipe name
        binding.textViewRecipeNamePrev.setText(
                recipe.getRecipeName() != null ? recipe.getRecipeName() : ""
        );

        // author, fetch username from author uid
        if (recipe.getAuthor() != null && recipe.getAuthor().get("uid") != null) {
            recipe.getAuthor().get("uid").get()
                    .addOnSuccessListener(docSnap -> {
                        String username = docSnap.getString("username");
                        binding.textViewRecipeAuthorPrev.setText("By: " + (username != null ? username : "Unknown"));
                    })
                    .addOnFailureListener(e -> binding.textViewRecipeAuthorPrev.setText("By: Unknown"));
        } else {
            binding.textViewRecipeAuthorPrev.setText("By: Unknown");
        }

        // description
        binding.textViewDescriptionPrev.setText(
                recipe.getRecipeDescription() != null ? recipe.getRecipeDescription() : ""
        );

        // image
        if (recipe.getRecipeImage() != null && !recipe.getRecipeImage().isEmpty()) {

            Log.d("RecipeImage", "Loading image URL: " + recipe.getRecipeImage());

            Glide.with(binding.imageViewFood.getContext())
                    .load(recipe.getRecipeImage())
                    .into(binding.imageViewFood);
        }

        // tags up to 2 only
        if (recipe.getTags() != null && !recipe.getTags().isEmpty()) {
            binding.textViewTagPrev.setText(recipe.getTags().get(0));
            binding.textViewTagPrev.setVisibility(View.VISIBLE);

            if (recipe.getTags().size() > 1) {
                binding.textViewTag2Prev.setText(recipe.getTags().get(1));
                binding.textViewTag2Prev.setVisibility(View.VISIBLE);
            } else {
                binding.textViewTag2Prev.setVisibility(View.GONE);
            }
        } else {
            binding.textViewTagPrev.setVisibility(View.GONE);
            binding.textViewTag2Prev.setVisibility(View.GONE);
        }

        // serves x people
        if (recipe.getServesPax() > 0) {
            binding.textViewServesPrev.setText("Serves " + (int) recipe.getServesPax());
            binding.textViewServesPrev.setVisibility(View.VISIBLE);
        } else {
            binding.textViewServesPrev.setVisibility(View.GONE);
        }

        // total time
        double totalTime = recipe.getPrepTime() + recipe.getCookTime();
        if (totalTime > 0) {
            String timeText;
            if (totalTime < 60) {
                timeText = (int) totalTime + " min";
            } else {
                int hours = (int) totalTime / 60;
                int minutes = (int) totalTime % 60;
                timeText = hours + " hr" + (minutes > 0 ? " " + minutes + " min" : "");
            }
            binding.textViewTimePrev.setText(timeText);
            binding.textViewTimePrev.setVisibility(View.VISIBLE);
        } else {
            binding.textViewTimePrev.setVisibility(View.GONE);
        }
    }

}
