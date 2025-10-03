package io.model.apiweather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LocationResponse(
        String name,
        String country,
        String state,
        BigDecimal lat,
        BigDecimal lon
) {
}
