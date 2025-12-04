package com.example.plately;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<ReviewModel> reviews;
    private String currentUserId;
    private OnReviewDeleteListener deleteListener;
    private android.content.Context context;

    public interface OnReviewDeleteListener {
        void onReviewDeleted(String reviewId);
    }

    public ReviewAdapter(List<ReviewModel> reviews) {
        this.reviews = reviews;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    public void setDeleteListener(OnReviewDeleteListener listener) {
        this.deleteListener = listener;
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
        String reviewId = review.getReviewId();

        // ratings as stars
        holder.ratingBarReview.setRating(review.getRating());

        // review text
        holder.textViewReviewText.setText(review.getText() != null ? review.getText() : "");

        // Display review images and hide empty image views
        List<String> reviewImages = review.getReviewImages();
        if (reviewImages != null && !reviewImages.isEmpty()) {
            // Show images based on how many we have
            if (reviewImages.size() >= 1) {
                Glide.with(holder.itemView.getContext())
                        .load(reviewImages.get(0))
                        .into(holder.imageViewReviewPhoto1);
                holder.imageViewReviewPhoto1.setVisibility(View.VISIBLE);
            } else {
                holder.imageViewReviewPhoto1.setVisibility(View.GONE);
            }

            if (reviewImages.size() >= 2) {
                Glide.with(holder.itemView.getContext())
                        .load(reviewImages.get(1))
                        .into(holder.imageViewReviewPhoto2);
                holder.imageViewReviewPhoto2.setVisibility(View.VISIBLE);
            } else {
                holder.imageViewReviewPhoto2.setVisibility(View.GONE);
            }

            if (reviewImages.size() >= 3) {
                Glide.with(holder.itemView.getContext())
                        .load(reviewImages.get(2))
                        .into(holder.imageViewReviewPhoto3);
                holder.imageViewReviewPhoto3.setVisibility(View.VISIBLE);
            } else {
                holder.imageViewReviewPhoto3.setVisibility(View.GONE);
            }
        } else {
            // No images, hide all image views
            holder.imageViewReviewPhoto1.setVisibility(View.GONE);
            holder.imageViewReviewPhoto2.setVisibility(View.GONE);
            holder.imageViewReviewPhoto3.setVisibility(View.GONE);
        }

        // Check if current user owns this review and show/hide menu button
        boolean isOwner = false;
        if (currentUserId != null && review.getAuthor() != null && review.getAuthor().get("uid") != null) {
            String authorId = review.getAuthor().get("uid").getId();
            isOwner = authorId.equals(currentUserId);
        }
        holder.imageButtonReviewOptions.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        // Set up menu button click listener
        holder.imageButtonReviewOptions.setOnClickListener(v -> {
            context = v.getContext();
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_review_options, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_delete_review) {
                    confirmDeleteReview(v.getContext(), reviewId, review);
                    return true;
                }
                return false;
            });
            popup.show();
        });

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

    private void confirmDeleteReview(android.content.Context context, String reviewId, ReviewModel review) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Review")
                .setMessage("Are you sure you want to delete this review?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReview(reviewId, review))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReview(String reviewId, ReviewModel review) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference reviewRef = db.collection("reviews").document(reviewId);

        // Delete the review document
        reviewRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Remove review reference from user's createdReviews
                    if (currentUserId != null && review.getAuthor() != null && review.getAuthor().get("uid") != null) {
                        DocumentReference authorRef = review.getAuthor().get("uid");
                        authorRef.update("createdReviews", FieldValue.arrayRemove(reviewRef))
                                .addOnFailureListener(e -> {
                                    // Log but don't block
                                    android.util.Log.e("ReviewAdapter", "Failed to remove review from user", e);
                                });
                    }

                    // Remove review reference from recipe's reviews array
                    if (review.getRecipeRef() != null) {
                        review.getRecipeRef().update("reviews", FieldValue.arrayRemove(reviewRef))
                                .addOnFailureListener(e -> {
                                    // Log but don't block
                                    android.util.Log.e("ReviewAdapter", "Failed to remove review from recipe", e);
                                });
                    }

                    // Notify listener to refresh
                    if (deleteListener != null) {
                        deleteListener.onReviewDeleted(reviewId);
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Failed to delete review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
        ImageButton imageButtonReviewOptions;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewReviewUsername = itemView.findViewById(R.id.textViewReviewUsername);
            ratingBarReview = itemView.findViewById(R.id.ratingBarReview);
            textViewReviewText = itemView.findViewById(R.id.textViewReviewText);
            imageViewReviewPhoto1 = itemView.findViewById(R.id.imageViewReviewPhoto1);
            imageViewReviewPhoto2 = itemView.findViewById(R.id.imageViewReviewPhoto2);
            imageViewReviewPhoto3 = itemView.findViewById(R.id.imageViewReviewPhoto3);
            imageButtonReviewOptions = itemView.findViewById(R.id.imageButtonReviewOptions);
        }
    }
}