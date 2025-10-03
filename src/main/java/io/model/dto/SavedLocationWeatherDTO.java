package io.model.dto;

public record SavedLocationWeatherDTO(
        Long id,
        String name,
        double temp,
        String country,
        String description,
        Integer feelsLike,
        Integer humidity,
        String iconUrl
) {
}
