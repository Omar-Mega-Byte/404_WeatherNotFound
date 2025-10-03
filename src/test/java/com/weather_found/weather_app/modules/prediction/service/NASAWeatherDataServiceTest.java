package com.weather_found.weather_app.modules.prediction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NASAWeatherDataService
 */
@ExtendWith(MockitoExtension.class)
class NASAWeatherDataServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NASAWeatherDataService nasaWeatherDataService;

    private Map<String, Object> mockNASAResponse;

    @BeforeEach
    void setUp() {
        // Create mock NASA POWER API response
        mockNASAResponse = new HashMap<>();
        Map<String, Object> properties = new HashMap<>();
        Map<String, Object> parameter = new HashMap<>();

        // Mock temperature data
        Map<String, Object> tempMinData = new HashMap<>();
        tempMinData.put("20231201", 18.5);
        tempMinData.put("20231202", 19.2);
        tempMinData.put("20231203", 17.8);
        parameter.put("T2M_MIN", tempMinData);

        Map<String, Object> tempMaxData = new HashMap<>();
        tempMaxData.put("20231201", 28.5);
        tempMaxData.put("20231202", 29.2);
        tempMaxData.put("20231203", 27.8);
        parameter.put("T2M_MAX", tempMaxData);

        // Mock precipitation data
        Map<String, Object> precipData = new HashMap<>();
        precipData.put("20231201", 2.5);
        precipData.put("20231202", 0.0);
        precipData.put("20231203", 5.2);
        parameter.put("PRECTOTCORR", precipData);

        // Mock wind data
        Map<String, Object> windData = new HashMap<>();
        windData.put("20231201", 8.5);
        windData.put("20231202", 12.2);
        windData.put("20231203", 6.8);
        parameter.put("WS10M", windData);

        // Mock humidity data
        Map<String, Object> humidityData = new HashMap<>();
        humidityData.put("20231201", 65.5);
        humidityData.put("20231202", 58.2);
        humidityData.put("20231203", 72.8);
        parameter.put("RH2M", humidityData);

        // Mock pressure data
        Map<String, Object> pressureData = new HashMap<>();
        pressureData.put("20231201", 1013.25);
        pressureData.put("20231202", 1015.50);
        pressureData.put("20231203", 1012.75);
        parameter.put("PS", pressureData);

        properties.put("parameter", parameter);
        mockNASAResponse.put("properties", properties);
    }

    @Test
    void testGetHistoricalWeatherData_Success() {
        // Arrange
        double latitude = 25.0;
        double longitude = 30.0;
        LocalDate startDate = LocalDate.of(2023, 12, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 3);

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(mockNASAResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class))).thenReturn(mockResponse);

        // Act
        NASAWeatherDataService.HistoricalWeatherData result = nasaWeatherDataService.getHistoricalWeatherData(latitude,
                longitude, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertFalse(result.getTemperatureMin().isEmpty());
        assertFalse(result.getTemperatureMax().isEmpty());
        assertFalse(result.getPrecipitation().isEmpty());
        assertFalse(result.getWindSpeed().isEmpty());
        assertFalse(result.getHumidity().isEmpty());
        assertFalse(result.getPressure().isEmpty());

        // Check specific values
        assertEquals(3, result.getTemperatureMin().size());
        assertEquals(3, result.getTemperatureMax().size());
        assertEquals(3, result.getPrecipitation().size());

        // Verify temperature ranges are reasonable
        assertTrue(result.getTemperatureMin().get(0) < result.getTemperatureMax().get(0));

        verify(restTemplate).exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class));
    }

    @Test
    void testGetHistoricalWeatherData_APIFailure_ReturnsFallback() {
        // Arrange
        double latitude = 25.0;
        double longitude = 30.0;
        LocalDate startDate = LocalDate.of(2023, 12, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 3);

        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class))).thenThrow(new RestClientException("API Error"));

        // Act
        NASAWeatherDataService.HistoricalWeatherData result = nasaWeatherDataService.getHistoricalWeatherData(latitude,
                longitude, startDate, endDate);

        // Assert - should return fallback data, not null
        assertNotNull(result);
        assertFalse(result.getTemperatureMin().isEmpty());
        assertFalse(result.getTemperatureMax().isEmpty());

        // Fallback data should have reasonable values for the location (Egypt)
        double avgTemp = result.getTemperatureMin().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        assertTrue(avgTemp > 0 && avgTemp < 50, "Fallback temperature should be reasonable for Egypt");
    }

    @Test
    void testGetWeatherStatisticsForDayOfYear() {
        // Arrange
        double latitude = 25.0;
        double longitude = 30.0;
        int dayOfYear = 359; // December 25

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(mockNASAResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class))).thenReturn(mockResponse);

        // Act
        NASAWeatherDataService.WeatherStatistics result = nasaWeatherDataService
                .getWeatherStatisticsForDayOfYear(latitude, longitude, dayOfYear);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAvgTemperature());
        assertNotNull(result.getAvgPrecipitation());
        assertNotNull(result.getAvgWindSpeed());
        assertNotNull(result.getAvgHumidity());

        // Check probability calculations
        assertNotNull(result.getExtremeHeatProbability());
        assertNotNull(result.getExtremeColdProbability());
        assertNotNull(result.getHeavyRainProbability());
        assertNotNull(result.getHighWindProbability());

        // Probabilities should be between 0 and 100
        assertTrue(result.getExtremeHeatProbability() >= 0 && result.getExtremeHeatProbability() <= 100);
        assertTrue(result.getExtremeColdProbability() >= 0 && result.getExtremeColdProbability() <= 100);
        assertTrue(result.getHeavyRainProbability() >= 0 && result.getHeavyRainProbability() <= 100);
        assertTrue(result.getHighWindProbability() >= 0 && result.getHighWindProbability() <= 100);
    }

    @Test
    void testParseNASAPOWERResponse_InvalidData() {
        // Arrange
        Map<String, Object> invalidResponse = new HashMap<>();
        invalidResponse.put("invalid", "data");

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(invalidResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class))).thenReturn(mockResponse);

        // Act
        NASAWeatherDataService.HistoricalWeatherData result = nasaWeatherDataService.getHistoricalWeatherData(25.0,
                30.0,
                LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 3));

        // Assert - should handle gracefully and return empty lists
        assertNotNull(result);
        assertTrue(result.getTemperatureMin().isEmpty());
        assertTrue(result.getTemperatureMax().isEmpty());
    }

    @Test
    void testBuildPOWERApiUrl() {
        // This is more of an integration test, but we can verify the service processes
        // coordinates correctly
        double latitude = 25.76691912986536;
        double longitude = 29.355468750000004;
        LocalDate startDate = LocalDate.of(2023, 12, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 3);

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(mockNASAResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class))).thenReturn(mockResponse);

        // Act
        NASAWeatherDataService.HistoricalWeatherData result = nasaWeatherDataService.getHistoricalWeatherData(latitude,
                longitude, startDate, endDate);

        // Assert
        assertNotNull(result);

        // Verify the URL was called with correct coordinates (via argument capture)
        verify(restTemplate).exchange(
                argThat(url -> url.toString().contains("longitude=" + longitude) &&
                        url.toString().contains("latitude=" + latitude)),
                eq(org.springframework.http.HttpMethod.GET),
                any(),
                eq(Map.class));
    }

    @Test
    void testFallbackDataGeneration() {
        // Arrange - Force fallback by simulating API failure
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class))).thenThrow(new RestClientException("Network error"));

        // Act
        NASAWeatherDataService.HistoricalWeatherData result = nasaWeatherDataService.getHistoricalWeatherData(25.0,
                30.0,
                LocalDate.of(2023, 12, 1), LocalDate.of(2023, 12, 10));

        // Assert fallback data quality
        assertNotNull(result);
        assertEquals(10, result.getTemperatureMin().size()); // 10 days of data
        assertEquals(10, result.getTemperatureMax().size());
        assertEquals(10, result.getPrecipitation().size());

        // Check that fallback data is reasonable
        for (int i = 0; i < result.getTemperatureMin().size(); i++) {
            assertTrue(result.getTemperatureMin().get(i) < result.getTemperatureMax().get(i),
                    "Min temperature should be less than max temperature");
            assertTrue(result.getPrecipitation().get(i) >= 0, "Precipitation should not be negative");
            assertTrue(result.getWindSpeed().get(i) >= 0, "Wind speed should not be negative");
            assertTrue(result.getHumidity().get(i) >= 0 && result.getHumidity().get(i) <= 100,
                    "Humidity should be between 0 and 100");
        }
    }

    @Test
    void testWeatherStatisticsCalculation() {
        // Arrange
        ResponseEntity<Map> mockResponse = new ResponseEntity<>(mockNASAResponse, HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(Map.class))).thenReturn(mockResponse);

        // Act
        NASAWeatherDataService.WeatherStatistics stats = nasaWeatherDataService.getWeatherStatisticsForDayOfYear(25.0,
                30.0, 359);

        // Assert temperature calculations
        assertNotNull(stats.getAvgTemperatureMin());
        assertNotNull(stats.getAvgTemperatureMax());
        assertTrue(stats.getAvgTemperatureMin() < stats.getAvgTemperatureMax());

        // Assert precipitation calculations
        assertNotNull(stats.getAvgPrecipitation());
        assertTrue(stats.getAvgPrecipitation() >= 0);

        // Assert probability calculations are reasonable
        assertTrue(stats.getExtremeHeatProbability() >= 0 && stats.getExtremeHeatProbability() <= 100);
        assertTrue(stats.getExtremeColdProbability() >= 0 && stats.getExtremeColdProbability() <= 100);
    }
}