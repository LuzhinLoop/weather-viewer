package io.service;


import io.api.openweather.OpenWeatherClient;
import io.exception.LocationException;
import io.mapper.LocationApiMapper;
import io.mapper.LocationMapper;
import io.mapper.WeatherApiMapper;
import io.model.apiweather.LocationResponse;
import io.model.apiweather.WeatherResponse;
import io.model.dto.LocationDTO;
import io.model.dto.SavedLocationWeatherDTO;
import io.model.dto.WeatherDTO;
import io.model.entity.Location;
import io.repository.LocationRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WeatherServiceTest {

    @Mock
    private OpenWeatherClient client;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationApiMapper locationApiMapper;

    @Mock
    private WeatherApiMapper weatherApiMapper;

    @Mock
    private LocationMapper locationMapper;

    @Spy
    @InjectMocks
    private WeatherService weatherService;

    private LocationDTO locationDto;

    @BeforeEach
    void setUp() {
        locationDto = new LocationDTO(1L, "Moscow", "RU", 57.7522, 37.6156);
    }

    @Test
    void shouldReturnLocation_whenNameExist() throws InterruptedException, IOException {
        String cityName = "Moscow";

        List<LocationResponse> apiResponse = List.of(
                new LocationResponse(cityName, "Russia", "RU", new BigDecimal("57.7522"), new BigDecimal("37.6156")));
        List<LocationDTO> expectedResult = List.of(locationDto);

        when(client.geoCode(cityName)).thenReturn(apiResponse);
        when(locationApiMapper.toDto(apiResponse)).thenReturn(expectedResult);

        List<LocationDTO> serviceResult = weatherService.findLocationsByName(cityName);

        assertThat(serviceResult).isEqualTo(expectedResult);
    }

    @Test
    void shouldSaveLocation_onHappyPath() {
        when(locationRepository.countByUser(1L)).thenReturn(1);
        when(locationRepository.existsByUserAndCoords(1L, 57.7522, 37.6156)).thenReturn(false);

        Location mappedEntity = new Location();
        when(locationMapper.toEntity(locationDto)).thenReturn(mappedEntity);

        weatherService.addLocation(locationDto, 1L);

        verify(locationRepository).countByUser(1L);
        verify(locationRepository).existsByUserAndCoords(1L, 57.7522, 37.6156);
        verify(locationRepository).save(mappedEntity);
    }

    @Test
    void shouldThrowLocationException_whenLocationListIsFull() {
        when(locationRepository.countByUser(1L)).thenReturn(5);

        assertThrows(LocationException.class, () -> weatherService.addLocation(locationDto, 1L));
    }

    @Test
    void shouldThrowLocationException_whenLocationAlreadyExists() {
        when(locationRepository.existsByUserAndCoords(1L, locationDto.getLat(), locationDto.getLon()))
                .thenReturn(true);

        assertThrows(LocationException.class, () -> weatherService.addLocation(locationDto, 1L));
    }

    @Test
    void shouldReturnWeatherForSavedLocations() {
        Location london = loc(1L, "London", 51.5073219, -0.1276474);
        Location paris = loc(2L, "Paris", 48.8534951, -2.3483915);
        List<Location> locationFromDB = List.of(london, paris);

        SavedLocationWeatherDTO weatherLondon =
                w(1L, "London", 11, "GB", "clear sky", 10, 79, "10d");
        SavedLocationWeatherDTO weatherParis =
                w(1L, "Weather", 11, "FR", "broken clouds", 11, 7973, "10d");

        when(locationRepository.findAllByUserId(1L)).thenReturn(locationFromDB);

        doReturn(Optional.of(weatherLondon)).when(weatherService).fetchAndMapWeather(london);
        doReturn(Optional.of(weatherParis)).when(weatherService).fetchAndMapWeather(paris);

        List<SavedLocationWeatherDTO> actualResult =
                weatherService.fetchSavedLocationsWeather(1L);

        assertThat(actualResult).hasSize(2);
        assertThat(actualResult).containsExactlyInAnyOrder(weatherLondon, weatherParis);
    }

    @ParameterizedTest
    @CsvSource({
            "91.0, 52.0",    // Invalid latitude > 90
            "-91.0, 52.0",   // Invalid latitude < -90
            "65.0, 181.0",   // Invalid longitude > 180
            "65.0, -181.0"   // Invalid longitude < -180
    })
    void addLocation_shouldThrowIllegalArgumentException_forInvalidCoordinates(double lat, double lon) {
        LocationDTO invalidDto = new LocationDTO();
        invalidDto.setLat(lat);
        invalidDto.setLon(lon);

        assertThrows(IllegalArgumentException.class, () -> {
            weatherService.addLocation(invalidDto, 1L);
        });
    }

    @Test
    void shouldIllegalArgumentException_whenUserIsNotNull() {
        assertThrows(IllegalArgumentException.class, () -> weatherService.fetchSavedLocationsWeather(null));
    }

    @Test
    void shouldReturnWeatherDto_whenLocationIsValid() throws InterruptedException {

        Location location = new Location(1L, "New York", 1L, 40.7127, -74.006);

        WeatherResponse weatherResponse = getWeatherResponse();

        WeatherDTO weatherDTO = wDTO("New York", "US", 23, 23, 68, "clear sky", "2");

        SavedLocationWeatherDTO expectedFinalDto = w(
                location.getId(),
                location.getName(),
                weatherDTO.getTemperature(),
                weatherDTO.getCountryName(),
                weatherDTO.getDescription(),
                weatherDTO.getFeelsLike(),
                weatherDTO.getHumidity(),
                weatherDTO.getIconUrl()
        );

        when(client.weather(40.7127, -74.006)).thenReturn(weatherResponse);
        when(weatherApiMapper.toDTO(weatherResponse)).thenReturn(weatherDTO);

        Optional<SavedLocationWeatherDTO> actualResult = weatherService.fetchAndMapWeather(location);

        assertThat(actualResult).isPresent().hasValue(expectedFinalDto);
    }

    private static @NotNull WeatherResponse getWeatherResponse() {
        WeatherResponse.Coord coord = new WeatherResponse.Coord(
                -74.006,
                40.7127);
        WeatherResponse.Weather weather = new WeatherResponse.Weather(
                "Clear",
                "clear sky",
                "2");
        WeatherResponse.Main main = new WeatherResponse.Main(
                23D,
                23D,
                68);
        WeatherResponse.Sys sys = new WeatherResponse.Sys("US");
        return new WeatherResponse(
                "New York",
                coord,
                List.of(weather),
                main,
                sys
        );
    }

    @Test
    void shouldDeleteLocation_whenLocationFound() {
        when(locationRepository.deleteLocationByUserId(1L, 1L)).thenReturn(1);

        weatherService.deleteLocationByUser(1L, 1L);

        verify(locationRepository).deleteLocationByUserId(1L, 1L);
    }

    @Test
    void shouldThrowLocationException_whenLocationNotForUser() {
        when(locationRepository.deleteLocationByUserId(1L, 1L)).thenReturn(0);

        assertThrows(LocationException.class, () -> weatherService.deleteLocationByUser(1L, 1L));
    }

    @Test
    void shouldThrowIllegalArgumentException_whenLocationIdIsNull() {

        assertThrows(IllegalArgumentException.class,
                () -> weatherService.deleteLocationByUser(1L, null));
    }

    @Test
    void shouldThrowIllegalArgumentException_whenUserIdIsNull() {

        assertThrows(IllegalArgumentException.class,
                () -> weatherService.deleteLocationByUser(null, 1L));
    }

    private static SavedLocationWeatherDTO w(
            long id, String name, int t, String cc, String d, int feels, int hum, String icon) {
        return new SavedLocationWeatherDTO(id, name, t, cc, d, feels, hum, icon);
    }

    private static Location loc(long id, String name, double lat, double lon) {
        return new Location(id, name, 1L, lat, lon);
    }

    private static WeatherDTO wDTO(
            String city, String country, Integer feels, Integer t, Integer hum, String desc, String icon) {
        return new WeatherDTO(city, country, feels, t, hum, desc, icon);
    }

}