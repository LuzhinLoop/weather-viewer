package io.model.apiweather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherResponse(
        @JsonProperty("name") String name,
        Coord coord,
        List<Weather> weather,
        Main main,
        Sys sys
) {
    public record Coord(double lon, double lat) {
    }

    public record Weather(String main, String description, String icon) {
    }

    public record Main(Double temp, @JsonProperty("feels_like") Double feelsLike, Integer humidity) {
    }

    public record Sys(String country) {
    }
}