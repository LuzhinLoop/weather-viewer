package io.mapper;

import io.model.apiweather.WeatherResponse;
import io.model.dto.WeatherDTO;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface WeatherApiMapper {

    @Mapping(target = "cityName", source = "name")
    @Mapping(target = "countryName", source = "sys.country")
    @Mapping(target = "temperature", source = "main.temp")
    @Mapping(target = "feelsLike", source = "main.feelsLike")
    @Mapping(target = "humidity", source = "main.humidity")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "iconUrl", ignore = true)
    WeatherDTO toDTO(WeatherResponse response);

    @AfterMapping
    default void fullDerivedFields(@MappingTarget WeatherDTO dto, WeatherResponse r) {
        if (r != null && r.weather() != null && !r.weather().isEmpty()) {
            var w = r.weather().get(0);
            if (w != null) {
                var desc = w.description();
                if (desc != null && !desc.isBlank()) {
                    dto.setDescription(desc);
                }
                var icon = w.icon();
                if (icon != null && !icon.isBlank()) {
                    dto.setIconUrl(toIconUrl(icon));
                }
            }
        }
    }

    @Named("toIconUrl")
    default String toIconUrl(String iconCode) {
        return "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";
    }
}