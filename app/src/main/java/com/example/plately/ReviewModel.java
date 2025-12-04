package com.example.plately;

import com.google.firebase.firestore.DocumentReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReviewModel {
    private HashMap<String, DocumentReference> author;
    private float rating;
    private String text;
    private DocumentReference recipeRef;
    private List<String> reviewImages;
    private String reviewId;

    private String displayAuthor;

    public ReviewModel() {

    }

    // constructor for review
    public ReviewModel(DocumentReference userRef, float rating, String text, DocumentReference recipeRef) {
        this.author = new HashMap<>();
        this.author.put("uid", userRef);
        this.rating = rating;
        this.text = text;
        this.recipeRef = recipeRef;
    }

    public HashMap<String, DocumentReference> getAuthor() {
        return author;
    }

    public float getRating() {
        return rating;
    }

    public String getText() {
        return text;
    }

    public String getDisplayAuthor() {
        return displayAuthor;
    }

    public DocumentReference getRecipeRef() {
        return recipeRef;
    }

    public void setAuthor(HashMap<String, DocumentReference> author) {
        this.author = author;
    }

    public void setDisplayAuthor(String displayAuthor) {this.displayAuthor = displayAuthor; }
    public void setRating(float rating) {
        this.rating = rating;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setRecipeRef(DocumentReference recipeRef) {
        this.recipeRef = recipeRef;
    }

    public List<String> getReviewImages() {
        return reviewImages;
    }

    public void setReviewImages(List<String> reviewImages) {
        this.reviewImages = reviewImages;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }
}
