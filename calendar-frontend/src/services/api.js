// All API calls use relative URLs — served by Vercel serverless functions in /api/

let currentUserId = null;

export const setUserId = (userId) => { currentUserId = userId; };
export const getUserId = () => currentUserId;

// --- Google OAuth token management (stored in localStorage) ---

export const getTokens = () => {
    try {
        const t = localStorage.getItem('google_tokens');
        return t ? JSON.parse(t) : null;
    } catch { return null; }
};

export const setTokens = (tokens) => {
    localStorage.setItem('google_tokens', JSON.stringify(tokens));
};

export const clearTokens = () => {
    localStorage.removeItem('google_tokens');
};

export const isAuthenticated = () => !!getTokens();

// --- API calls ---

export const testConnection = async () => {
    try {
        const res = await fetch('/api/events/health');
        const data = await res.json();
        return { success: true, data };
    } catch (e) {
        return { success: false, error: e.message };
    }
};

export const parseNaturalLanguage = async (text) => {
    const res = await fetch('/api/events/parse', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.errorMessage || 'Failed to parse');
    return data;
};

export const getOAuthUrl = async (userId) => {
    const res = await fetch(`/api/events/auth/url?userId=${encodeURIComponent(userId || '')}`);
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || 'Failed to get auth URL');
    return data.authUrl;
};

export const createEvent = async (eventData) => {
    const tokens = getTokens();
    if (!tokens) throw new Error('Not authenticated with Google Calendar');

    const tokenB64 = btoa(JSON.stringify(tokens));

    const res = await fetch('/api/events/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${tokenB64}`
        },
        body: JSON.stringify(eventData)
    });
    const data = await res.json();
    return data;
};

export const registerUser = async (username, email) => {
    const res = await fetch('/api/users/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email })
    });
    if (!res.ok) {
        const err = await res.json();
        throw new Error(err.error || 'Registration failed');
    }
    return await res.json();
};
