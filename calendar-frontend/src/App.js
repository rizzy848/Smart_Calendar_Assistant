import React, { useState, useEffect, useRef } from 'react';
import { Calendar, User, LogOut, MessageSquare, CheckCircle, AlertCircle } from 'lucide-react';
import { parseNaturalLanguage, createEvent, setUserId } from './services/api';
import './App.css';

function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [currentUser, setCurrentUser] = useState(null);
    const [userInput, setUserInput] = useState('');
    const [parsedEvent, setParsedEvent] = useState(null);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [showConfirmation, setShowConfirmation] = useState(false);

    // Add ref for input field
    const inputRef = useRef(null);

    // Login Screen
    const LoginScreen = () => {
        const [username, setUsername] = useState('');
        const [email, setEmail] = useState('');
        const [loggingIn, setLoggingIn] = useState(false);
        const [loginError, setLoginError] = useState('');

        const handleLogin = async () => {
            if (!email || !username) return;

            setLoggingIn(true);
            setLoginError('');

            try {
                // Register user with backend
                console.log('Registering user:', username, email);
                const response = await fetch('http://localhost:8080/api/users/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, email })
                });

                if (!response.ok) {
                    throw new Error('Failed to register user');
                }

                const userData = await response.json();
                console.log('User registered:', userData);

                // Set user ID and login
                setUserId(userData.userId);
                setCurrentUser({
                    userId: userData.userId,
                    username: userData.username || username,
                    email: userData.email || email
                });
                setIsLoggedIn(true);
                setMessage({ type: 'success', text: `Welcome, ${username}!` });

            } catch (error) {
                console.error('Login failed:', error);
                setLoginError('Failed to connect to server. Make sure backend is running.');
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

                    {loginError && (
                        <div className="message message-error" style={{marginBottom: '20px'}}>
                            <AlertCircle size={20} />
                            <span>{loginError}</span>
                        </div>
                    )}

                    <div className="login-form">
                        <input
                            type="text"
                            placeholder="Your Name"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="input-field"
                            disabled={loggingIn}
                        />
                        <input
                            type="email"
                            placeholder="Google Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && !loggingIn && handleLogin()}
                            className="input-field"
                            disabled={loggingIn}
                        />
                        <button
                            onClick={handleLogin}
                            disabled={!username || !email || loggingIn}
                            className="btn-primary"
                        >
                            {loggingIn ? 'Connecting...' : 'Continue'}
                        </button>
                    </div>
                </div>
            </div>
        );
    };

    // Parse user input
    const handleParse = async () => {
        if (!userInput.trim()) return;

        setLoading(true);
        setMessage(null);
        setParsedEvent(null);
        setShowConfirmation(false);

        try {
            const result = await parseNaturalLanguage(userInput);

            if (result.successful) {
                setParsedEvent(result);
                setShowConfirmation(true);
                setMessage({
                    type: 'success',
                    text: '✓ I understood your request!'
                });
            } else {
                setMessage({
                    type: 'error',
                    text: result.errorMessage || 'Could not understand the request'
                });
            }
        } catch (error) {
            setMessage({
                type: 'error',
                text: 'Error parsing request. Please try again.'
            });
        } finally {
            setLoading(false);
            // Keep focus on input after parsing
            if (inputRef.current) {
                inputRef.current.focus();
            }
        }
    };

    // Create event
    const handleCreateEvent = async () => {
        if (!parsedEvent) return;

        setLoading(true);
        setMessage(null);

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
                    text: '✓ Event created successfully in your Google Calendar!'
                });
                setUserInput('');
                setParsedEvent(null);
                setShowConfirmation(false);

                // Focus back to input after success
                setTimeout(() => {
                    if (inputRef.current) {
                        inputRef.current.focus();
                    }
                }, 100);
            } else {
                setMessage({
                    type: 'error',
                    text: result.message || 'Failed to create event'
                });
            }
        } catch (error) {
            setMessage({
                type: 'error',
                text: 'Error creating event. Please check your Google Calendar connection.'
            });
        } finally {
            setLoading(false);
        }
    };

    const handleLogout = () => {
        setIsLoggedIn(false);
        setCurrentUser(null);
        setUserInput('');
        setParsedEvent(null);
        setMessage(null);
    };

    const handleCancel = () => {
        setShowConfirmation(false);
        setParsedEvent(null);
        setMessage(null);
        // Focus back to input
        if (inputRef.current) {
            inputRef.current.focus();
        }
    };

    // Main Calendar Interface
    const CalendarInterface = () => (
        <div className="app-container">
            {/* Header */}
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
                        <button onClick={handleLogout} className="btn-icon">
                            <LogOut size={20} />
                        </button>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="main-content">
                {/* Message Display */}
                {message && (
                    <div className={`message message-${message.type}`}>
                        {message.type === 'success' ?
                            <CheckCircle size={20} /> :
                            <AlertCircle size={20} />
                        }
                        <span>{message.text}</span>
                    </div>
                )}

                {/* Input Section */}
                <div className="input-section">
                    <h2>What would you like to schedule?</h2>
                    <div className="examples">
                        <p>💡 Try saying:</p>
                        <ul>
                            <li>"Schedule team meeting tomorrow at 2 PM"</li>
                            <li>"Add dentist appointment next Monday at 10:30 AM"</li>
                            <li>"Create workout session today at 6 PM for 1 hour"</li>
                        </ul>
                    </div>

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
                            placeholder="Type your request here..."
                            className="main-input"
                            disabled={loading}
                            autoFocus
                        />
                        <button
                            onClick={handleParse}
                            disabled={loading || !userInput.trim()}
                            className="btn-primary"
                        >
                            {loading ? 'Processing...' : 'Parse'}
                        </button>
                    </div>
                </div>

                {/* Confirmation Section */}
                {showConfirmation && parsedEvent && (
                    <div className="confirmation-section">
                        <h3>✓ Parsed Event Details</h3>
                        <div className="event-details">
                            <div className="detail-row">
                                <span className="label">Action:</span>
                                <span className="value">{parsedEvent.actionType}</span>
                            </div>
                            <div className="detail-row">
                                <span className="label">Title:</span>
                                <span className="value">{parsedEvent.title}</span>
                            </div>
                            <div className="detail-row">
                                <span className="label">Date:</span>
                                <span className="value">{parsedEvent.date}</span>
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
                                {loading ? 'Creating...' : 'Create Event'}
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

                {/* Coming Soon Features */}
                <div className="features-section">
                    <h3>Other Features</h3>
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