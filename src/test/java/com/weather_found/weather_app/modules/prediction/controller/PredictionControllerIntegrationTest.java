package com.weather_found.weather_app.modules.prediction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionRequest;
import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionResponse;
import com.weather_found.weather_app.modules.prediction.service.WeatherPredictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PredictionController
 */
@WebMvcTest(PredictionController.class)
class PredictionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherPredictionService weatherPredictionService;

    @Autowired
    private ObjectMapper objectMapper;

    private WeatherPredictionRequest validRequest;
    private WeatherPredictionResponse mockResponse;

    @BeforeEach
    void setUp() {
        // Create valid request
        validRequest = new WeatherPredictionRequest();
        validRequest.setId(10L);
        validRequest.setName("New Valley");
        validRequest.setLatitude(25.76691912986536);
        validRequest.setLongitude(29.355468750000004);
        validRequest.setCountry("Egypt");
        validRequest.setState("New Valley");
        validRequest.setCity(null);
        validRequest.setAddress("New Valley, Egypt");
        validRequest.setBeginDate(new int[] { 2025, 11, 15 });
        validRequest.setCreatedAt(new long[] { 2025, 10, 3, 10, 17, 52, 982199000L });
        validRequest.setUpdatedAt(new long[] { 2025, 10, 3, 10, 17, 52, 982199000L });

        // Create mock response
        WeatherPredictionResponse.WeatherForecast forecast = WeatherPredictionResponse.WeatherForecast.builder()
                .temperatureMin(18.5)
                .temperatureMax(32.1)
                .temperatureAvg(25.3)
                .humidity(45.2)
                .precipitation(2.1)
                .windSpeed(8.7)
                .windDirection(245.3)
                .pressure(1015.2)
                .skyCondition("Partly Cloudy")
                .weatherDescription("Warm and dry, moderate winds")
                .build();

        WeatherPredictionResponse.WeatherProbabilities probabilities = WeatherPredictionResponse.WeatherProbabilities
                .builder()
                .extremeHeatProbability(15.3)
                .extremeColdProbability(0.1)
                .heavyRainProbability(8.2)
                .highWindProbability(12.5)
                .stormProbability(5.1)
                .comfortableWeatherProbability(72.8)
                .build();

        WeatherPredictionResponse.HistoricalContext historicalContext = WeatherPredictionResponse.HistoricalContext
                .builder()
                .yearsOfData(10)
                .historicalAvgTemp(24.8)
                .historicalAvgPrecipitation(1.8)
                .climateTrend("warming")
                .seasonalPattern("Autumn pattern - cooling temperatures, increased precipitation")
                .build();

        mockResponse = WeatherPredictionResponse.builder()
                .locationName("New Valley")
                .latitude(25.76691912986536)
                .longitude(29.355468750000004)
                .country("Egypt")
                .state("New Valley")
                .city(null)
                .predictionDate(LocalDate.of(2025, 11, 15))
                .forecast(forecast)
                .probabilities(probabilities)
                .historicalContext(historicalContext)
                .generatedAt(LocalDateTime.now())
                .dataSource("NASA POWER API + Statistical Analysis")
                .confidenceLevel("High (85-90%)")
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetWeatherPrediction_Success() throws Exception {
        // Arrange
        when(weatherPredictionService.generateWeatherPrediction(any(WeatherPredictionRequest.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.locationName").value("New Valley"))
                .andExpect(jsonPath("$.latitude").value(25.76691912986536))
                .andExpect(jsonPath("$.longitude").value(29.355468750000004))
                .andExpect(jsonPath("$.country").value("Egypt"))
                .andExpect(jsonPath("$.predictionDate").value("2025-11-15"))
                .andExpect(jsonPath("$.forecast").exists())
                .andExpect(jsonPath("$.forecast.temperatureMin").value(18.5))
                .andExpect(jsonPath("$.forecast.temperatureMax").value(32.1))
                .andExpect(jsonPath("$.forecast.temperatureAvg").value(25.3))
                .andExpect(jsonPath("$.forecast.humidity").value(45.2))
                .andExpect(jsonPath("$.forecast.precipitation").value(2.1))
                .andExpect(jsonPath("$.forecast.windSpeed").value(8.7))
                .andExpect(jsonPath("$.forecast.skyCondition").value("Partly Cloudy"))
                .andExpect(jsonPath("$.probabilities").exists())
                .andExpect(jsonPath("$.probabilities.extremeHeatProbability").value(15.3))
                .andExpect(jsonPath("$.probabilities.extremeColdProbability").value(0.1))
                .andExpect(jsonPath("$.probabilities.comfortableWeatherProbability").value(72.8))
                .andExpect(jsonPath("$.historicalContext").exists())
                .andExpect(jsonPath("$.historicalContext.yearsOfData").value(10))
                .andExpect(jsonPath("$.historicalContext.climateTrend").value("warming"))
                .andExpect(jsonPath("$.dataSource").value("NASA POWER API + Statistical Analysis"))
                .andExpect(jsonPath("$.confidenceLevel").value("High (85-90%)"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetWeatherPrediction_InvalidRequest() throws Exception {
        // Arrange - Invalid latitude
        WeatherPredictionRequest invalidRequest = new WeatherPredictionRequest();
        invalidRequest.setName("Test Location");
        invalidRequest.setLatitude(200.0); // Invalid latitude
        invalidRequest.setLongitude(30.0);
        invalidRequest.setBeginDate(new int[] { 2025, 12, 25 });

        when(weatherPredictionService.generateWeatherPrediction(any(WeatherPredictionRequest.class)))
                .thenThrow(
                        new IllegalArgumentException("Invalid request: Latitude must be between -90 and 90 degrees"));

        // Act & Assert
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetWeatherPrediction_ServiceException() throws Exception {
        // Arrange
        when(weatherPredictionService.generateWeatherPrediction(any(WeatherPredictionRequest.class)))
                .thenThrow(new RuntimeException("NASA API unavailable"));

        // Act & Assert
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testGetWeatherPrediction_Unauthorized() throws Exception {
        // Act & Assert - No authentication
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GUEST") // Wrong role
    void testGetWeatherPrediction_Forbidden() throws Exception {
        // Act & Assert - Wrong role
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN") // Admin should also work
    void testGetWeatherPrediction_AdminRole() throws Exception {
        // Arrange
        when(weatherPredictionService.generateWeatherPrediction(any(WeatherPredictionRequest.class)))
                .thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationName").value("New Valley"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetWeatherPrediction_InvalidJson() throws Exception {
        // Act & Assert - Invalid JSON
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetWeatherPrediction_MissingRequiredFields() throws Exception {
        // Arrange - Missing required fields
        WeatherPredictionRequest incompleteRequest = new WeatherPredictionRequest();
        incompleteRequest.setName("Test Location");
        // Missing latitude, longitude, beginDate

        when(weatherPredictionService.generateWeatherPrediction(any(WeatherPredictionRequest.class)))
                .thenThrow(new IllegalArgumentException(
                        "Invalid request: Latitude is required, Longitude is required, Begin date is required"));

        // Act & Assert
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(incompleteRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testGetWeatherPrediction_ResponseStructure() throws Exception {
        // Arrange
        when(weatherPredictionService.generateWeatherPrediction(any(WeatherPredictionRequest.class)))
                .thenReturn(mockResponse);

        // Act & Assert - Verify complete response structure
        mockMvc.perform(post("/api/predictions/forecast")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationName").exists())
                .andExpect(jsonPath("$.latitude").exists())
                .andExpect(jsonPath("$.longitude").exists())
                .andExpect(jsonPath("$.predictionDate").exists())
                .andExpect(jsonPath("$.forecast").exists())
                .andExpect(jsonPath("$.forecast.temperatureMin").exists())
                .andExpect(jsonPath("$.forecast.temperatureMax").exists())
                .andExpect(jsonPath("$.forecast.temperatureAvg").exists())
                .andExpect(jsonPath("$.forecast.humidity").exists())
                .andExpect(jsonPath("$.forecast.precipitation").exists())
                .andExpect(jsonPath("$.forecast.windSpeed").exists())
                .andExpect(jsonPath("$.forecast.pressure").exists())
                .andExpect(jsonPath("$.forecast.skyCondition").exists())
                .andExpect(jsonPath("$.forecast.weatherDescription").exists())
                .andExpect(jsonPath("$.probabilities").exists())
                .andExpect(jsonPath("$.probabilities.extremeHeatProbability").exists())
                .andExpect(jsonPath("$.probabilities.extremeColdProbability").exists())
                .andExpect(jsonPath("$.probabilities.heavyRainProbability").exists())
                .andExpect(jsonPath("$.probabilities.highWindProbability").exists())
                .andExpect(jsonPath("$.probabilities.stormProbability").exists())
                .andExpect(jsonPath("$.probabilities.comfortableWeatherProbability").exists())
                .andExpect(jsonPath("$.historicalContext").exists())
                .andExpect(jsonPath("$.historicalContext.yearsOfData").exists())
                .andExpect(jsonPath("$.historicalContext.historicalAvgTemp").exists())
                .andExpect(jsonPath("$.historicalContext.historicalAvgPrecipitation").exists())
                .andExpect(jsonPath("$.historicalContext.climateTrend").exists())
                .andExpect(jsonPath("$.historicalContext.seasonalPattern").exists())
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.dataSource").exists())
                .andExpect(jsonPath("$.confidenceLevel").exists());
    }
}