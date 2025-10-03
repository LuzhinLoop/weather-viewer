package io.service;

import io.api.openweather.OpenWeatherClient;
import io.exception.LocationException;
import io.mapper.LocationApiMapper;
import io.mapper.LocationMapper;
import io.mapper.WeatherApiMapper;
import io.model.apiweather.LocationResponse;
import io.model.dto.LocationDTO;
import io.model.dto.SavedLocationWeatherDTO;
import io.model.entity.Location;
import io.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WeatherService {

    private final OpenWeatherClient client;
    private final LocationRepository locationRepository;
    private final WeatherApiMapper weatherApiMapper;

    private final LocationApiMapper locationApiMapper;
    private final LocationMapper locationMapper;

    public static final int MAX_LOCATION_PER_USER = 5;

    public List<LocationDTO> findLocationsByName(String query) throws IOException, InterruptedException {
        List<LocationResponse> responses = client.geoCode(query);
        return locationApiMapper.toDto(responses);
    }

    public void addLocation(LocationDTO locationDTO, Long userId) {
        double validatedLat = requireLatitude(locationDTO.getLat());
        double validatedLon = requireLongitude(locationDTO.getLon());
        double lat4 = round4(validatedLat);
        double lon4 = round4(validatedLon);

        if (locationRepository.countByUser(userId) >= MAX_LOCATION_PER_USER) {
            throw new LocationException("You can't add more saved locations.");
        }

        if (locationRepository.existsByUserAndCoords(userId, lat4, lon4)) {
            throw new LocationException("This location has already been added.");
        }

        Location entity = locationMapper.toEntity(locationDTO);
        entity.setUserId(userId);
        entity.setLatitude(lat4);
        entity.setLongitude(lon4);
        locationRepository.save(entity);
    }

    public List<SavedLocationWeatherDTO> fetchSavedLocationsWeather(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId is not found!");
        }

        List<Location> locationByUser = locationRepository.findAllByUserId(userId);
        List<SavedLocationWeatherDTO> result = new ArrayList<>(locationByUser.size());

        for (Location location : locationByUser) {
            Optional<SavedLocationWeatherDTO> dto = fetchAndMapWeather(location);
            dto.ifPresent(result::add);
        }
        return result;
    }

    public Optional<SavedLocationWeatherDTO> fetchAndMapWeather(Location location) {
        try {

            var weatherResponse = client.weather(location.getLatitude(), location.getLongitude());
            var weatherDto = weatherApiMapper.toDTO(weatherResponse);

            var finalDto = new SavedLocationWeatherDTO(
                    location.getId(),
                    location.getName(),
                    weatherDto.getTemperature(),
                    weatherDto.getCountryName(),
                    weatherDto.getDescription(),
                    weatherDto.getFeelsLike(),
                    weatherDto.getHumidity(),
                    weatherDto.getIconUrl()
            );
            return Optional.of(finalDto);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("The weather request was interrupted for '{}' (id={})",
                    location.getName(), location.getId());
            return Optional.empty();
        }
    }

    public void deleteLocationByUser(Long userId, Long locationId) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId is not found!");
        }
        if (locationId == null) {
            throw new IllegalArgumentException("LocationId is not found!");
        }
        int deletedRows = locationRepository.deleteLocationByUserId(userId, locationId);
        if (deletedRows == 0) {
            throw new LocationException("Location with id=" + locationId + " not found for this user.");
        }
    }

    private static double requireLatitude(double lat) {
        if (Double.isNaN(lat) || Double.isInfinite(lat)) {
            throw new IllegalArgumentException("latitude must be a finite number");
        }
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        return lat;
    }

    private static double requireLongitude(double lon) {
        if (Double.isNaN(lon) || Double.isInfinite(lon)) {
            throw new IllegalArgumentException("longitude must be a finite number");
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        return lon;
    }

    private static double round4(double v) {
        return Math.round(v * 1e4) / 1e4d;
    }
}

