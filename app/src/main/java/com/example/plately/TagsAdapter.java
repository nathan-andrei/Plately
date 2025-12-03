package com.example.plately;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeTagsListLayoutBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TagsAdapter extends RecyclerView.Adapter<TagsViewHolder>{
    //ArrayList<String> tagList = new ArrayList<>();
    ArrayList<HashMap<String, Integer>> tagList = new ArrayList<>();
    private OnTagsClickListener onTagsClickListener;
    
    public TagsAdapter(ArrayList<RecipeModel> recipeList, ArrayList<String> tags, OnTagsClickListener onTagsClickListener){
        this.onTagsClickListener = onTagsClickListener;
        
        //For each tag, run through all the recipes and find which ones have it.
        for(String tag : tags){
            Integer count = 0;
            for(RecipeModel recipe : recipeList){
                if(!recipe.getTags().isEmpty()){
                    //run through all the tags of the current recipe
                    for(String recipeTag : recipe.getTags()){
                        if(Objects.equals(recipeTag, tag)){
                            count++;
                        }
                    }
                }
            }
            if(count > 0){
                tagList.add(new HashMap<>(Map.of(tag, count)));
            }
        }
    }

    @NonNull
    @Override
    public TagsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecipeTagsListLayoutBinding binding = RecipeTagsListLayoutBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        
        return new TagsViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TagsViewHolder holder, int position) {
        holder.bindData(tagList.get(position), onTagsClickListener);
    }


    @Override
    public int getItemCount() {
        return tagList.size();
    }

    public interface OnTagsClickListener {
        void onTagClick(String name, boolean state);
    }
    
}