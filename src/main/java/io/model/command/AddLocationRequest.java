package io.model.command;

import jakarta.validation.constraints.Size;

public record AddLocationRequest(
        @Size(min = 1, max = 25) String name,
        Double lat,
        Double lon,
        String country
) {
}
