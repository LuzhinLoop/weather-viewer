package io.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDTO {
    private String cityName;
    private String countryName;
    private Integer feelsLike;
    private Integer temperature;
    private Integer humidity;
    private String description;
    private String iconUrl;
}