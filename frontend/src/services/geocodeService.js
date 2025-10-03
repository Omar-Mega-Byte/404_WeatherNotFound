import axios from 'axios';

const NOMINATIM_BASE = 'https://nominatim.openstreetmap.org/reverse';

const reverseGeocode = async (lat, lon) => {
  try {
    const resp = await axios.get(NOMINATIM_BASE, {
      params: {
        lat,
        lon,
        format: 'jsonv2',
        addressdetails: 1,
      },
      headers: {
        'Accept-Language': 'en'
      }
    });
    return resp.data;
  } catch (err) {
    console.error('Reverse geocode failed', err);
    return null;
  }
};

export default { reverseGeocode };
