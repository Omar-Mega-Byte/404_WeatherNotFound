import axios from 'axios';

// Base path uses the frontend proxy (package.json) to avoid hardcoding host
const LOCATION_API_BASE = '/api/v1/locations';

const createLocation = async (payload) => {
  try {
    const resp = await axios.post(LOCATION_API_BASE, payload);
    return resp.data;
  } catch (err) {
    // Normalize error to make handling in components easier
    const errorPayload = err.response?.data || { message: err.message || 'Unknown error' };
    throw errorPayload;
  }
};

const locationService = {
  createLocation,
};

export default locationService;