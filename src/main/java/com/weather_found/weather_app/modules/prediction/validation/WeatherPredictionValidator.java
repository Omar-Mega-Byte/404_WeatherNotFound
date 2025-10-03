package com.weather_found.weather_app.modules.prediction.validation;

import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionRequest;
import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for weather prediction requests and responses
 */
@Component
@Slf4j
public class WeatherPredictionValidator {

    /**
     * Validate weather prediction request
     */
    public ValidationResult validateRequest(WeatherPredictionRequest request) {
        List<String> errors = new ArrayList<>();

        // Validate basic required fields
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            errors.add("Location name is required");
        }

        if (request.getLatitude() == null) {
            errors.add("Latitude is required");
        } else if (request.getLatitude() < -90.0 || request.getLatitude() > 90.0) {
            errors.add("Latitude must be between -90 and 90 degrees");
        }

        if (request.getLongitude() == null) {
            errors.add("Longitude is required");
        } else if (request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
            errors.add("Longitude must be between -180 and 180 degrees");
        }

        // Validate begin date
        if (request.getBeginDate() == null || request.getBeginDate().length < 3) {
            errors.add("Begin date is required and must be in format [year, month, day]");
        } else {
            try {
                LocalDate beginDate = request.getBeginDateAsLocalDate();
                if (beginDate == null) {
                    errors.add("Invalid begin date format");
                } else {
                    // Check if date is in the future
                    if (beginDate.isBefore(LocalDate.now())) {
                        errors.add("Begin date must be in the future for weather prediction");
                    }

                    // Check if date is not too far in the future (2 year limit)
                    if (beginDate.isAfter(LocalDate.now().plusYears(2))) {
                        errors.add("Begin date cannot be more than 2 years in the future");
                    }
                }
            } catch (Exception e) {
                errors.add("Invalid begin date: " + e.getMessage());
            }
        }

        // Validate end date if provided
        if (request.getEndDate() != null && request.getEndDate().length >= 3) {
            try {
                LocalDate endDate = request.getEndDateAsLocalDate();
                LocalDate beginDate = request.getBeginDateAsLocalDate();

                if (endDate != null && beginDate != null && endDate.isBefore(beginDate)) {
                    errors.add("End date must be after begin date");
                }
            } catch (Exception e) {
                errors.add("Invalid end date: " + e.getMessage());
            }
        }

        // Validate location name length
        if (request.getName() != null && request.getName().length() > 255) {
            errors.add("Location name cannot exceed 255 characters");
        }

        // Validate elevation if provided
        if (request.getElevation() != null && (request.getElevation() < -500 || request.getElevation() > 9000)) {
            errors.add("Elevation must be between -500m and 9000m");
        }

