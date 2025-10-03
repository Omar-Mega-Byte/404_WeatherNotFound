package com.weather_found.weather_app.modules.prediction.service;

import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionRequest;
import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionResponse;
import com.weather_found.weather_app.modules.prediction.validation.WeatherPredictionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherPredictionService
 */
@ExtendWith(MockitoExtension.class)
class WeatherPredictionServiceTest {

    @Mock
    private NASAWeatherDataService nasaWeatherDataService;

    @Mock
    private WeatherPredictionValidator validator;

    @InjectMocks
    private WeatherPredictionService weatherPredictionService;

    private WeatherPredictionRequest validRequest;
    private NASAWeatherDataService.WeatherStatistics mockStats;

    @BeforeEach
    void setUp() {
        // Create a valid request
        validRequest = new WeatherPredictionRequest();
        validRequest.setName("Test Location");
        validRequest.setLatitude(25.0);
        validRequest.setLongitude(30.0);
        validRequest.setCountry("Test Country");
        validRequest.setBeginDate(new int[] { 2025, 12, 25 }); // Future date

        // Create mock weather statistics
        mockStats = new NASAWeatherDataService.WeatherStatistics();
        mockStats.setAvgTemperature(25.0);
        mockStats.setAvgTemperatureMin(20.0);
        mockStats.setAvgTemperatureMax(30.0);
        mockStats.setAvgPrecipitation(5.0);
        mockStats.setAvgWindSpeed(10.0);
        mockStats.setAvgHumidity(60.0);
        mockStats.setAvgPressure(1013.25);
        mockStats.setExtremeHeatProbability(10.0);
        mockStats.setExtremeColdProbability(5.0);
        mockStats.setHeavyRainProbability(15.0);
        mockStats.setHighWindProbability(8.0);
    }

