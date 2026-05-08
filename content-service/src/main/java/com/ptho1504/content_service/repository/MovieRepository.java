package com.ptho1504.content_service.repository;


import com.ptho1504.content_service.model.Genre;
import com.ptho1504.content_service.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, String> {

    List<Movie> findByGenre(Genre genre);

    List<Movie> findByTitleContainingIgnoreCase(String title);
}
