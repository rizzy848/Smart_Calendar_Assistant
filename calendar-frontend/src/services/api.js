import axios from 'axios';

export const API_BASE_URL = process.env.REACT_APP_API_URL ||
    (window.location.hostname === 'localhost'
        ? 'http://localhost:8080/api'
        : 'https://smartcalendarassistant-production.up.railway.app/api');

// Store user ID in memory
let currentUserId = null;

export const setUserId = (userId) => {
    currentUserId = userId;
    console.log('✅ User ID set:', userId);
};

export const getUserId = () => currentUserId;

// Test backend connection
export const testConnection = async () => {
    try {
        console.log('🔍 Testing backend connection...');
        const response = await axios.get(`${API_BASE_URL}/events/health`, {
            timeout: 5000
        });
        console.log('✅ Backend is reachable:', response.data);
        return { success: true, data: response.data };
    } catch (error) {
        console.error('❌ Backend connection failed:', error.message);
        return {
            success: false,
            error: error.message,
            details: getErrorDetails(error)
        };
    }
};

// Helper to extract detailed error information
const getErrorDetails = (error) => {
    if (error.code === 'ECONNREFUSED') {
        return {
            type: 'CONNECTION_REFUSED',
            message: 'Backend server is not running on port 8080',
            solution: 'Start the backend: mvn spring-boot:run'
        };
    }

    if (error.code === 'ECONNABORTED') {
        return {
            type: 'TIMEOUT',
            message: 'Request timed out - backend is not responding',
            solution: 'Check if backend is running and not frozen'
        };
    }

    if (error.response) {
        return {
            type: 'HTTP_ERROR',
            status: error.response.status,
            message: error.response.data?.message || error.response.statusText,
            solution: 'Check backend logs for error details'
        };
    }

    if (error.request) {
        return {
            type: 'NO_RESPONSE',
            message: 'Request was sent but no response received',
            solution: 'Check network connectivity and firewall settings'
        };
    }

    return {
        type: 'UNKNOWN',
        message: error.message,
        solution: 'Check browser console for more details'
    };
};

// Parse natural language input with better error handling
export const parseNaturalLanguage = async (text) => {
    console.log('📤 Parsing request:', text);
    console.log('📍 API URL:', `${API_BASE_URL}/events/parse`);

    try {
        const response = await axios.post(`${API_BASE_URL}/events/parse`, {
            text: text
        }, {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 30000 // 30 seconds for AI parsing
        });

        console.log('✅ Parse response:', response.data);
        return response.data;

    } catch (error) {
        console.error('❌ Parse error:', getErrorDetails(error));

        const details = getErrorDetails(error);
        throw new Error(details.message);
    }
};

// Create event with EXTENDED timeout for OAuth flow
export const createEvent = async (eventData) => {
    console.log('📅 Creating event:', eventData);
    console.log('👤 User ID:', currentUserId);

    if (!currentUserId) {
        throw new Error('User not logged in');
    }

    try {
        // IMPORTANT: 5 minute timeout to allow for Google OAuth authentication
        // First-time users need time to authenticate in the browser
        const response = await axios.post(`${API_BASE_URL}/events/create`, eventData, {
            headers: {
                'Content-Type': 'application/json',
                'User-Id': currentUserId
            },
            timeout: 300000 // 5 minutes (300 seconds) for OAuth flow
        });

        console.log('✅ Create event response:', response.data);
        return response.data;

    } catch (error) {
        console.error('❌ Create event error:', getErrorDetails(error));

        const details = getErrorDetails(error);

        // Special handling for timeout during OAuth
        if (details.type === 'TIMEOUT') {
            throw new Error('Authentication took too long. Please try again and complete the Google sign-in promptly.');
        }

        throw new Error(details.message);
    }
};

// User management with better error handling
export const registerUser = async (username, email) => {
    console.log('🔐 Registering user:', { username, email });
    console.log('📍 API URL:', `${API_BASE_URL}/users/register`);

    try {
        const requestData = { username, email };
        console.log('📤 Request body:', requestData);

        const response = await axios.post(`${API_BASE_URL}/users/register`, requestData, {
            headers: {
                'Content-Type': 'application/json'
            },
            timeout: 10000
        });

        console.log('✅ Registration successful:', response.data);
        return response.data;

    } catch (error) {
        console.error('❌ Registration error details:', {
            message: error.message,
            code: error.code,
            response: error.response?.data,
            status: error.response?.status
        });

        const details = getErrorDetails(error);

        // Create user-friendly error message
        let userMessage = 'Registration failed: ';

        if (details.type === 'CONNECTION_REFUSED') {
            userMessage = 'Cannot connect to server. Please make sure the backend is running on port 8080.';
        } else if (details.type === 'TIMEOUT') {
            userMessage = 'Server is not responding. Please check if the backend is running.';
        } else if (details.type === 'HTTP_ERROR') {
            userMessage = `Server error (${details.status}): ${details.message}`;
        } else {
            userMessage = details.message;
        }

        throw new Error(userMessage);
    }
};

export const loginUser = async (email) => {
    try {
        const response = await axios.post(`${API_BASE_URL}/users/login`, {
            email
        });
        console.log('✅ Login response:', response.data);
        setUserId(response.data.userId);
        return response.data;
    } catch (error) {
        console.error('❌ Login error:', error);
        throw error;
    }
};

export const getUsers = async () => {
    try {
        const response = await axios.get(`${API_BASE_URL}/users`);
        return response.data;
    } catch (error) {
        console.error('❌ Get users error:', error);
        throw error;
    }
};