    @Test
    void testGenerateWeatherPrediction_Success() {
        // Arrange
        when(validator.validateRequest(any(WeatherPredictionRequest.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(validator.validateResponse(any(WeatherPredictionResponse.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(nasaWeatherDataService.getWeatherStatisticsForDayOfYear(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(mockStats);

        // Act
        WeatherPredictionResponse response = weatherPredictionService.generateWeatherPrediction(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Test Location", response.getLocationName());
        assertEquals(25.0, response.getLatitude());
        assertEquals(30.0, response.getLongitude());
        assertEquals("Test Country", response.getCountry());
        assertEquals(LocalDate.of(2025, 12, 25), response.getPredictionDate());

        // Verify forecast data is present
        assertNotNull(response.getForecast());
        assertNotNull(response.getForecast().getTemperatureMin());
        assertNotNull(response.getForecast().getTemperatureMax());
        assertNotNull(response.getForecast().getTemperatureAvg());

        // Verify probabilities are present
        assertNotNull(response.getProbabilities());
        assertNotNull(response.getProbabilities().getExtremeHeatProbability());

        // Verify historical context is present
        assertNotNull(response.getHistoricalContext());
        assertEquals(Integer.valueOf(10), response.getHistoricalContext().getYearsOfData());

        // Verify service calls
        verify(validator).validateRequest(validRequest);
        verify(validator).validateResponse(response);
        verify(nasaWeatherDataService).getWeatherStatisticsForDayOfYear(25.0, 30.0, 359); // Dec 25 is day 359
    }

    @Test
    void testGenerateWeatherPrediction_InvalidRequest() {
        // Arrange
        WeatherPredictionRequest invalidRequest = new WeatherPredictionRequest();
        invalidRequest.setLatitude(200.0); // Invalid latitude

        WeatherPredictionValidator.ValidationResult invalidResult = new WeatherPredictionValidator.ValidationResult(
                false,
                java.util.List.of("Latitude must be between -90 and 90 degrees"));

        when(validator.validateRequest(any(WeatherPredictionRequest.class)))
                .thenReturn(invalidResult);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> weatherPredictionService.generateWeatherPrediction(invalidRequest));

        assertTrue(exception.getMessage().contains("Invalid request"));
        assertTrue(exception.getMessage().contains("Latitude must be between -90 and 90"));

        // Verify no NASA service call was made
        verify(nasaWeatherDataService, never()).getWeatherStatisticsForDayOfYear(anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void testGenerateWeatherPrediction_NullBeginDate() {
        // Arrange
        validRequest.setBeginDate(null);

        when(validator.validateRequest(any(WeatherPredictionRequest.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> weatherPredictionService.generateWeatherPrediction(validRequest));

        assertEquals("Begin date is required for weather prediction", exception.getMessage());
    }

    @Test
    void testTemperatureConsistency() {
        // Arrange
        when(validator.validateRequest(any(WeatherPredictionRequest.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(validator.validateResponse(any(WeatherPredictionResponse.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(nasaWeatherDataService.getWeatherStatisticsForDayOfYear(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(mockStats);

        // Act
        WeatherPredictionResponse response = weatherPredictionService.generateWeatherPrediction(validRequest);

        // Assert temperature consistency
        WeatherPredictionResponse.WeatherForecast forecast = response.getForecast();
        assertNotNull(forecast.getTemperatureMin());
        assertNotNull(forecast.getTemperatureMax());
        assertNotNull(forecast.getTemperatureAvg());

        // Min should be less than or equal to average
        assertTrue(forecast.getTemperatureMin() <= forecast.getTemperatureAvg(),
                "Min temperature should be less than or equal to average");

        // Average should be less than or equal to max
        assertTrue(forecast.getTemperatureAvg() <= forecast.getTemperatureMax(),
                "Average temperature should be less than or equal to max");
    }

    @Test
    void testProbabilityRanges() {
        // Arrange
        when(validator.validateRequest(any(WeatherPredictionRequest.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(validator.validateResponse(any(WeatherPredictionResponse.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(nasaWeatherDataService.getWeatherStatisticsForDayOfYear(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(mockStats);

        // Act
        WeatherPredictionResponse response = weatherPredictionService.generateWeatherPrediction(validRequest);

        // Assert probability ranges
        WeatherPredictionResponse.WeatherProbabilities probabilities = response.getProbabilities();

        assertNotNull(probabilities.getExtremeHeatProbability());
        assertTrue(probabilities.getExtremeHeatProbability() >= 0 && probabilities.getExtremeHeatProbability() <= 100,
                "Extreme heat probability should be between 0 and 100");

        assertNotNull(probabilities.getExtremeColdProbability());
        assertTrue(probabilities.getExtremeColdProbability() >= 0 && probabilities.getExtremeColdProbability() <= 100,
                "Extreme cold probability should be between 0 and 100");

        assertNotNull(probabilities.getHeavyRainProbability());
        assertTrue(probabilities.getHeavyRainProbability() >= 0 && probabilities.getHeavyRainProbability() <= 100,
                "Heavy rain probability should be between 0 and 100");

        assertNotNull(probabilities.getComfortableWeatherProbability());
        assertTrue(
                probabilities.getComfortableWeatherProbability() >= 0
                        && probabilities.getComfortableWeatherProbability() <= 100,
                "Comfortable weather probability should be between 0 and 100");
    }

    @Test
    void testResponseValidationWarning() {
        // Arrange
        when(validator.validateRequest(any(WeatherPredictionRequest.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));

        // Mock response validation to fail (but don't throw exception)
        WeatherPredictionValidator.ValidationResult invalidResponse = new WeatherPredictionValidator.ValidationResult(
                false,
                java.util.List.of("Temperature out of range"));
        when(validator.validateResponse(any(WeatherPredictionResponse.class)))
                .thenReturn(invalidResponse);

        when(nasaWeatherDataService.getWeatherStatisticsForDayOfYear(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(mockStats);

        // Act - should not throw exception even with invalid response
        WeatherPredictionResponse response = weatherPredictionService.generateWeatherPrediction(validRequest);

        // Assert
        assertNotNull(response);
        verify(validator).validateResponse(response);
    }

    @Test
    void testDataSourceAndConfidenceLevel() {
        // Arrange
        when(validator.validateRequest(any(WeatherPredictionRequest.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(validator.validateResponse(any(WeatherPredictionResponse.class)))
                .thenReturn(new WeatherPredictionValidator.ValidationResult(true, new ArrayList<>()));
        when(nasaWeatherDataService.getWeatherStatisticsForDayOfYear(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(mockStats);

        // Act
        WeatherPredictionResponse response = weatherPredictionService.generateWeatherPrediction(validRequest);

        // Assert
        assertEquals("NASA POWER API + Statistical Analysis", response.getDataSource());
        assertNotNull(response.getConfidenceLevel());
        assertTrue(response.getConfidenceLevel().contains("%"));
        assertNotNull(response.getGeneratedAt());
    }
}