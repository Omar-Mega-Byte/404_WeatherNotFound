package com.weather_found.weather_app.modules.location.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import java.time.LocalDate;

/**
 * DTO for creating a new location
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLocationRequest {

    @NotBlank(message = "Location name is required")
    @Size(max = 255, message = "Location name must not exceed 255 characters")
    private String name;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Size(max = 100, message = "Country name must not exceed 100 characters")
    private String country;

    @Size(max = 100, message = "State name must not exceed 100 characters")
    private String state;

    @Size(max = 100, message = "City name must not exceed 100 characters")
    private String city;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    /**
     * Elevation in meters. Optional — validation on range is handled when provided
     */
    private Integer elevation;

    @NotNull(message = "Begin date is required")
    private LocalDate beginDate;

    private LocalDate endDate;
}