import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './Dashboard.css';
import weatherService from '../services/weatherService';
import locationService from '../services/locationService';
import geocodeService from '../services/geocodeService';
import authService from '../services/authService';
import WeatherPredictionCard from '../components/WeatherPredictionCard';
// Leaflet map
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// fix default icon issues with webpack bundles
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: require('leaflet/dist/images/marker-icon-2x.png'),
  iconUrl: require('leaflet/dist/images/marker-icon.png'),
  shadowUrl: require('leaflet/dist/images/marker-shadow.png'),
});

const Dashboard = () => {
  const navigate = useNavigate();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [location, setLocation] = useState('');
  const [specificDate, setSpecificDate] = useState('');
  const [marker, setMarker] = useState(null);
  const [predicting, setPredicting] = useState(false);
  const [prediction, setPrediction] = useState(null);
  const [predictError, setPredictError] = useState(null);
  // Create location form state
  const [createName, setCreateName] = useState('');
  const [createCountry, setCreateCountry] = useState('');
  const [createStateField, setCreateStateField] = useState('');
  const [createCity, setCreateCity] = useState('');
  const [createAddress, setCreateAddress] = useState('');
  const [createTimezone, setCreateTimezone] = useState('');
  const [createElevation, setCreateElevation] = useState('');
  // Using specificDate from weather prediction form as the date for location creation
  const [creatingLocation, setCreatingLocation] = useState(false);
  const [createdLocation, setCreatedLocation] = useState(null);
  const [createError, setCreateError] = useState(null);
  const [createLatitude, setCreateLatitude] = useState('');
  const [createLongitude, setCreateLongitude] = useState('');

  // Check authentication status on component mount
  useEffect(() => {
    const checkAuth = async () => {
      try {
        setAuthLoading(true);
        const authenticated = authService.isAuthenticated();
        setIsAuthenticated(authenticated);
        
        if (authenticated) {
          const user = authService.getCurrentUser();
          setCurrentUser(user);
        } else {
          // Redirect to login if not authenticated
          navigate('/login');
          return;
        }
      } catch (error) {
        console.error('Authentication check failed:', error);
        navigate('/login');
      } finally {
        setAuthLoading(false);
      }
    };
    
    checkAuth();
  }, [navigate]);

  // ClickableMap helper component to drop a pin on click
  const ClickableMap = ({ onDrop }) => {
    const map = useMapEvents({
      click(e) {
        onDrop(e.latlng);
      },
      resize() {
        // Handle map resize events
        setTimeout(() => {
          map.invalidateSize();
        }, 100);
      }
    });

    // Invalidate size when component mounts
    useEffect(() => {
      if (map) {
        setTimeout(() => {
          map.invalidateSize();
        }, 100);
      }
    }, [map]);

    return null;
  };

  // Sync marker -> form fields and reverse-geocode to prefill fields (overwrite behavior)
  useEffect(() => {
    if (!marker) return;
    setCreateLatitude(marker.lat);
    setCreateLongitude(marker.lng);

    let cancelled = false;
    const fillFromGeocode = async () => {
      try {
        const lat = marker.lat;
        const lon = marker.lng;
        const data = await geocodeService.reverseGeocode(lat, lon);
        if (!data || cancelled) return;
        const addr = data.address || {};
        const nameCandidate = data.name || addr.neighbourhood || addr.suburb || addr.village || addr.town || addr.city || `Location ${lat.toFixed(3)},${lon.toFixed(3)}`;
        setCreateName(nameCandidate);
        setCreateCity(addr.city || addr.town || addr.village || '');
        setCreateStateField(addr.state || '');
        setCreateCountry(addr.country || '');
        setCreateAddress(data.display_name || '');
        // timezone left for manual input
      } catch (err) {
        // ignore geocode errors
        console.warn('Reverse geocode failed', err);
      }
    };
    fillFromGeocode();
    return () => { cancelled = true; };
  }, [marker]);

  // Simple download helper used by the Download buttons (CSV / JSON)
  const jsonToCsv = (obj) => {
    if (!obj) return '';
    // If array, assume array of objects
    const rows = Array.isArray(obj) ? obj : [obj];
    const keys = Array.from(new Set(rows.flatMap(r => Object.keys(r))));
    const header = keys.join(',');
    const lines = rows.map(r => keys.map(k => {
      const v = r[k] === undefined || r[k] === null ? '' : String(r[k]).replace(/"/g, '""');
      return `"${v}"`;
    }).join(','));
    return [header, ...lines].join('\n');
  };

  const handleDownload = (format) => {
    // Build a minimal payload with location or coords, and date
    const coords = marker ? { latitude: marker.lat, longitude: marker.lng } : (createLatitude && createLongitude ? { latitude: createLatitude, longitude: createLongitude } : null);
    const payload = {
      location: location || null,
      coords,
      date: specificDate || null
    };
    if (format === 'json') {
      const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'weather_payload.json';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } else if (format === 'csv') {
      const csv = jsonToCsv(payload);
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'weather_payload.csv';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    }
  };

  // Show loading screen while checking authentication
  if (authLoading) {
    return (
      <div className="dashboard-page">
        <div className="auth-loading">
          <div className="loading-spinner">
            <svg className="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
          <p className="loading-text">Checking authentication...</p>
        </div>
      </div>
    );
  }

  // Don't render the dashboard if not authenticated (will redirect)
  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="dashboard-page">
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
            {isAuthenticated && currentUser && (
              <div className="nav-user-info">
                <span className="user-welcome">Welcome, {currentUser.username || currentUser.name || 'User'}!</span>
                <button 
                  onClick={() => {
                    authService.logout();
                    navigate('/login');
                  }}
                  className="logout-btn"
                >
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <div className="container mx-auto p-6 min-h-screen flex flex-col">
        <h1 className="text-3xl font-bold text-blue-700 mb-6">Weather Probability Dashboard</h1>

        {/* Date Selection (single day only) */}
        <div className="mb-6">
          <label className="block text-lg font-medium text-gray-700 mb-2">Date</label>
          <input
            type="date"
            value={specificDate}
            onChange={(e) => setSpecificDate(e.target.value)}
            className="w-full p-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        {/* Data Visualization Placeholder */}
        <div className="mb-6">
          <h2 className="text-xl font-semibold text-gray-700 mb-4">Drop your pin</h2>
          <div className="bg-gray-100 p-6 rounded-lg text-center">
            {/* Map: click to drop a pin */} 
            <div style={{ height: 400, width: '100%', marginTop: 20 }}>
              <MapContainer 
                center={[39.8283, -98.5795]} 
                zoom={6} 
                minZoom={3}
                maxZoom={18}
                style={{ height: '100%', width: '100%' }}
                key="dashboard-map"
                whenReady={(map) => {
                  // Force map to invalidate size after container is ready
                  setTimeout(() => {
                    map.target.invalidateSize();
                  }, 100);
                }}
              >
                <TileLayer
                  attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                  url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  noWrap={true}
                />
                <ClickableMap onDrop={(latlng) => setMarker(latlng)} />
                {marker && <Marker position={[marker.lat, marker.lng]} />}
              </MapContainer>
            </div>

            <div className="mt-4 text-left">
              <strong>Selected Coordinates:</strong>
              <div>{marker ? `Lat: ${marker.lat.toFixed(5)}, Lon: ${marker.lng.toFixed(5)}` : 'No pin placed yet. Click the map to drop a pin.'}</div>
            </div>
          </div>
        </div>

        {/* Create Location Form */}
        <div className="mb-6">
          <h2 className="text-xl font-semibold text-gray-700 mb-4">Fill the Location Details</h2>
          <div className="bg-white p-6 rounded-lg">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <input value={createName} onChange={(e) => setCreateName(e.target.value)} placeholder="Name*" className="p-2 border rounded" />
              <input value={createCountry} onChange={(e) => setCreateCountry(e.target.value)} placeholder="Country" className="p-2 border rounded" />
              <input value={createStateField} onChange={(e) => setCreateStateField(e.target.value)} placeholder="State" className="p-2 border rounded" />
              <input value={createCity} onChange={(e) => setCreateCity(e.target.value)} placeholder="City" className="p-2 border rounded" />
              <input value={createAddress} onChange={(e) => setCreateAddress(e.target.value)} placeholder="Address" className="p-2 border rounded col-span-2" />
              <input value={createTimezone} onChange={(e) => setCreateTimezone(e.target.value)} placeholder="Timezone" className="p-2 border rounded" />
              <input value={createElevation} onChange={(e) => setCreateElevation(e.target.value)} placeholder="Elevation (meters)" type="number" className="p-2 border rounded" />
              <input value={createLatitude} onChange={(e) => setCreateLatitude(e.target.value)} placeholder="Latitude*" className="p-2 border rounded" />
              <input value={createLongitude} onChange={(e) => setCreateLongitude(e.target.value)} placeholder="Longitude*" className="p-2 border rounded" />
            </div>

            <div className="mt-4 flex space-x-3">
              <button
                onClick={async () => {
                  setCreateError(null);
                  setCreatedLocation(null);
                  // validation: require name, date (from weather form above), and either marker or coordinates
                  if (!createName) { setCreateError('Name is required'); return; }
                  if (!specificDate) { setCreateError('Date is required (set in weather prediction section above)'); return; }
                  // require lat/lon either from form or marker
                  const latVal = createLatitude || (marker && marker.lat);
                  const lonVal = createLongitude || (marker && marker.lng);
                  if (!latVal || !lonVal) { setCreateError('Latitude and longitude are required (drop a pin or enter them)'); return; }

                  const payload = {
                    name: createName,
                    latitude: Number(latVal),
                    longitude: Number(lonVal),
                    country: createCountry || undefined,
                    state: createStateField || undefined,
                    city: createCity || undefined,
                    address: createAddress || undefined,
                    timezone: createTimezone || undefined,
                    elevation: createElevation ? Number(createElevation) : undefined,
                    beginDate: specificDate
                  };

                  try {
                    setCreatingLocation(true);
                    const resp = await locationService.createLocation(payload);
                    setCreatedLocation(resp);
                    // pre-fill name field and location input with returned name
                    setLocation(resp.name || createName);
                    setCreateError(null);
                  } catch (err) {
                    console.error('Create location failed', err);
                    // err may be a structured object or string
                    const msg = err?.message || JSON.stringify(err) || 'Unknown error';
                    setCreateError(msg);
                  } finally {
                    setCreatingLocation(false);
                  }
                }}
                className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700"
                disabled={creatingLocation}
              >
                {creatingLocation ? 'Creating...' : 'Choose Location'}
              </button>

            </div>

            {createError && <div className="mt-3 text-red-600">{createError}</div>}
            {createdLocation && (
              <div className="created-location-card">
                <h4 className="created-location-title">Location Created Successfully</h4>
                <div className="created-location-details">
                  <p><strong>Name:</strong> {createdLocation.name}</p>
                  <p><strong>Coordinates:</strong> {createdLocation.latitude}, {createdLocation.longitude}</p>
                  <p><strong>Address:</strong> {createdLocation.address}</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Weather Prediction Section */}
        <div className="mb-6">
          <h2 className="text-xl font-semibold text-gray-700 mb-4">Get Weather Prediction</h2>
          <div className="bg-white p-6 rounded-lg">
            <p className="text-gray-600 mb-4">
              Generate a weather prediction using the location and date specified above.
            </p>
            
            <button
              onClick={async () => {
                setPredictError(null);
                setPrediction(null);
                
                // Validation
                if (!specificDate) {
                  setPredictError('Please select a date for the prediction');
                  return;
                }
                
                // Get location data - prioritize created location, then form data, then marker
                let locationData = null;
                
                if (createdLocation) {
                  // Use the created location
                  locationData = {
                    ...createdLocation,
                    beginDate: specificDate.split('-').map(Number) // Convert YYYY-MM-DD to [YYYY, MM, DD]
                  };
                } else {
                  // Build location from form data
                  const latVal = createLatitude || (marker && marker.lat);
                  const lonVal = createLongitude || (marker && marker.lng);
                  
                  if (!latVal || !lonVal) {
                    setPredictError('Please create a location or drop a pin on the map first');
                    return;
                  }
                  
                  if (!createName) {
                    setPredictError('Please enter a location name');
                    return;
                  }
                  
                  locationData = {
                    name: createName,
                    latitude: Number(latVal),
                    longitude: Number(lonVal),
                    country: createCountry || null,
                    state: createStateField || null,
                    city: createCity || null,
                    address: createAddress || null,
                    timezone: createTimezone || null,
                    elevation: createElevation ? Number(createElevation) : null,
                    beginDate: specificDate.split('-').map(Number),
                    endDate: null
                  };
                }
                
                try {
                  setPredicting(true);
                  const predictionResult = await weatherService.getWeatherPrediction(locationData);
                  setPrediction(predictionResult);
                } catch (err) {
                  console.error('Weather prediction failed:', err);
                  
                  // Handle specific error cases
                  if (err.message && err.message.includes('Authentication')) {
                    setPredictError('Authentication expired. Please log out and log back in.');
                  } else if (err.message && err.message.includes('401')) {
                    setPredictError('Session expired. Please refresh the page and try again.');
                  } else {
                    setPredictError(err.message || 'Failed to get weather prediction. Please try again.');
                  }
                } finally {
                  setPredicting(false);
                }
              }}
              className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              disabled={predicting}
            >
              {predicting ? (
                <span className="flex items-center">
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Generating Prediction...
                </span>
              ) : (
                '🌦️ Get Weather Prediction'
              )}
            </button>
          </div>
        </div>

        {/* Prediction Result */}
        {prediction && <WeatherPredictionCard prediction={prediction} />}
        {predictError && (
          <div className="mt-6 text-red-600">{predictError}</div>
        )}
      </div>

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
              <p className="copyright">© 2025 WeatherVision. All rights reserved.</p>
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

export default Dashboard;