package com.ptho1504.content_service.serivce;

import com.ptho1504.content_service.dto.MovieRequest;
import com.ptho1504.content_service.dto.MovieResponse;
import com.ptho1504.content_service.model.Genre;
import com.ptho1504.content_service.model.Movie;
import com.ptho1504.content_service.model.VideoStatus;
import com.ptho1504.content_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {
    private final MovieRepository movieRepository;


    /*
    * Add a new Movie to the catalog
    * Video is not uploaded yet at this stage
    * */
    public MovieResponse addMovie(MovieRequest movieRequest) {
        log.info("Adding new movie: {}", movieRequest.getTitle());
        Movie movie = Movie.builder()
                .title(movieRequest.getTitle())
                .description(movieRequest.getDescription())
                .genre(movieRequest.getGenre())
                .director(movieRequest.getDirector())
                .cast(movieRequest.getCast())
                .releaseYear(movieRequest.getReleaseYear())
                .rating(movieRequest.getRating())
                .thumbnailUrl(movieRequest.getThumbnailUrl())
                .durationMinutes(movieRequest.getDurationMinutes())
                .videoStatus(VideoStatus.PENDING)
                .build();

        Movie savedMovie = movieRepository.save(movie);

        log.info("Movie saved with ID: {}", savedMovie.getId());

        return mapToResponse(savedMovie);
    }


    /*
    *  Get all movies in the catalog
    * */
    public List<MovieResponse> getAll() {
        return movieRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<MovieResponse> getMoviesByGenre(Genre genre) {
        List<Movie> movies = movieRepository.findByGenre(genre);
        return movies.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public MovieResponse getMovieById(String movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found:" + movieId));
        return mapToResponse(movie);
    }

    public List<MovieResponse> searchMovies(String title) {
        List<Movie> movies = movieRepository.findByTitleContainingIgnoreCase(title);
        return movies.stream().map(this::mapToResponse).collect(Collectors.toList());
    }
    public void updateVideoKey(String movieId, String videoKey) {

        log.info("Updating video key for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found:" + movieId));

        movie.setVideoKey(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
    }

    public void updateHslUrl(String movieId, String hslUrl) {
        log.info("Updating hsl url for movie: {}", movieId);
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found:" + movieId));

        movie.setHlsUrl(hslUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);

        log.info("Movie {} is ready for streaming", movieId);
    }


    private MovieResponse mapToResponse(Movie savedMovie) {
        MovieResponse movieResponse = new MovieResponse();
        movieResponse.setId(savedMovie.getId());
        movieResponse.setTitle(savedMovie.getTitle());
        movieResponse.setDescription(savedMovie.getDescription());
        movieResponse.setGenre(savedMovie.getGenre());
        movieResponse.setDirector(savedMovie.getDirector());
        movieResponse.setCast(savedMovie.getCast());
        movieResponse.setReleaseYear(savedMovie.getReleaseYear());
        movieResponse.setRating(savedMovie.getRating());
        movieResponse.setThumbnailUrl(savedMovie.getThumbnailUrl());
        movieResponse.setDurationMinutes(savedMovie.getDurationMinutes());
        movieResponse.setVideoStatus(savedMovie.getVideoStatus());
        return movieResponse;
    }
}
