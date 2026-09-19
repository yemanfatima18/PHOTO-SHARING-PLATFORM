import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { ErrorResponse } from '../types';

const API_BASE_URL = '/api';

const PUBLIC_AUTH_ENDPOINTS_NO_AUTH = [
  '/auth/register',
  '/auth/login',
];

const PUBLIC_AUTH_ENDPOINTS = [
  '/auth/register',
  '/auth/login',
  '/auth/me',
];

const PUBLIC_ENDPOINT_PREFIXES = [
  '/public/',
];

function isPublicAuthRequestNoAuth(url?: string): boolean {
  if (!url) return false;
  return PUBLIC_AUTH_ENDPOINTS_NO_AUTH.some(endpoint => url.includes(endpoint));
}

function isPublicAuthRequest(url?: string): boolean {
  if (!url) return false;
  return PUBLIC_AUTH_ENDPOINTS.some(endpoint => url.includes(endpoint)) ||
    PUBLIC_ENDPOINT_PREFIXES.some(prefix => url.includes(prefix));
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const requestUrl = config.url || '';
    if (isPublicAuthRequestNoAuth(requestUrl)) {
      return config;
    }
    const token = localStorage.getItem('accessToken');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorResponse>) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || '';
      
      if (!isPublicAuthRequest(requestUrl)) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('user');
        if (!window.location.pathname.startsWith('/login') && !window.location.pathname.startsWith('/register')) {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export const handleApiError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ErrorResponse>;
    
    // Network/connection errors
    if (!axiosError.response) {
      if (axiosError.code === 'ECONNREFUSED' || axiosError.message === 'Network Error') {
        return 'Cannot connect to API server. Is the backend running on port 8080?';
      }
      if (axiosError.message.includes('timeout')) {
        return 'Request timed out. Please try again.';
      }
      return 'Cannot connect to API. Check your connection.';
    }
    
    const status = axiosError.response.status;
    const data = axiosError.response.data;
    
    switch (status) {
      case 400:
        if (data?.validationErrors) {
          return Object.values(data.validationErrors).join(', ');
        }
        return data?.message || 'Validation failed. Please check your input.';
      
      case 401:
        return data?.message || 'Authentication required. Please log in.';
      
      case 403:
        return data?.message || 'Access denied. You do not have permission.';
      
      case 404:
        return data?.message || 'Resource not found.';
      
      case 409:
        return data?.message || 'Email already registered.';
      
      case 500:
        return data?.message || 'Server error. Please try again later.';
      
      default:
        return data?.message || `Request failed with status ${status}`;
    }
  }
  return 'An unexpected error occurred';
};

export default api;