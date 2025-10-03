package io.model.command;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 25) String login,
        @NotBlank @Size(min = 6, max = 50) @Pattern(regexp = "^(?! ).*(?<! )$", message = "{password.noEdgeSpace}")  String password,
        @NotBlank String confirmPassword
) {}