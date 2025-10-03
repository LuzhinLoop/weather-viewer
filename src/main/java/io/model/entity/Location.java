package io.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;


@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "locations")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank
    private String name;

    @Column(name = "user_id", nullable = false)
    @NotNull
    @Positive
    private Long userId;

    @Column(nullable = false)
    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @Column(nullable = false)
    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    public Location(String name, Long userId, Double latitude, Double longitude) {
        this.name = name;
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
