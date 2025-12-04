package com.example.plately;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
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
                .inflate(R.layout.review_item_layout, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        ReviewModel review = reviews.get(position);

        // ratings as stars
        holder.ratingBarReview.setRating(review.getRating());

        // review text
        holder.textViewReviewText.setText(review.getText() != null ? review.getText() : "");

        // Image placeholders (gray boxes)

        // Get username from author UID
        if (review.getAuthor() != null && review.getAuthor().get("uid") != null) {
            review.getAuthor().get("uid").get()
                    .addOnSuccessListener(userDoc -> {
                        String username = userDoc.getString("username");
                        holder.textViewReviewUsername.setText(username != null ? username : "Unknown");
                    })
                    .addOnFailureListener(e -> {
                        holder.textViewReviewUsername.setText("Unknown");
                    });
        } else {
            holder.textViewReviewUsername.setText("Unknown");
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }


    // view holder
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView textViewReviewUsername;
        RatingBar ratingBarReview;
        TextView textViewReviewText;
        ImageView imageViewReviewPhoto1;
        ImageView imageViewReviewPhoto2;
        ImageView imageViewReviewPhoto3;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewReviewUsername = itemView.findViewById(R.id.textViewReviewUsername);
            ratingBarReview = itemView.findViewById(R.id.ratingBarReview);
            textViewReviewText = itemView.findViewById(R.id.textViewReviewText);
            imageViewReviewPhoto1 = itemView.findViewById(R.id.imageViewReviewPhoto1);
            imageViewReviewPhoto2 = itemView.findViewById(R.id.imageViewReviewPhoto2);
            imageViewReviewPhoto3 = itemView.findViewById(R.id.imageViewReviewPhoto3);
        }
    }
}
