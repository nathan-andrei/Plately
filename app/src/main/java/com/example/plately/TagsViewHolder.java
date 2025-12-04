package com.example.plately;

import android.graphics.Color;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeTagsListLayoutBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class TagsViewHolder extends RecyclerView.ViewHolder {
    private final RecipeTagsListLayoutBinding binding;
    private boolean state = false;

    public TagsViewHolder(@NonNull RecipeTagsListLayoutBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
    
    public void bindData(HashMap<String, Integer> tag, TagsAdapter.OnTagsClickListener listener){
        String tagName = tag.keySet().iterator().next();
        
        binding.tagContainer.setBackgroundColor(Color.parseColor("#FFFFFF"));
        binding.tagName.setText(tagName);
        binding.tagCount.setText(Integer.toString(tag.get(tagName)));
        
        binding.tagContainer.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTagClick(tagName, state);
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