package com.example.plately;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeListLayoutBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class MyViewHolder extends RecyclerView.ViewHolder {
    private final RecipeListLayoutBinding binding;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    //private FirebaseAuth auth;

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
        if(!Objects.equals(recipe.getAuthor().get("uid").getId(), FirebaseAuth.getInstance().getCurrentUser().getUid())){
            //If not, add the button binding
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
            //If the user IS the author, hide the favourites button.
            binding.buttonFavorite.setVisibility(View.GONE);
        }


        binding.getRoot().setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onRecipeClick(recipe);
            }
        });

        DocumentReference recipeRef = db.collection("recipes").document(recipe.getId());

        db.collection("reviews")
                .whereEqualTo("recipeRef", recipeRef).get().addOnCompleteListener((Activity) binding.getRoot().getContext(), getReviewsTask ->{
                    if(getReviewsTask.isSuccessful()){
                        if(getReviewsTask.getResult().isEmpty()){
                            binding.textViewRatingPrev.setText("0.0/5.0");
                        }
                        else{
                            double sum  = 0.0;
                            double total = 0;
                            for(DocumentSnapshot review : getReviewsTask.getResult()){
                                sum += review.getDouble("rating");
                                total += 1;
                            }
                            String ratingText = String.format("%.1f", sum/total);
                            binding.textViewRatingPrev.setText(ratingText+"/5.0");
                        }
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