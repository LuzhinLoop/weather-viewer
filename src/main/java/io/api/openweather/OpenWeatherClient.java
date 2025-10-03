package io.api.openweather;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.exception.WeatherException;
import io.model.apiweather.LocationResponse;
import io.model.apiweather.WeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class OpenWeatherClient {

    private final HttpClient http;
    private final ObjectMapper om;

    private final String base;
    private final String apiKey;
    private final String lang;
    private final Duration timeout;
    private final int geocodeDefaultLimit;

    public OpenWeatherClient(
            @Value("${openweather.api.base}") String base,
            @Value("${openweather.api.key}") String apiKey,
            @Value("${openweather.lang:ru}") String lang,
            @Value("${openweather.timeout-sec:5}") int timeoutSec,
            @Value("${openweather.geo.default-limit:5}") int geocodeDefaultLimit,
            ObjectMapper objectMapper
    ) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSec))
                .build();
        this.om = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.base = trimTrailingSlash(base);
        this.apiKey = apiKey;
        this.lang = lang;
        this.timeout = Duration.ofSeconds(timeoutSec);
        this.geocodeDefaultLimit = geocodeDefaultLimit;
    }

    public WeatherResponse weather(double lat, double lon) throws InterruptedException {
        URI uri = buildWeatherUri(lat, lon);
        HttpRequest req = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET().build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            ensure2XXOrThrow(uri, resp);
            return om.readValue(resp.body(), WeatherResponse.class);
        } catch (IOException | RuntimeException e) {
            throw new WeatherException("OpenWeather I/O error: " + e.getMessage(), e);
        }
    }

    public List<LocationResponse> geoCode(String query) throws InterruptedException {
        return geoCode(query, geocodeDefaultLimit);
    }

    public List<LocationResponse> geoCode(String query, int limit) throws InterruptedException {
        String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) {
            throw new WeatherException("Geocode query must not be empty");
        }
        int lim = (limit > 0) ? limit : geocodeDefaultLimit;

        URI uri = UriComponentsBuilder.fromUriString(base)
                .path("/geo/1.0/direct")
                .queryParam("q", q)
                .queryParam("limit", lim)
                .queryParam("appid", apiKey)
                .encode()
                .build()
                .toUri();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(timeout)
                .GET()
                .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            ensure2XXOrThrow(uri, resp);
            LocationResponse[] arr = om.readValue(resp.body(), LocationResponse[].class);
            return Arrays.asList(arr);
        } catch (IOException | RuntimeException e) {
            throw new WeatherException("OpenWeather I/O error: " + e.getMessage(), e);
        }
    }

    private URI buildWeatherUri(double lat, double lon) {
        return UriComponentsBuilder.fromUriString(base)
                .path("/data/2.5/weather")
                .queryParam("lat", lat)
                .queryParam("lon", lon)
                .queryParam("units", "metric")
                .queryParam("appid", apiKey)
                .queryParam("lang", lang)
                .encode()
                .build()
                .toUri();
    }

    private void ensure2XXOrThrow(URI uri, HttpResponse<String> resp) {
        int code = resp.statusCode();
        if (code / 100 != 2) {
            String body = resp.body();
            String shortBody = (body != null && body.length() > 500) ? body.substring(0, 500) + "..." : body;
            log.warn("OpenWeather non-2xx: code={} url={} body={}", code, safeUri(uri), shortBody);
            throw new WeatherException("OpenWeather returned code " + code);
        }
    }

    private URI safeUri(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam("appid", maskApiKey(apiKey))
                .build(true)
                .toUri();
    }

    private static String maskApiKey(String key) {
        if (key == null || key.isEmpty()) {
            return "****";
        }
        int keep = Math.min(4, key.length());
        int stars = Math.max(0, key.length() - keep);
        return "*".repeat(stars) + key.substring(key.length() - keep);
    }

    private static String trimTrailingSlash(String s) {
        return (s != null && s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }
}
