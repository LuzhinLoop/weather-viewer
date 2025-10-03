package io.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString(exclude = "password")
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Login can contain only letters, numbers, dots, underscores and hyphens")
    @NotBlank(message = "Login is required")
    @Size(min = 3, max = 25, message = "Login must be between 3 and 25 characters")
    @Column(name = "login", nullable = false, unique = true, length = 25)
    private String login;

    @Column(name = "password", nullable = false, length = 60)
    private String password;
}