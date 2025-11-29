package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Show;

public interface ShowRepository extends JpaRepository<Show, String> {
    List<Show> findByShowTitleContainingIgnoreCase(String title);
    List<Show> findBySeasonsContaining(String season);
    boolean existsByShowTitle(String showTitle);
    Show findByShowTitle(String showTitle);
}
