package com.weather_found.weather_app.modules.prediction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for weather prediction requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherPredictionRequest {

    private Long id;

    @NotNull(message = "Location name is required")
    private String name;

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    private String country;
    private String state;
    private String city;
    private String address;
    private String timezone;
    private Integer elevation;

    @NotNull(message = "Begin date is required")
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private int[] beginDate; // [year, month, day]

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private int[] endDate; // [year, month, day] - optional

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private long[] createdAt; // [year, month, day, hour, minute, second, nanosecond] - optional

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    private long[] updatedAt; // [year, month, day, hour, minute, second, nanosecond] - optional

    // Helper methods to convert arrays to LocalDate/LocalDateTime
    public LocalDate getBeginDateAsLocalDate() {
        if (beginDate != null && beginDate.length >= 3) {
            return LocalDate.of(beginDate[0], beginDate[1], beginDate[2]);
        }
        return null;
    }

    public LocalDate getEndDateAsLocalDate() {
        if (endDate != null && endDate.length >= 3) {
            return LocalDate.of(endDate[0], endDate[1], endDate[2]);
        }
        return null;
    }

    public LocalDateTime getCreatedAtAsLocalDateTime() {
        if (createdAt != null && createdAt.length >= 7) {
            return LocalDateTime.of(
                    (int) createdAt[0], (int) createdAt[1], (int) createdAt[2],
                    (int) createdAt[3], (int) createdAt[4], (int) createdAt[5],
                    (int) createdAt[6]);
        }
        return null;
    }

    public LocalDateTime getUpdatedAtAsLocalDateTime() {
        if (updatedAt != null && updatedAt.length >= 7) {
            return LocalDateTime.of(
                    (int) updatedAt[0], (int) updatedAt[1], (int) updatedAt[2],
                    (int) updatedAt[3], (int) updatedAt[4], (int) updatedAt[5],
                    (int) updatedAt[6]);
        }
        return null;
    }
}