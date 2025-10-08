package io.controller;

import io.exception.LocationException;
import io.model.dto.LocationDTO;
import io.model.dto.SavedLocationWeatherDTO;
import io.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {


    @Mock
    private WeatherService weatherService;

    @InjectMocks
    private WeatherController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/views/");
        viewResolver.setSuffix(".html");


        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void locationAdd_Get_ShouldReturnSearchView() throws Exception {
        mockMvc.perform(get("/locations/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attributeExists("addLocationRequest"));
    }

    @Test
    void dashboard_ShouldFetchLocationsAndReturnIndexView() throws Exception {
        Long userId = 1L;

        SavedLocationWeatherDTO mockLocation =
                new SavedLocationWeatherDTO(1L, "Moscow", 15.0, "RU", "Sunny", 15, 70, "10d");
        List<SavedLocationWeatherDTO> savedLocations = List.of(mockLocation);

        when(weatherService.fetchSavedLocationsWeather(userId)).thenReturn(savedLocations);

        mockMvc.perform(get("/locations/dashboard")
                        .requestAttr("userId", userId))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("locations"))
                .andExpect(model().attribute("locations", savedLocations));
    }

    @Test
    void locationSearch_WithValidQuery_ShouldReturnResults() throws Exception {
        String query = "London";
        LocationDTO foundLocation = new LocationDTO(1L ,"London", "GB", 51.5072, -0.1276);
        List<LocationDTO> locations = List.of(foundLocation);

        when(weatherService.findLocationsByName(query)).thenReturn(locations);

        mockMvc.perform(get("/locations/search").param("query", query))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attribute("findLocation", locations));
    }

    @Test
    void locationSearch_WithNullQuery_ShouldReturnEmptyList() throws Exception {
        mockMvc.perform(get("/locations/search"))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attribute("locations", java.util.Collections.emptyList()))
                .andExpect(model().attribute("query", ""));
    }

    @Test
    void locationSearch_WhenServiceIsInterrupted_ShouldShowError() throws Exception {
        String query = "Berlin";
        when(weatherService.findLocationsByName(query)).thenThrow(new InterruptedException("Search was interrupted"));

        mockMvc.perform(get("/locations/search").param("query", query))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Search was interrupted!"));
    }

    @Test
    void locationSearch_WhenServiceThrowsIOException_ShouldShowError() throws Exception {
        String query = "Paris";
        when(weatherService.findLocationsByName(query)).thenThrow(new IOException("API Error"));

        mockMvc.perform(get("/locations/search").param("query", query))
                .andExpect(status().isOk())
                .andExpect(view().name("search"))
                .andExpect(model().attributeExists("errorMessage"))
                .andExpect(model().attribute("errorMessage", "Unable to search locations"));
    }

    @Test
    void locationAdd_Post_WithValidData_ShouldAddLocationAndRedirect() throws Exception {
        Long userId = 1L;

        doNothing().when(weatherService).addLocation(any(LocationDTO.class), anyLong());

        mockMvc.perform(post("/locations/add")
                        .requestAttr("userId", userId)
                        .param("name", "Berlin")
                        .param("lat", "52.5200")
                        .param("lon", "13.4050")
                        .param("country", "DE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"))
                .andExpect(flash().attribute("successMessage", "Location added!"));
    }

    @Test
    void locationAdd_Post_WithInvalidData_ShouldFailValidationAndRedirect() throws Exception {
        Long userId = 1L;

        mockMvc.perform(post("/locations/add")
                        .requestAttr("userId", userId)
                        .param("name", "")
                        .param("lat", "52.5200")
                        .param("lon", "13.4050")
                        .param("country", "DE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.addLocationRequest"))
                .andExpect(flash().attributeExists("addLocationRequest"));
    }

    @Test
    void locationAdd_Post_WhenLocationAlreadyExists_ShouldShowError() throws Exception {
        Long userId = 1L;
        String errorMessage = "This location has already been added.";

        // Настраиваем мок на выброс исключения
        doThrow(new LocationException(errorMessage))
                .when(weatherService).addLocation(any(LocationDTO.class), anyLong());

        mockMvc.perform(post("/locations/add")
                        .requestAttr("userId", userId)
                        .param("name", "Berlin")
                        .param("lat", "52.5200")
                        .param("lon", "13.4050")
                        .param("country", "DE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"))
                .andExpect(flash().attribute("errorMessage", errorMessage));
    }

    @Test
    void locationDelete_WithValidId_ShouldDeleteAndRedirect() throws Exception {
        Long userId = 1L;
        Long locationId = 10L;

        doNothing().when(weatherService).deleteLocationByUser(userId, locationId);

        mockMvc.perform(post("/locations/{id}/delete", locationId)
                        .requestAttr("userId", userId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"))
                .andExpect(flash().attribute("successMessage", "Location deleted!"));
    }

    @Test
    void locationDelete_WhenServiceThrowsException_ShouldShowError() throws Exception {
        Long userId = 1L;
        Long locationId = 10L;

        doThrow(new LocationException("Cannot delete location"))
                .when(weatherService).deleteLocationByUser(userId, locationId);

        mockMvc.perform(post("/locations/{id}/delete", locationId)
                        .requestAttr("userId", userId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/locations/dashboard"))
                .andExpect(flash().attribute("errorMessage", "Location delete is failed"));
    }

}