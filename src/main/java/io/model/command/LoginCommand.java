package io.model.command;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand (
        @NotBlank String login,
        @NotBlank String password
        )
{}
