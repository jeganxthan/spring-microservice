package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    // Custom query to find ratings by showId
    List<Rating> findByShowId(String showId);
}
