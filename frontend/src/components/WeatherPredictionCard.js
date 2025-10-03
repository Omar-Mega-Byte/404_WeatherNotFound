import React from 'react';
import './WeatherPredictionCard.css';

const WeatherPredictionCard = ({ prediction }) => {
  if (!prediction) return null;

  const { 
    latitude, 
    longitude, 
    country, 
    state, 
    city, 
    predictionDate,
    forecast, 
    probabilities, 
    historicalContext, 
    generatedAt, 
    dataSource, 
    confidenceLevel 
  } = prediction;

  // Format date for display
  const formatDate = (dateArray) => {
    if (!Array.isArray(dateArray) || dateArray.length < 3) return 'N/A';
    const [year, month, day] = dateArray;
    return new Date(year, month - 1, day).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  // Format temperature with unit
  const formatTemp = (temp) => `${temp.toFixed(1)}°C`;

  // Get weather icon based on conditions
  const getWeatherIcon = (skyCondition) => {
    if (skyCondition === 'Clear') return '☀️';
    if (skyCondition === 'Partly Cloudy') return '⛅';
    if (skyCondition === 'Cloudy' || skyCondition === 'Overcast') return '☁️';
    return '🌤️';
  };

  // Get probability color based on percentage
  const getProbabilityColor = (probability) => {
    if (probability >= 70) return '#ef4444'; // red-500
    if (probability >= 40) return '#f97316'; // orange-500
    if (probability >= 20) return '#eab308'; // yellow-500
    return '#84cc16'; // light green-500
  };

  // Get comfort level description
  const getComfortLevel = (probability) => {
    if (probability >= 80) return 'Excellent';
    if (probability >= 60) return 'Good';
    if (probability >= 40) return 'Fair';
    return 'Poor';
  };

  const generateWeatherDescription = (forecast) => {
    const { temperatureAvg, precipitation, windSpeed } = forecast;
    let tempDesc;
    if (temperatureAvg < 5) tempDesc = 'Very cold';
    else if (temperatureAvg < 15) tempDesc = 'Cold';
    else if (temperatureAvg < 25) tempDesc = 'Warm';
    else tempDesc = 'Hot';

    const precipDesc = precipitation < 1 ? 'dry' : 'wet';
    
    let windDesc;
    if (windSpeed < 1.5) windDesc = 'calm winds';
    else if (windSpeed < 5.5) windDesc = 'light winds';
    else if (windSpeed < 10.8) windDesc = 'moderate winds';
    else windDesc = 'strong winds';

    return `${tempDesc} and ${precipDesc}, with ${windDesc}.`;
  };

  const handleDownloadJSON = () => {
    const jsonString = JSON.stringify(prediction, null, 2);
    const blob = new Blob([jsonString], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `weather_prediction_${prediction.latitude}_${prediction.longitude}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  const handleDownloadCSV = () => {
    const { forecast, probabilities, historicalContext, ...rest } = prediction;
    
    // Flatten the object
    const flatData = {
      ...rest,
      ...Object.keys(forecast).reduce((acc, key) => ({ ...acc, [`forecast_${key}`]: forecast[key] }), {}),
      ...Object.keys(probabilities).reduce((acc, key) => ({ ...acc, [`prob_${key}`]: probabilities[key] }), {}),
      ...Object.keys(historicalContext).reduce((acc, key) => ({ ...acc, [`hist_${key}`]: historicalContext[key] }), {}),
    };

    // Create CSV content
    const header = Object.keys(flatData).join(',');
    const row = Object.values(flatData).map(val => {
      if (Array.isArray(val)) {
        return `"${val.join('; ')}"`;
      }
      if (typeof val === 'string' && val.includes(',')) {
        return `"${val}"`;
      }
      return val;
    }).join(',');

    const csvString = `${header}\n${row}`;
    const blob = new Blob([csvString], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `weather_prediction_${prediction.latitude}_${prediction.longitude}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  return (
    <div className="weather-prediction-card">
      {/* Header */}
      <div className="prediction-header">
        <div className="location-info">
          <h3 className="location-name">
            <span role="img" aria-label="weather-icon" style={{fontSize: '2rem'}}>
              {getWeatherIcon(forecast.skyCondition)}
            </span>
            Location {latitude.toFixed(3)},{longitude.toFixed(3)}
          </h3>
          <p className="location-details">
            {city && `${city}, `}{state && `${state}, `}{country}
          </p>
          <p className="coordinates">
            📍 {latitude.toFixed(3)}°, {longitude.toFixed(3)}°
          </p>
        </div>
        <div className="prediction-date">
          <h4>{formatDate(predictionDate)}</h4>
          <p className="confidence">Confidence: {confidenceLevel}</p>
        </div>
      </div>

      {/* Main Weather Forecast */}
      <div className="forecast-main">
        <div className="temperature-range">
          <div className="temp-item">
            <span className="temp-label">Min</span>
            <span className="temp-value">{formatTemp(forecast.temperatureMin)}</span>
          </div>
          <div className="temp-item main-temp">
            <span className="temp-label">Avg</span>
            <span className="temp-value main">{formatTemp(forecast.temperatureAvg)}</span>
          </div>
          <div className="temp-item">
            <span className="temp-label">Max</span>
            <span className="temp-value">{formatTemp(forecast.temperatureMax)}</span>
          </div>
        </div>

        <div className="weather-details">
          <div className="detail-item">
            <span className="detail-icon">💧</span>
            <span className="detail-label">Humidity</span>
            <span className="detail-value">{forecast.humidity}%</span>
          </div>
          <div className="detail-item">
            <span className="detail-icon">💨</span>
            <span className="detail-label">Wind Speed</span>
            <span className="detail-value">{forecast.windSpeed} m/s</span>
          </div>
          <div className="detail-item">
            <span className="detail-icon">🧭</span>
            <span className="detail-label">Wind Direction</span>
            <span className="detail-value">{forecast.windDirection}°</span>
          </div>
          <div className="detail-item">
            <span className="detail-icon">🌤️</span>
            <span className="detail-label">Sky</span>
            <span className="detail-value">{forecast.skyCondition}</span>
          </div>
        </div>

        <div className="weather-description">
          <p>{generateWeatherDescription(forecast)}</p>
        </div>
      </div>

      {/* Weather Probabilities */}
      <div className="probabilities-section">
        <h4>Weather Probabilities</h4>
        <div className="probabilities-grid">
          <div className="probability-item">
            <div className="prob-header">
              <span className="prob-icon">🔥</span>
              <span className="prob-label">Extreme Heat</span>
            </div>
            <div className="prob-bar">
              <div 
                className="prob-fill" 
                style={{ 
                  width: `${probabilities.extremeHeatProbability}%`,
                  backgroundColor: getProbabilityColor(probabilities.extremeHeatProbability)
                }}
              ></div>
            </div>
            <span className="prob-value">{probabilities.extremeHeatProbability}%</span>
          </div>

          <div className="probability-item">
            <div className="prob-header">
              <span className="prob-icon">❄️</span>
              <span className="prob-label">Extreme Cold</span>
            </div>
            <div className="prob-bar">
              <div 
                className="prob-fill" 
                style={{ 
                  width: `${probabilities.extremeColdProbability}%`,
                  backgroundColor: getProbabilityColor(probabilities.extremeColdProbability)
                }}
              ></div>
            </div>
            <span className="prob-value">{probabilities.extremeColdProbability}%</span>
          </div>

          <div className="probability-item">
            <div className="prob-header">
              <span className="prob-icon">💧</span>
              <span className="prob-label">Heavy Rain</span>
            </div>
            <div className="prob-bar">
              <div 
                className="prob-fill" 
                style={{ 
                  width: `${probabilities.heavyRainProbability}%`,
                  backgroundColor: getProbabilityColor(probabilities.heavyRainProbability)
                }}
              ></div>
            </div>
            <span className="prob-value">{probabilities.heavyRainProbability}%</span>
          </div>

          <div className="probability-item">
            <div className="prob-header">
              <span className="prob-icon">🌬️</span>
              <span className="prob-label">High Wind</span>
            </div>
            <div className="prob-bar">
              <div 
                className="prob-fill" 
                style={{ 
                  width: `${probabilities.highWindProbability}%`,
                  backgroundColor: getProbabilityColor(probabilities.highWindProbability)
                }}
              ></div>
            </div>
            <span className="prob-value">{probabilities.highWindProbability}%</span>
          </div>

          <div className="probability-item">
            <div className="prob-header">
              <span className="prob-icon">⛈️</span>
              <span className="prob-label">Storm</span>
            </div>
            <div className="prob-bar">
              <div 
                className="prob-fill" 
                style={{ 
                  width: `${probabilities.stormProbability}%`,
                  backgroundColor: getProbabilityColor(probabilities.stormProbability)
                }}
              ></div>
            </div>
            <span className="prob-value">{probabilities.stormProbability}%</span>
          </div>

          <div className="probability-item comfort">
            <div className="prob-header">
              <span className="prob-icon">😊</span>
              <span className="prob-label">Comfortable Weather</span>
            </div>
            <div className="prob-bar">
              <div 
                className="prob-fill" 
                style={{ 
                  width: `${probabilities.comfortableWeatherProbability}%`,
                  backgroundColor: '#22c55e' // green-500
                }}
              ></div>
            </div>
            <span className="prob-value comfort-value">
              {probabilities.comfortableWeatherProbability}% - {getComfortLevel(probabilities.comfortableWeatherProbability)}
            </span>
          </div>
        </div>
      </div>

      {/* Historical Context */}
      <div className="historical-section">
        <h4>Historical Context</h4>
        <div className="historical-grid">
          <div className="historical-item">
            <span className="hist-label">Years of Data</span>
            <span className="hist-value">{historicalContext.yearsOfData} years</span>
          </div>
          <div className="historical-item">
            <span className="hist-label">Historical Avg Temp</span>
            <span className="hist-value">{formatTemp(historicalContext.historicalAvgTemp)}</span>
          </div>
          <div className="historical-item">
            <span className="hist-label">Historical Avg Precipitation</span>
            <span className="hist-value">{historicalContext.historicalAvgPrecipitation} mm</span>
          </div>
          <div className="historical-item">
            <span className="hist-label">Climate Trend</span>
            <span className="hist-value trend">{historicalContext.climateTrend}</span>
          </div>
        </div>
      </div>

      {/* Footer */}
      <div className="prediction-footer">
        <div className="data-source">
          <span className="footer-label">Data Source:</span>
          <span className="footer-value">{dataSource}</span>
        </div>
        <div className="download-buttons">
          <button onClick={handleDownloadJSON} className="download-btn json-btn">Download JSON</button>
          <button onClick={handleDownloadCSV} className="download-btn csv-btn">Download CSV</button>
        </div>
        <div className="generated-at">
          <span className="footer-label">Generated:</span>
          <span className="footer-value">
            {new Date(generatedAt[0], generatedAt[1] - 1, generatedAt[2], 
                     generatedAt[3], generatedAt[4], generatedAt[5]).toLocaleString()}
          </span>
        </div>
      </div>
    </div>
  );
};

export default WeatherPredictionCard;
