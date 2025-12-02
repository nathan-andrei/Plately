package com.example.plately;

import android.graphics.Color;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeTagsListLayoutBinding;

import java.util.ArrayList;

public class TagsViewHolder extends RecyclerView.ViewHolder {
    private final RecipeTagsListLayoutBinding binding;
    private boolean state = false;

    public TagsViewHolder(@NonNull RecipeTagsListLayoutBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
    
    public void bindData(String tag, TagsAdapter.OnTagsClickListener listener){
        binding.tagContainer.setBackgroundColor(Color.parseColor("#FFFFFF"));
        binding.tagName.setText(tag);
        //Make sure we can count ocurrences from database!!
        binding.tagCount.setText("1");
        
        binding.tagContainer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTagClick(tag, state);
            }

            state = !state;
            if(state){ //Is currently active
                binding.tagContainer.setBackgroundColor(Color.parseColor("#CCCCCC"));
            }
            else{ //Is not active
                binding.tagContainer.setBackgroundColor(Color.parseColor("#FFFFFF"));
            }
            
        });
    }
}