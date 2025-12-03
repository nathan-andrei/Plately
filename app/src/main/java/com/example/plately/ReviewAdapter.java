package com.example.plately;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<ReviewModel> reviews;

    public ReviewAdapter(List<ReviewModel> reviews) {
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.review_item_layout, parent, false); // item_review.xml should have a TextView with id textViewReview
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewModel review = reviews.get(position);

        // get username from author UID
        if (review.getAuthor() != null && review.getAuthor().get("uid") != null) {
            review.getAuthor().get("uid").get()
                    .addOnSuccessListener(userDoc -> {
                        String username = userDoc.getString("username");
                        holder.textViewReview.setText(
                                (username != null ? username : "Unknown") +
                                        " (" + review.getRating() + " stars): " + review.getText()
                        );
                    })
                    .addOnFailureListener(e -> {
                        holder.textViewReview.setText(
                                "Unknown (" + review.getRating() + " stars): " + review.getText()
                        );
                    });
        } else {
            holder.textViewReview.setText(
                    "Unknown (" + review.getRating() + " stars): " + review.getText()
            );
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }


    // view holder
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView textViewReview;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewReview = itemView.findViewById(R.id.textViewReview);
        }
    }
}