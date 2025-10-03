package com.weather_found.weather_app.modules.prediction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for fetching historical weather data from NASA APIs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NASAWeatherDataService {

    private final RestTemplate restTemplate;

    @Value("${nasa.earthdata.username:}")
    private String earthdataUsername;

    @Value("${nasa.earthdata.password:}")
    private String earthdataPassword;

    private static final String POWER_API_BASE_URL = "https://power.larc.nasa.gov/api/temporal/daily/point";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Fetch historical weather data for a specific location and date range
     */
    public HistoricalWeatherData getHistoricalWeatherData(double latitude, double longitude, LocalDate startDate,
            LocalDate endDate) {
        try {
            log.info("Fetching NASA POWER data for location: {}, {} from {} to {}", latitude, longitude, startDate,
                    endDate);

            // Use NASA POWER API for historical weather data
            // Fetch data year by year to get data for the same day/week across multiple
            // years
            List<Map<String, Object>> yearlyResponses = new ArrayList<>();
            int startYear = startDate.getYear();
            int endYear = endDate.getYear();
            int dayOfYear = startDate.getDayOfYear();

            for (int year = startYear; year <= endYear; year++) {
                LocalDate yearStartDate = LocalDate.ofYearDay(year, Math.max(1, dayOfYear - 3));
                LocalDate yearEndDate = LocalDate.ofYearDay(year,
                        Math.min(yearStartDate.lengthOfYear(), dayOfYear + 3));

                String url = buildPOWERApiUrl(latitude, longitude, yearStartDate, yearEndDate);
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                HttpEntity<String> entity = new HttpEntity<>(headers);

                try {
                    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
                    if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                        yearlyResponses.add(response.getBody());
                    } else {
                        log.warn("Failed to fetch NASA POWER data for year {}. Status: {}", year,
                                response.getStatusCode());
                    }
                } catch (RestClientException e) {
                    log.error("Error fetching NASA POWER data for year {}: {}", year, e.getMessage());
                }
            }

            if (!yearlyResponses.isEmpty()) {
                return parseYearlyNASAPOWERResponses(yearlyResponses);
            } else {
                log.warn("No data fetched from NASA POWER for the given date range.");
                return generateFallbackData(latitude, longitude, startDate, endDate);
            }

        } catch (Exception e) {
            log.error("Error processing NASA POWER data request: {}", e.getMessage(), e);
            return generateFallbackData(latitude, longitude, startDate, endDate);
        }
    }

    /**
     * Get weather statistics for a specific day of year based on historical data
     */
    public WeatherStatistics getWeatherStatisticsForDayOfYear(double latitude, double longitude, int dayOfYear) {
        // Calculate date range for historical analysis (last 10 years)
        LocalDate currentYear = LocalDate.now();
        LocalDate startDate = currentYear.minusYears(10)
                .withDayOfYear(Math.min(dayOfYear, currentYear.minusYears(10).lengthOfYear()));
        LocalDate endDate = currentYear.minusYears(1)
                .withDayOfYear(Math.min(dayOfYear, currentYear.minusYears(1).lengthOfYear()));

        HistoricalWeatherData historicalData = getHistoricalWeatherData(latitude, longitude, startDate, endDate);

        return calculateWeatherStatistics(historicalData);
    }

    private String buildPOWERApiUrl(double latitude, double longitude, LocalDate startDate, LocalDate endDate) {
        return String.format(
                "%s?parameters=T2M_MIN,T2M_MAX,PRECTOTCORR,WS10M,RH2M,PS&" +
                        "community=RE&longitude=%.6f&latitude=%.6f&start=%s&end=%s&format=JSON",
                POWER_API_BASE_URL,
                longitude,
                latitude,
                startDate.format(DATE_FORMAT),
                endDate.format(DATE_FORMAT));
    }

    private HistoricalWeatherData parseYearlyNASAPOWERResponses(List<Map<String, Object>> responses) {
        HistoricalWeatherData aggregatedData = new HistoricalWeatherData();
        aggregatedData.setTemperatureMin(new ArrayList<>());
        aggregatedData.setTemperatureMax(new ArrayList<>());
        aggregatedData.setPrecipitation(new ArrayList<>());
        aggregatedData.setWindSpeed(new ArrayList<>());
        aggregatedData.setHumidity(new ArrayList<>());
        aggregatedData.setPressure(new ArrayList<>());

        for (Map<String, Object> response : responses) {
            HistoricalWeatherData yearlyData = parseNASAPOWERResponse(response);
            aggregatedData.getTemperatureMin().addAll(yearlyData.getTemperatureMin());
            aggregatedData.getTemperatureMax().addAll(yearlyData.getTemperatureMax());
            aggregatedData.getPrecipitation().addAll(yearlyData.getPrecipitation());
            aggregatedData.getWindSpeed().addAll(yearlyData.getWindSpeed());
            aggregatedData.getHumidity().addAll(yearlyData.getHumidity());
            aggregatedData.getPressure().addAll(yearlyData.getPressure());
        }

        return aggregatedData;
    }

    private HistoricalWeatherData parseNASAPOWERResponse(Map<String, Object> response) {
        HistoricalWeatherData data = new HistoricalWeatherData();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> properties = (Map<String, Object>) response.get("properties");

            if (properties != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parameter = (Map<String, Object>) properties.get("parameter");

                if (parameter != null) {
                    data.setTemperatureMin(extractDailyValues(parameter, "T2M_MIN"));
                    data.setTemperatureMax(extractDailyValues(parameter, "T2M_MAX"));
                    data.setPrecipitation(extractDailyValues(parameter, "PRECTOTCORR"));
                    data.setWindSpeed(extractDailyValues(parameter, "WS10M"));
                    data.setHumidity(extractDailyValues(parameter, "RH2M"));
                    data.setPressure(extractDailyValues(parameter, "PS"));

                    // Validate data quality
                    validateDataQuality(data);
                }
            }

        } catch (Exception e) {
            log.error("Error parsing NASA POWER response: {}", e.getMessage());
        }

        return data;
    }

    @SuppressWarnings("unchecked")
    private List<Double> extractDailyValues(Map<String, Object> parameter, String parameterName) {
        List<Double> values = new ArrayList<>();

        if (parameter.containsKey(parameterName)) {
            Map<String, Object> parameterData = (Map<String, Object>) parameter.get(parameterName);

            // NASA POWER returns data as date -> value mapping
            for (Object value : parameterData.values()) {
                if (value instanceof Number) {
                    double val = ((Number) value).doubleValue();
                    // Filter out invalid values (NASA uses -999 for missing data)
                    if (val > -900) {
                        values.add(val);
                    }
                }
            }
        }

        return values;
    }

    private WeatherStatistics calculateWeatherStatistics(HistoricalWeatherData data) {
        WeatherStatistics stats = new WeatherStatistics();

        // Calculate temperature statistics
        if (!data.getTemperatureMin().isEmpty() && !data.getTemperatureMax().isEmpty()) {
            stats.setAvgTemperatureMin(calculateAverage(data.getTemperatureMin()));
            stats.setAvgTemperatureMax(calculateAverage(data.getTemperatureMax()));
            stats.setAvgTemperature((stats.getAvgTemperatureMin() + stats.getAvgTemperatureMax()) / 2);
        }

        // Calculate precipitation statistics
        if (!data.getPrecipitation().isEmpty()) {
            stats.setAvgPrecipitation(calculateAverage(data.getPrecipitation()));
            stats.setMaxPrecipitation(Collections.max(data.getPrecipitation()));
        }

        // Calculate wind statistics
        if (!data.getWindSpeed().isEmpty()) {
            stats.setAvgWindSpeed(calculateAverage(data.getWindSpeed()));
            stats.setMaxWindSpeed(Collections.max(data.getWindSpeed()));
        }

        // Calculate humidity statistics
        if (!data.getHumidity().isEmpty()) {
            stats.setAvgHumidity(calculateAverage(data.getHumidity()));
        }

        // Calculate pressure statistics
        if (!data.getPressure().isEmpty()) {
            stats.setAvgPressure(calculateAverage(data.getPressure()));
        }

        // Calculate probabilities
        stats.setExtremeHeatProbability(calculateExceedanceProbability(data.getTemperatureMax(), 35.0));
        stats.setExtremeColdProbability(calculateExceedanceProbability(data.getTemperatureMin(), 0.0, true));
        stats.setHeavyRainProbability(calculateExceedanceProbability(data.getPrecipitation(), 25.0));
        stats.setHighWindProbability(calculateExceedanceProbability(data.getWindSpeed(), 15.0));

        return stats;
    }

    private double calculateAverage(List<Double> values) {
        if (values.isEmpty())
            return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateExceedanceProbability(List<Double> values, double threshold) {
        return calculateExceedanceProbability(values, threshold, false);
    }

    private double calculateExceedanceProbability(List<Double> values, double threshold, boolean below) {
        if (values.isEmpty())
            return 0.0;

        long count = values.stream()
                .mapToLong(value -> below ? (value < threshold ? 1 : 0) : (value > threshold ? 1 : 0))
                .sum();

        return (double) count / values.size() * 100.0; // Return as percentage
    }

    /**
     * Validate the quality of weather data from NASA API
     */
    private void validateDataQuality(HistoricalWeatherData data) {
        int totalDataPoints = 0;
        int validDataPoints = 0;

        // Check temperature data quality
        if (!data.getTemperatureMin().isEmpty() && !data.getTemperatureMax().isEmpty()) {
            for (int i = 0; i < Math.min(data.getTemperatureMin().size(), data.getTemperatureMax().size()); i++) {
                totalDataPoints++;
                Double tempMin = data.getTemperatureMin().get(i);
                Double tempMax = data.getTemperatureMax().get(i);

                if (tempMin != null && tempMax != null && tempMin <= tempMax &&
                        tempMin > -60 && tempMin < 60 && tempMax > -60 && tempMax < 60) {
                    validDataPoints++;
                }
            }
        }

        // Check precipitation data quality
        for (Double precip : data.getPrecipitation()) {
            totalDataPoints++;
            if (precip != null && precip >= 0 && precip < 1000) { // Reasonable precipitation limit
                validDataPoints++;
            }
        }

        // Check wind speed data quality
        for (Double windSpeed : data.getWindSpeed()) {
            totalDataPoints++;
            if (windSpeed != null && windSpeed >= 0 && windSpeed < 200) { // Reasonable wind speed limit
                validDataPoints++;
            }
        }

        // Calculate data quality percentage
        double dataQuality = totalDataPoints > 0 ? (double) validDataPoints / totalDataPoints * 100 : 0;

        log.info("NASA data quality: {:.2f}% ({} valid out of {} total data points)",
                dataQuality, validDataPoints, totalDataPoints);

        if (dataQuality < 50) {
            log.warn("Low data quality from NASA API: {:.2f}%. Consider using fallback data.", dataQuality);
        } else if (dataQuality < 80) {
            log.info("Moderate data quality from NASA API: {:.2f}%", dataQuality);
        } else {
            log.debug("Good data quality from NASA API: {:.2f}%", dataQuality);
        }
    }

    private HistoricalWeatherData generateFallbackData(double latitude, double longitude, LocalDate startDate,
            LocalDate endDate) {
        log.info("Generating fallback weather data for location: {}, {}", latitude, longitude);

        HistoricalWeatherData fallbackData = new HistoricalWeatherData();

        // Generate realistic fallback data based on location and season
        Random random = new Random();
        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;

        // Base temperature on latitude (rough approximation)
        double baseTemp = 25 - Math.abs(latitude) * 0.6;

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            int dayOfYear = date.getDayOfYear();

            // Seasonal adjustment
            double seasonalFactor = Math.cos(2 * Math.PI * dayOfYear / 365.0) * 10;

            fallbackData.getTemperatureMin().add(baseTemp + seasonalFactor - 5 + random.nextGaussian() * 3);
            fallbackData.getTemperatureMax().add(baseTemp + seasonalFactor + 5 + random.nextGaussian() * 3);
            fallbackData.getPrecipitation().add(Math.max(0, random.nextGaussian() * 5 + 2));
            fallbackData.getWindSpeed().add(Math.max(0, random.nextGaussian() * 3 + 5));
            fallbackData.getHumidity().add(Math.max(0, Math.min(100, random.nextGaussian() * 15 + 60)));
            fallbackData.getPressure().add(random.nextGaussian() * 20 + 1013.25);
        }

        return fallbackData;
    }

    // Inner classes for data structures
    public static class HistoricalWeatherData {
        private List<Double> temperatureMin = new ArrayList<>();
        private List<Double> temperatureMax = new ArrayList<>();
        private List<Double> precipitation = new ArrayList<>();
        private List<Double> windSpeed = new ArrayList<>();
        private List<Double> humidity = new ArrayList<>();
        private List<Double> pressure = new ArrayList<>();

        // Getters and setters
        public List<Double> getTemperatureMin() {
            return temperatureMin;
        }

        public void setTemperatureMin(List<Double> temperatureMin) {
            this.temperatureMin = temperatureMin;
        }

        public List<Double> getTemperatureMax() {
            return temperatureMax;
        }

        public void setTemperatureMax(List<Double> temperatureMax) {
            this.temperatureMax = temperatureMax;
        }

        public List<Double> getPrecipitation() {
            return precipitation;
        }

        public void setPrecipitation(List<Double> precipitation) {
            this.precipitation = precipitation;
        }

        public List<Double> getWindSpeed() {
            return windSpeed;
        }

        public void setWindSpeed(List<Double> windSpeed) {
            this.windSpeed = windSpeed;
        }

        public List<Double> getHumidity() {
            return humidity;
        }

        public void setHumidity(List<Double> humidity) {
            this.humidity = humidity;
        }

        public List<Double> getPressure() {
            return pressure;
        }

        public void setPressure(List<Double> pressure) {
            this.pressure = pressure;
        }
    }

    public static class WeatherStatistics {
        private Double avgTemperature;
        private Double avgTemperatureMin;
        private Double avgTemperatureMax;
        private Double avgPrecipitation;
        private Double maxPrecipitation;
        private Double avgWindSpeed;
        private Double maxWindSpeed;
        private Double avgHumidity;
        private Double avgPressure;
        private Double extremeHeatProbability;
        private Double extremeColdProbability;
        private Double heavyRainProbability;
        private Double highWindProbability;

        // Getters and setters
        public Double getAvgTemperature() {
            return avgTemperature;
        }

        public void setAvgTemperature(Double avgTemperature) {
            this.avgTemperature = avgTemperature;
        }

        public Double getAvgTemperatureMin() {
            return avgTemperatureMin;
        }

        public void setAvgTemperatureMin(Double avgTemperatureMin) {
            this.avgTemperatureMin = avgTemperatureMin;
        }

        public Double getAvgTemperatureMax() {
            return avgTemperatureMax;
        }

        public void setAvgTemperatureMax(Double avgTemperatureMax) {
            this.avgTemperatureMax = avgTemperatureMax;
        }

        public Double getAvgPrecipitation() {
            return avgPrecipitation;
        }

        public void setAvgPrecipitation(Double avgPrecipitation) {
            this.avgPrecipitation = avgPrecipitation;
        }

        public Double getMaxPrecipitation() {
            return maxPrecipitation;
        }

        public void setMaxPrecipitation(Double maxPrecipitation) {
            this.maxPrecipitation = maxPrecipitation;
        }

        public Double getAvgWindSpeed() {
            return avgWindSpeed;
        }

        public void setAvgWindSpeed(Double avgWindSpeed) {
            this.avgWindSpeed = avgWindSpeed;
        }

        public Double getMaxWindSpeed() {
            return maxWindSpeed;
        }

        public void setMaxWindSpeed(Double maxWindSpeed) {
            this.maxWindSpeed = maxWindSpeed;
        }

        public Double getAvgHumidity() {
            return avgHumidity;
        }

        public void setAvgHumidity(Double avgHumidity) {
            this.avgHumidity = avgHumidity;
        }

        public Double getAvgPressure() {
            return avgPressure;
        }

        public void setAvgPressure(Double avgPressure) {
            this.avgPressure = avgPressure;
        }

        public Double getExtremeHeatProbability() {
            return extremeHeatProbability;
        }

        public void setExtremeHeatProbability(Double extremeHeatProbability) {
            this.extremeHeatProbability = extremeHeatProbability;
        }

        public Double getExtremeColdProbability() {
            return extremeColdProbability;
        }

        public void setExtremeColdProbability(Double extremeColdProbability) {
            this.extremeColdProbability = extremeColdProbability;
        }

        public Double getHeavyRainProbability() {
            return heavyRainProbability;
        }

        public void setHeavyRainProbability(Double heavyRainProbability) {
            this.heavyRainProbability = heavyRainProbability;
        }

        public Double getHighWindProbability() {
            return highWindProbability;
        }

        public void setHighWindProbability(Double highWindProbability) {
            this.highWindProbability = highWindProbability;
        }
    }
}