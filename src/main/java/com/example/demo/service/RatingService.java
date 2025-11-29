package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Rating;
import com.example.demo.repository.RatingRepository;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;

    // Constructor injection for better readability and testability
    @Autowired
    public RatingService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    // Add a rating for a specific show
    public void addRating(String userId, String showId, int rating, String review) {
        Rating newRating = new Rating();
        newRating.setUserId(userId);
        newRating.setShowId(showId);
        newRating.setRating(rating);
        newRating.setReview(review);
        ratingRepository.save(newRating);
    }

    // Get all ratings for a particular show
    public List<Rating> getRatingsForShow(String showId) {
        return ratingRepository.findByShowId(showId);
    }
}
