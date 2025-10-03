package com.weather_found.weather_app.modules.prediction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for weather prediction responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeatherPredictionResponse {

    private String locationName;
    private Double latitude;
    private Double longitude;
    private String country;
    private String state;
    private String city;
    private LocalDate predictionDate;

    // Weather prediction data
    private WeatherForecast forecast;
    private WeatherProbabilities probabilities;
    private HistoricalContext historicalContext;

    private LocalDateTime generatedAt;
    private String dataSource;
    private String confidenceLevel;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherForecast {
        private Double temperatureMin; // Celsius
        private Double temperatureMax; // Celsius
        private Double temperatureAvg; // Celsius
        private Double humidity; // Percentage
        private Double precipitation; // mm
        private Double windSpeed; // m/s
        private Double windDirection; // degrees
        private Double pressure; // hPa
        private String skyCondition; // Clear, Cloudy, Partly Cloudy, etc.
        private String weatherDescription;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeatherProbabilities {
        private Double extremeHeatProbability; // >35°C
        private Double extremeColdProbability; // <0°C
        private Double heavyRainProbability; // >25mm
        private Double highWindProbability; // >15 m/s
        private Double stormProbability;
        private Double comfortableWeatherProbability; // 18-25°C, low wind, no rain
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistoricalContext {
        private Integer yearsOfData;
        private Double historicalAvgTemp;
        private Double historicalAvgPrecipitation;
        private String climateTrend; // "warming", "cooling", "stable"
        private String seasonalPattern;
    }
}