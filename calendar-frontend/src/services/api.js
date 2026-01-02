import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Store user ID in memory
let currentUserId = null;

export const setUserId = (userId) => {
    currentUserId = userId;
    console.log('User ID set:', userId);
};

export const getUserId = () => currentUserId;

// Parse natural language input with better error handling
export const parseNaturalLanguage = async (text) => {
    console.log('Attempting to parse:', text);
    console.log('API URL:', `${API_BASE_URL}/events/parse`);

    try {
        const response = await axios.post(`${API_BASE_URL}/events/parse`, {
            text: text
        }, {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 30000 // 30 second timeout
        });

        console.log('Parse response:', response.data);
        return response.data;
    } catch (error) {
        console.error('Parse error details:', {
            message: error.message,
            response: error.response?.data,
            status: error.response?.status,
            url: error.config?.url
        });

        if (error.response) {
            // Server responded with error
            throw new Error(error.response.data.message || 'Server error');
        } else if (error.request) {
            // Request made but no response
            throw new Error('Cannot connect to server. Make sure backend is running on port 8080.');
        } else {
            // Something else happened
            throw new Error(error.message);
        }
    }
};

// Create event with better error handling
export const createEvent = async (eventData) => {
    console.log('Creating event:', eventData);
    console.log('User ID:', currentUserId);

    if (!currentUserId) {
        throw new Error('User not logged in');
    }

    try {
        const response = await axios.post(`${API_BASE_URL}/events/create`, eventData, {
            headers: {
                'Content-Type': 'application/json',
                'User-Id': currentUserId
            },
            timeout: 30000
        });

        console.log('Create event response:', response.data);
        return response.data;
    } catch (error) {
        console.error('Create event error:', {
            message: error.message,
            response: error.response?.data,
            status: error.response?.status
        });

        if (error.response) {
            throw new Error(error.response.data.message || 'Failed to create event');
        } else if (error.request) {
            throw new Error('Cannot connect to server. Make sure backend is running.');
        } else {
            throw new Error(error.message);
        }
    }
};

// User management
export const registerUser = async (username, email) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/users/register`, {
            username,
            email
        });
        console.log('Register response:', response.data);
        return response.data;
    } catch (error) {
        console.error('Register error:', error);
        throw error;
    }
};

export const loginUser = async (email) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/users/login`, {
            email
        });
        console.log('Login response:', response.data);
        setUserId(response.data.userId);
        return response.data;
    } catch (error) {
        console.error('Login error:', error);
        throw error;
    }
};

export const getUsers = async () => {
    try {
        const response = await axios.get(`${API_BASE_URL}/users`);
        return response.data;
    } catch (error) {
        console.error('Get users error:', error);
        throw error;
    }
};