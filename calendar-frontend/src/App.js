import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Calendar, User, LogOut, MessageSquare, CheckCircle, AlertCircle, Wifi, WifiOff, Clock, Loader, Info } from 'lucide-react';
import { parseNaturalLanguage, createEvent, setUserId, registerUser, testConnection } from './services/api';
import './App.css';

function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [currentUser, setCurrentUser] = useState(null);
    const [userInput, setUserInput] = useState('');
    const [parsedEvent, setParsedEvent] = useState(null);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [showConfirmation, setShowConfirmation] = useState(false);
    const [backendStatus, setBackendStatus] = useState({ checking: true, connected: false });
    const [authStatus, setAuthStatus] = useState({ checking: false, authenticated: false });

    const inputRef = useRef(null);
    const authCheckInterval = useRef(null);

    // Test backend connection on mount
    useEffect(() => {
        checkBackendConnection();
    }, []);

    // Check authentication status when user logs in
    const checkAuthStatus = useCallback(async () => {
        if (!currentUser) return;

        setAuthStatus({ checking: true, authenticated: false });

        try {
            const response = await fetch(`http://localhost:8080/api/events/auth/check/${currentUser.userId}`);
            const data = await response.json();

            setAuthStatus({
                checking: false,
                authenticated: !data.needsAuth
            });

            console.log('🔐 Auth status:', !data.needsAuth ? 'Authenticated' : 'Needs authentication');

            return !data.needsAuth; // Return authentication status
        } catch (error) {
            console.error('❌ Failed to check auth status:', error);
            setAuthStatus({ checking: false, authenticated: false });
            return false;
        }
    }, [currentUser]);

    useEffect(() => {
        if (currentUser) {
            checkAuthStatus();
        }
    }, [currentUser, checkAuthStatus]);

    const checkBackendConnection = async () => {
        console.log('🔍 Checking backend connection...');
        setBackendStatus({ checking: true, connected: false });

        const result = await testConnection();

        setBackendStatus({
            checking: false,
            connected: result.success,
            details: result.details || result.error
        });

        if (!result.success) {
            console.error('❌ Backend not reachable:', result.details);
        } else {
            console.log('✅ Backend is connected');
        }
    };

    // Focus management - only when needed
    useEffect(() => {
        if (isLoggedIn && !loading && !showConfirmation && inputRef.current) {
            inputRef.current.focus();
        }
    }, [isLoggedIn, loading, showConfirmation]);

    // Auto-dismiss success messages
    useEffect(() => {
        if (message && message.type === 'success' && !message.persistent) {
            const timer = setTimeout(() => setMessage(null), 5000);
            return () => clearTimeout(timer);
        }
    }, [message]);

    // Cleanup on unmount
    useEffect(() => {
        return () => {
            if (authCheckInterval.current) {
                clearInterval(authCheckInterval.current);
            }
        };
    }, []);

    // Parse input
    const handleParse = async (e) => {
        if (e) e.preventDefault();

        const trimmed = userInput.trim();
        if (!trimmed) return;

        setLoading(true);
        setMessage(null);
        setParsedEvent(null);
        setShowConfirmation(false);

        try {
            console.log('🔍 Parsing:', trimmed);
            const result = await parseNaturalLanguage(trimmed);

            if (result.successful) {
                setParsedEvent(result);
                setShowConfirmation(true);
                setMessage({
                    type: 'success',
                    text: '✓ I understood your request! Please review and confirm.',
                    persistent: false
                });
            } else {
                setMessage({
                    type: 'error',
                    text: result.errorMessage || 'Could not understand your request. Please try being more specific.',
                    persistent: false
                });
            }
        } catch (error) {
            console.error('❌ Parse error:', error);
            setMessage({
                type: 'error',
                text: 'Error parsing your request: ' + error.message,
                persistent: false
            });
        } finally {
            setLoading(false);
        }
    };

    // Create event with improved OAuth flow
    const handleCreateEvent = async () => {
        if (!parsedEvent || !currentUser) return;

        setLoading(true);
        setMessage({
            type: 'success',
            text: '⏳ Checking authentication...',
            persistent: true
        });

        try {
            // Check if user needs OAuth
            const authCheck = await fetch(`http://localhost:8080/api/events/auth/check/${currentUser.userId}`);
            const authData = await authCheck.json();

            if (authData.needsAuth) {
                // User needs to authenticate
                setMessage({
                    type: 'success',
                    text: '🔐 Opening Google authentication...',
                    persistent: true
                });

                // Get OAuth URL
                const urlResponse = await fetch(`http://localhost:8080/api/events/auth/url/${currentUser.userId}`);
                const urlData = await urlResponse.json();

                if (urlData.authUrl) {
                    // Open OAuth in new window
                    const width = 600;
                    const height = 700;
                    const left = window.screen.width / 2 - width / 2;
                    const top = window.screen.height / 2 - height / 2;

                    const authWindow = window.open(
                        urlData.authUrl,
                        'Google Calendar Authorization',
                        `width=${width},height=${height},left=${left},top=${top}`
                    );

                    if (!authWindow) {
                        setMessage({
                            type: 'error',
                            text: 'Popup was blocked! Please allow popups for this site and try again.',
                            persistent: false
                        });
                        setLoading(false);
                        return;
                    }

                    setMessage({
                        type: 'success',
                        text: '👆 Complete authentication in the popup window. Waiting...',
                        persistent: true
                    });

                    // Poll for authentication completion
                    let pollAttempts = 0;
                    const maxPollAttempts = 120; // 2 minutes

                    const pollAuth = async () => {
                        pollAttempts++;

                        // Check if window is closed
                        if (authWindow.closed) {
                            console.log('🔒 Auth window closed, checking authentication...');

                            // Wait a moment for backend to save tokens
                            await new Promise(resolve => setTimeout(resolve, 2000));

                            // Check auth status
                            const isAuthenticated = await checkAuthStatus();

                            if (authCheckInterval.current) {
                                clearInterval(authCheckInterval.current);
                                authCheckInterval.current = null;
                            }

                            if (isAuthenticated) {
                                setMessage({
                                    type: 'success',
                                    text: '✓ Authentication successful! Creating event...',
                                    persistent: true
                                });

                                // Create the event
                                await performEventCreation();
                            } else {
                                setMessage({
                                    type: 'error',
                                    text: 'Authentication was not completed. Please try again and make sure to click "Allow" in the Google authentication window.',
                                    persistent: false
                                });
                                setLoading(false);
                            }
                            return;
                        }

                        // Timeout after max attempts
                        if (pollAttempts >= maxPollAttempts) {
                            if (authCheckInterval.current) {
                                clearInterval(authCheckInterval.current);
                                authCheckInterval.current = null;
                            }
                            if (!authWindow.closed) {
                                authWindow.close();
                            }
                            setMessage({
                                type: 'error',
                                text: 'Authentication timed out. Please try again.',
                                persistent: false
                            });
                            setLoading(false);
                        }
                    };

                    // Start polling
                    authCheckInterval.current = setInterval(pollAuth, 1000);

                    return; // Don't continue, wait for auth
                }
            } else {
                // User is already authenticated, create event
                setMessage({
                    type: 'success',
                    text: '📅 Creating event...',
                    persistent: true
                });
                await performEventCreation();
            }

        } catch (error) {
            console.error('❌ Error:', error);
            setMessage({
                type: 'error',
                text: 'Error: ' + error.message,
                persistent: false
            });
            setLoading(false);
        }
    };

    const performEventCreation = async () => {
        try {
            const result = await createEvent({
                title: parsedEvent.title,
                date: parsedEvent.date,
                startTime: parsedEvent.startTime,
                endTime: parsedEvent.endTime,
                location: parsedEvent.location
            });

            if (result.success) {
                setMessage({
                    type: 'success',
                    text: '✓ Event created successfully in your Google Calendar!',
                    persistent: false
                });
                setUserInput('');
                setParsedEvent(null);
                setShowConfirmation(false);

                // Update auth status
                setAuthStatus({ checking: false, authenticated: true });
            } else {
                setMessage({
                    type: 'error',
                    text: result.message || 'Failed to create event. Please try again.',
                    persistent: false
                });
            }
        } catch (error) {
            console.error('❌ Creation error:', error);
            setMessage({
                type: 'error',
                text: 'Error creating event: ' + error.message,
                persistent: false
            });
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        if (window.confirm('Are you sure you want to logout?')) {
            // Clear any running intervals
            if (authCheckInterval.current) {
                clearInterval(authCheckInterval.current);
            }

            setIsLoggedIn(false);
            setCurrentUser(null);
            setUserInput('');
            setParsedEvent(null);
            setMessage(null);
            setShowConfirmation(false);
            setAuthStatus({ checking: false, authenticated: false });
        }
    };

    const handleCancel = () => {
        setShowConfirmation(false);
        setParsedEvent(null);
        setMessage(null);
    };

    return (
        <div className="App">
            {!isLoggedIn ? <LoginScreen /> : <CalendarInterface />}
        </div>
    );

    // Login Screen Component
    function LoginScreen() {
        const [username, setUsername] = useState('');
        const [email, setEmail] = useState('');
        const [loggingIn, setLoggingIn] = useState(false);
        const [loginError, setLoginError] = useState('');

        const handleLogin = async (e) => {
            if (e) e.preventDefault();

            if (!email || !username) {
                setLoginError('Please enter both name and email');
                return;
            }

            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(email)) {
                setLoginError('Please enter a valid email address');
                return;
            }

            if (!backendStatus.connected) {
                setLoginError('Backend server is not running. Please start it with: mvn spring-boot:run');
                return;
            }

            setLoggingIn(true);
            setLoginError('');

            try {
                console.log('📝 Registering user:', { username, email });
                const userData = await registerUser(username, email);

                setUserId(userData.userId);
                setCurrentUser({
                    userId: userData.userId,
                    username: userData.username || username,
                    email: userData.email || email
                });
                setIsLoggedIn(true);
                setMessage({
                    type: 'success',
                    text: `Welcome, ${username}! 🎉`,
                    persistent: false
                });

            } catch (error) {
                console.error('❌ Login failed:', error);
                setLoginError(error.message || 'Failed to connect. Please check if backend is running.');
            } finally {
                setLoggingIn(false);
            }
        };

        return (
            <div className="login-container">
                <div className="login-card">
                    <div className="login-header">
                        <Calendar size={48} className="logo-icon" />
                        <h1>AI Smart Calendar</h1>
                        <p>Natural language calendar assistant powered by Google Gemini AI</p>
                    </div>

                    {/* Backend Status */}
                    <div style={{
                        padding: '12px',
                        borderRadius: '8px',
                        marginBottom: '20px',
                        display: 'flex',
                        alignItems: 'center',
                        gap: '10px',
                        background: backendStatus.checking ? '#f0f0f0' :
                            backendStatus.connected ? '#d4edda' : '#f8d7da',
                        color: backendStatus.checking ? '#666' :
                            backendStatus.connected ? '#155724' : '#721c24',
                        border: `1px solid ${backendStatus.checking ? '#ddd' :
                            backendStatus.connected ? '#c3e6cb' : '#f5c6cb'}`
                    }}>
                        {backendStatus.checking ? (
                            <>
                                <Loader size={20} className="spinner" />
                                <span>Checking connection...</span>
                            </>
                        ) : backendStatus.connected ? (
                            <>
                                <Wifi size={20} />
                                <span>✓ Server Connected</span>
                            </>
                        ) : (
                            <>
                                <WifiOff size={20} />
                                <div style={{flex: 1}}>
                                    <div><strong>Server Offline</strong></div>
                                    <div style={{fontSize: '12px', marginTop: '4px'}}>
                                        Run: <code>mvn spring-boot:run</code>
                                    </div>
                                </div>
                                <button
                                    onClick={checkBackendConnection}
                                    className="btn-retry"
                                >
                                    Retry
                                </button>
                            </>
                        )}
                    </div>

                    {loginError && (
                        <div className="message message-error" style={{marginBottom: '20px'}}>
                            <AlertCircle size={20} />
                            <span>{loginError}</span>
                        </div>
                    )}

                    <form className="login-form" onSubmit={handleLogin}>
                        <input
                            type="text"
                            placeholder="Your Name"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="input-field"
                            disabled={loggingIn || !backendStatus.connected}
                            autoFocus
                        />
                        <input
                            type="email"
                            placeholder="Google Email (for calendar access)"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="input-field"
                            disabled={loggingIn || !backendStatus.connected}
                        />
                        <button
                            type="submit"
                            disabled={!username || !email || loggingIn || !backendStatus.connected}
                            className="btn-primary"
                        >
                            {loggingIn ? (
                                <>
                                    <Loader size={16} className="spinner" style={{marginRight: '8px'}} />
                                    Connecting...
                                </>
                            ) : 'Get Started'}
                        </button>
                    </form>

                    <div style={{marginTop: '20px', padding: '12px', background: '#f8f9fa', borderRadius: '8px', fontSize: '14px', color: '#666'}}>
                        <Info size={16} style={{display: 'inline', marginRight: '6px'}} />
                        You'll need to authenticate with Google Calendar on first use
                    </div>
                </div>
            </div>
        );
    }

    // Calendar Interface Component
    function CalendarInterface() {
        return (
            <div className="app-container">
                <header className="app-header">
                    <div className="header-content">
                        <div className="header-left">
                            <Calendar size={32} className="header-icon" />
                            <h1>AI Calendar Assistant</h1>
                        </div>
                        <div className="header-right">
                            <div className="user-info">
                                <User size={20} />
                                <span>{currentUser?.username}</span>
                            </div>
                            {authStatus.checking ? (
                                <div className="auth-status checking">
                                    <Loader size={16} className="spinner" />
                                </div>
                            ) : authStatus.authenticated ? (
                                <div className="auth-status authenticated" title="Connected to Google Calendar">
                                    <CheckCircle size={16} />
                                </div>
                            ) : (
                                <div className="auth-status not-authenticated" title="Not connected to Google Calendar">
                                    <AlertCircle size={16} />
                                </div>
                            )}
                            <button onClick={handleLogout} className="btn-icon" title="Logout">
                                <LogOut size={20} />
                            </button>
                        </div>
                    </div>
                </header>

                <main className="main-content">
                    {message && (
                        <div className={`message message-${message.type}`}>
                            {message.type === 'success' ?
                                loading ? <Clock size={20} className="spinner" /> : <CheckCircle size={20} /> :
                                <AlertCircle size={20} />
                            }
                            <span>{message.text}</span>
                        </div>
                    )}

                    <div className="input-section">
                        <h2>What would you like to schedule?</h2>
                        <div className="examples">
                            <p>💡 Try saying:</p>
                            <ul>
                                <li>"Meeting with John tomorrow at 2 PM"</li>
                                <li>"Dentist appointment next Monday at 10:30 AM"</li>
                                <li>"Workout session today at 6 PM for 1 hour"</li>
                                <li>"Team standup next Friday at 9 AM at Office"</li>
                            </ul>
                        </div>

                        <form onSubmit={handleParse}>
                            <div className="input-container">
                                <MessageSquare className="input-icon" size={20} />
                                <input
                                    ref={inputRef}
                                    type="text"
                                    value={userInput}
                                    onChange={(e) => setUserInput(e.target.value)}
                                    placeholder="Type your event here... (e.g., 'Lunch with Sarah tomorrow at noon')"
                                    className="main-input"
                                    disabled={loading}
                                    autoComplete="off"
                                />
                                <button
                                    type="submit"
                                    disabled={loading || !userInput.trim()}
                                    className="btn-primary"
                                >
                                    {loading ? (
                                        <>
                                            <Loader size={16} className="spinner" />
                                        </>
                                    ) : 'Parse'}
                                </button>
                            </div>
                        </form>
                    </div>

                    {showConfirmation && parsedEvent && (
                        <div className="confirmation-section">
                            <h3>
                                <CheckCircle size={20} />
                                Review Event Details
                            </h3>
                            <div className="event-details">
                                <div className="detail-row">
                                    <span className="label">Event Title:</span>
                                    <span className="value">{parsedEvent.title}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="label">Date:</span>
                                    <span className="value">{new Date(parsedEvent.date + 'T00:00:00').toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}</span>
                                </div>
                                <div className="detail-row">
                                    <span className="label">Start Time:</span>
                                    <span className="value">{parsedEvent.startTime}</span>
                                </div>
                                {parsedEvent.endTime && (
                                    <div className="detail-row">
                                        <span className="label">End Time:</span>
                                        <span className="value">{parsedEvent.endTime}</span>
                                    </div>
                                )}
                                {parsedEvent.location && (
                                    <div className="detail-row">
                                        <span className="label">Location:</span>
                                        <span className="value">{parsedEvent.location}</span>
                                    </div>
                                )}
                            </div>

                            <div className="confirmation-buttons">
                                <button
                                    onClick={handleCreateEvent}
                                    disabled={loading}
                                    className="btn-success"
                                >
                                    {loading ? (
                                        <>
                                            <Loader size={16} className="spinner" style={{marginRight: '8px'}} />
                                            Processing...
                                        </>
                                    ) : (
                                        <>
                                            <CheckCircle size={16} style={{marginRight: '8px'}} />
                                            Create Event
                                        </>
                                    )}
                                </button>
                                <button
                                    onClick={handleCancel}
                                    disabled={loading}
                                    className="btn-secondary"
                                >
                                    Cancel
                                </button>
                            </div>
                        </div>
                    )}

                    <div className="features-section">
                        <h3>Coming Soon</h3>
                        <div className="feature-cards">
                            <div className="feature-card disabled">
                                <h4>📋 View Schedule</h4>
                                <p>See all your upcoming events</p>
                            </div>
                            <div className="feature-card disabled">
                                <h4>🗑️ Delete Events</h4>
                                <p>Remove events easily</p>
                            </div>
                            <div className="feature-card disabled">
                                <h4>✏️ Update Events</h4>
                                <p>Modify existing events</p>
                            </div>
                        </div>
                    </div>
                </main>
            </div>
        );
    }
}

export default App;