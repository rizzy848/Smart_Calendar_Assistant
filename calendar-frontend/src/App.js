import React, { useState, useEffect, useRef } from 'react';
import { Calendar, User, LogOut, MessageSquare, CheckCircle, AlertCircle, Wifi, WifiOff, Clock, ExternalLink } from 'lucide-react';
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
    const [showOAuthModal, setShowOAuthModal] = useState(false);
    const [oauthUrl, setOAuthUrl] = useState('');

    const inputRef = useRef(null);
    const oauthWindowRef = useRef(null);

    // Test backend connection on mount
    useEffect(() => {
        checkBackendConnection();
    }, []);

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

    // Focus management
    useEffect(() => {
        if (isLoggedIn && !loading && !showOAuthModal && inputRef.current) {
            setTimeout(() => inputRef.current.focus(), 100);
        }
    }, [isLoggedIn, loading, showConfirmation, showOAuthModal]);

    // Auto-dismiss messages
    useEffect(() => {
        if (message && message.type === 'success' && !message.persistent) {
            const timer = setTimeout(() => setMessage(null), 5000);
            return () => clearTimeout(timer);
        }
    }, [message]);

    // Open OAuth in popup
    const openOAuthPopup = async (userId) => {
        try {
            console.log('🔐 Fetching OAuth URL for user:', userId);

            const response = await fetch(`http://localhost:8080/api/events/auth/url/${userId}`);
            const data = await response.json();

            if (data.authUrl) {
                setOAuthUrl(data.authUrl);
                setShowOAuthModal(true);

                // Open in new window
                const width = 600;
                const height = 700;
                const left = window.screen.width / 2 - width / 2;
                const top = window.screen.height / 2 - height / 2;

                oauthWindowRef.current = window.open(
                    data.authUrl,
                    'Google Calendar Authorization',
                    `width=${width},height=${height},left=${left},top=${top}`
                );

                console.log('✅ OAuth window opened');

                // Monitor window
                const checkWindow = setInterval(() => {
                    if (oauthWindowRef.current && oauthWindowRef.current.closed) {
                        clearInterval(checkWindow);
                        setShowOAuthModal(false);
                        console.log('🔒 OAuth window closed');
                    }
                }, 1000);

                return true;
            } else {
                throw new Error(data.error || 'Failed to get OAuth URL');
            }
        } catch (error) {
            console.error('❌ OAuth error:', error);
            setMessage({
                type: 'error',
                text: 'Failed to open authentication: ' + error.message,
                persistent: false
            });
            return false;
        }
    };

    // Login Screen
    const LoginScreen = () => {
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
                setLoginError('Backend server is not running');
                return;
            }

            setLoggingIn(true);
            setLoginError('');

            try {
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
                setLoginError(error.message);
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
                        <p>Natural language calendar assistant</p>
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
                                <Wifi size={20} />
                                <span>Checking...</span>
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
                                    <div style={{fontSize: '12px'}}>Run: mvn spring-boot:run</div>
                                </div>
                                <button
                                    onClick={checkBackendConnection}
                                    style={{
                                        padding: '6px 12px',
                                        background: 'white',
                                        border: '1px solid #ccc',
                                        borderRadius: '4px',
                                        cursor: 'pointer'
                                    }}
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
                            placeholder="Google Email"
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
                            {loggingIn ? 'Connecting...' : 'Continue'}
                        </button>
                    </form>
                </div>
            </div>
        );
    };

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
            const result = await parseNaturalLanguage(trimmed);

            if (result.successful) {
                setParsedEvent(result);
                setShowConfirmation(true);
                setMessage({
                    type: 'success',
                    text: '✓ I understood your request!',
                    persistent: false
                });
            } else {
                setMessage({
                    type: 'error',
                    text: result.errorMessage || 'Could not understand',
                    persistent: false
                });
            }
        } catch (error) {
            setMessage({
                type: 'error',
                text: 'Error: ' + error.message,
                persistent: false
            });
        } finally {
            setLoading(false);
        }
    };

    // Create event with OAuth check
    const handleCreateEvent = async () => {
        if (!parsedEvent || !currentUser) return;

        setLoading(true);
        setMessage({
            type: 'success',
            text: '⏳ Preparing to create event...',
            persistent: true
        });

        try {
            // Check if user needs OAuth
            const authCheck = await fetch(`http://localhost:8080/api/events/auth/check/${currentUser.userId}`);
            const authData = await authCheck.json();

            if (authData.needsAuth) {
                setMessage({
                    type: 'success',
                    text: '🔐 Opening Google authentication...',
                    persistent: true
                });

                // Open OAuth popup
                const opened = await openOAuthPopup(currentUser.userId);

                if (!opened) {
                    setLoading(false);
                    return;
                }

                // Show instructions
                setMessage({
                    type: 'success',
                    text: '👆 Please complete authentication in the popup window. Then click "Create Event" again.',
                    persistent: true
                });

                setLoading(false);
                return;
            }

            // User is authenticated, create event
            setMessage({
                type: 'success',
                text: '📅 Creating event...',
                persistent: true
            });

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
                    text: '✓ Event created successfully!',
                    persistent: false
                });
                setUserInput('');
                setParsedEvent(null);
                setShowConfirmation(false);
            } else {
                setMessage({
                    type: 'error',
                    text: result.message || 'Failed to create event',
                    persistent: false
                });
            }
        } catch (error) {
            console.error('❌ Error:', error);
            setMessage({
                type: 'error',
                text: 'Error: ' + error.message,
                persistent: false
            });
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        if (window.confirm('Logout?')) {
            setIsLoggedIn(false);
            setCurrentUser(null);
            setUserInput('');
            setParsedEvent(null);
            setMessage(null);
            setShowConfirmation(false);
        }
    };

    const handleCancel = () => {
        setShowConfirmation(false);
        setParsedEvent(null);
        setMessage(null);
    };

    // Calendar Interface
    const CalendarInterface = () => (
        <div className="app-container">
            {/* OAuth Modal */}
            {showOAuthModal && (
                <div style={{
                    position: 'fixed',
                    top: 0,
                    left: 0,
                    right: 0,
                    bottom: 0,
                    background: 'rgba(0,0,0,0.7)',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    zIndex: 1000
                }}>
                    <div style={{
                        background: 'white',
                        padding: '30px',
                        borderRadius: '15px',
                        maxWidth: '500px',
                        textAlign: 'center'
                    }}>
                        <ExternalLink size={48} color="#667eea" style={{marginBottom: '15px'}} />
                        <h2>Google Authentication</h2>
                        <p style={{margin: '15px 0', color: '#666'}}>
                            A popup window has opened for Google Calendar authentication.
                        </p>
                        <p style={{margin: '15px 0', fontWeight: 'bold'}}>
                            Please complete the authentication in the popup.
                        </p>
                        <p style={{fontSize: '14px', color: '#999'}}>
                            If popup was blocked, <a href={oauthUrl} target="_blank" rel="noopener noreferrer">click here</a>
                        </p>
                        <button
                            onClick={() => setShowOAuthModal(false)}
                            className="btn-secondary"
                            style={{marginTop: '20px'}}
                        >
                            Close
                        </button>
                    </div>
                </div>
            )}

            <header className="app-header">
                <div className="header-content">
                    <div className="header-left">
                        <Calendar size={32} className="header-icon" />
                        <h1>AI Calendar</h1>
                    </div>
                    <div className="header-right">
                        <div className="user-info">
                            <User size={20} />
                            <span>{currentUser?.username}</span>
                        </div>
                        <button onClick={handleLogout} className="btn-icon">
                            <LogOut size={20} />
                        </button>
                    </div>
                </div>
            </header>

            <main className="main-content">
                {message && (
                    <div className={`message message-${message.type}`}>
                        {message.type === 'success' ?
                            loading ? <Clock size={20} /> : <CheckCircle size={20} /> :
                            <AlertCircle size={20} />
                        }
                        <span>{message.text}</span>
                    </div>
                )}

                <div className="input-section">
                    <h2>What would you like to schedule?</h2>
                    <div className="examples">
                        <p>💡 Examples:</p>
                        <ul>
                            <li>"Meeting tomorrow at 2 PM"</li>
                            <li>"Dentist next Monday at 10:30 AM"</li>
                            <li>"Workout today at 6 PM for 1 hour"</li>
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
                                onKeyDown={(e) => {
                                    if (e.key === 'Enter' && !e.shiftKey) {
                                        e.preventDefault();
                                        handleParse();
                                    }
                                }}
                                placeholder="Type your event here..."
                                className="main-input"
                                disabled={loading}
                                autoComplete="off"
                                autoCorrect="off"
                                autoCapitalize="off"
                                spellCheck="false"
                            />
                            <button
                                type="submit"
                                disabled={loading || !userInput.trim()}
                                className="btn-primary"
                            >
                                {loading ? 'Processing...' : 'Parse'}
                            </button>
                        </div>
                    </form>
                </div>

                {showConfirmation && parsedEvent && (
                    <div className="confirmation-section">
                        <h3>
                            <CheckCircle size={20} style={{display: 'inline', marginRight: '8px'}} />
                            Event Details
                        </h3>
                        <div className="event-details">
                            <div className="detail-row">
                                <span className="label">Title:</span>
                                <span className="value">{parsedEvent.title}</span>
                            </div>
                            <div className="detail-row">
                                <span className="label">Date:</span>
                                <span className="value">{parsedEvent.date}</span>
                            </div>
                            <div className="detail-row">
                                <span className="label">Start:</span>
                                <span className="value">{parsedEvent.startTime}</span>
                            </div>
                            {parsedEvent.endTime && (
                                <div className="detail-row">
                                    <span className="label">End:</span>
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
                                {loading ? 'Processing...' : 'Create Event'}
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
                            <p>Coming Soon</p>
                        </div>
                        <div className="feature-card disabled">
                            <h4>🗑️ Delete Event</h4>
                            <p>Coming Soon</p>
                        </div>
                        <div className="feature-card disabled">
                            <h4>✏️ Update Event</h4>
                            <p>Coming Soon</p>
                        </div>
                    </div>
                </div>
            </main>
        </div>
    );

    return (
        <div className="App">
            {!isLoggedIn ? <LoginScreen /> : <CalendarInterface />}
        </div>
    );
}

export default App;