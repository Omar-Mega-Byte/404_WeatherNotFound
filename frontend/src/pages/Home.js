import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import weatherService from '../services/weatherService';
import './Home_new.css';

const Home = () => {
  const [weatherData, setWeatherData] = useState(null);
  const [isLoadingWeather, setIsLoadingWeather] = useState(true);

  // Fetch real weather data on component mount
  useEffect(() => {
    const fetchWeatherData = async () => {
      try {
        setIsLoadingWeather(true);
        const data = await weatherService.getRandomWeather();
        setWeatherData(data);
      } catch (error) {
        console.error('Error fetching weather data:', error);
        // Set fallback data if API fails
        setWeatherData({
          location: {
            name: "New York",
            country: "US"
          },
          current: {
            temperature: "22°C",
            condition: "Partly Cloudy",
            humidity: "65%",
            pressure: "1013 hPa",
            windSpeed: "12 km/h",
            visibility: "10 km"
          }
        });
      } finally {
        setIsLoadingWeather(false);
      }
    };

    fetchWeatherData();

    // Refresh weather data every 5 minutes
    const weatherInterval = setInterval(fetchWeatherData, 5 * 60 * 1000);

    return () => clearInterval(weatherInterval);
  }, []);

  // Helper function to get weather icon based on condition
  const getWeatherIcon = (condition) => {
    const conditionLower = condition?.toLowerCase() || '';
    if (conditionLower.includes('clear') || conditionLower.includes('sunny')) return '☀️';
    if (conditionLower.includes('cloud')) return '⛅';
    if (conditionLower.includes('rain')) return '🌧️';
    if (conditionLower.includes('snow')) return '❄️';
    if (conditionLower.includes('storm')) return '⛈️';
    if (conditionLower.includes('fog') || conditionLower.includes('mist')) return '🌫️';
    return '☀️'; // default
  };

  // Helper function to format temperature
  const formatTemperature = (temp) => {
    if (typeof temp === 'string') return temp;
    if (typeof temp === 'number') return `${Math.round(temp)}°C`;
    return '22°C';
  };

  // Helper function to format wind speed
  const formatWindSpeed = (speed) => {
    if (typeof speed === 'string') return speed;
    if (typeof speed === 'number') return `${Math.round(speed)} km/h`;
    return '12 km/h';
  };

  // Helper function to format pressure
  const formatPressure = (pressure) => {
    if (typeof pressure === 'string') return pressure;
    if (typeof pressure === 'number') return `${Math.round(pressure)} hPa`;
    return '1013 hPa';
  };

  return (
    <div className="home-page">
      {/* Navigation */}
      <nav className="nav">
        <div className="nav-container">
          <div className="nav-brand">
            <Link to="/" className="brand-logo">
              <span className="logo-icon">🌦️</span>
              <span className="brand-text">WeatherVision</span>
            </Link>
          </div>
          <div className="nav-menu">
            <Link to="/features" className="nav-item">Features</Link>
            <Link to="/about" className="nav-item">About</Link>
            <Link to="/login" className="nav-item nav-signin">Sign In</Link>
            <Link to="/register" className="nav-cta">Get Started</Link>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="hero">
        <div className="hero-container">
          <div className="hero-content">
            <div className="hero-badge">
              <span className="badge-icon">✨</span>
              <span>Powered by NASA Earth Data & Advanced AI</span>
            </div>
            <h1 className="hero-title">
              Weather Intelligence
              <span className="title-highlight"> Redefined</span>
            </h1>
            <p className="hero-subtitle">
              Planning an outdoor event? Our app uses NASA's historical Earth observation data 
              to tell you the likelihood of adverse weather conditions for any location and time you choose.
            </p>
            <div className="hero-actions">
              <Link to="/dashboard" className="btn btn-primary">
                <span>Get Started</span>
                <span className="btn-icon">→</span>
              </Link>
            </div>
          </div>

          <div className="hero-visual">
            <div className="weather-dashboard">
              <div className="dashboard-header">
                <div className="header-left">
                  <span className="location-icon">📍</span>
                  <span>
                    {isLoadingWeather
                      ? 'Loading...'
                      : `${weatherData?.location?.name || 'Unknown'}, ${weatherData?.location?.country || 'Unknown'}`
                    }
                  </span>
                </div>
                <div className="header-right">{new Date().toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</div>
              </div>

              <div className="weather-main">
                <div className="weather-icon-container">
                  <div className="weather-effects"></div>
                  <div className="weather-icon">
                    {isLoadingWeather ? '🔄' : getWeatherIcon(weatherData?.current?.condition)}
                  </div>
                </div>
                <div className="weather-info">
                  <div className="temperature">
                    {isLoadingWeather ? '--°C' : formatTemperature(weatherData?.current?.temperature)}
                  </div>
                  <div className="condition">
                    {isLoadingWeather ? 'Loading...' : (weatherData?.current?.condition || 'Unknown')}
                  </div>
                  <div className="feels-like">
                    Feels like {isLoadingWeather ? '--°C' : formatTemperature(weatherData?.current?.feelsLike || weatherData?.current?.temperature)}
                  </div>
                </div>
              </div>

              <div className="weather-details-grid">
                <div className="detail-item">
                  <div className="detail-icon">💨</div>
                  <div className="detail-content">
                    <div className="detail-value">
                      {isLoadingWeather ? '--' : formatWindSpeed(weatherData?.current?.windSpeed)}
                    </div>
                    <div className="detail-label">Wind</div>
                  </div>
                </div>
                <div className="detail-item">
                  <div className="detail-icon">💧</div>
                  <div className="detail-content">
                    <div className="detail-value">
                      {isLoadingWeather ? '--' : (weatherData?.current?.humidity || '65%')}
                    </div>
                    <div className="detail-label">Humidity</div>
                  </div>
                </div>
                <div className="detail-item">
                  <div className="detail-icon">👁️</div>
                  <div className="detail-content">
                    <div className="detail-value">
                      {isLoadingWeather ? '--' : (weatherData?.current?.visibility || '10 km')}
                    </div>
                    <div className="detail-label">Visibility</div>
                  </div>
                </div>
                <div className="detail-item">
                  <div className="detail-icon">📊</div>
                  <div className="detail-content">
                    <div className="detail-value">
                      {isLoadingWeather ? '--' : formatPressure(weatherData?.current?.pressure)}
                    </div>
                    <div className="detail-label">Pressure</div>
                  </div>
                </div>
              </div>

              <div className="forecast-hourly">
                <div className="hourly-item">
                  <div className="hourly-time">12 PM</div>
                  <div className="hourly-icon">{getWeatherIcon(weatherData?.current?.condition)}</div>
                  <div className="hourly-temp">{formatTemperature(weatherData?.current?.temperature)}</div>
                </div>
                <div className="hourly-item">
                  <div className="hourly-time">1 PM</div>
                  <div className="hourly-icon">{getWeatherIcon(weatherData?.current?.condition)}</div>
                  <div className="hourly-temp">{formatTemperature(weatherData?.current?.temperature)}</div>
                </div>
                <div className="hourly-item active">
                  <div className="hourly-time">2 PM</div>
                  <div className="hourly-icon">{getWeatherIcon(weatherData?.current?.condition)}</div>
                  <div className="hourly-temp">{formatTemperature(weatherData?.current?.temperature)}</div>
                </div>
                <div className="hourly-item">
                  <div className="hourly-time">3 PM</div>
                  <div className="hourly-icon">⛅</div>
                  <div className="hourly-temp">{formatTemperature((weatherData?.current?.temperature || 22) - 2)}</div>
                </div>
                <div className="hourly-item">
                  <div className="hourly-time">4 PM</div>
                  <div className="hourly-icon">🌧️</div>
                  <div className="hourly-temp">{formatTemperature((weatherData?.current?.temperature || 22) - 3)}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="features">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">How It Works</h2>
            <p className="section-subtitle">
              Leveraging decades of NASA data to give you personalized weather insights.
            </p>
          </div>

          <div className="features-grid">
            <div className="feature-card">
              <span className="feature-icon">�</span>
              <h3 className="feature-title">Select Location & Time</h3>
              <p className="feature-description">
                Choose any location on the globe and a specific day of the year.
              </p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">�</span>
              <h3 className="feature-title">Define Your Conditions</h3>
              <p className="feature-description">
                Specify what you consider "very hot," "windy," or "wet" to customize the analysis.
              </p>
            </div>

            <div className="feature-card">
              <span className="feature-icon">📊</span>
              <h3 className="feature-title">Get Probabilities</h3>
              <p className="feature-description">
                Receive a likelihood assessment of your defined weather conditions based on historical data.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="cta">
        <div className="container">
          <div className="cta-content">
            <h2 className="cta-title">
              Ready to Plan Your Perfect Day?
            </h2>
            <p className="cta-subtitle">
              Use historical data to make informed decisions about your future outdoor activities.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="footer">
        <div className="container">
          <div className="footer-content">
            <div className="footer-brand">
              <div className="brand-logo">
                <span className="logo-icon">🌦️</span>
                <span className="brand-text">WeatherVision</span>
              </div>
              <p className="footer-description">
                Advanced weather intelligence powered by NASA data and artificial intelligence.
              </p>
            </div>

            <div className="link-group">
              <h4 className="link-title">Product</h4>
              <Link to="/features" className="link-item">Features</Link>
              <Link to="/pricing" className="link-item">Pricing</Link>
              <Link to="/api" className="link-item">API</Link>
              <Link to="/mobile" className="link-item">Mobile Apps</Link>
            </div>

            <div className="link-group">
              <h4 className="link-title">Company</h4>
              <Link to="/about" className="link-item">About</Link>
              <Link to="/careers" className="link-item">Careers</Link>
              <Link to="/press" className="link-item">Press</Link>
              <Link to="/contact" className="link-item">Contact</Link>
            </div>

            <div className="link-group">
              <h4 className="link-title">Resources</h4>
              <Link to="/docs" className="link-item">Documentation</Link>
              <Link to="/help" className="link-item">Help Center</Link>
              <Link to="/status" className="link-item">Status</Link>
              <Link to="/blog" className="link-item">Blog</Link>
            </div>
          </div>

          <div className="footer-bottom">
            <div className="footer-bottom-content">
              <p className="copyright">© 2024 WeatherVision. All rights reserved.</p>
              <div className="legal-links">
                <Link to="/privacy" className="link-item">Privacy</Link>
                <Link to="/terms" className="link-item">Terms</Link>
                <Link to="/cookies" className="link-item">Cookies</Link>
              </div>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Home;
