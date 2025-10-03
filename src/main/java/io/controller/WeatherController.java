package io.controller;

import io.exception.LocationException;
import io.model.command.AddLocationRequest;
import io.model.dto.LocationDTO;
import io.model.dto.SavedLocationWeatherDTO;
import io.service.WeatherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);
    private final WeatherService weatherService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestAttribute("userId") Long userId,
                            Model model) {
        List<SavedLocationWeatherDTO> saveLocation;

        saveLocation = weatherService.fetchSavedLocationsWeather(userId);

        model.addAttribute("locations", saveLocation);

        return "index";
    }

    @GetMapping("/add")
    public String locationAdd(Model model) {
        if (!model.containsAttribute("addLocationRequest")) {
            model.addAttribute("addLocationRequest", new AddLocationRequest("", 0.0, 0.0, ":"));
        }
        return "search";
    }

    @GetMapping("/search")
    public String locationSearch(@RequestParam(name = "query", required = false) String query,
                                 Model model) {
        if (query == null || query.isBlank()) {
            model.addAttribute("locations", java.util.List.of());
            model.addAttribute("query", "");
            return "search";
        }
        try {
            List<LocationDTO> findLocation = weatherService.findLocationsByName(query);
            model.addAttribute("findLocation", findLocation);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            model.addAttribute("errorMessage", "Search was interrupted!");
            return "search";
        } catch (IOException e) {
            log.warn("API error during location search", e);
            model.addAttribute("errorMessage", "Unable to search locations");
            return "search";
        }

        return "search";
    }

    @PostMapping("/add")
    public String locationAdd(@RequestAttribute("userId") Long userId,
                              @Valid @ModelAttribute AddLocationRequest req,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.addLocationRequest",
                    bindingResult
            );
            redirectAttributes.addFlashAttribute("addLocationRequest", req);
            return "redirect:/locations/dashboard";
        }

        try {
            LocationDTO locationDTO = LocationDTO.builder()
                    .lat(req.lat())
                    .lon(req.lon())
                    .name(req.name())
                    .country(req.country())
                    .build();
            weatherService.addLocation(locationDTO, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Location added!");

        } catch (LocationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("addLocationRequest", req);
            return "redirect:/locations/dashboard";
        }

        return "redirect:/locations/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String LocationDelete(@RequestAttribute("userId") Long userId,
                                 @PathVariable("id") Long locationId,
                                 RedirectAttributes redirectAttributes) {
        try {
            weatherService.deleteLocationByUser(userId, locationId);
            redirectAttributes.addFlashAttribute("successMessage", "Location deleted!");
        } catch (LocationException e) {
            log.warn("Failed to delete location with id={}: {}", locationId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Location delete is failed");
        }

        return "redirect:/locations/dashboard";
    }
}