        log.info("Request validation completed. Found {} errors", errors.size());
        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate weather prediction response for consistency and realistic values
     */
    public ValidationResult validateResponse(WeatherPredictionResponse response) {
        List<String> errors = new ArrayList<>();

        if (response == null) {
            errors.add("Response cannot be null");
            return new ValidationResult(false, errors);
        }

        // Validate forecast data
        WeatherPredictionResponse.WeatherForecast forecast = response.getForecast();
        if (forecast != null) {
            validateTemperatures(forecast, errors);
            validatePrecipitation(forecast, errors);
            validateWind(forecast, errors);
            validateHumidity(forecast, errors);
            validatePressure(forecast, errors);
        } else {
            errors.add("Forecast data is missing");
        }

        // Validate probabilities
        WeatherPredictionResponse.WeatherProbabilities probabilities = response.getProbabilities();
        if (probabilities != null) {
            validateProbabilities(probabilities, errors);
        } else {
            errors.add("Probability data is missing");
        }

        // Validate historical context
        WeatherPredictionResponse.HistoricalContext context = response.getHistoricalContext();
        if (context != null) {
            validateHistoricalContext(context, errors);
        }

        log.info("Response validation completed. Found {} errors", errors.size());
        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void validateTemperatures(WeatherPredictionResponse.WeatherForecast forecast, List<String> errors) {
        if (forecast.getTemperatureMin() != null && forecast.getTemperatureMax() != null) {
            if (forecast.getTemperatureMin() > forecast.getTemperatureMax()) {
                errors.add("Minimum temperature cannot be higher than maximum temperature");
            }

            // Check for realistic temperature ranges (-50°C to 60°C)
            if (forecast.getTemperatureMin() < -50 || forecast.getTemperatureMin() > 60) {
                errors.add("Minimum temperature is outside realistic range (-50°C to 60°C)");
            }

            if (forecast.getTemperatureMax() < -50 || forecast.getTemperatureMax() > 60) {
                errors.add("Maximum temperature is outside realistic range (-50°C to 60°C)");
            }
        }

        if (forecast.getTemperatureAvg() != null) {
            if (forecast.getTemperatureAvg() < -50 || forecast.getTemperatureAvg() > 60) {
                errors.add("Average temperature is outside realistic range (-50°C to 60°C)");
            }

            // Check if average is between min and max
            if (forecast.getTemperatureMin() != null && forecast.getTemperatureMax() != null) {
                if (forecast.getTemperatureAvg() < forecast.getTemperatureMin() ||
                        forecast.getTemperatureAvg() > forecast.getTemperatureMax()) {
                    errors.add("Average temperature should be between minimum and maximum temperatures");
                }
            }
        }
    }

    private void validatePrecipitation(WeatherPredictionResponse.WeatherForecast forecast, List<String> errors) {
        if (forecast.getPrecipitation() != null) {
            if (forecast.getPrecipitation() < 0) {
                errors.add("Precipitation cannot be negative");
            }

            if (forecast.getPrecipitation() > 500) {
                errors.add("Precipitation value seems unrealistic (>500mm)");
            }
        }
    }

    private void validateWind(WeatherPredictionResponse.WeatherForecast forecast, List<String> errors) {
        if (forecast.getWindSpeed() != null) {
            if (forecast.getWindSpeed() < 0) {
                errors.add("Wind speed cannot be negative");
            }

            if (forecast.getWindSpeed() > 100) {
                errors.add("Wind speed seems unrealistic (>100 m/s)");
            }
        }

        if (forecast.getWindDirection() != null) {
            if (forecast.getWindDirection() < 0 || forecast.getWindDirection() >= 360) {
                errors.add("Wind direction must be between 0 and 359 degrees");
            }
        }
    }

    private void validateHumidity(WeatherPredictionResponse.WeatherForecast forecast, List<String> errors) {
        if (forecast.getHumidity() != null) {
            if (forecast.getHumidity() < 0 || forecast.getHumidity() > 100) {
                errors.add("Humidity must be between 0 and 100 percent");
            }
        }
    }

    private void validatePressure(WeatherPredictionResponse.WeatherForecast forecast, List<String> errors) {
        if (forecast.getPressure() != null) {
            // Typical atmospheric pressure range: 870-1085 hPa
            if (forecast.getPressure() < 870 || forecast.getPressure() > 1085) {
                errors.add("Atmospheric pressure is outside realistic range (870-1085 hPa)");
            }
        }
    }

    private void validateProbabilities(WeatherPredictionResponse.WeatherProbabilities probabilities,
            List<String> errors) {
        validateProbabilityValue(probabilities.getExtremeHeatProbability(), "Extreme heat probability", errors);
        validateProbabilityValue(probabilities.getExtremeColdProbability(), "Extreme cold probability", errors);
        validateProbabilityValue(probabilities.getHeavyRainProbability(), "Heavy rain probability", errors);
        validateProbabilityValue(probabilities.getHighWindProbability(), "High wind probability", errors);
        validateProbabilityValue(probabilities.getStormProbability(), "Storm probability", errors);
        validateProbabilityValue(probabilities.getComfortableWeatherProbability(), "Comfortable weather probability",
                errors);
    }

    private void validateProbabilityValue(Double probability, String fieldName, List<String> errors) {
        if (probability != null && (probability < 0 || probability > 100)) {
            errors.add(fieldName + " must be between 0 and 100 percent");
        }
    }

    private void validateHistoricalContext(WeatherPredictionResponse.HistoricalContext context, List<String> errors) {
        if (context.getYearsOfData() != null && context.getYearsOfData() <= 0) {
            errors.add("Years of data must be positive");
        }

        if (context.getHistoricalAvgTemp() != null &&
                (context.getHistoricalAvgTemp() < -50 || context.getHistoricalAvgTemp() > 60)) {
            errors.add("Historical average temperature is outside realistic range");
        }

        if (context.getHistoricalAvgPrecipitation() != null && context.getHistoricalAvgPrecipitation() < 0) {
            errors.add("Historical average precipitation cannot be negative");
        }
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        @Override
        public String toString() {
            if (valid) {
                return "Validation passed";
            } else {
                return "Validation failed: " + String.join(", ", errors);
            }
        }
    }
}