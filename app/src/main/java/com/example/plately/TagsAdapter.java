package com.example.plately;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plately.databinding.RecipeTagsListLayoutBinding;

import java.util.ArrayList;

public class TagsAdapter extends RecyclerView.Adapter<TagsViewHolder>{
    ArrayList<String> tagList = new ArrayList<>();
    private OnTagsClickListener onTagsClickListener;
    
    public TagsAdapter(OnTagsClickListener onTagsClickListener){
        this.onTagsClickListener = onTagsClickListener;
        //PUT ALL THE TAGS HERE
        this.tagList = new ArrayList<>();
        
        //Retrieve from DB rather than hardcode!!
        tagList.add("Breakfast");
        tagList.add("Comfort Food");
        tagList.add("Dessert");
        tagList.add("Low Carb");
        tagList.add("Spicy");
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