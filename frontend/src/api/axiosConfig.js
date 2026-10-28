import axios from 'axios';
import { clearAuthSession, getAuthValue } from './authStorage.js';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT only to protected API requests.
// Authentication endpoints do not need an existing token.
api.interceptors.request.use(
  (config) => {
    const isAuthRequest =
      config.url?.startsWith('/auth/');
    const token = isAuthRequest
      ? null
      : getAuthValue('mindmeter_token');

    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`);
    } else {
      config.headers.delete('Authorization');
    }

    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthRequest =
      error.config?.url?.startsWith('/auth/');

    if (
      error.response?.status === 401
      && !isAuthRequest
      && window.location.pathname !== '/login'
    ) {
      clearAuthSession();
      window.location.assign('/login');
    }

    return Promise.reject(error);
  }
);

export default api;