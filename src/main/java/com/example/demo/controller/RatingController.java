package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Rating;
import com.example.demo.service.RatingService;

@RestController
@RequestMapping("/ratings")
public class RatingController {

    @Autowired
    private RatingService ratingService;

    @PostMapping("/rate")
    @PreAuthorize("isAuthenticated()")
    public void rateShow(@RequestParam String userId, @RequestParam String showId,
            @RequestParam int rating, @RequestParam(required = false) String review) {
        ratingService.addRating(userId, showId, rating, review);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/show/{showId}")
    public List<Rating> getRatings(@PathVariable String showId) {
        return ratingService.getRatingsForShow(showId);
    }
}
