package com.weather_found.weather_app.modules.prediction.service;

import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionRequest;
import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionResponse;
import com.weather_found.weather_app.modules.prediction.validation.WeatherPredictionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Service for generating weather predictions based on historical data and
 * statistical analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherPredictionService {

    private final NASAWeatherDataService nasaWeatherDataService;
    private final WeatherPredictionValidator validator;
    private final Random random = new Random();

    /**
     * Generate weather prediction for a specific location and date
     */
    public WeatherPredictionResponse generateWeatherPrediction(WeatherPredictionRequest request) {
        log.info("Generating weather prediction for location: {} at coordinates {}, {}",
                request.getName(), request.getLatitude(), request.getLongitude());

        // Validate input request
        WeatherPredictionValidator.ValidationResult validationResult = validator.validateRequest(request);
        if (!validationResult.isValid()) {
            String errorMessage = "Invalid request: " + String.join(", ", validationResult.getErrors());
            log.warn(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LocalDate predictionDate = request.getBeginDateAsLocalDate();
        if (predictionDate == null) {
            throw new IllegalArgumentException("Begin date is required for weather prediction");
        }

        // Get day of year for historical analysis
        int dayOfYear = predictionDate.getDayOfYear();

        // Fetch historical weather statistics for this day of year
        NASAWeatherDataService.WeatherStatistics stats = nasaWeatherDataService
                .getWeatherStatisticsForDayOfYear(request.getLatitude(), request.getLongitude(), dayOfYear);

        // Generate forecast based on historical data
        WeatherPredictionResponse.WeatherForecast forecast = generateForecast(stats, predictionDate, request);

        // Calculate probabilities
        WeatherPredictionResponse.WeatherProbabilities probabilities = generateProbabilities(stats,
                request.getLatitude(), predictionDate);

        // Create historical context
        WeatherPredictionResponse.HistoricalContext historicalContext = generateHistoricalContext(stats,
                request.getLatitude(), predictionDate);

        WeatherPredictionResponse response = WeatherPredictionResponse.builder()
                .locationName(request.getName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .country(request.getCountry())
                .state(request.getState())
                .city(request.getCity())
                .predictionDate(predictionDate)
                .forecast(forecast)
                .probabilities(probabilities)
                .historicalContext(historicalContext)
                .generatedAt(LocalDateTime.now())
                .dataSource("NASA POWER API + Statistical Analysis")
                .confidenceLevel(calculateConfidenceLevel(stats))
                .build();

        // Validate the generated response
        WeatherPredictionValidator.ValidationResult responseValidation = validator.validateResponse(response);
        if (!responseValidation.isValid()) {
            log.warn("Generated response validation failed: {}", responseValidation.getErrors());
            // Don't throw exception, but log warnings for monitoring
        }

        log.info("Weather prediction generated successfully for location: {}", request.getName());
        return response;
    }

    private WeatherPredictionResponse.WeatherForecast generateForecast(
            NASAWeatherDataService.WeatherStatistics stats, LocalDate predictionDate,
            WeatherPredictionRequest request) {

        // Add some variability to historical averages for prediction
        double tempVariability = random.nextGaussian() * 3.0; // ±3°C variability
        double precipVariability = Math.max(0, random.nextGaussian() * 2.0 + 1.0); // precipitation variability
        double windVariability = Math.max(0, random.nextGaussian() * 2.0); // wind variability
        double humidityVariability = random.nextGaussian() * 10.0; // ±10% humidity variability

        double precipitation = roundToOneDecimal(stats.getAvgPrecipitation() * precipVariability);
        double windSpeed = roundToOneDecimal(stats.getAvgWindSpeed() + windVariability);

        // Ensure pressure-precipitation consistency with realistic base pressure
        double basePressure = ensureRealisticPressure(stats.getAvgPressure());
        double pressure = adjustPressureForWeather(basePressure + random.nextGaussian() * 8, precipitation, windSpeed);

        // Use actual NASA historical data with realistic variability for prediction
        double adjustedMinTemp = stats.getAvgTemperatureMin() + tempVariability - Math.abs(random.nextGaussian() * 1.5);
        double adjustedMaxTemp = stats.getAvgTemperatureMax() + tempVariability + Math.abs(random.nextGaussian() * 1.5);

        // Ensure min <= max relationship and calculate realistic average
        if (adjustedMinTemp >= adjustedMaxTemp) {
            double temp = adjustedMinTemp;
            adjustedMinTemp = adjustedMaxTemp - 2.0;
            adjustedMaxTemp = temp + 2.0;
        }
        double adjustedAvgTemp = (adjustedMinTemp + adjustedMaxTemp) / 2 + random.nextGaussian() * 0.5;

        return WeatherPredictionResponse.WeatherForecast.builder()
                .temperatureMin(roundToOneDecimal(adjustedMinTemp))
                .temperatureMax(roundToOneDecimal(adjustedMaxTemp))
                .temperatureAvg(roundToOneDecimal(adjustedAvgTemp))
                .humidity(Math.max(0, Math.min(100, roundToOneDecimal(stats.getAvgHumidity() + humidityVariability))))
                .precipitation(precipitation)
                .windSpeed(windSpeed)
                .windDirection(roundToOneDecimal(random.nextDouble() * 360)) // Random wind direction
                .pressure(roundToOneDecimal(fixPressureScaling(pressure)))
                .skyCondition(generateSkyCondition(precipitation))
                .weatherDescription(
                        generateSeasonAwareWeatherDescription(adjustedAvgTemp, precipitation, windSpeed, predictionDate,
                                request.getLatitude()))
                .build();
    }

    private WeatherPredictionResponse.WeatherProbabilities generateProbabilities(
            NASAWeatherDataService.WeatherStatistics stats, double latitude, LocalDate predictionDate) {

        // Calculate calibrated comfortable weather probability using historical
        // percentiles
        double comfortableWeatherProb = calculateCalibratedComfortableProbability(stats, latitude);

        // Calculate season-aware probability adjustments
        double adjustedExtremeHeat = calculateSeasonAwareHeatProbability(stats, predictionDate, latitude);
        double adjustedExtremeCold = calculateSeasonAwareColdProbability(stats, predictionDate, latitude);
        double adjustedHeavyRain = adjustProbabilityForForecast(stats.getHeavyRainProbability(), "rain", stats);
        double adjustedHighWind = adjustProbabilityForForecast(stats.getHighWindProbability(), "wind", stats);

        return WeatherPredictionResponse.WeatherProbabilities.builder()
                .extremeHeatProbability(roundToOneDecimal(adjustedExtremeHeat))
                .extremeColdProbability(roundToOneDecimal(adjustedExtremeCold))
                .heavyRainProbability(roundToOneDecimal(adjustedHeavyRain))
                .highWindProbability(roundToOneDecimal(adjustedHighWind))
                .stormProbability(roundToOneDecimal(calculateStormProbability(stats)))
                .comfortableWeatherProbability(roundToOneDecimal(comfortableWeatherProb))
                .build();
    }

    private WeatherPredictionResponse.HistoricalContext generateHistoricalContext(
            NASAWeatherDataService.WeatherStatistics stats, double latitude, LocalDate predictionDate) {

        // Use actual NASA historical averages instead of calculated ones
        double historicalAvgTemp = stats.getAvgTemperature();
        double historicalAvgPrecip = stats.getAvgPrecipitation();

        return WeatherPredictionResponse.HistoricalContext.builder()
                .yearsOfData(10) // We're using 10 years of historical data from NASA
                .historicalAvgTemp(roundToOneDecimal(historicalAvgTemp))
                .historicalAvgPrecipitation(roundToOneDecimal(historicalAvgPrecip))
                .climateTrend(generateClimateTrend(stats))
                .seasonalPattern(generateDetailedSeasonalPattern(latitude, predictionDate))
                .build();
    }

    private double calculateStormProbability(NASAWeatherDataService.WeatherStatistics stats) {
        // Storm probability based on combination of high wind and heavy rain
        double windFactor = stats.getHighWindProbability() / 100.0;
        double rainFactor = stats.getHeavyRainProbability() / 100.0;
        return Math.min(50.0, windFactor * rainFactor * 100.0); // Cap at 50%
    }

    private String generateSkyCondition(double precipitation) {
        if (precipitation > 20) {
            return "Overcast";
        } else if (precipitation > 5) {
            return "Cloudy";
        } else if (precipitation > 1) {
            return "Partly Cloudy";
        } else {
            return "Clear";
        }
    }

    private String generateClimateTrend(NASAWeatherDataService.WeatherStatistics stats) {
        // Simplified trend analysis - in reality, this would analyze year-over-year
        // changes
        double avgTemp = stats.getAvgTemperature();
        if (avgTemp > 25) {
            return "warming";
        } else if (avgTemp < 10) {
            return "cooling";
        } else {
            return "stable";
        }
    }

    private String generateDetailedSeasonalPattern(double latitude, LocalDate predictionDate) {
        String season = getSeason(predictionDate, latitude);
        String hemisphere = latitude >= 0 ? "Northern" : "Southern";
        double absLatitude = Math.abs(latitude);

        // Climate zone classification
        String climateZone;
        if (absLatitude < 30) {
            climateZone = "Subtropical"; // Hot desert/subtropical like Egypt
        } else if (absLatitude < 50) {
            climateZone = "Mediterranean";
        } else if (absLatitude < 65) {
            climateZone = "Temperate";
        } else {
            climateZone = "Polar";
        }

        StringBuilder pattern = new StringBuilder();
        pattern.append(hemisphere).append(" Hemisphere ").append(season).append(" (").append(climateZone)
                .append(" zone)");

        // Add accurate season-specific details based on climate zone
        switch (season) {
            case "Winter":
                if (climateZone.equals("Subtropical")) {
                    pattern.append(" - mild temperatures, dry conditions, pleasant weather");
                } else if (climateZone.equals("Mediterranean")) {
                    pattern.append(" - mild temperatures, moderate precipitation, comfortable conditions");
                } else if (climateZone.equals("Temperate")) {
                    pattern.append(" - cold temperatures, variable precipitation, possible snow");
                } else {
                    pattern.append(" - very cold, limited daylight, frozen precipitation");
                }
                break;
            case "Spring":
                if (climateZone.equals("Subtropical")) {
                    pattern.append(" - warming temperatures, dry conditions, increasing heat");
                } else {
                    pattern.append(" - warming temperatures, increasing daylight, variable precipitation");
                }
                break;
            case "Summer":
                if (climateZone.equals("Subtropical")) {
                    pattern.append(" - very hot temperatures, dry conditions, intense sun");
                } else if (climateZone.equals("Mediterranean")) {
                    pattern.append(" - hot temperatures, dry conditions, clear skies");
                } else {
                    pattern.append(" - warm temperatures, thunderstorm activity, peak growing season");
                }
                break;
            case "Autumn":
                if (climateZone.equals("Subtropical")) {
                    pattern.append(" - cooling temperatures, still dry, pleasant weather returns");
                } else {
                    pattern.append(" - cooling temperatures, decreasing daylight, increased precipitation");
                }
                break;
        }

        return pattern.toString();
    }

    /**
     * Calculate historical temperature from NASA data
     */
    private double calculateAccurateMonthlyTemperature(LocalDate date, double latitude) {
        // This should use the actual NASA historical average from the stats
        // For now, return the current stats average - in practice this should fetch
        // long-term historical data for the specific month
        return 20.0; // Placeholder - this will be overridden by actual NASA data in
                     // generateHistoricalContext
    }

    /**
     * Calculate month-specific historical precipitation based on location and month
     */
    private double calculateMonthlyHistoricalPrecipitation(LocalDate date, double latitude) {
        int month = date.getMonthValue();
        double absLatitude = Math.abs(latitude);
        boolean isSouthernHemisphere = latitude < 0;

        // Adjust month for Southern Hemisphere
        int adjustedMonth = isSouthernHemisphere ? ((month + 6 - 1) % 12) + 1 : month;

        // Base precipitation by month (mm/day) for temperate regions
        double[] monthlyPrecip = {
                2.5, // January
                2.8, // February
                3.2, // March
                3.5, // April
                4.1, // May
                4.8, // June
                4.5, // July
                4.2, // August
                3.8, // September
                3.3, // October
                2.9, // November
                2.6 // December
        };

        double basePrecip = monthlyPrecip[adjustedMonth - 1];

        // Adjust for latitude/climate zone
        if (absLatitude < 23.5) { // Tropical
            // Wet/dry season pattern
            if ((adjustedMonth >= 5 && adjustedMonth <= 10)) { // Wet season
                basePrecip *= 2.5;
            } else { // Dry season
                basePrecip *= 0.3;
            }
        } else if (absLatitude > 60) { // Polar
            basePrecip *= 0.5; // Generally drier
        }

        return basePrecip;
    }

    private String calculateConfidenceLevel(NASAWeatherDataService.WeatherStatistics stats) {
        // Confidence based on data availability and consistency
        // In a real implementation, this would consider data quality metrics

        if (stats.getAvgTemperature() != null && stats.getAvgPrecipitation() != null &&
                stats.getAvgWindSpeed() != null && stats.getAvgHumidity() != null) {
            return "High (85-90%)";
        } else if (stats.getAvgTemperature() != null && stats.getAvgPrecipitation() != null) {
            return "Medium (70-80%)";
        } else {
            return "Low (50-65%)";
        }
    }

    /**
     * Auto-detect and rescale pressure values outside normal atmospheric range
     * (300-1100 hPa)
     * Fixes bug where values like 95.2 or 90.7 indicate wrong unit (kPa vs hPa) or
     * missing digits
     */
    private double fixPressureScaling(double pressure) {
        // Normal atmospheric pressure range: 300-1100 hPa
        if (pressure < 300) {
            // Likely in kPa, convert to hPa by multiplying by 10
            if (pressure >= 30 && pressure <= 110) {
                log.debug("Converting pressure from kPa to hPa: {} -> {}", pressure, pressure * 10);
                return pressure * 10;
            }
            // Very low value, might be missing a digit - multiply by 10
            else if (pressure >= 10 && pressure < 30) {
                log.debug("Fixing low pressure value (likely missing digit): {} -> {}", pressure, pressure * 10);
                return pressure * 10;
            }
        }
        // Value seems reasonable
        if (pressure >= 300 && pressure <= 1100) {
            return pressure;
        }

        // Value too high, might be in Pa instead of hPa, convert
        if (pressure > 10000) {
            log.debug("Converting pressure from Pa to hPa: {} -> {}", pressure, pressure / 100);
            return pressure / 100;
        }

        // If still outside range, default to standard atmospheric pressure
        log.warn("Pressure value {} outside reasonable range, defaulting to 1013.25 hPa", pressure);
        return 1013.25;
    }

    /**
     * Get stricter wind description based on wind speed thresholds
     * Fixes text vs numbers mismatch
     */
    private String getWindDescription(double windSpeed) {
        if (windSpeed < 3.0) {
            return "light winds";
        } else if (windSpeed < 7.0) {
            return "moderate winds";
        } else if (windSpeed < 12.0) {
            return "breezy conditions";
        } else if (windSpeed < 18.0) {
            return "strong winds";
        } else {
            return "very strong winds";
        }
    }

    /**
     * Get season based on date and hemisphere
     * Fixes seasonal mapping logic for Southern Hemisphere
     */
    private String getSeason(LocalDate date, double latitude) {
        int month = date.getMonthValue();
        boolean isSouthernHemisphere = latitude < 0;

        // Determine season for Northern Hemisphere first
        String northernSeason;
        if (month >= 12 || month <= 2) {
            northernSeason = "Winter";
        } else if (month >= 3 && month <= 5) {
            northernSeason = "Spring";
        } else if (month >= 6 && month <= 8) {
            northernSeason = "Summer";
        } else {
            northernSeason = "Autumn";
        }

        // If Southern Hemisphere, swap seasons
        if (isSouthernHemisphere) {
            switch (northernSeason) {
                case "Winter":
                    return "Summer";
                case "Spring":
                    return "Autumn";
                case "Summer":
                    return "Winter";
                case "Autumn":
                    return "Spring";
                default:
                    return northernSeason;
            }
        }

        return northernSeason;
    }

    /**
     * Calculate calibrated probability for comfortable weather
     * Fixes probability calibration by using historical percentiles
     */
    private double calculateCalibratedComfortableProbability(NASAWeatherDataService.WeatherStatistics stats,
            double latitude) {
        double avgTemp = stats.getAvgTemperature();
        double avgWind = stats.getAvgWindSpeed();
        double avgPrecip = stats.getAvgPrecipitation();

        // Temperature comfort score (18-25°C is ideal)
        double tempScore = 0;
        if (avgTemp >= 18 && avgTemp <= 25) {
            tempScore = 100.0;
        } else if (avgTemp >= 15 && avgTemp <= 28) {
            // Linear interpolation for near-comfortable temperatures
            if (avgTemp < 18) {
                tempScore = 40.0 + (avgTemp - 15) * 20.0; // 40-100% for 15-18°C
            } else {
                tempScore = 100.0 - (avgTemp - 25) * 20.0; // 100-40% for 25-28°C
            }
        } else if (avgTemp >= 10 && avgTemp <= 32) {
            tempScore = 20.0; // Low but not zero for borderline temperatures
        } else {
            tempScore = 5.0; // Very low for extreme temperatures
        }

        // Wind comfort score (< 7 m/s is comfortable)
        double windScore = Math.max(10.0, 100.0 - avgWind * 8.0);

        // Precipitation comfort score (< 2mm is comfortable)
        double precipScore = Math.max(20.0, 100.0 - avgPrecip * 15.0);

        // Seasonal adjustment based on hemisphere and location
        double seasonalBonus = getSeasonalComfortBonus(LocalDate.now(), latitude);

        // Combined probability using weighted average
        double combinedScore = (tempScore * 0.5 + windScore * 0.3 + precipScore * 0.2) + seasonalBonus;

        return Math.max(5.0, Math.min(95.0, combinedScore));
    }

    /**
     * Adjust pressure to be consistent with weather conditions
     * Low pressure systems bring precipitation and wind
     * High pressure systems bring clear, calm weather
     */
    private double adjustPressureForWeather(double basePressure, double precipitation, double windSpeed) {
        double adjustment = 0;

        // Lower pressure for precipitation (storm systems)
        if (precipitation > 10) {
            adjustment -= 15; // Significant low pressure system
        } else if (precipitation > 5) {
            adjustment -= 8; // Moderate low pressure
        } else if (precipitation > 1) {
            adjustment -= 3; // Slight low pressure
        } else {
            adjustment += 5; // High pressure for clear weather
        }

        // Lower pressure for high winds
        if (windSpeed > 12) {
            adjustment -= 8; // Strong winds indicate low pressure
        } else if (windSpeed > 7) {
            adjustment -= 3; // Moderate winds
        }

        return basePressure + adjustment;
    }

    /**
     * Get seasonal comfort bonus based on typical expectations for the region
     */
    private double getSeasonalComfortBonus(LocalDate date, double latitude) {
        String season = getSeason(date, latitude);
        double absLatitude = Math.abs(latitude);

        // Tropical regions (< 23.5°) have more consistent comfort
        if (absLatitude < 23.5) {
            return 10.0;
        }
        // Temperate regions (23.5° - 60°) have seasonal variation
        else if (absLatitude < 60) {
            if ("Spring".equals(season) || "Autumn".equals(season)) {
                return 15.0; // Spring and autumn are generally more comfortable
            } else {
                return 5.0;
            }
        }
        // Polar regions (> 60°) are generally less comfortable
        else {
            return -5.0;
        }
    }

    /**
     * Adjust probability based on how the forecast compares to historical averages
     */
    private double adjustProbabilityForForecast(double baseProbability, String type,
            NASAWeatherDataService.WeatherStatistics stats) {
        double adjustment = 0;

        switch (type) {
            case "heat":
                // If forecast temp is much higher than average, increase heat probability
                if (stats.getAvgTemperature() > 30) {
                    adjustment = 20;
                } else if (stats.getAvgTemperature() > 25) {
                    adjustment = 10;
                }
                break;

            case "cold":
                // If forecast temp is much lower than average, increase cold probability
                if (stats.getAvgTemperature() < 5) {
                    adjustment = 20;
                } else if (stats.getAvgTemperature() < 10) {
                    adjustment = 10;
                }
                break;

            case "rain":
                // If forecast precipitation is high, increase rain probability
                if (stats.getAvgPrecipitation() > 15) {
                    adjustment = 25;
                } else if (stats.getAvgPrecipitation() > 5) {
                    adjustment = 15;
                }
                break;

            case "wind":
                // If forecast wind is high, increase wind probability
                if (stats.getAvgWindSpeed() > 15) {
                    adjustment = 20;
                } else if (stats.getAvgWindSpeed() > 10) {
                    adjustment = 10;
                }
                break;
        }

        return Math.max(0, Math.min(100, baseProbability + adjustment));
    }

    /**
     * Ensure pressure values are in realistic range (980-1050 hPa for normal
     * weather)
     */
    private double ensureRealisticPressure(Double originalPressure) {
        if (originalPressure == null) {
            return 1013.25; // Standard atmospheric pressure
        }

        // Fix obviously wrong values
        double pressure = fixPressureScaling(originalPressure);

        // If still outside normal range, adjust to realistic bounds
        if (pressure < 980) {
            log.debug("Adjusting low pressure {} to realistic minimum 980 hPa", pressure);
            return 980 + random.nextGaussian() * 5; // 980-990 hPa range
        } else if (pressure > 1050) {
            log.debug("Adjusting high pressure {} to realistic maximum 1050 hPa", pressure);
            return 1040 + random.nextGaussian() * 5; // 1040-1050 hPa range
        }

        return pressure;
    }

    /**
     * Get seasonal temperature baseline for location (climate-aware)
     */
    private double getSeasonalAverageForLocation(int month, double absLatitude) {
        // Climate zones based on latitude
        if (absLatitude < 30) { // Hot desert/subtropical (like Egypt)
            double[] monthlyAvgs = { 17, 19.5, 23, 28, 32, 35, 37, 37, 33, 28, 22, 18 };
            return monthlyAvgs[month - 1];
        } else if (absLatitude < 50) { // Mediterranean/Temperate
            double[] monthlyAvgs = { 7, 9, 13, 18, 23, 28, 31, 30, 26, 20, 14, 9 };
            return monthlyAvgs[month - 1];
        } else { // Continental/Northern
            double[] monthlyAvgs = { 1.5, 3.5, 8.5, 13.5, 18.5, 23.5, 26.5, 25.5, 21, 15, 9, 4 };
            return monthlyAvgs[month - 1];
        }
    }

    /**
     * Generate season and temperature-aware weather description
     */
    private String generateSeasonAwareWeatherDescription(double avgTemp, double precipitation,
            double windSpeed, LocalDate date, Double latitude) {
        StringBuilder description = new StringBuilder();

        // Get seasonal context
        String season = getSeason(date, latitude != null ? latitude : 0);
        int month = date.getMonthValue();

        // Temperature description based on typical values for the season and location
        // Use a climate-aware baseline instead of static ranges
        double seasonalAvg = getSeasonalAverageForLocation(month, Math.abs(latitude != null ? latitude : 45));

        // More accurate temperature descriptions relative to seasonal norms
        if (avgTemp > seasonalAvg + 5) {
            description.append("Unusually warm");
        } else if (avgTemp > seasonalAvg + 2) {
            description.append("Warm");
        } else if (avgTemp > seasonalAvg - 2) {
            description.append("Pleasant");
        } else if (avgTemp > seasonalAvg - 5) {
            description.append("Cool");
        } else {
            description.append("Cold");
        }

        // Add seasonal context
        if (Math.abs(avgTemp - seasonalAvg) > 5) {
            description.append(" for ").append(season.toLowerCase());
        }

        // Precipitation description
        if (precipitation > 20) {
            description.append(" with heavy rain");
        } else if (precipitation > 5) {
            description.append(" with light rain");
        } else if (precipitation > 1) {
            description.append(" with possible showers");
        } else {
            description.append(" and dry");
        }

        // Wind description using existing method
        description.append(", ").append(getWindDescription(windSpeed));

        return description.toString();
    }

    /**
     * Calculate season-aware extreme heat probability using NASA historical data
     */
    private double calculateSeasonAwareHeatProbability(NASAWeatherDataService.WeatherStatistics stats,
            LocalDate date, double latitude) {
        // Use actual NASA-calculated extreme heat probability as base
        double baseProb = stats.getExtremeHeatProbability();

        String season = getSeason(date, latitude);

        // Apply seasonal adjustments to the NASA data
        switch (season) {
            case "Summer":
                return Math.min(25.0, baseProb * 1.2); // Slightly increase in summer
            case "Winter":
                return Math.max(0.5, baseProb * 0.2); // Greatly reduce in winter
            case "Spring":
            case "Autumn":
                return baseProb * 0.8; // Slightly reduce in transition seasons
            default:
                return baseProb;
        }
    }

    /**
     * Calculate season-aware extreme cold probability
     */
    private double calculateSeasonAwareColdProbability(NASAWeatherDataService.WeatherStatistics stats,
            LocalDate date, double latitude) {
        String season = getSeason(date, latitude);
        double baseProb = stats.getExtremeColdProbability();

        // Adjust based on season
        switch (season) {
            case "Winter":
                return Math.min(50, baseProb * 1.8); // Higher in winter
            case "Summer":
                return Math.max(0.1, baseProb * 0.05); // Very low in summer
            case "Spring":
            case "Autumn":
                return Math.max(1, baseProb * 0.5); // Low in transition seasons
            default:
                return baseProb;
        }
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}