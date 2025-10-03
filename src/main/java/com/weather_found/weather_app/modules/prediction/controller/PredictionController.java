package com.weather_found.weather_app.modules.prediction.controller;

import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionRequest;
import com.weather_found.weather_app.modules.prediction.dto.WeatherPredictionResponse;
import com.weather_found.weather_app.modules.prediction.service.WeatherPredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for weather prediction management
 */
@RestController
@RequestMapping("/api/predictions")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Weather Predictions", description = "NASA data-powered weather prediction endpoints")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PredictionController {

    private final WeatherPredictionService weatherPredictionService;

    /**
     * Get weather prediction for location and date
     */
    @PostMapping("/forecast")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get weather prediction", description = "Get statistical weather prediction based on NASA historical data for specific location and future date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weather prediction generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WeatherPredictionResponse> getWeatherPrediction(
            @Valid @RequestBody @Parameter(description = "Location and date information for weather prediction") WeatherPredictionRequest predictionRequest,
            Authentication authentication) {

        try {
            log.info("Weather prediction requested by user: {} for location: {}",
                    authentication.getName(), predictionRequest.getName());

            WeatherPredictionResponse prediction = weatherPredictionService
                    .generateWeatherPrediction(predictionRequest);

            return ResponseEntity.ok(prediction);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid prediction request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating weather prediction: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get prediction for specific event
     */
    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get event prediction", description = "Get weather prediction for a specific event")
    public ResponseEntity<String> getEventPrediction(@PathVariable Long eventId, Authentication authentication) {
        // TODO: Implement event prediction logic
        return ResponseEntity.ok("{}");
    }

    /**
     * Get prediction history for user
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get prediction history", description = "Get user's weather prediction history")
    public ResponseEntity<String> getPredictionHistory(Authentication authentication) {
        // TODO: Implement prediction history logic
        return ResponseEntity.ok("{}");
    }

    /**
     * Get prediction accuracy metrics - Admin only
     */
    @GetMapping("/accuracy")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get prediction accuracy", description = "Get prediction accuracy metrics (Admin only)")
    public ResponseEntity<String> getPredictionAccuracy(Authentication authentication) {
        // TODO: Implement prediction accuracy logic
        return ResponseEntity.ok("{}");
    }

    /**
     * Retrain ML models - Admin only
     */
    @PostMapping("/retrain")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Retrain ML models", description = "Trigger ML model retraining (Admin only)")
    public ResponseEntity<String> retrainModels(Authentication authentication) {
        // TODO: Implement ML model retraining logic
        return ResponseEntity.ok("{}");
    }
}
