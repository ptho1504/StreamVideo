package com.ptho1504.content_service.controller;


import com.ptho1504.content_service.dto.MovieRequest;
import com.ptho1504.content_service.dto.MovieResponse;
import com.ptho1504.content_service.model.Genre;
import com.ptho1504.content_service.serivce.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
@RequiredArgsConstructor
public class ContentController {
    private final ContentService contentService;

    // Add new movie to catalog
    @PostMapping
    public ResponseEntity<MovieResponse> addMovie(
            @Valid @RequestBody  MovieRequest movieRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.addMovie(movieRequest));
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(contentService.getAll());
    }

    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<MovieResponse>> getMoviesByGenre(
            @PathVariable Genre genre) {
        return ResponseEntity.ok(contentService.getMoviesByGenre(genre));
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMoviesById(
            @PathVariable String movieId) {
        return ResponseEntity.ok(contentService.getMovieById(movieId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(
            @RequestParam String title) {
        return ResponseEntity.ok(contentService.searchMovies(title));
    }
}
